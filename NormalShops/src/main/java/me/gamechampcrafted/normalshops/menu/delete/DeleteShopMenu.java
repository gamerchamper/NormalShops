package me.gamechampcrafted.normalshops.menu.delete;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.menu.BackButton;
import me.gamechampcrafted.normalshops.menu.ClickHandler;
import me.gamechampcrafted.normalshops.menu.MenuColor;
import me.gamechampcrafted.normalshops.menu.ShopMenu;
import me.gamechampcrafted.normalshops.menu.edit.EditShopMenu;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.entity.Player;

public class DeleteShopMenu extends ShopMenu {
    public DeleteShopMenu(Player player, ItemShop shop) {
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
        addButton(new ForceChangeOwnerButton(9, getShop()));
        addButton(new ForceEditButton(11, getShop()));
        addButton(new DeleteShopButton(13, getShop()));
        addButton(new ShopHistoryButton(15, getShop()));
        addButton(new EmulateSaleButton(17, getShop()));
        // If deleter is the owner
        if (getShop().isOwner(getPlayer())) {
            addButton(new BackButton(18, new EditShopMenu(getPlayer(), getShop())));
        }
    }

}
