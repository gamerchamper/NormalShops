package me.gamechampcrafted.normalshops.menu.settings;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.menu.BackButton;
import me.gamechampcrafted.normalshops.menu.ClickHandler;
import me.gamechampcrafted.normalshops.menu.ShopMenu;
import me.gamechampcrafted.normalshops.menu.edit.EditShopMenu;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.entity.Player;

public class SettingsMenu extends ShopMenu {

    public SettingsMenu(Player player, ItemShop shop) {
        super(player, shop, 45, new ClickHandler(), Message.MENU_SETTINGS);
    }

    @Override
    protected void setupButtons() {
        ItemShop shop = getShop();

        addButton(new NotificationsButton(12, shop));
        addButton(new StockWarningButton(13, shop));
        addButton(new ClearStockpilesButton(14, shop));

        addButton(new BackButton(36, new EditShopMenu(getPlayer(), shop)));

        if (Permission.UNLIMITED_STOCK.has(getPlayer())) {
            addButton(new UnlimitedStockButton(16, shop));
        }
    }

    @Override
    protected boolean enforceManagementAccess() {
        return true;
    }
}
