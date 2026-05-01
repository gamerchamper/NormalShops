package me.gamechampcrafted.normalshops.shop;

import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.menu.edit.EditShopMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EarningsChestManager implements Listener {

    private static final int PAGE_SIZE = 45;
    private static final int PREV_SLOT = 45;
    private static final int INFO_SLOT = 48;
    private static final int BACK_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public void open(Player player, ItemShop shop) {
        if (!hasManagementAccess(player, shop)) {
            player.sendMessage(ChatColor.RED + "You no longer have access to this shop.");
            return;
        }
        if (hasActiveViewersForShopByOther(shop, player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Earnings chest is already in use.");
            return;
        }
        Session session = new Session(player.getUniqueId(), shop);
        sessions.put(player.getUniqueId(), session);
        openPage(player, session, 0, false);
    }

    private void openPage(Player player, Session session, int targetPage, boolean persistCurrent) {
        if (persistCurrent && session.inventory != null) {
            persistPage(session);
        }
        session.page = Math.max(0, targetPage);
        session.pageInitialBundles = session.shop.getEarningsBundlesOnPage(session.page, PAGE_SIZE);
        session.viewedRevision = session.shop.getEarningsRevision();

        Inventory inventory = Bukkit.createInventory(null, 54, getPageTitle(session.page));
        List<ItemStack> pageItems = session.shop.getEarningsPageContents(session.page, PAGE_SIZE);
        for (int i = 0; i < PAGE_SIZE; i++) {
            inventory.setItem(i, pageItems.get(i));
        }
        inventory.setItem(PREV_SLOT, createNavItem(Material.ARROW, ChatColor.YELLOW + "Previous Page"));
        inventory.setItem(INFO_SLOT, createNavItem(Material.GOLD_INGOT, ChatColor.GOLD + "Page " + (session.page + 1)));
        inventory.setItem(BACK_SLOT, createNavItem(Material.FLOWER_BANNER_PATTERN, ChatColor.WHITE + "Back"));
        inventory.setItem(NEXT_SLOT, createNavItem(Material.ARROW, ChatColor.YELLOW + "Next Page"));
        session.inventory = inventory;
        player.openInventory(inventory);
    }

    private String getPageTitle(int page) {
        return ChatColor.GOLD + "Earnings Chest " + ChatColor.DARK_GRAY + "[" + (page + 1) + "]";
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

        int rawSlot = event.getRawSlot();
        if (rawSlot == PREV_SLOT || rawSlot == NEXT_SLOT || rawSlot == INFO_SLOT || rawSlot == BACK_SLOT) {
            event.setCancelled(true);
            if (rawSlot == INFO_SLOT) return;
            if (rawSlot == BACK_SLOT) {
                Bukkit.getScheduler().runTask(NormalShops.getInstance(), () -> new EditShopMenu(player, session.shop).open());
                return;
            }
            int target = session.page + (rawSlot == NEXT_SLOT ? 1 : -1);
            if (target < 0) return;
            Bukkit.getScheduler().runTask(NormalShops.getInstance(), () -> openPage(player, session, target, true));
            return;
        }

        // Top inventory is withdraw-only. Block all insert/swap/place actions.
        if (rawSlot >= 0 && rawSlot < PAGE_SIZE) {
            if (isInsertOrSwapAction(event.getAction())) {
                event.setCancelled(true);
                return;
            }
        } else {
            // Allow normal bottom-inventory management, but block shift-move into earnings chest.
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
                return;
            }
        }

        Bukkit.getScheduler().runTask(NormalShops.getInstance(), () -> persistPage(session));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        Session session = sessions.get(event.getWhoClicked().getUniqueId());
        if (session == null || event.getInventory() != session.inventory) return;
        for (int slot : event.getRawSlots()) {
            if (slot < PAGE_SIZE || slot == PREV_SLOT || slot == INFO_SLOT || slot == BACK_SLOT || slot == NEXT_SLOT) {
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
        int currentBundles = 0;
        for (int i = 0; i < PAGE_SIZE; i++) {
            ItemStack item = session.inventory.getItem(i);
            currentBundles += session.shop.getBundlesFromEarningsItem(item);
        }
        int removed = Math.max(0, session.pageInitialBundles - currentBundles);
        if (removed > 0) {
            session.shop.removeEarningsBundles(removed);
        }
        session.pageInitialBundles = session.shop.getEarningsBundlesOnPage(session.page, PAGE_SIZE);
        session.viewedRevision = session.shop.getEarningsRevision();
    }

    public void onShopEarningsMutated(ItemShop shop) {
        sessions.values().forEach(session -> {
            if (!session.shop.getLocation().equals(shop.getLocation())) return;
            if (session.inventory == null) return;
            List<ItemStack> pageItems = session.shop.getEarningsPageContents(session.page, PAGE_SIZE);
            for (int i = 0; i < PAGE_SIZE; i++) {
                session.inventory.setItem(i, pageItems.get(i));
            }
            session.pageInitialBundles = session.shop.getEarningsBundlesOnPage(session.page, PAGE_SIZE);
            session.viewedRevision = session.shop.getEarningsRevision();
        });
    }

    public boolean hasActiveViewersForShopByOther(ItemShop shop, UUID playerUUID) {
        return sessions.values().stream()
                .anyMatch(session -> session.shop.getLocation().equals(shop.getLocation())
                        && !session.viewerUUID.equals(playerUUID));
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

    private boolean hasManagementAccess(Player player, ItemShop shop) {
        return shop.isOwner(player) || shop.isTrusted(player) || Permission.DELETE.has(player);
    }

    private boolean isInsertOrSwapAction(InventoryAction action) {
        return action == InventoryAction.PLACE_ALL
                || action == InventoryAction.PLACE_ONE
                || action == InventoryAction.PLACE_SOME
                || action == InventoryAction.SWAP_WITH_CURSOR
                || action == InventoryAction.HOTBAR_SWAP
                || action == InventoryAction.HOTBAR_MOVE_AND_READD;
    }

    private static class Session {
        private final UUID viewerUUID;
        private final ItemShop shop;
        private int page;
        private int pageInitialBundles;
        private long viewedRevision;
        private Inventory inventory;

        private Session(UUID viewerUUID, ItemShop shop) {
            this.viewerUUID = viewerUUID;
            this.shop = shop;
            this.page = 0;
            this.pageInitialBundles = 0;
            this.viewedRevision = shop.getEarningsRevision();
        }
    }
}
