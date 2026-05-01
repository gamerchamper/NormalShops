package me.gamechampcrafted.normalshops.menu.customize;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.menu.ShopButton;
import me.gamechampcrafted.normalshops.menu.color.ColorMenu;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ColorMenuButton extends ShopButton {

    public ColorMenuButton(int slot, ItemShop shop) {
        super(slot, shop);
        this.item = createItem(
                Message.BUTTON_THEME, Material.BRUSH, false
        );
    }

    private final ItemStack item;

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        new ColorMenu(player, getShop()).open();
    }
}
