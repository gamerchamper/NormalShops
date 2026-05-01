package me.gamechampcrafted.normalshops.menu.buy;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.menu.ClickHandler;
import me.gamechampcrafted.normalshops.menu.MenuColor;
import me.gamechampcrafted.normalshops.menu.ShopMenu;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.utils.MessageParametizer;
import org.bukkit.entity.Player;

public class BuyMenu extends ShopMenu {

    public BuyMenu(Player player, ItemShop shop) {
        super(player, shop, 45, new ClickHandler(), getTitle(shop));
    }

    @Override
    protected void setupButtons() {
        ItemShop shop = getShop();
        addButton(new BuyButton(21, shop));

        addPriceAndProductButtons(false);
    }

    private static String getTitle(ItemShop shop) {
        MenuColor color = shop.getColor();
        String customName = shop.getCustomName();
        MessageParametizer parametizer = new MessageParametizer(Message.MENU_BUY)
                .setColorizeParameters(false)
                .put("1", color.getPrimaryColor())
                .put("2", color.getSecondaryColor());
        if (customName == null) {
            parametizer.put("player", shop.getOwnerName());
        } else {
            parametizer.setMessage(Message.MENU_BUY_CUSTOM)
                    .put("name", customName);
        }

        return parametizer.toString();
    }
}
