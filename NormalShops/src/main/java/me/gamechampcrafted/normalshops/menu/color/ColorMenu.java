package me.gamechampcrafted.normalshops.menu.color;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.menu.BackButton;
import me.gamechampcrafted.normalshops.menu.ClickHandler;
import me.gamechampcrafted.normalshops.menu.MenuColor;
import me.gamechampcrafted.normalshops.menu.ShopMenu;
import me.gamechampcrafted.normalshops.menu.customize.CustomizeShopMenu;
import me.gamechampcrafted.normalshops.shop.BuySound;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.entity.Player;

public class ColorMenu extends ShopMenu {

    public ColorMenu(Player player, ItemShop shop) {
        super(player, shop, 45, new ClickHandler(),
                shop.getColor().getPrimaryColorFilteredGreen() + Message.MENU_COLOR.toString());
    }

    @Override
    protected void setupButtons() {
        ItemShop shop = getShop();
        MenuColor[] colors = MenuColor.values();
        for (int i = 0; i < colors.length; i++) {
            addButton(new ColorButton(i, shop, colors[i]));
        }
        BuySound[] sounds = BuySound.values();
        for (int i = 0; i < sounds.length; i++) {
            addButton(new BuySoundButton(i + 20, getShop(), sounds[i]));
        }
        addButton(new BackButton(36, new CustomizeShopMenu(getPlayer(), getShop())));
    }

    @Override
    protected boolean enforceManagementAccess() {
        return true;
    }
}
