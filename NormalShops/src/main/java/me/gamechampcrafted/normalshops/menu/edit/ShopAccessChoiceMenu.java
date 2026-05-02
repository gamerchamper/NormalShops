package me.gamechampcrafted.normalshops.menu.edit;

import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.data.Setting;
import me.gamechampcrafted.normalshops.menu.Button;
import me.gamechampcrafted.normalshops.menu.ClickHandler;
import me.gamechampcrafted.normalshops.menu.MenuColor;
import me.gamechampcrafted.normalshops.menu.ShopMenu;
import me.gamechampcrafted.normalshops.menu.buy.BuyMenu;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.utils.Parameterizer;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class ShopAccessChoiceMenu extends ShopMenu {

    public ShopAccessChoiceMenu(Player player, ItemShop shop) {
        super(player, shop, 27, new ClickHandler(), MenuColor.BLACK, Utils.colorize("&8Choose Shop View"));
    }

    @Override
    protected void setupButtons() {
        addButton(new StockQuickButton(10, getShop()));
        addButton(new EditorButton(12, getShop()));
        addButton(new TradeButton(14, getShop()));
        addButton(new EarningsQuickButton(16, getShop()));
    }

    private static class StockQuickButton extends Button {
        private final ItemShop shop;

        private StockQuickButton(int slot, ItemShop shop) {
            super(slot);
            this.shop = shop;
        }

        @Override
        public ItemStack getItem() {
            return createItem(Message.BUTTON_STOCK_CHEST, Material.CHEST, false);
        }

        @Override
        public void clickSound(Player player) {
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, .5f, 1f);
        }

        @Override
        protected void onClick(InventoryClickEvent event) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            NormalShops.getInstance().getStockChestManager().open(player, shop);
        }
    }

    private static class EarningsQuickButton extends Button {
        private final ItemShop shop;

        private EarningsQuickButton(int slot, ItemShop shop) {
            super(slot);
            this.shop = shop;
        }

        @Override
        public ItemStack getItem() {
            return createItem(
                    Message.BUTTON_COLLECT.toString(),
                    earningsQuickLore(shop),
                    Material.DIAMOND,
                    false);
        }

        @Override
        public void clickSound(Player player) {
        }

        @Override
        protected void onClick(InventoryClickEvent event) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            NormalShops.getInstance().getEarningsChestManager().open(player, shop);
        }

        private static List<String> earningsQuickLore(ItemShop shop) {
            int total = shop.getEarnings() * shop.getPrice().getAmount();
            return Message.BUTTON_COLLECT.getParameterizedLore(new Parameterizer<>()
                    .put("earnings", total > 0
                            ? total + " " + Utils.getFormattedName(shop.getPrice())
                            : Message.BUTTON_NONE));
        }
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
