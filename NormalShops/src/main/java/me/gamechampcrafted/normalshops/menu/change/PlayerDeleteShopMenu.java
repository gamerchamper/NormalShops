package me.gamechampcrafted.normalshops.menu.change;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.menu.BackButton;
import me.gamechampcrafted.normalshops.menu.ClickHandler;
import me.gamechampcrafted.normalshops.menu.MenuSlotRegistry;
import me.gamechampcrafted.normalshops.menu.MenuColor;
import me.gamechampcrafted.normalshops.menu.ShopMenu;
import me.gamechampcrafted.normalshops.menu.delete.DeleteShopButton;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.entity.Player;

public class PlayerDeleteShopMenu extends ShopMenu {

    public PlayerDeleteShopMenu(Player player, ItemShop shop) {
        super(player, shop, 27, new ClickHandler(), MenuColor.RED,
                Message.MENU_DELETE.parameterizer()
                        .put("owner", getSafeOwnerName(shop))
                        .toString());
    }

    private static String getSafeOwnerName(ItemShop shop) {
        String ownerName = shop.getOwnerName();
        if (ownerName == null || ownerName.isEmpty()) {
            return shop.getOwnerUUID().toString().substring(0, 8);
        }
        return ownerName;
    }

    @Override
    protected void setupButtons() {
        addButton(new DeleteShopButton(MenuSlotRegistry.slot("delete-shop-player", "delete", 13), getShop()));
        addButton(new BackButton(MenuSlotRegistry.slot("delete-shop-player", "back", 18),
                new ChangeShopMenu(getPlayer(), getShop())));
    }

    @Override
    protected boolean enforceManagementAccess() {
        return true;
    }
}
