package me.gamechampcrafted.normalshops.menu.edit;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.menu.ShopButton;
import me.gamechampcrafted.normalshops.menu.change.ChangeShopMenu;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ChangeShopButton extends ShopButton {

    public ChangeShopButton(int slot, ItemShop shop) {
        super(slot, shop);
    }

    private final ItemStack item =
            createItem(Message.BUTTON_CHANGE_SHOP, Material.ENDER_EYE, false);

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        new ChangeShopMenu(player, getShop()).open();
    }
}
