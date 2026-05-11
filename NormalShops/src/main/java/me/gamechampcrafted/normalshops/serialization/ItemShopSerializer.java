package me.gamechampcrafted.normalshops.serialization;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.menu.ChangeShopButton;
import me.gamechampcrafted.normalshops.menu.MenuColor;
import me.gamechampcrafted.normalshops.shop.BuySound;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.shop.display.ShopDisplay;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static java.util.Locale.ROOT;

public class ItemShopSerializer extends Serializer<ItemShop> {

    public ItemShopSerializer(Map<String, Object> map) {
        super(map);
    }

    @Override
    protected @NotNull ItemShop deserializeMap() throws DeserializationException {
        Location location = getOrAbort("location");
        setLocation(location);
        UUID ownerUUID = UUID.fromString(getOrAbort("owner"));
        ItemStack price = getOrAbort("price");
        List<ItemStack> products = ChangeShopButton.filterListingProducts(getOrAbort("products"));

        int earnings = getOrDefault("earnings", 0);
        List<Location> stockpileList = getListOrDefault("stockpiles", new ArrayList<>());
        Set<Location> stockpiles = new HashSet<>(stockpileList);
        List<String> trustedPlayersRaw = getListOrDefault("trusted-players", new ArrayList<>());
        Set<UUID> trustedPlayers = new HashSet<>();
        for (String uuidString : trustedPlayersRaw) {
            try {
                trustedPlayers.add(UUID.fromString(uuidString));
            } catch (IllegalArgumentException ignored) {
            }
        }

        List<ItemStack> stockContents = deserializeStockContents();

        ShopDisplay display = getOrDefault("display", null);
        MenuColor color = getValueOfOrDefault("color", MenuColor.BLACK);

        boolean admin = getOrDefault("admin", false);
        String customName = getOrDefault("name", null);
        BuySound buySound = getValueOfOrDefault("sound", BuySound.DEFAULT);

        boolean notifications = getOrDefault("notifications", true);
        boolean stockWarning = getOrDefault("warning", true);
        boolean privateStatsHologram = getOrDefault("private-stats-hologram", true);
        long lifetimeSales = getLongOrDefault("lifetime-sales", 0L);
        long lifetimeRevenue = getLongOrDefault("lifetime-revenue", 0L);
        long lifetimeProductsSold = getLongOrDefault("lifetime-products-sold", 0L);
        long lifetimeStockAdded = getLongOrDefault("lifetime-stock-added", 0L);
        long lifetimeStockRemoved = getLongOrDefault("lifetime-stock-removed", 0L);
        long lifetimeImpressions = getLongOrDefault("lifetime-impressions", 0L);
        List<CoreProtectLogger.HistoryEntry> historyEntries = deserializeHistoryEntries();

        Material containerMaterial = deserializeContainerMaterial();
        @Nullable String containerBlockData = deserializeContainerBlockData();

        ItemShop shop = new ItemShop(
                location, ownerUUID, price, products, earnings, stockpiles,
                trustedPlayers,
                stockContents, color, display, admin, customName, buySound,
                notifications, stockWarning, privateStatsHologram, lifetimeSales, lifetimeRevenue,
                lifetimeProductsSold, lifetimeStockAdded, lifetimeStockRemoved, lifetimeImpressions, historyEntries,
                containerMaterial,
                containerBlockData
        );
        if (display != null) display.setShop(shop);
        return shop;
    }

    @Nullable
    private Material deserializeContainerMaterial() {
        Object raw = map.get("container-material");
        if (raw == null) {
            return null;
        }
        try {
            return Material.valueOf(String.valueOf(raw).trim().toUpperCase(ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    private String deserializeContainerBlockData() {
        Object raw = map.get("container-block-data");
        if (raw == null) {
            return null;
        }
        String s = String.valueOf(raw).trim();
        return s.isEmpty() ? null : s;
    }

    private List<ItemStack> deserializeStockContents() {
        List<ItemStack> contents = getOrDefault("stock-contents", null);
        if (contents == null) {
            // Legacy key support for older versions.
            contents = getOrDefault("inventory", new ArrayList<>());
        }
        List<ItemStack> stockContents = new ArrayList<>();
        for (ItemStack item : contents) {
            stockContents.add(item == null ? null : item.clone());
        }
        return stockContents;
    }

    private List<CoreProtectLogger.HistoryEntry> deserializeHistoryEntries() {
        List<Map<String, Object>> history = getOrDefault("history", new ArrayList<>());
        List<CoreProtectLogger.HistoryEntry> entries = new ArrayList<>();
        for (Map<String, Object> entry : history) {
            if (entry == null) continue;
            long timestamp = parseLong(entry.get("timestamp"), System.currentTimeMillis() / 1000);
            String actor = String.valueOf(entry.getOrDefault("actor", "unknown"));
            String action = String.valueOf(entry.getOrDefault("action", "INTERACTION"));
            String material = String.valueOf(entry.getOrDefault("material", "unknown"));
            String details = String.valueOf(entry.getOrDefault("details", ""));
            entries.add(new CoreProtectLogger.HistoryEntry(timestamp, actor, action, material, details));
        }
        return entries;
    }

    private long getLongOrDefault(String key, long defaultValue) {
        Object value = getOrDefault(key, null);
        return parseLong(value, defaultValue);
    }

    private long parseLong(Object value, long defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public static Map<String, Object> serialize(ItemShop shop) {
        Map<String, Object> map = new HashMap<>();
        map.put("v", ItemShop.VERSION);
        map.put("location", shop.getLocation());
        map.put("owner", shop.getOwnerUUID().toString());
        map.put("price", shop.getPrice());
        map.put("products", shop.getProducts());
        map.put("color", shop.getColor().name());
        map.put("stockpiles", new ArrayList<>(shop.getStockpileSet()));
        List<String> trustedPlayers = new ArrayList<>();
        for (UUID uuid : shop.getTrustedPlayers()) {
            trustedPlayers.add(uuid.toString());
        }
        map.put("trusted-players", trustedPlayers);
        map.put("earnings", shop.getEarnings());
        map.put("admin", shop.isAdminShop());

        List<ItemStack> contents = new ArrayList<>();
        for (ItemStack item : shop.getStockContents()) {
            contents.add(item == null ? null : item.clone());
        }
        map.put("stock-contents", contents);
        map.put("display", shop.getDisplay());
        map.put("sound", shop.getBuySound().toString());

        map.put("notifications", shop.isNotificationsEnabled());
        map.put("warning", shop.isStockWarningEnabled());
        map.put("private-stats-hologram", shop.isPrivateStatsHologramEnabled());
        map.put("lifetime-sales", shop.getLifetimeSales());
        map.put("lifetime-revenue", shop.getLifetimeRevenue());
        map.put("lifetime-products-sold", shop.getLifetimeProductsSold());
        map.put("lifetime-stock-added", shop.getLifetimeStockAdded());
        map.put("lifetime-stock-removed", shop.getLifetimeStockRemoved());
        map.put("lifetime-impressions", shop.getLifetimeImpressions());
        List<Map<String, Object>> history = new ArrayList<>();
        for (CoreProtectLogger.HistoryEntry entry : shop.getHistoryEntries()) {
            Map<String, Object> row = new HashMap<>();
            row.put("timestamp", entry.timestamp());
            row.put("actor", entry.actor());
            row.put("action", entry.action());
            row.put("material", entry.material());
            row.put("details", entry.details());
            history.add(row);
        }
        map.put("history", history);

        if (shop.hasCustomName()) {
            map.put("name", shop.getCustomName());
        }
        if (shop.getContainerMaterial() != null) {
            map.put("container-material", shop.getContainerMaterial().name());
        }
        if (shop.getContainerBlockData() != null) {
            map.put("container-block-data", shop.getContainerBlockData());
        }
        return map;
    }
}
