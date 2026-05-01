package me.gamechampcrafted.normalshops.menu.edit;

import me.gamechampcrafted.normalshops.menu.Button;
import me.gamechampcrafted.normalshops.menu.ClickHandler;
import me.gamechampcrafted.normalshops.menu.MenuColor;
import me.gamechampcrafted.normalshops.menu.ShopMenu;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.UUID;

public class RemoveTrustedConfirmMenu extends ShopMenu {

    private final UUID trustedUUID;

    public RemoveTrustedConfirmMenu(Player player, ItemShop shop, UUID trustedUUID) {
        super(player, shop, 27, new ClickHandler(), MenuColor.RED, Utils.colorize("&cConfirm Removal"));
        this.trustedUUID = trustedUUID;
    }

    @Override
    protected void setupButtons() {
        addButton(new ConfirmButton(11, getShop(), trustedUUID));
        addButton(new CancelButton(15, getShop()));
    }

    @Override
    protected boolean enforceManagementAccess() {
        return true;
    }

    private static class ConfirmButton extends Button {
        private final ItemShop shop;
        private final UUID trustedUUID;
        private final ItemStack item = createItem(
                Utils.colorize("&a✔ &lCONFIRM REMOVE"),
                Arrays.asList(Utils.colorize("&7Remove this player from trusted list.")),
                Material.LIME_WOOL,
                false
        );

        private ConfirmButton(int slot, ItemShop shop, UUID trustedUUID) {
            super(slot);
            this.shop = shop;
            this.trustedUUID = trustedUUID;
        }

        @Override
        public ItemStack getItem() {
            return item;
        }

        @Override
        protected void onClick(InventoryClickEvent event) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            ItemShop current = ItemShop.get(shop.getLocation());
            if (current == null || current.isDeleted()) {
                player.sendMessage(ChatColor.RED + "Shop no longer exists.");
                player.closeInventory();
                return;
            }
            boolean removed = current.removeTrustedPlayer(trustedUUID);
            OfflinePlayer trusted = Bukkit.getOfflinePlayer(trustedUUID);
            String name = trusted.getName() != null ? trusted.getName() : trustedUUID.toString();
            if (removed) {
                player.sendMessage(ChatColor.GREEN + "Removed " + name + " from trusted players.");
            } else {
                player.sendMessage(ChatColor.RED + name + " is not in trusted players.");
            }
            new TrustedPlayersMenu(player, current).open();
        }
    }

    private static class CancelButton extends Button {
        private final ItemShop shop;
        private final ItemStack item = createItem(
                Utils.colorize("&c✖ &lCANCEL"),
                Arrays.asList(Utils.colorize("&7Return without removing.")),
                Material.RED_WOOL,
                false
        );

        private CancelButton(int slot, ItemShop shop) {
            super(slot);
            this.shop = shop;
        }

        @Override
        public ItemStack getItem() {
            return item;
        }

        @Override
        protected void onClick(InventoryClickEvent event) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            new TrustedPlayersMenu(player, shop).open();
        }
    }
}
