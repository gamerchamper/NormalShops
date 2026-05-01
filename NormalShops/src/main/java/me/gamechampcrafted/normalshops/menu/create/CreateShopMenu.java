package me.gamechampcrafted.normalshops.menu.create;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.menu.EditableClickHandler;
import me.gamechampcrafted.normalshops.menu.Menu;
import me.gamechampcrafted.normalshops.menu.MenuColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class CreateShopMenu extends Menu {

    public CreateShopMenu(Player player, Location loc) {
        super(player, loc, 45, new EditableClickHandler(), MenuColor.BLACK, Message.MENU_CREATE_SHOP);
    }

    @Override
    protected void setupButtons() {
        addButton(new CreateShopButton(21, getLocation()));
        addButton(new PriceButton(PRICE_SLOT));
        PRODUCT_SLOTS.forEach(i -> addButton(new ProductButton(i)));
    }
}
