package me.gamechampcrafted.normalshops.menu.change;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.menu.ShopButton;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class DeleteRedirectButton extends ShopButton {

    public DeleteRedirectButton(int slot, ItemShop shop) {
        super(slot, shop);
    }

    private final ItemStack item = createItem(
            Message.BUTTON_DELETE_REDIRECT, Material.BARRIER, false
    );

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        new PlayerDeleteShopMenu(player, getShop()).open();
    }
}
