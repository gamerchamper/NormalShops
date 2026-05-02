package me.gamechampcrafted.normalshops.shop;

import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.menu.edit.EditShopMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StockChestManager implements Listener {

    private static final int PAGE_SIZE = 45;
    private static final int BOTTOM_ROW_FIRST = 45;
    private static final int BOTTOM_ROW_LAST = 53;
    private static final int PREV_SLOT = 45;
    private static final int INFO_SLOT = 48;
    private static final int BACK_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private final Map<UUID, Session> sessions = new HashMap<>();

    public void open(Player player, ItemShop shop) {
        if (hasActiveStockViewersForShopByOther(shop, player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "This shop stock is already being viewed by another player.");
            return;
        }
        if (NormalShops.getInstance().getTradingMenuManager().hasActiveSessionForShopByOther(shop, player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Shop is currently in use by another buyer.");
            return;
        }
        if (NormalShops.getInstance().getEarningsChestManager().hasActiveViewersForShopByOther(shop, player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "This shop's earnings are being viewed by another player.");
            return;
        }
        Session session = new Session(player.getUniqueId(), shop, 0);
        sessions.put(player.getUniqueId(), session);
        enforceExclusiveViewer(shop, player.getUniqueId());
        openPage(player, session, 0, false);
    }

    private void openPage(Player player, Session session, int targetPage, boolean persistCurrent) {
        if (persistCurrent && session.inventory != null) {
            persistPage(session);
        }
        session.page = Math.max(0, targetPage);
        session.viewedRevision = session.shop.getStockRevision();
        Inventory inventory = Bukkit.createInventory(null, 54, getPageTitle(session.shop, session.page));
        List<ItemStack> pageItems = session.shop.getStockPageContents(session.page, PAGE_SIZE);
        for (int i = 0; i < PAGE_SIZE; i++) {
            inventory.setItem(i, pageItems.get(i));
        }
        inventory.setItem(PREV_SLOT, createNavItem(Material.ARROW, ChatColor.YELLOW + "Previous Page"));
        inventory.setItem(INFO_SLOT, createNavItem(Material.CHEST, ChatColor.GOLD + "Page " + (session.page + 1)));
        inventory.setItem(BACK_SLOT, createNavItem(Material.FLOWER_BANNER_PATTERN, Message.BUTTON_BACK.toString()));
        inventory.setItem(NEXT_SLOT, createNavItem(Material.ARROW, ChatColor.YELLOW + "Next Page"));
        fillBottomRowGlass(inventory);
        session.inventory = inventory;
        player.openInventory(inventory);
    }

    private int shopPageLimit(ItemShop shop) {
        return Math.max(0, shop.getStockPageCount(PAGE_SIZE) - 1);
    }

    private String getPageTitle(ItemShop shop, int page) {
        return Message.MENU_STOCK_CHEST + "" + ChatColor.DARK_GRAY + " [" + (page + 1) + "]";
    }

    private ItemStack createNavItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static boolean isBottomRowNavSlot(int slot) {
        return slot == PREV_SLOT || slot == INFO_SLOT || slot == BACK_SLOT || slot == NEXT_SLOT;
    }

    /**
     * Fills unused bottom-row slots so shift-clicks do not land on empty slots and confuse persistence.
     */
    private void fillBottomRowGlass(Inventory inventory) {
        ItemStack pane = createBottomRowFillerPane();
        for (int slot = BOTTOM_ROW_FIRST; slot <= BOTTOM_ROW_LAST; slot++) {
            if (isBottomRowNavSlot(slot)) continue;
            inventory.setItem(slot, pane.clone());
        }
    }

    private static ItemStack createBottomRowFillerPane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private static boolean isBottomRowGlassSlot(int slot) {
        return slot >= BOTTOM_ROW_FIRST && slot <= BOTTOM_ROW_LAST && !isBottomRowNavSlot(slot);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Session session = sessions.get(player.getUniqueId());
        if (session == null || event.getInventory() != session.inventory) return;
        if (!hasManagementAccess(player, session.shop)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You no longer have access to this shop.");
            player.closeInventory();
            return;
        }
        if (!enforceExclusiveViewer(session.shop, player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (event.getClickedInventory() == null) return;

        int slot = event.getRawSlot();
        if (isBottomRowGlassSlot(slot)) {
            event.setCancelled(true);
            return;
        }

        if (event.getClick() == ClickType.DOUBLE_CLICK || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        if (slot == PREV_SLOT || slot == NEXT_SLOT || slot == INFO_SLOT || slot == BACK_SLOT) {
            event.setCancelled(true);
            if (slot == INFO_SLOT) return;
            if (slot == BACK_SLOT) {
                player.playSound(player, Sound.UI_LOOM_SELECT_PATTERN, SoundCategory.MASTER, 1f, 1f);
                Bukkit.getScheduler().runTask(NormalShops.getInstance(), () -> {
                    ItemShop freshShop = ItemShop.get(session.shop.getLocation());
                    if (freshShop == null || freshShop.isDeleted()) {
                        player.sendMessage(ChatColor.RED + "Shop no longer exists.");
                        return;
                    }
                    if (!freshShop.isOwner(player) && !freshShop.isTrusted(player) && Permission.DELETE.lacks(player)) {
                        player.sendMessage(ChatColor.RED + "You no longer have access to this shop.");
                        return;
                    }
                    new EditShopMenu(player, freshShop).open();
                });
                return;
            }
            if (event.getView().getCursor() != null && event.getView().getCursor().getType() != Material.AIR) return;

            int currentPage = session.page;
            int targetPage = currentPage + (slot == NEXT_SLOT ? 1 : -1);
            if (targetPage < 0) return;

            Bukkit.getScheduler().runTask(NormalShops.getInstance(), () -> openPage(player, session, targetPage, true));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        Session session = sessions.get(event.getWhoClicked().getUniqueId());
        if (session == null || event.getInventory() != session.inventory) return;
        if (event.getWhoClicked() instanceof Player player && !hasManagementAccess(player, session.shop)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You no longer have access to this shop.");
            player.closeInventory();
            return;
        }
        if (!enforceExclusiveViewer(session.shop, event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        for (int slot : event.getRawSlots()) {
            if (isBottomRowGlassSlot(slot) || slot == PREV_SLOT || slot == INFO_SLOT || slot == BACK_SLOT || slot == NEXT_SLOT) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Session session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null || event.getInventory() != session.inventory) return;
        if (event.getPlayer() instanceof Player player && hasManagementAccess(player, session.shop)) {
            persistPage(session);
        }
        sessions.remove(event.getPlayer().getUniqueId());
    }

    private void persistPage(Session session) {
        if (session.inventory == null) return;
        if (session.viewedRevision != session.shop.getStockRevision()) {
            Player player = Bukkit.getPlayer(session.viewerUUID);
            if (player != null) {
                player.sendMessage(ChatColor.RED + "Stock changed while you were editing. View refreshed.");
                Bukkit.getScheduler().runTask(NormalShops.getInstance(), () -> openPage(player, session, session.page, false));
            }
            return;
        }
        List<ItemStack> pageContents = new ArrayList<>(PAGE_SIZE);
        for (int i = 0; i < PAGE_SIZE; i++) {
            ItemStack item = session.inventory.getItem(i);
            pageContents.add(item == null ? null : item.clone());
        }
        session.shop.setStockPageContents(session.page, PAGE_SIZE, pageContents);
        session.viewedRevision = session.shop.getStockRevision();
    }

    public void onShopStockMutated(ItemShop shop) {
        sessions.values().forEach(session -> {
            if (session.shop != shop || session.inventory == null) return;
            List<ItemStack> pageItems = session.shop.getStockPageContents(session.page, PAGE_SIZE);
            for (int i = 0; i < PAGE_SIZE; i++) {
                session.inventory.setItem(i, pageItems.get(i));
            }
            session.viewedRevision = session.shop.getStockRevision();
        });
    }

    public void closeAllSessions() {
        sessions.forEach((uuid, session) -> {
            if (session.inventory == null) return;
            for (HumanEntity viewer : new ArrayList<>(session.inventory.getViewers())) {
                viewer.closeInventory();
            }
        });
        sessions.clear();
    }

    private boolean enforceExclusiveViewer(ItemShop shop, UUID callerUUID) {
        List<Session> viewers = sessions.values().stream()
                .filter(session -> session.shop.getLocation().equals(shop.getLocation()) && session.inventory != null)
                .sorted((a, b) -> Long.compare(a.openedAt, b.openedAt))
                .toList();
        if (viewers.size() <= 1) return true;

        Session winner = viewers.get(0);
        for (int i = 1; i < viewers.size(); i++) {
            Session duplicate = viewers.get(i);
            Player player = Bukkit.getPlayer(duplicate.viewerUUID);
            if (player != null) {
                player.sendMessage(ChatColor.RED + "This shop stock is already in use. Your stock view was closed.");
                player.closeInventory();
            } else {
                sessions.remove(duplicate.viewerUUID);
            }
        }
        return winner.viewerUUID.equals(callerUUID);
    }

    private boolean hasManagementAccess(Player player, ItemShop shop) {
        return shop.isOwner(player) || shop.isTrusted(player) || Permission.DELETE.has(player);
    }

    public boolean hasActiveStockViewersForShopByOther(ItemShop shop, UUID playerUUID) {
        return sessions.values().stream()
                .anyMatch(session -> !session.viewerUUID.equals(playerUUID)
                        && session.shop.getLocation().equals(shop.getLocation()));
    }

    private static class Session {
        private final UUID viewerUUID;
        private final ItemShop shop;
        private int page;
        private Inventory inventory;
        private long viewedRevision;
        private final long openedAt;

        private Session(UUID viewerUUID, ItemShop shop, int page) {
            this.viewerUUID = viewerUUID;
            this.shop = shop;
            this.page = page;
            this.viewedRevision = shop.getStockRevision();
            this.openedAt = System.currentTimeMillis();
        }
    }
}
