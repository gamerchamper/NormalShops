package me.gamechampcrafted.normalshops.menu.delete;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShopHistoryMenuManager implements Listener {

    private static final int PAGE_SIZE = 45;
    private static final int PREV_SLOT = 45;
    private static final int BACK_SLOT = 49;
    private static final int NEXT_SLOT = 53;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<UUID, Session> sessions = new HashMap<>();

    public void open(Player player, ItemShop shop) {
        List<CoreProtectLogger.HistoryEntry> entries =
                CoreProtectLogger.getShopHistory(shop.getLocation(), 60 * 60 * 24 * 30);
        Session session = new Session(shop, entries);
        sessions.put(player.getUniqueId(), session);
        openPage(player, session, 0);
    }

    private void openPage(Player player, Session session, int page) {
        int maxPage = Math.max(0, (session.entries.size() - 1) / PAGE_SIZE);
        session.page = Math.max(0, Math.min(page, maxPage));
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.DARK_RED + "Shop History");

        int start = session.page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, session.entries.size());
        for (int i = start; i < end; i++) {
            CoreProtectLogger.HistoryEntry entry = session.entries.get(i);
            inventory.setItem(i - start, createHistoryItem(entry, i + 1));
        }

        inventory.setItem(PREV_SLOT, createNavItem(Material.ARROW, ChatColor.YELLOW + "Previous Page"));
        inventory.setItem(BACK_SLOT, createNavItem(Material.FLOWER_BANNER_PATTERN, Message.BUTTON_BACK.toString()));
        inventory.setItem(NEXT_SLOT, createNavItem(Material.ARROW, ChatColor.YELLOW + "Next Page"));

        session.inventory = inventory;
        player.openInventory(inventory);
    }

    private ItemStack createHistoryItem(CoreProtectLogger.HistoryEntry entry, int index) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Utils.colorize("&6#" + index + " &f" + safe(entry.action())));
            List<String> lore = new ArrayList<>();
            lore.add(Utils.colorize("&7Actor: &e" + safe(entry.actor())));
            lore.add(Utils.colorize("&7Block: &b" + safe(entry.material())));
            if (entry.details() != null && !entry.details().isEmpty()) {
                lore.add(Utils.colorize("&7Details: &f" + entry.details()));
            }
            lore.add(Utils.colorize("&7Time: &f" + formatTimestamp(entry.timestamp())));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatTimestamp(long timestampSeconds) {
        return Instant.ofEpochSecond(timestampSeconds)
                .atZone(ZoneId.systemDefault())
                .format(TIME_FORMAT);
    }

    private String safe(String input) {
        return input == null || input.isEmpty() ? "unknown" : input;
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

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Session session = sessions.get(player.getUniqueId());
        if (session == null || event.getInventory() != session.inventory) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot == BACK_SLOT) {
            new DeleteShopMenu(player, session.shop).open();
            return;
        }
        if (slot == PREV_SLOT) {
            openPage(player, session, session.page - 1);
            return;
        }
        if (slot == NEXT_SLOT) {
            openPage(player, session, session.page + 1);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Session session = sessions.get(event.getWhoClicked().getUniqueId());
        if (session == null || event.getInventory() != session.inventory) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Session session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null || event.getInventory() != session.inventory) return;
        sessions.remove(event.getPlayer().getUniqueId());
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

    private static class Session {
        private final ItemShop shop;
        private final List<CoreProtectLogger.HistoryEntry> entries;
        private int page;
        private Inventory inventory;

        private Session(ItemShop shop, List<CoreProtectLogger.HistoryEntry> entries) {
            this.shop = shop;
            this.entries = entries;
            this.page = 0;
        }
    }
}
