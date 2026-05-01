package me.gamechampcrafted.normalshops.menu.color;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.menu.MenuColor;
import me.gamechampcrafted.normalshops.menu.ShopButton;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ColorButton extends ShopButton {

    private final MenuColor color;

    public ColorButton(int slot, ItemShop shop, MenuColor color) {
        super(slot, shop);
        this.color = color;
        Material mat = color.getIcon();
        boolean selected = mat == shop.getColor().getIcon();
        String name = selected
                ? Message.BUTTON_COLOR_SELECTED.toString()
                : rainbow(Message.BUTTON_COLOR.toString());
        this.item = createItem(
                name, Message.BUTTON_COLOR.getLore(), mat, selected
        );
    }

    private final ItemStack item;

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemShop shop = getShop();
        shop.setColor(color);
        CoreProtectLogger.logShopEdit(player, shop.getLocation(), "changed color to " + color.name());
        new ColorMenu(player, shop).open();
    }
}
