package me.gamechampcrafted.normalshops.shop;

import me.gamechampcrafted.normalshops.Logger;
import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.data.*;
import me.gamechampcrafted.normalshops.serialization.ShopManagerSerializer;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ShopManager implements ConfigurationSerializable {

    public static final int VERSION = 1;

    private final Map<Location, UUID> uuidMap;
    private final Map<UUID, PlayerShopManager> managerMap = new HashMap<>();
    private final Map<UUID, DataManager> dataManagerMap = new HashMap<>();

    private final Map<Location, UUID> stockpileMap;

    private final Map<UUID, Set<Location>> warningMap;

    public ShopManager() {
        this(new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    public ShopManager(
            Map<Location, UUID> uuidMap,
            Map<Location, UUID> stockpileMap,
            Map<UUID, Set<Location>> warningMap
    ) {
        this.uuidMap = uuidMap;
        this.stockpileMap = stockpileMap;
        this.warningMap = warningMap;
    }

    public void registerShop(ItemShop shop) {
        UUID ownerUUID = shop.getOwnerUUID();
        getShopManager(ownerUUID).registerShop(shop);
        uuidMap.put(shop.getLocation(), ownerUUID);
        saveData(ownerUUID);
        save();
        shop.applySavedContainerBlockState();
        shop.refreshOutOfStockAppearance();
    }

    /** Re-applies {@link Setting#DISPLAY_OUT_OF_STOCK} visuals for every loaded shop (e.g. after reload). */
    public void refreshAllOutOfStockDisplays() {
        Set<UUID> owners = new HashSet<>(uuidMap.values());
        for (UUID ownerUUID : owners) {
            for (ItemShop shop : getShopManager(ownerUUID).getShops()) {
                shop.refreshOutOfStockAppearance();
            }
        }
    }

    public void registerPile(Pile pile) {
        UUID ownerUUID = pile.getOwnerUUID();
        getShopManager(pile.getOwnerUUID()).registerPile(pile);
        uuidMap.put(pile.getLocation(), ownerUUID);
        saveData(ownerUUID);
        save();
    }

    public void registerStockpileAt(UUID ownerUUID, Location... locations) {
        for (Location location : locations) {
            stockpileMap.put(location, ownerUUID);
        }
        save();
    }

    public void unregisterShop(ItemShop shop) {
        UUID ownerUUID = shop.getOwnerUUID();
        getShopManager(ownerUUID).removeShop(shop);
        uuidMap.remove(shop.getLocation());
        saveData(ownerUUID);
        save();
    }

    public void unregisterPile(Pile pile) {
        UUID ownerUUID = pile.getOwnerUUID();
        getShopManager(ownerUUID).removePile(pile);
        uuidMap.remove(pile.getLocation());
        saveData(ownerUUID);
        save();
    }

    public void unregisterStockpileAt(Location... locations) {
        for (Location location : locations) {
            stockpileMap.remove(location);
        }
        save();
    }

    public ItemShop transferShopOwnership(ItemShop shop, UUID newOwnerUUID) {
        if (shop == null || newOwnerUUID == null || shop.getOwnerUUID().equals(newOwnerUUID)) {
            return shop;
        }
        UUID oldOwnerUUID = shop.getOwnerUUID();
        ItemShop transferredShop = new ItemShop(
                shop.getLocation(),
                newOwnerUUID,
                shop.getPrice().clone(),
                new ArrayList<>(shop.getProducts()),
                shop.getEarnings(),
                new HashSet<>(shop.getStockpileSet()),
                new HashSet<>(shop.getTrustedPlayers()),
                new ArrayList<>(shop.getStockContents()),
                shop.getColor(),
                shop.getDisplay(),
                shop.isAdminShop(),
                shop.getCustomName(),
                shop.getBuySound(),
                shop.isNotificationsEnabled(),
                shop.isStockWarningEnabled(),
                shop.getLifetimeSales(),
                shop.getLifetimeRevenue(),
                shop.getLifetimeProductsSold(),
                shop.getLifetimeStockAdded(),
                shop.getLifetimeStockRemoved(),
                shop.getLifetimeImpressions(),
                shop.getHistoryEntries(),
                shop.getContainerMaterial(),
                shop.getContainerBlockData()
        );
        if (transferredShop.getDisplay() != null) {
            transferredShop.getDisplay().setShop(transferredShop);
        }
        shop.captureContainerAppearanceFromWorld();
        NormalShops plugin = NormalShops.getInstance();
        if (plugin != null && plugin.getShopBackupService() != null) {
            plugin.getShopBackupService().recordRemoval(shop);
        }
        unregisterShop(shop);
        registerShop(transferredShop);
        if (transferredShop.getDisplay() != null) {
            transferredShop.updateDisplay();
        }
        transferredShop.getStockpileSet().forEach(location ->
                registerStockpileAt(newOwnerUUID, ItemShop.getStockpileSides(location)));

        Set<Location> warnings = warningMap.remove(oldOwnerUUID);
        if (warnings != null && !warnings.isEmpty()) {
            warningMap.computeIfAbsent(newOwnerUUID, ignored -> new HashSet<>()).addAll(warnings);
        }
        saveData(oldOwnerUUID);
        saveData(newOwnerUUID);
        save();
        return transferredShop;
    }

    /**
     * Every shop this player owns or is trusted on (loaded shops only).
     */
    public List<ItemShop> listShopsAccessibleTo(Player player) {
        List<ItemShop> out = new ArrayList<>();
        UUID pid = player.getUniqueId();
        for (Location loc : new ArrayList<>(uuidMap.keySet())) {
            ItemShop shop = getShop(loc);
            if (shop == null || shop.isDeleted()) continue;
            if (shop.isOwner(player) || shop.isTrusted(player)) {
                out.add(shop);
            }
        }
        out.sort(Comparator
                .comparing((ItemShop s) -> s.getLocation().getWorld() != null
                        ? s.getLocation().getWorld().getName() : "")
                .thenComparingInt(s -> s.getLocation().getBlockY())
                .thenComparingInt(s -> s.getLocation().getBlockX())
                .thenComparingInt(s -> s.getLocation().getBlockZ()));
        return out;
    }

    public int getShopCount(UUID uuid) {
        return getShopManager(uuid).getShops().size();
    }

    public boolean isShopLimitReachedForAndWarn(Player player) {
        int maxShopCount = Setting.SHOP_LIMIT_PER_PLAYER.getInt();
        int shopCount = getShopCount(player.getUniqueId());
        if (shopCount >= maxShopCount) {
            Message.SHOP_LIMIT.parameterizer()
                    .put("limit", maxShopCount)
                    .send(player);
            return true;
        }
        return false;
    }

    public int getPileCount(UUID uuid) {
        return getShopManager(uuid).getPiles().size();
    }

    @Nullable
    public ItemShop getShop(Location location) {
        UUID ownerUUID = uuidMap.get(location);
        if (ownerUUID == null) return null;
        PlayerShopManager manager = getShopManager(ownerUUID);
        return manager.getShop(location);
    }

    @Nullable
    public Pile getPile(Location location) {
        UUID ownerUUID = uuidMap.get(location);
        if (ownerUUID == null) return null;
        PlayerShopManager manager = getShopManager(ownerUUID);
        return manager.getPile(location);
    }

    @Nullable
    public UUID getStockpileOwner(Location location) {
        return stockpileMap.get(location);
    }

    /**
     * Whether the player may open this registered stockpile chest/barrel or place hoppers under it:
     * shop owner, or trusted on a shop that lists this stockpile block.
     */
    public boolean playerMayAccessRegisteredStockpile(Location stockpileBlock, Player player) {
        UUID ownerUuid = stockpileMap.get(stockpileBlock);
        if (ownerUuid == null) {
            return false;
        }
        if (player.getUniqueId().equals(ownerUuid)) {
            return true;
        }
        Location key = stockpileBlock.getBlock().getLocation();
        for (ItemShop shop : getShopManager(ownerUuid).getShops()) {
            if (!shop.isTrusted(player)) {
                continue;
            }
            if (shopLinksStockpileBlock(shop, key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Re-runs out-of-stock / storefront updates for any shop that links this stockpile block
     * (e.g. after items are changed in a physical chest or by hoppers).
     */
    public void refreshShopsUsingStockpile(Location stockpileBlock) {
        if (stockpileBlock.getWorld() == null) {
            return;
        }
        Location key = stockpileBlock.getBlock().getLocation();
        UUID owner = stockpileMap.get(key);
        if (owner == null) {
            return;
        }
        for (ItemShop shop : getShopManager(owner).getShops()) {
            if (shopLinksStockpileBlock(shop, key)) {
                shop.refreshOutOfStockAppearance();
            }
        }
    }

    private static boolean shopLinksStockpileBlock(ItemShop shop, Location stockpileBlock) {
        for (Location sl : shop.getStockpileSet()) {
            if (sl.getWorld() == null || !sl.getWorld().equals(stockpileBlock.getWorld())) {
                continue;
            }
            if (sl.getBlockX() == stockpileBlock.getBlockX()
                    && sl.getBlockY() == stockpileBlock.getBlockY()
                    && sl.getBlockZ() == stockpileBlock.getBlockZ()) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private PlayerShopManager getShopManager(UUID ownerUUID) {
        PlayerShopManager shopManager = managerMap.get(ownerUUID);
        if (shopManager != null) {
            return shopManager;
        }
        // Read in
        DataManager dataManager = getDataManager(ownerUUID);
        if (dataManager != null) {
            shopManager = (PlayerShopManager) dataManager.getConfig().get("player-shop-manager");
            if (shopManager != null) {
                managerMap.put(ownerUUID, shopManager);
                return shopManager;
            }
        }
        shopManager = new PlayerShopManager();
        managerMap.put(ownerUUID, shopManager);
        return shopManager;
    }

    public void saveData(UUID ownerUUID) {
        DataManager dataManager = getDataManager(ownerUUID);
        if (dataManager != null) {
            dataManager.getConfig().set("player-shop-manager", getShopManager(ownerUUID));
            dataManager.saveConfig();
        }
    }

    public void saveAll() {
        managerMap.forEach((uuid, manager) -> {
            saveData(uuid);
        });
    }

    public void save() {
        NormalShops.getInstance().saveData();
    }

    @Nullable
    private DataManager getDataManager(UUID ownerUUID) {
        DataManager dataManager = dataManagerMap.get(ownerUUID);
        if (dataManager != null) {
            return dataManager;
        }
        try {
            dataManager = new YAMLDataManager(
                    NormalShops.getInstance(),
                    new File(NormalShops.getInstance().getResolvedDataFolder(), "shops"),
                    ownerUUID.toString(),
                    NormalShops.getInstance().getResolvedDataFolder()
            );
            dataManagerMap.put(ownerUUID, dataManager);
            return dataManager;
        } catch (IOException exception) {
            Logger.severe("Couldn't open/create shop data of player with UUID: " + ownerUUID);
            return null;
        }
    }

    public void addWarning(ItemShop shop) {
        UUID owner = shop.getOwnerUUID();
        Player player = Bukkit.getPlayer(owner);
        if (!shop.isStockWarningEnabled()) return;
        if (player != null) {
            MessageType.STOCK_WARNING.playSound(player);
            sendStockWarning(player, shop.getLocation());
            return;
        }
        warningMap.putIfAbsent(owner, new HashSet<>());
        warningMap.get(owner).add(shop.getLocation());
    }

    public void sendWarning(Player player) {
        Set<Location> locations = warningMap.get(player.getUniqueId());
        if (locations == null) return;
        if (!locations.isEmpty()) {
            MessageType.STOCK_WARNING.playSound(player);
        }
        locations.forEach(location -> sendStockWarning(player, location));
        warningMap.remove(player.getUniqueId());
    }

    private void sendStockWarning(Player player, Location location) {
        Message.STOCK_WARNING.parameterizer()
                .put("location", Utils.formatLocation(location))
                .sendSilently(player);
    }

    public Map<Location, UUID> getUUIDMap() {
        return uuidMap;
    }

    public Map<Location, UUID> getStockpileMap() {
        return stockpileMap;
    }

    public Map<UUID, Set<Location>> getWarningMap() {
        return warningMap;
    }

    public void recoverAllManagers() {
        File folder = new File(NormalShops.getInstance().getResolvedDataFolder(), "shops");
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            UUID uuid = player.getUniqueId();
            File file = new File(folder, uuid + ".yml");
            if (!file.exists()) return;
            // Load manager if file existed before
            PlayerShopManager manager = getShopManager(player.getUniqueId());
            manager.getShops().forEach(shop -> {
                registerShop(shop);
                shop.getStockpileSet().forEach(location -> {
                    registerStockpileAt(uuid, ItemShop.getStockpileSides(location));
                });
            });
            manager.getPiles().forEach(this::registerPile);
            Logger.warning("Recovered shop files for: " + uuid);
        }
    }

    public static ShopManager deserialize(Map<String, Object> map) {
        return new ShopManagerSerializer(map).deserialize();
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return ShopManagerSerializer.serialize(this);
    }
}
