package me.gamechampcrafted.normalshops.menu.edit;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.menu.ClickHandler;
import me.gamechampcrafted.normalshops.menu.Menu;
import me.gamechampcrafted.normalshops.menu.ShopGuiLayout;
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
        addButton(new ChangeShopButton(Menu.ownerChangeListingSlot(), shop));
        addButton(new CollectButton(Menu.ownerCollectSlot(), shop));
        addButton(new CustomizeButton(Menu.ownerCustomizeSlot(), shop));
        addButton(new ConnectStockpileButton(Menu.ownerConnectStockpileSlot(), shop));
        addButton(new StockChestButton(Menu.ownerStockChestSlot(), shop));
        addButton(new SettingsButton(Menu.ownerSettingsSlot(), shop));
        addButton(new AnalyticsDashboardButton(Menu.ownerAnalyticsSlot(), shop));
        addButton(new TrustedPlayersButton(Menu.ownerTrustedPlayersSlot(), shop));
        addButton(new ShopInfoButton(Menu.ownerShopInfoSlot(), shop));
    }

    @Override
    protected void onAfterPlaceButtons() {
        paintLayoutFillers("owner", ShopGuiLayout.get().ownerFillerSlots());
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
