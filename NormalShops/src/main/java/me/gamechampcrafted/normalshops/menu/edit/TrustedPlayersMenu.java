package me.gamechampcrafted.normalshops.menu.edit;

import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.menu.BackButton;
import me.gamechampcrafted.normalshops.menu.Button;
import me.gamechampcrafted.normalshops.menu.ClickHandler;
import me.gamechampcrafted.normalshops.menu.GuiIcons;
import me.gamechampcrafted.normalshops.menu.MenuColor;
import me.gamechampcrafted.normalshops.menu.MenuSlotRegistry;
import me.gamechampcrafted.normalshops.menu.ShopMenu;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class TrustedPlayersMenu extends ShopMenu {

    private static final List<Integer> LIST_SLOTS = Arrays.asList(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    );

    public TrustedPlayersMenu(Player player, ItemShop shop) {
        super(player, shop, 45, new ClickHandler(), MenuColor.BLUE, Utils.colorize("&bTrusted Players"));
    }

    @Override
    protected void setupButtons() {
        ItemShop shop = getShop();
        List<Integer> entrySlots = MenuSlotRegistry.slots("trusted-players", "entry", LIST_SLOTS);
        addButton(new BackButton(MenuSlotRegistry.slot("trusted-players", "back", 36), new EditShopMenu(getPlayer(), shop)));
        addButton(new AddTrustedPlayerButton(MenuSlotRegistry.slot("trusted-players", "add-player", 38), shop));

        List<UUID> trustedPlayers = new ArrayList<>(shop.getTrustedPlayers());
        for (int i = 0; i < entrySlots.size() && i < trustedPlayers.size(); i++) {
            addButton(new TrustedPlayerEntryButton(entrySlots.get(i), shop, trustedPlayers.get(i)));
        }
    }

    @Override
    protected boolean enforceManagementAccess() {
        return true;
    }

    private static class AddTrustedPlayerButton extends Button {
        private final ItemShop shop;
        private final ItemStack item;

        private AddTrustedPlayerButton(int slot, ItemShop shop) {
            super(slot);
            this.shop = shop;
            this.item = createItem(
                    Utils.colorize("&a➕ &lADD TRUSTED PLAYER"),
                    Arrays.asList(
                            Utils.colorize("&7Type a username in chat."),
                            Utils.colorize("&7They will get trusted access.")
                    ),
                    GuiIcons.material("trusted-players.add-player", Material.LIME_DYE),
                    false
            );
        }

        @Override
        public ItemStack getItem() {
            return item;
        }

        @Override
        protected void onClick(org.bukkit.event.inventory.InventoryClickEvent event) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Type a username to trust (10s).");
            NormalShops.getInstance().getChatInputListener().addChatCallback(player, input ->
                    Bukkit.getScheduler().runTask(NormalShops.getInstance(), () -> {
                        ItemShop current = ItemShop.get(shop.getLocation());
                        if (current == null || current.isDeleted()) {
                            player.sendMessage(ChatColor.RED + "Shop no longer exists.");
                            return;
                        }
                        String username = input == null ? "" : input.trim();
                        if (username.isEmpty()) {
                            player.sendMessage(ChatColor.RED + "Username cannot be empty.");
                            return;
                        }
                        OfflinePlayer target = Bukkit.getOfflinePlayer(username);
                        String targetName = target.getName() != null ? target.getName() : username;
                        boolean added = current.addTrustedPlayer(target.getUniqueId());
                        if (!added) {
                            player.sendMessage(ChatColor.RED + targetName + " is already trusted or invalid.");
                            return;
                        }
                        player.sendMessage(ChatColor.GREEN + "Trusted " + targetName + " for this shop.");
                        new TrustedPlayersMenu(player, current).open();
                    }), 200, null);
        }
    }
}
