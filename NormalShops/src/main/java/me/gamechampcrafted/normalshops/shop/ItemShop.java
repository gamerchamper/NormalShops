package me.gamechampcrafted.normalshops.shop;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.Setting;
import me.gamechampcrafted.normalshops.menu.MenuColor;
import me.gamechampcrafted.normalshops.serialization.ItemShopSerializer;
import me.gamechampcrafted.normalshops.shop.display.HintOnlyDisplay;
import me.gamechampcrafted.normalshops.shop.display.ShopDisplay;
import me.gamechampcrafted.normalshops.shop.display.ShopDisplayType;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Nameable;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

public class ItemShop implements ConfigurationSerializable {

    public static final int VERSION = 1;

    @Nullable
    public static ItemShop get(Location location) {
        return NormalShops.getInstance().getShopManager().getShop(location);
    }

    private final Location location;
    private final UUID ownerUUID;
    private ItemStack price;
    private List<ItemStack> products;
    private int earnings;
    private final Set<Location> stockpiles;
    private final Set<UUID> trustedPlayers;
    private final List<ItemStack> stockContents;
    private MenuColor color;
    private ShopDisplay display;
    private boolean admin;
    private String customName;

    private BuySound buySound;

    private final static int MAX_NAME_LENGTH = 26;

    private boolean deleted = false;

    private boolean notifications;
    private boolean stockWarning;
    private long lifetimeSales;
    private long lifetimeRevenue;
    private long lifetimeProductsSold;
    private long lifetimeStockAdded;
    private long lifetimeStockRemoved;
    private long lifetimeImpressions;
    private transient long stockRevision = 0L;
    private transient long earningsRevision = 0L;
    private final List<CoreProtectLogger.HistoryEntry> historyEntries;

    /**
     * Block type to restore when {@link Setting#DISPLAY_OUT_OF_STOCK} replaces the shop with bedrock.
     */
    @Nullable
    private Material containerMaterial;

    /**
     * Serialized {@link org.bukkit.block.data.BlockData} (facing, etc.) for {@link #containerMaterial}.
     */
    @Nullable
    private String containerBlockData;

    public ItemShop(
            Location location,
            Player owner,
            ItemStack price,
            List<ItemStack> products
    ) {
        this(
                location.getBlock().getLocation(),
                owner.getUniqueId(),
                price,
                products,
                0,
                new HashSet<>(),
                new HashSet<>(),
                new ArrayList<>(),
                MenuColor.BLACK,
                null,
                false,
                null,
                BuySound.DEFAULT,
                true,
                true,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                new ArrayList<>(),
                safeContainerMaterial(location.getBlock().getType()),
                null
        );
    }

    public ItemShop(
            Location location,
            UUID ownerUUID,
            ItemStack price,
            List<ItemStack> products,
            int earnings,
            Set<Location> stockpiles,
            Set<UUID> trustedPlayers,
            List<ItemStack> stockContents,
            MenuColor color,
            ShopDisplay display,
            boolean admin,
            String customName,
            BuySound buySound,
            boolean notifications,
            boolean stockWarning,
            long lifetimeSales,
            long lifetimeRevenue,
            long lifetimeProductsSold,
            long lifetimeStockAdded,
            long lifetimeStockRemoved,
            long lifetimeImpressions,
            List<CoreProtectLogger.HistoryEntry> historyEntries,
            @Nullable Material containerMaterial,
            @Nullable String containerBlockData
    ) {
        this.location = location;
        this.ownerUUID = ownerUUID;
        this.price = price;
        this.products = products;
        this.earnings = earnings;
        this.admin = admin;
        this.stockpiles = stockpiles;
        this.trustedPlayers = trustedPlayers;
        this.stockContents = stockContents;
        this.color = color;
        this.display = display;
        this.customName = customName;
        this.buySound = buySound;
        this.notifications = notifications;
        this.stockWarning = stockWarning;
        this.lifetimeSales = lifetimeSales;
        this.lifetimeRevenue = lifetimeRevenue;
        this.lifetimeProductsSold = lifetimeProductsSold;
        this.lifetimeStockAdded = lifetimeStockAdded;
        this.lifetimeStockRemoved = lifetimeStockRemoved;
        this.lifetimeImpressions = lifetimeImpressions;
        this.historyEntries = historyEntries;
        this.containerMaterial = containerMaterial;
        this.containerBlockData = containerBlockData;
    }

    private static Material safeContainerMaterial(Material type) {
        if (type == null || type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR) {
            return Material.CHEST;
        }
        if (type == Material.BEDROCK) {
            return Material.CHEST;
        }
        return type;
    }

    /**
     * Block item to return when the owner replaces the shop container block (customize menu).
     * When the storefront is the bedrock out-of-stock placeholder, refunds the stored real
     * container type — never bedrock.
     */
    public Material getRefundMaterialForShopBlockReplace(Material previousWorldType) {
        if (previousWorldType != Material.BEDROCK) {
            return safeContainerMaterial(previousWorldType);
        }
        Material real = containerMaterial;
        if (real == null) {
            real = Material.CHEST;
        }
        return safeContainerMaterial(real);
    }

    public static Inventory createStockInventory() {
        return Bukkit.createInventory(null, 54, Message.MENU_STOCK_CHEST.toString());
    }

    public void saveData() {
        NormalShops.getInstance().getShopManager().saveData(ownerUUID);
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void delete(Player deleter) {
        captureContainerAppearanceFromWorld();
        NormalShops plugin = NormalShops.getInstance();
        if (plugin != null && plugin.getShopBackupService() != null) {
            plugin.getShopBackupService().recordRemoval(this);
        }
        // Collect earnings
        if (isOwner(deleter) || Setting.OPERATOR_DELETE_SHOP_COLLECT.isEnabled()) {
            collectEarnings(deleter, true);
        }
        restoreShopBlockFromBedrockPlaceholder();
        clearDisplay();
        //Drop stock chest contents
        for (ItemStack item : stockContents) {
            if (item != null && location.getWorld() != null) {
                location.getWorld().dropItem(location, item);
            }
        }
        ShopManager shopManager = NormalShops.getInstance().getShopManager();
        stockpiles.forEach(location -> shopManager.unregisterStockpileAt(getStockpileSides(location)));
        shopManager.unregisterShop(this);
        deleted = true;
    }

    public void incrementEarnings() {
        if (admin) return;
        earnings++;
        markEarningsChanged();
        lifetimeSales++;
        lifetimeRevenue += price.getAmount();
    }

    /**
     * Collects earnings from this shop to the player given.
     * Handles messages.
     *
     * @param message sends confirmation message
     * @return collected earnings
     */
    public int collectEarnings(Player player, boolean message) {
        if (earnings <= 0) return 0;
        int collected = earnings;
        ItemStack item = price.clone();
        item.setAmount(price.getAmount() * earnings);
        Utils.addItem(player, item);
        earnings = 0;
        markEarningsChanged();
        saveData();
        if (!message) return collected;
        Message.COLLECT_EARNINGS.parameterizer()
                .put("earnings", collected * price.getAmount() + " " + Utils.getFormattedName(price))
                .send(player);
        return collected;
    }

    public int getEarningsBundleCount() {
        return earnings;
    }

    public long getEarningsRevision() {
        return earningsRevision;
    }

    public int getEarningsPageCount(int pageSize) {
        if (pageSize <= 0) return 1;
        int capacity = pageSize * getBundlesPerStack();
        int pages = (int) Math.ceil((double) earnings / Math.max(1, capacity));
        return Math.max(1, pages);
    }

    public List<ItemStack> getEarningsPageContents(int page, int pageSize) {
        List<ItemStack> pageContents = new ArrayList<>(Collections.nCopies(pageSize, null));
        if (page < 0 || pageSize <= 0 || earnings <= 0) return pageContents;
        int bundlesPerStack = getBundlesPerStack();
        int start = page * (pageSize * bundlesPerStack);
        if (start >= earnings) return pageContents;
        int remaining = earnings - start;
        for (int i = 0; i < pageSize && remaining > 0; i++) {
            int bundlesInStack = Math.min(bundlesPerStack, remaining);
            ItemStack stack = price.clone();
            stack.setAmount(bundlesInStack * Math.max(1, price.getAmount()));
            pageContents.set(i, stack);
            remaining -= bundlesInStack;
        }
        return pageContents;
    }

    public int getEarningsBundlesOnPage(int page, int pageSize) {
        if (page < 0 || pageSize <= 0 || earnings <= 0) return 0;
        int capacity = pageSize * getBundlesPerStack();
        int start = page * capacity;
        if (start >= earnings) return 0;
        return Math.min(capacity, earnings - start);
    }

    public void removeEarningsBundles(int bundles) {
        if (bundles <= 0) return;
        earnings = Math.max(0, earnings - bundles);
        markEarningsChanged();
        saveData();
    }

    public int getBundlesFromEarningsItem(ItemStack item) {
        if (item == null || price == null) return 0;
        if (!item.isSimilar(price)) return 0;
        int unit = Math.max(1, price.getAmount());
        return item.getAmount() / unit;
    }

    private int getBundlesPerStack() {
        int unit = Math.max(1, price.getAmount());
        int maxStack = Math.max(unit, price.getMaxStackSize());
        return Math.max(1, maxStack / unit);
    }

    /**
     * True when the shop can complete one sale. Uses the same aggregate count as
     * {@link #getMinimumStockFulfillmentRatio()} (internal stock + all linked stockpiles), not
     * {@link #getNextStockedInventory()} (which requires a single chest to hold the full bundle).
     */
    public boolean hasStock() {
        if (isAdminShop()) {
            return true;
        }
        if (products == null || products.isEmpty()) {
            return false;
        }
        return getMinimumStockFulfillmentRatio() >= 1.0;
    }

    /**
     * When true, the physical shop is in “out of stock” mode (bedrock placeholder when enabled in config)
     * and full display entities must not be built — only the stock-status hologram and related state apply.
     */
    public boolean isBedrockOutOfStockStorefront() {
        return Setting.DISPLAY_OUT_OF_STOCK.isEnabled() && !isAdminShop() && !hasStock();
    }

    private static boolean containsItems(Inventory inventory, List<ItemStack> items) {
        Map<ItemStack, Integer> amountMap = new HashMap<>();
        items.forEach(item -> amountMap.put(item, amountMap.getOrDefault(item, 0) + item.getAmount()));
        for (Map.Entry<ItemStack, Integer> entry : amountMap.entrySet()) {
            ItemStack item = entry.getKey();
            int amount = entry.getValue();
            if (!containsAtLeast(inventory, item, amount)) {
                return false;
            }
        }
        return true;
    }

    static boolean containsAtLeast(Inventory inventory, ItemStack item, int amount) {
        if (Setting.FULL_NBT_CHECK.isEnabled()) {
            return inventory.containsAtLeast(item, amount);
        }
        return countMatchingDisplayName(inventory, item) >= amount;
    }

    /**
     * Removes items from the inventory using the same matching criteria as {@link #containsAtLeast}.
     * When FULL_NBT_CHECK is disabled, matches by display name only.
     *
     * @return true if all items were removed successfully
     */
    static boolean removeMatchingItems(Inventory inventory, ItemStack item, int amount) {
        if (Setting.FULL_NBT_CHECK.isEnabled()) {
            return inventory.removeItem(item).isEmpty();
        }
        String displayName = getDisplayName(item);
        int remaining = amount;
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null || !displayName.equals(getDisplayName(stack))) continue;
            int toRemove = Math.min(stack.getAmount(), remaining);
            stack.setAmount(stack.getAmount() - toRemove);
            if (stack.getAmount() <= 0) {
                inventory.setItem(i, null);
            }
            remaining -= toRemove;
            if (remaining <= 0) break;
        }
        return remaining <= 0;
    }

    public static boolean removeStockFromInventory(Inventory inventory, ItemStack item, int amount) {
        return removeMatchingItems(inventory, item, amount);
    }

    private static int countMatchingDisplayName(Inventory inventory, ItemStack item) {
        String displayName = getDisplayName(item);
        int count = 0;
        for (ItemStack stack : inventory.getContents()) {
            if (stack != null && displayName.equals(getDisplayName(stack))) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    private static String getDisplayName(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return item == null ? "" : item.getType().name();
        }
        var meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() ? meta.getDisplayName() : item.getType().name();
    }

    @Nullable
    public Inventory getNextStockedInventory() {
        for (Location location : stockpiles) {
            Inventory inventory = getInventoryFromLocation(location);
            if (inventory != null && containsItems(inventory, products)) {
                return inventory;
            }
        }
        return null;
    }

    public void addStockpile(Location location) {
        Inventory inventory = getInventoryFromLocation(location);
        if (inventory == null) {
            return;
        }
        stockpiles.add(location);
        setChestName(location, Message.MENU_STOCKPILE.parameterizer()
                .put("location", Utils.formatLocation(this.location, false))
                .toString());
        NormalShops.getInstance().getShopManager().registerStockpileAt(ownerUUID, getStockpileSides(location));
    }

    private void setChestName(Location location, @Nullable String customName) {
        Block block = location.getBlock();
        BlockState state = block.getState();
        ((Nameable) state).setCustomName(customName);
        state.update();
    }

    public void removeStockpile(Location location) {
        stockpiles.remove(location);
        setChestName(location, null);
        NormalShops.getInstance().getShopManager().unregisterStockpileAt(getStockpileSides(location));
    }

    public static Location[] getStockpileSides(Location location) {
        BlockState state = location.getBlock().getState();
        if (!(state instanceof InventoryHolder)) {
            return new Location[0];
        }
        InventoryHolder holder = ((InventoryHolder) state).getInventory().getHolder();
        if (holder instanceof DoubleChest) {
            DoubleChest doubleChest = (DoubleChest) holder;

            Chest left = (Chest) doubleChest.getLeftSide();
            Chest right = (Chest) doubleChest.getRightSide();
            if (left != null && right != null) {
                return new Location[]{left.getLocation(), right.getLocation()};
            }
        }
        return new Location[]{location};
    }

    @Nullable
    private static Inventory getInventoryFromLocation(Location location) {
        Block block = location.getBlock();
        if (block.getType() != Material.CHEST && block.getType() != Material.BARREL) return null;
        return ((InventoryHolder) block.getState()).getInventory();
    }

    public void setPrice(ItemStack price) {
        this.price = price;
    }

    public void setProducts(List<ItemStack> products) {
        this.products = products;
        // Restore the real shop block before rebuilding displays (avoid spawning displays while still bedrock).
        refreshOutOfStockAppearance();
    }

    public void setColor(MenuColor color) {
        this.color = color;
    }

    /**
     * Clears old display
     *
     * @param display new display, set null to clear.
     */
    public void setDisplay(@Nullable ShopDisplay display) {
        if (this.display != null) this.display.clear();
        this.display = display;
        updateDisplay();
    }

    /**
     * Updates the current display if there is one.
     */
    public void updateDisplay() {
        if (display != null) display.update();
    }

    /**
     * Clears the current display.
     */
    public void clearDisplay() {
        setDisplay(null);
    }

    public void setAdminShop(boolean admin) {
        this.admin = admin;
    }

    /**
     * @return new copy of shop location
     */
    public Location getLocation() {
        return location.clone();
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public boolean isOwner(Player player) {
        return player.getUniqueId().equals(ownerUUID);
    }

    public String getOwnerName() {
        return Bukkit.getOfflinePlayer(ownerUUID).getName();
    }

    public boolean isAdminShop() {
        return admin;
    }

    public ItemStack getPrice() {
        return price;
    }

    public List<ItemStack> getProducts() {
        return products;
    }

    public int getEarnings() {
        return earnings;
    }

    public MenuColor getColor() {
        return color;
    }

    @Nullable
    public ShopDisplay getDisplay() {
        return display;
    }

    @NotNull
    public ShopDisplayType getDisplayType() {
        return display == null ? ShopDisplayType.NONE : display.getType();
    }

    public Set<Location> getStockpileSet() {
        return stockpiles;
    }

    public Set<UUID> getTrustedPlayers() {
        return new HashSet<>(trustedPlayers);
    }

    public boolean isTrusted(Player player) {
        return player != null && trustedPlayers.contains(player.getUniqueId());
    }

    public boolean addTrustedPlayer(UUID playerUUID) {
        if (playerUUID == null) return false;
        if (playerUUID.equals(ownerUUID)) return false;
        boolean added = trustedPlayers.add(playerUUID);
        if (added) {
            saveData();
        }
        return added;
    }

    public boolean removeTrustedPlayer(UUID playerUUID) {
        if (playerUUID == null) return false;
        boolean removed = trustedPlayers.remove(playerUUID);
        if (removed) {
            saveData();
        }
        return removed;
    }

    public Inventory getStockInventory() {
        Inventory inventory = createStockInventory();
        for (int i = 0; i < stockContents.size() && i < inventory.getSize(); i++) {
            ItemStack item = stockContents.get(i);
            if (item != null) {
                inventory.setItem(i, item.clone());
            }
        }
        return inventory;
    }

    public List<ItemStack> getStockContents() {
        return stockContents;
    }

    public int getStockPageCount(int pageSize) {
        if (pageSize <= 0) return 1;
        int pages = (int) Math.ceil((double) stockContents.size() / pageSize);
        return Math.max(1, pages);
    }

    public List<ItemStack> getStockPageContents(int page, int pageSize) {
        List<ItemStack> pageContents = new ArrayList<>(Collections.nCopies(pageSize, null));
        if (pageSize <= 0 || page < 0) return pageContents;
        int offset = page * pageSize;
        for (int i = 0; i < pageSize; i++) {
            int sourceIndex = offset + i;
            if (sourceIndex >= stockContents.size()) break;
            ItemStack item = stockContents.get(sourceIndex);
            pageContents.set(i, item == null ? null : item.clone());
        }
        return pageContents;
    }

    public void setStockPageContents(int page, int pageSize, List<ItemStack> pageContents) {
        if (page < 0 || pageSize <= 0 || pageContents == null) return;
        int offset = page * pageSize;
        int targetSize = offset + pageSize;
        long beforeCount = 0;
        while (stockContents.size() < targetSize) {
            stockContents.add(null);
        }
        for (int i = 0; i < pageSize; i++) {
            ItemStack oldItem = stockContents.get(offset + i);
            if (oldItem != null) {
                beforeCount += oldItem.getAmount();
            }
            ItemStack item = i < pageContents.size() ? pageContents.get(i) : null;
            stockContents.set(offset + i, item == null ? null : item.clone());
        }
        long afterCount = 0;
        for (int i = 0; i < pageSize; i++) {
            ItemStack updatedItem = stockContents.get(offset + i);
            if (updatedItem != null) {
                afterCount += updatedItem.getAmount();
            }
        }
        if (afterCount > beforeCount) {
            lifetimeStockAdded += (afterCount - beforeCount);
        } else if (beforeCount > afterCount) {
            lifetimeStockRemoved += (beforeCount - afterCount);
        }
        trimStockContents();
        markStockChanged();
        saveData();
    }

    private void trimStockContents() {
        for (int i = stockContents.size() - 1; i >= 0; i--) {
            if (stockContents.get(i) != null) {
                break;
            }
            stockContents.remove(i);
        }
    }

    public int countInternalStock(ItemStack item) {
        return countMatchingDisplayName(stockContents, item);
    }

    public boolean removeInternalStock(ItemStack item, int amount) {
        boolean removed = removeMatchingItems(stockContents, item, amount);
        if (removed) {
            lifetimeStockRemoved += amount;
            markStockChanged();
        }
        return removed;
    }

    /**
     * Items matching {@code template} across internal stock list and all connected stockpiles.
     */
    public int countTotalStockMatching(ItemStack template) {
        int n = countMatchingDisplayName(stockContents, template);
        for (Location loc : stockpiles) {
            Inventory inv = getInventoryFromLocation(loc);
            if (inv != null) {
                n += countMatchingDisplayName(inv, template);
            }
        }
        return n;
    }

    /**
     * Per product line: available units / units required for one sale. Returns the minimum ratio (bottleneck).
     * Merges listing lines by {@link #stockLineKey(ItemStack)} so ratio matches how {@link #countMatchingDisplayName} counts stock
     * (HashMap&lt;ItemStack&gt; keys often never merge because stacks are different instances).
     */
    public double getMinimumStockFulfillmentRatio() {
        if (products == null || products.isEmpty()) {
            return 1.0;
        }
        Map<String, Integer> requiredByLine = new HashMap<>();
        Map<String, ItemStack> templateByLine = new HashMap<>();
        for (ItemStack line : products) {
            if (line == null) {
                continue;
            }
            String key = stockLineKey(line);
            requiredByLine.merge(key, line.getAmount(), Integer::sum);
            templateByLine.putIfAbsent(key, line.clone());
        }
        double min = Double.MAX_VALUE;
        for (Map.Entry<String, Integer> entry : requiredByLine.entrySet()) {
            ItemStack template = templateByLine.get(entry.getKey());
            if (template == null) {
                continue;
            }
            int required = Math.max(1, entry.getValue());
            int available = countTotalStockMatching(template);
            min = Math.min(min, (double) available / (double) required);
        }
        return min == Double.MAX_VALUE ? 1.0 : min;
    }

    /**
     * Groups product requirements the same way {@link #countMatchingDisplayName} matches stacks (type + display name).
     */
    private static String stockLineKey(ItemStack item) {
        return item.getType().name() + "\0" + getDisplayName(item);
    }

    public long getStockRevision() {
        return stockRevision;
    }

    private void markStockChanged() {
        stockRevision++;
        if (NormalShops.getInstance() != null && NormalShops.getInstance().getStockChestManager() != null) {
            NormalShops.getInstance().getStockChestManager().onShopStockMutated(this);
        }
        refreshOutOfStockAppearance();
        scheduleLowStockHintRefresh();
    }

    private void scheduleLowStockHintRefresh() {
        if (NormalShops.getInstance() == null) {
            return;
        }
        Runnable run = () -> {
            if (display != null) {
                display.refreshLowStockHint();
            }
        };
        if (Bukkit.isPrimaryThread()) {
            run.run();
        } else {
            Bukkit.getScheduler().runTask(NormalShops.getInstance(), run);
        }
    }

    @Nullable
    public Material getContainerMaterial() {
        return containerMaterial;
    }

    @Nullable
    public String getContainerBlockData() {
        return containerBlockData;
    }

    /**
     * Saves material + orientation from the block at the shop location (skipped for air/bedrock).
     */
    public void captureContainerAppearanceFromWorld() {
        if (location.getWorld() == null) {
            return;
        }
        Block block = location.getBlock();
        Material t = block.getType();
        if (t == Material.AIR || t == Material.CAVE_AIR || t == Material.VOID_AIR || t == Material.BEDROCK) {
            return;
        }
        containerMaterial = t;
        containerBlockData = block.getBlockData().getAsString();
    }

    /**
     * Applies stored material and {@link org.bukkit.block.data.BlockData} to the world block (e.g. after restore).
     */
    public void applySavedContainerBlockState() {
        if (location.getWorld() == null) {
            return;
        }
        restoreBlockFromSavedAppearance(location.getBlock());
    }

    /**
     * Updates shop block + holograms when {@link Setting#DISPLAY_OUT_OF_STOCK} applies.
     * Safe to call from the main thread only (schedules async callers).
     */
    public void refreshOutOfStockAppearance() {
        if (NormalShops.getInstance() == null) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            refreshOutOfStockAppearanceSync();
        } else {
            Bukkit.getScheduler().runTask(NormalShops.getInstance(), this::refreshOutOfStockAppearanceSync);
        }
    }

    private void refreshOutOfStockAppearanceSync() {
        if (deleted) {
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        if (!Setting.DISPLAY_OUT_OF_STOCK.isEnabled()) {
            restoreShopBlockFromBedrockPlaceholder();
            if (display != null) {
                display.applyOutOfStockVisual(false);
                display.update();
            }
            return;
        }
        if (isAdminShop()) {
            restoreShopBlockFromBedrockPlaceholder();
            if (display != null) {
                display.applyOutOfStockVisual(false);
                display.update();
            }
            return;
        }
        boolean out = !hasStock();
        Block block = location.getBlock();
        if (out) {
            if (block.getType() != Material.BEDROCK) {
                containerMaterial = block.getType();
                containerBlockData = block.getBlockData().getAsString();
                saveData();
            } else if (containerMaterial == null) {
                ensureContainerMaterialInitialized();
            }
            block.setType(Material.BEDROCK);
            if (display == null && !isAdminShop()) {
                setDisplay(new HintOnlyDisplay(this));
            }
            if (display != null) {
                display.applyOutOfStockVisual(true);
                display.refreshLowStockHint();
            }
        } else {
            restoreShopBlockFromBedrockPlaceholder();
            if (display instanceof HintOnlyDisplay) {
                display.clear();
                this.display = null;
                saveData();
            } else if (display != null) {
                display.applyOutOfStockVisual(false);
                display.update();
            }
        }
    }

    private void ensureContainerMaterialInitialized() {
        if (containerMaterial != null) {
            return;
        }
        Block block = location.getBlock();
        Material t = block.getType();
        if (t == Material.BEDROCK || t == Material.AIR || t == Material.CAVE_AIR || t == Material.VOID_AIR) {
            containerMaterial = Material.CHEST;
        } else {
            containerMaterial = t;
            containerBlockData = block.getBlockData().getAsString();
        }
        saveData();
    }

    private void restoreBlockFromSavedAppearance(Block block) {
        if (containerBlockData != null && !containerBlockData.isEmpty()) {
            try {
                block.setBlockData(Bukkit.createBlockData(containerBlockData), false);
                return;
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (containerMaterial != null) {
            block.setType(containerMaterial);
        }
    }

    private void restoreShopBlockFromBedrockPlaceholder() {
        if (location.getWorld() == null) {
            return;
        }
        if (containerMaterial == null) {
            ensureContainerMaterialInitialized();
        }
        if (containerMaterial == null && (containerBlockData == null || containerBlockData.isEmpty())) {
            return;
        }
        Block block = location.getBlock();
        if (block.getType() != Material.BEDROCK) {
            return;
        }
        restoreBlockFromSavedAppearance(block);
    }

    private void markEarningsChanged() {
        earningsRevision++;
        if (NormalShops.getInstance() != null && NormalShops.getInstance().getEarningsChestManager() != null) {
            NormalShops.getInstance().getEarningsChestManager().onShopEarningsMutated(this);
        }
    }

    public void recordProductsSold(int amount) {
        if (amount > 0) {
            lifetimeProductsSold += amount;
        }
    }

    public void incrementImpressions() {
        lifetimeImpressions++;
    }

    static boolean containsItems(List<ItemStack> contents, List<ItemStack> items) {
        Map<ItemStack, Integer> amountMap = new HashMap<>();
        items.forEach(item -> amountMap.put(item, amountMap.getOrDefault(item, 0) + item.getAmount()));
        for (Map.Entry<ItemStack, Integer> entry : amountMap.entrySet()) {
            ItemStack item = entry.getKey();
            int amount = entry.getValue();
            if (countMatchingDisplayName(contents, item) < amount) {
                return false;
            }
        }
        return true;
    }

    static boolean removeMatchingItems(List<ItemStack> contents, ItemStack item, int amount) {
        String displayName = getDisplayName(item);
        int remaining = amount;
        for (int i = 0; i < contents.size(); i++) {
            ItemStack stack = contents.get(i);
            if (stack == null || !displayName.equals(getDisplayName(stack))) continue;
            int toRemove = Math.min(stack.getAmount(), remaining);
            stack.setAmount(stack.getAmount() - toRemove);
            if (stack.getAmount() <= 0) {
                contents.set(i, null);
            } else {
                contents.set(i, stack);
            }
            remaining -= toRemove;
            if (remaining <= 0) break;
        }
        return remaining <= 0;
    }

    private static int countMatchingDisplayName(List<ItemStack> contents, ItemStack item) {
        String displayName = getDisplayName(item);
        int count = 0;
        for (ItemStack stack : contents) {
            if (stack != null && displayName.equals(getDisplayName(stack))) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    public boolean hasCustomName() {
        return customName != null;
    }

    public void setCustomName(@Nullable String customName) {
        this.customName = customName;
    }

    @Nullable
    public String getCustomName() {
        return customName;
    }

    public static boolean isValidCustomName(String customName) {
        return customName.length() <= MAX_NAME_LENGTH;
    }

    public BuySound getBuySound() {
        return buySound;
    }

    public void setBuySound(BuySound buySound) {
        this.buySound = buySound;
    }

    public boolean isNotificationsEnabled() {
        return notifications;
    }

    public void setNotificationsEnabled(boolean notifications) {
        this.notifications = notifications;
    }

    public boolean isStockWarningEnabled() {
        return stockWarning;
    }

    public void setStockWarningEnabled(boolean stockWarning) {
        this.stockWarning = stockWarning;
    }

    public long getLifetimeSales() {
        return lifetimeSales;
    }

    public long getLifetimeRevenue() {
        return lifetimeRevenue;
    }

    public long getLifetimeProductsSold() {
        return lifetimeProductsSold;
    }

    public long getLifetimeStockAdded() {
        return lifetimeStockAdded;
    }

    public long getLifetimeStockRemoved() {
        return lifetimeStockRemoved;
    }

    public long getNetStockChange() {
        return lifetimeStockAdded - lifetimeStockRemoved;
    }

    public long getCurrentInternalStockAmount() {
        long count = 0;
        for (ItemStack item : stockContents) {
            if (item != null) {
                count += item.getAmount();
            }
        }
        return count;
    }

    public long getLifetimeImpressions() {
        return lifetimeImpressions;
    }

    public List<CoreProtectLogger.HistoryEntry> getHistoryEntries() {
        return new ArrayList<>(historyEntries);
    }

    public void addHistoryEntry(CoreProtectLogger.HistoryEntry entry) {
        historyEntries.add(entry);
        int maxHistory = 200;
        if (historyEntries.size() > maxHistory) {
            historyEntries.remove(0);
        }
        saveData();
    }

    public static ItemShop deserialize(Map<String, Object> map) {
        return new ItemShopSerializer(map).deserialize();
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return ItemShopSerializer.serialize(this);
    }
}
