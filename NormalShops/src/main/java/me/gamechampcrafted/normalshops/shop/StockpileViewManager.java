package me.gamechampcrafted.normalshops.shop;

import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.menu.GuiDisplayItem;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.utils.MessageParametizer;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Read-only browse/peek UIs for linked stockpiles opened from the stock chest nav row.
 * Uses mirrored inventories (clones) for anti-dupe; one peek viewer per physical stockpile at a time.
 */
public final class StockpileViewManager implements Listener {

    private static final int INVENTORY_SIZE = 54;
    private static final int BACK_SLOT = 49;

    private final NormalShops plugin;

    private final Map<UUID, BrowseSession> browseSessions = new HashMap<>();
    private final Map<UUID, PeekSession> peekSessions = new HashMap<>();

    /** One peek viewer per stockpile block location. */
    private final Map<String, UUID> peekViewerByLocationKey = new ConcurrentHashMap<>();

    public StockpileViewManager(NormalShops plugin) {
        this.plugin = plugin;
    }

    public void openBrowse(Player player, ItemShop shop, int stockChestReturnPage) {
        if (!plugin.isEnabled()) {
            return;
        }
        browseSessions.remove(player.getUniqueId());

        ItemShop live = ItemShop.get(shop.getLocation());
        if (live == null || live.isDeleted()) {
            player.sendMessage(ChatColor.RED + "Shop no longer exists.");
            return;
        }

        List<Location> anchors = live.listUniqueStockpileAnchorBlocks();
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, browseTitle());
        ItemStack glass = pane(Material.BLACK_STAINED_GLASS_PANE);

        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            inventory.setItem(slot, glass.clone());
        }

        int idx = 0;
        for (Location anchor : anchors) {
            if (idx >= 14) {
                break;
            }
            int slot = browseSlotForIndex(idx);
            MessageParametizer row = Message.MENU_STOCKPILE_BROWSER_ENTRY.parameterizer()
                    .put("index", String.valueOf(idx + 1))
                    .put("location", Utils.formatLocation(anchor));
            ItemStack icon = GuiDisplayItem.paperIconWithMeta(
                    Material.CHEST,
                    row.toString(),
                    Message.MENU_STOCKPILE_BROWSER_ENTRY.getParameterizedLore(row));
            inventory.setItem(slot, icon);
            idx++;
        }

        inventory.setItem(BACK_SLOT, backButton());

        BrowseSession session = new BrowseSession(live, stockChestReturnPage, anchors, inventory);
        browseSessions.put(player.getUniqueId(), session);
        player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, SoundCategory.BLOCKS, 0.5f, 1.2f);
        player.openInventory(inventory);
    }

    private void openPeek(Player player, Location anchor, ItemShop shop, int stockChestReturnPage) {
        ItemShop live = ItemShop.get(shop.getLocation());
        if (live == null || live.isDeleted()) {
            player.sendMessage(ChatColor.RED + "Shop no longer exists.");
            return;
        }
        if (anchor.getWorld() == null || !anchor.getWorld().isChunkLoaded(anchor.getBlockX() >> 4, anchor.getBlockZ() >> 4)) {
            Message.STOCKPILE_NOT_LOADED.send(player);
            return;
        }

        List<String> lockKeys = tryAcquirePeekLocks(player.getUniqueId(), anchor);
        if (lockKeys == null) {
            Message.STOCKPILE_VIEW_IN_USE.send(player);
            return;
        }

        ItemStack[] snapshot = live.snapshotStockpileContents(anchor);
        if (snapshot == null) {
            releasePeekLocks(lockKeys, player.getUniqueId());
            Message.STOCKPILE_NOT_LOADED.send(player);
            return;
        }

        String title = peekTitle(compactLocationForTitle(anchor));
        Inventory inv = Bukkit.createInventory(null, snapshot.length, title);
        for (int i = 0; i < snapshot.length; i++) {
            ItemStack it = snapshot[i];
            inv.setItem(i, it == null ? null : it.clone());
        }

        PeekSession peek = new PeekSession(live, stockChestReturnPage, inv, lockKeys);
        peekSessions.put(player.getUniqueId(), peek);
        browseSessions.remove(player.getUniqueId());

        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.MASTER, 0.4f, 1.4f);
        player.openInventory(inv);
    }

    private void closeBrowseToStockChest(Player player, BrowseSession browse) {
        browseSessions.remove(player.getUniqueId());
        NormalShops.getInstance().getStockChestManager().resumeStockChest(player, browse.shop, browse.stockChestReturnPage);
    }

    private static String browseTitle() {
        String t = Message.MENU_STOCKPILE_BROWSER.toString();
        return t.length() > 32 ? t.substring(0, 32) : t;
    }

    private static String peekTitle(String locationSnippet) {
        String t = Message.MENU_STOCKPILE_VIEW_READONLY.parameterizer().put("location", locationSnippet).toString();
        return t.length() > 32 ? t.substring(0, 32) : t;
    }

    /** Short coords so chest titles stay within the client limit. */
    private static String compactLocationForTitle(Location loc) {
        if (loc.getWorld() == null) {
            return "?";
        }
        return loc.getWorld().getName() + " · " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
    }

    private static ItemStack pane(Material material) {
        return GuiDisplayItem.paperIconWithMeta(material, " ", List.of());
    }

    private static ItemStack backButton() {
        List<String> lore = Message.BUTTON_STOCKPILE_BROWSER_NAV_BACK.getLore();
        return GuiDisplayItem.paperIconWithMeta(
                Material.FLOWER_BANNER_PATTERN,
                Message.BUTTON_STOCKPILE_BROWSER_NAV_BACK.toString(),
                lore.isEmpty() ? List.of() : lore);
    }

    private static int browseSlotForIndex(int index) {
        if (index < 7) {
            return 10 + index;
        }
        return 19 + (index - 7);
    }

    private static int indexForBrowseSlot(int rawSlot) {
        if (rawSlot >= 10 && rawSlot <= 16) {
            return rawSlot - 10;
        }
        if (rawSlot >= 19 && rawSlot <= 25) {
            return 7 + (rawSlot - 19);
        }
        return -1;
    }

    private static String locationKey(Location loc) {
        Location b = loc.getBlock().getLocation();
        if (b.getWorld() == null) {
            return "";
        }
        return b.getWorld().getName() + "|" + b.getBlockX() + "|" + b.getBlockY() + "|" + b.getBlockZ();
    }

    /**
     * Locks all block sides (e.g. double chest) for this stockpile. Rolls back on conflict.
     *
     * @return unique keys acquired, or {@code null} if another viewer owns any side
     */
    private List<String> tryAcquirePeekLocks(UUID viewerId, Location anchorBlock) {
        LinkedHashSet<String> uniqueKeys = new LinkedHashSet<>();
        for (Location side : ItemShop.getStockpileSides(anchorBlock.getBlock().getLocation())) {
            String k = locationKey(side);
            if (!k.isEmpty()) {
                uniqueKeys.add(k);
            }
        }
        List<String> acquired = new ArrayList<>();
        for (String k : uniqueKeys) {
            UUID prev = peekViewerByLocationKey.putIfAbsent(k, viewerId);
            if (prev != null && !prev.equals(viewerId)) {
                for (String ak : acquired) {
                    peekViewerByLocationKey.remove(ak, viewerId);
                }
                return null;
            }
            acquired.add(k);
        }
        return acquired;
    }

    private void releasePeekLocks(List<String> keys, UUID viewerId) {
        if (keys == null) {
            return;
        }
        for (String k : keys) {
            peekViewerByLocationKey.remove(k, viewerId);
        }
    }

    /**
     * True if another player is previewing this stockpile in the mirror GUI (any linked block side).
     */
    private boolean isPhysicalOpenBlockedByPeek(Location clickedBlock, Player player) {
        for (Location side : ItemShop.getStockpileSides(clickedBlock.getBlock().getLocation())) {
            String k = locationKey(side);
            UUID viewer = peekViewerByLocationKey.get(k);
            if (viewer != null && !viewer.equals(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Blocks opening a registered stockpile chest/barrel in-world while someone else has the read-only peek open.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPhysicalStockpileOpen(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        if (block.getType() != Material.CHEST && block.getType() != Material.BARREL) {
            return;
        }
        Player player = event.getPlayer();
        if (Permission.BYPASS_STOCKPILE.has(player)) {
            return;
        }
        NormalShops plugin = NormalShops.getInstance();
        if (plugin == null) {
            return;
        }
        ShopManager sm = plugin.getShopManager();
        if (sm == null) {
            return;
        }
        Location blockLoc = block.getLocation().getBlock().getLocation();
        if (sm.getStockpileOwner(blockLoc) == null) {
            return;
        }
        if (!sm.playerMayAccessRegisteredStockpile(blockLoc, player)) {
            return;
        }
        if (isPhysicalOpenBlockedByPeek(block.getLocation(), player)) {
            Message.STOCKPILE_VIEW_IN_USE.send(player);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        UUID id = player.getUniqueId();

        PeekSession peek = peekSessions.get(id);
        if (peek != null && event.getInventory() == peek.inventory) {
            event.setCancelled(true);
            if (event.getClick() == ClickType.DOUBLE_CLICK || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                return;
            }
            return;
        }

        BrowseSession browse = browseSessions.get(id);
        if (browse == null || event.getInventory() != browse.inventory) {
            return;
        }
        event.setCancelled(true);
        if (event.getClick() == ClickType.DOUBLE_CLICK || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            return;
        }

        int raw = event.getRawSlot();
        if (raw == BACK_SLOT) {
            if (event.getView().getCursor() != null && event.getView().getCursor().getType() != Material.AIR) {
                return;
            }
            player.playSound(player, Sound.UI_LOOM_SELECT_PATTERN, SoundCategory.MASTER, 1f, 1f);
            closeBrowseToStockChest(player, browse);
            return;
        }

        int idx = indexForBrowseSlot(raw);
        if (idx < 0 || idx >= browse.anchors.size()) {
            return;
        }
        if (event.getView().getCursor() != null && event.getView().getCursor().getType() != Material.AIR) {
            return;
        }
        Location anchor = browse.anchors.get(idx).clone();
        ItemShop shop = browse.shop;
        int page = browse.stockChestReturnPage;

        browseSessions.remove(id);
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            openPeek(player, anchor, shop, page);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        PeekSession peek = peekSessions.get(player.getUniqueId());
        if (peek != null && event.getInventory() == peek.inventory) {
            event.setCancelled(true);
            return;
        }
        BrowseSession browse = browseSessions.get(player.getUniqueId());
        if (browse != null && event.getInventory() == browse.inventory) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        UUID id = player.getUniqueId();

        PeekSession peek = peekSessions.remove(id);
        if (peek != null && event.getInventory() == peek.inventory) {
            releasePeekLocks(peek.lockKeys, id);
            if (player.isOnline() && NormalShops.getInstance().isEnabled()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline() || !plugin.isEnabled()) {
                        return;
                    }
                    openBrowse(player, peek.shop, peek.stockChestReturnPage);
                });
            }
            return;
        }

        BrowseSession browse = browseSessions.get(id);
        if (browse != null && event.getInventory() == browse.inventory) {
            browseSessions.remove(id);
            NormalShops.getInstance().getStockChestManager().resumeStockChest(player, browse.shop, browse.stockChestReturnPage);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        PeekSession peek = peekSessions.remove(id);
        if (peek != null) {
            releasePeekLocks(peek.lockKeys, id);
        }
        BrowseSession browse = browseSessions.remove(id);
        if (browse != null && browse.inventory != null) {
            for (HumanEntity he : new ArrayList<>(browse.inventory.getViewers())) {
                he.closeInventory();
            }
        }
    }

    public void closeAllSessions() {
        for (UUID id : new ArrayList<>(browseSessions.keySet())) {
            Player pl = Bukkit.getPlayer(id);
            if (pl != null) {
                pl.closeInventory();
            }
        }
        for (UUID id : new ArrayList<>(peekSessions.keySet())) {
            Player pl = Bukkit.getPlayer(id);
            if (pl != null) {
                pl.closeInventory();
            }
        }
        peekSessions.clear();
        browseSessions.clear();
        peekViewerByLocationKey.clear();
    }

    private static final class BrowseSession {
        private final ItemShop shop;
        private final int stockChestReturnPage;
        private final List<Location> anchors;
        private final Inventory inventory;

        private BrowseSession(ItemShop shop, int stockChestReturnPage, List<Location> anchors, Inventory inventory) {
            this.shop = shop;
            this.stockChestReturnPage = stockChestReturnPage;
            this.anchors = new ArrayList<>(anchors);
            this.inventory = inventory;
        }
    }

    private static final class PeekSession {
        private final ItemShop shop;
        private final int stockChestReturnPage;
        private final Inventory inventory;
        private final List<String> lockKeys;

        private PeekSession(ItemShop shop, int stockChestReturnPage, Inventory inventory, List<String> lockKeys) {
            this.shop = shop;
            this.stockChestReturnPage = stockChestReturnPage;
            this.inventory = inventory;
            this.lockKeys = lockKeys;
        }
    }
}
