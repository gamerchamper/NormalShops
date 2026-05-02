package me.gamechampcrafted.normalshops.menu.customize;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.menu.BackButton;
import me.gamechampcrafted.normalshops.menu.ClickHandler;
import me.gamechampcrafted.normalshops.menu.ShopMenu;
import me.gamechampcrafted.normalshops.menu.edit.EditShopMenu;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.entity.Player;

public class CustomizeShopMenu extends ShopMenu {

    public CustomizeShopMenu(Player player, ItemShop shop) {
        super(player, shop, 45, new ClickHandler(),
                shop.getColor().getPrimaryColorFilteredGreen() + Message.MENU_CUSTOMIZE.toString());
    }

    @Override
    public void open() {
        ItemShop shop = getShop();
        if (!shop.isAdminShop() && !shop.hasStock()) {
            Message.CUSTOMIZE_REQUIRES_STOCK.send(getPlayer());
            return;
        }
        super.open();
    }

    @Override
    protected void setupButtons() {
        ItemShop shop = getShop();

        addButton(new ColorMenuButton(10, shop));
        addButton(new ChangeBlockButton(19, shop));
        addButton(new CustomNameButton(28, shop));

        addButton(new BuildGlassDisplayButton(16, shop));
        addButton(new BuildFrameDisplayButton(25, shop));
        addButton(new ClearDisplayButton(34, shop));

        addButton(new BackButton(36, new EditShopMenu(getPlayer(), shop)));

        switch (shop.getDisplayType()) {
            case GLASS:
                addButton(new DisplayChangeBaseButton(15, shop));
                addButton(new DisplayChangeGlassButton(14, shop));
                addButton(new DisplayLightButton(13, shop));
                addButton(new DisplaySaleTextButton(12, shop));
                break;
            case FRAME:
                addButton(new DisplayMoveToTopButton(24, shop));
                addButton(new DisplayAddFrameButton(23, shop));
                addButton(new DisplayRemoveFrameButton(22, shop));
                break;
        }
    }

    @Override
    protected boolean enforceManagementAccess() {
        return true;
    }
}
