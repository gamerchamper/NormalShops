package me.gamechampcrafted.normalshops.menu.edit;

import me.gamechampcrafted.normalshops.menu.Button;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.UUID;

public class TrustedPlayerEntryButton extends Button {

    private final ItemShop shop;
    private final UUID trustedUUID;

    public TrustedPlayerEntryButton(int slot, ItemShop shop, UUID trustedUUID) {
        super(slot);
        this.shop = shop;
        this.trustedUUID = trustedUUID;
    }

    @Override
    public ItemStack getItem() {
        OfflinePlayer trusted = Bukkit.getOfflinePlayer(trustedUUID);
        String name = trusted.getName() != null ? trusted.getName() : trustedUUID.toString();
        return createItem(
                Utils.colorize("&f" + name),
                Arrays.asList(
                        Utils.colorize("&7UUID: &8" + trustedUUID),
                        Utils.colorize("&cClick to remove this trusted player.")
                ),
                Material.PLAYER_HEAD,
                false
        );
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        new RemoveTrustedConfirmMenu(player, shop, trustedUUID).open();
    }
}
