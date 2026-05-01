package me.gamechampcrafted.normalshops.menu.edit;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.menu.ClickHandler;
import me.gamechampcrafted.normalshops.menu.ShopMenu;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class EditShopMenu extends ShopMenu {

    public EditShopMenu(Player player, ItemShop shop) throws IllegalArgumentException {
        super(player, shop, 45, new ClickHandler(), shop.hasStock() ? Message.MENU_SHOP_OPEN : Message.MENU_SHOP_CLOSED);
        if (!shop.isOwner(player) && !shop.isTrusted(player) && Permission.DELETE.lacks(player)) {
            throw new IllegalArgumentException("Player is not owner");
        }
    }

    @Override
    public void open() {
        if (!hasManagementAccess(getPlayer())) {
            getPlayer().sendMessage(ChatColor.RED + "You no longer have access to this shop.");
            return;
        }
        super.open();
    }

    @Override
    protected void setupButtons() {
        ItemShop shop = getShop();
        addButton(new ChangeShopButton(19, shop));
        addButton(new CollectButton(13, shop));
        addButton(new CustomizeButton(25, shop));
        addButton(new ConnectStockpileButton(30, shop));
        addButton(new StockChestButton(32, shop));
        addButton(new SettingsButton(22, shop));
        addButton(new AnalyticsDashboardButton(40, shop));
        addButton(new TrustedPlayersButton(36, shop));
        addButton(new ShopInfoButton(44, shop));
    }

    @Override
    protected boolean enforceManagementAccess() {
        return true;
    }

    @Override
    protected void onClose(InventoryCloseEvent event) {
        super.onClose(event);
    }

}
