package me.gamechampcrafted.normalshops.menu.edit;

import me.gamechampcrafted.normalshops.menu.ShopButton;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class TrustedPlayersButton extends ShopButton {

    public TrustedPlayersButton(int slot, ItemShop shop) {
        super(slot, shop);
    }

    @Override
    public ItemStack getItem() {
        List<String> lore = Arrays.asList(
                Utils.colorize("&7Manage trusted players for this shop."),
                Utils.colorize("&7Trusted players can open this shop editor.")
        );
        return createItem(Utils.colorize("&b👥 &lTRUSTED PLAYERS"), lore, Material.PLAYER_HEAD, false);
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        new TrustedPlayersMenu((org.bukkit.entity.Player) event.getWhoClicked(), getShop()).open();
    }
}
