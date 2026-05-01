package me.gamechampcrafted.normalshops.menu.change;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.menu.BackButton;
import me.gamechampcrafted.normalshops.menu.EditableClickHandler;
import me.gamechampcrafted.normalshops.menu.ShopMenu;
import me.gamechampcrafted.normalshops.menu.edit.EditShopMenu;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.entity.Player;

public class ChangeShopMenu extends ShopMenu {

    public ChangeShopMenu(Player player, ItemShop shop) {
        super(player, shop, 45, new EditableClickHandler(), Message.MENU_CHANGE_SHOP);
    }

    @Override
    protected void setupButtons() {
        ItemShop shop = getShop();
        addButton(new SaveChangesButton(12, shop));
        addButton(new DeleteRedirectButton(30, shop));

        addButton(new BackButton(36, new EditShopMenu(getPlayer(), shop)));

        addPriceAndProductButtons(true);
    }

    @Override
    protected boolean enforceManagementAccess() {
        return true;
    }
}
