package me.gamechampcrafted.normalshops.menu.settings;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.data.Setting;
import me.gamechampcrafted.normalshops.menu.BackButton;
import me.gamechampcrafted.normalshops.menu.ClickHandler;
import me.gamechampcrafted.normalshops.menu.MenuSlotRegistry;
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

        addButton(new NotificationsButton(MenuSlotRegistry.slot("settings", "notifications", 12), shop));
        addButton(new StockWarningButton(MenuSlotRegistry.slot("settings", "stock-warning", 13), shop));
        addButton(new ClearStockpilesButton(MenuSlotRegistry.slot("settings", "clear-stockpiles", 14), shop));

        addButton(new BackButton(MenuSlotRegistry.slot("settings", "back", 36), new EditShopMenu(getPlayer(), shop)));

        if (Setting.PRIVATE_SHOP_STATS_HOLOGRAMS.isEnabled()) {
            addButton(new PrivateStatsHologramButton(MenuSlotRegistry.slot("settings", "stats-hologram", 15), shop));
        }

        if (Permission.UNLIMITED_STOCK.has(getPlayer())) {
            addButton(new UnlimitedStockButton(MenuSlotRegistry.slot("settings", "unlimited-stock", 16), shop));
        }
    }

    @Override
    protected boolean enforceManagementAccess() {
        return true;
    }
}
