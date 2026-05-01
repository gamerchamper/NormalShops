package me.gamechampcrafted.normalshops.menu.edit;

import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.data.Setting;
import me.gamechampcrafted.normalshops.menu.Button;
import me.gamechampcrafted.normalshops.menu.ClickHandler;
import me.gamechampcrafted.normalshops.menu.MenuColor;
import me.gamechampcrafted.normalshops.menu.ShopMenu;
import me.gamechampcrafted.normalshops.menu.buy.BuyMenu;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class ShopAccessChoiceMenu extends ShopMenu {

    public ShopAccessChoiceMenu(Player player, ItemShop shop) {
        super(player, shop, 27, new ClickHandler(), MenuColor.BLACK, Utils.colorize("&8Choose Shop View"));
    }

    @Override
    protected void setupButtons() {
        addButton(new EditorButton(11, getShop()));
        addButton(new TradeButton(15, getShop()));
    }

    private static class EditorButton extends Button {
        private final ItemShop shop;

        private EditorButton(int slot, ItemShop shop) {
            super(slot);
            this.shop = shop;
        }

        @Override
        public ItemStack getItem() {
            return createItem(
                    Utils.colorize("&6🛠 &lOPEN EDITOR"),
                    Arrays.asList(Utils.colorize("&7Manage this shop.")),
                    Material.ANVIL,
                    false
            );
        }

        @Override
        protected void onClick(InventoryClickEvent event) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            new EditShopMenu(player, shop).open();
        }
    }

    private static class TradeButton extends Button {
        private final ItemShop shop;

        private TradeButton(int slot, ItemShop shop) {
            super(slot);
            this.shop = shop;
        }

        @Override
        public ItemStack getItem() {
            return createItem(
                    Utils.colorize("&a💱 &lOPEN TRADE"),
                    Arrays.asList(Utils.colorize("&7Open customer trade view.")),
                    Material.EMERALD,
                    false
            );
        }

        @Override
        protected void onClick(InventoryClickEvent event) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            if (Permission.BUY.lacksAndNotify(player)) {
                return;
            }
            shop.incrementImpressions();
            shop.saveData();
            if (Setting.VILLAGER_TRADING_MENU.isEnabled()) {
                NormalShops.getInstance().getTradingMenuManager().openTradingMenu(player, shop);
            } else {
                new BuyMenu(player, shop).open();
            }
        }
    }
}
