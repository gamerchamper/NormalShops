package me.gamechampcrafted.normalshops.menu.edit;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.menu.BackButton;
import me.gamechampcrafted.normalshops.menu.Button;
import me.gamechampcrafted.normalshops.menu.ClickHandler;
import me.gamechampcrafted.normalshops.menu.GuiIcons;
import me.gamechampcrafted.normalshops.menu.MenuColor;
import me.gamechampcrafted.normalshops.menu.MenuSlotRegistry;
import me.gamechampcrafted.normalshops.menu.ShopMenu;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.ArrayList;

public class ShopAnalyticsMenu extends ShopMenu {

    public ShopAnalyticsMenu(Player player, ItemShop shop) {
        super(player, shop, 45, new ClickHandler(), MenuColor.BLUE, Utils.colorize("&bShop Analytics"));
    }

    @Override
    protected void setupButtons() {
        ItemShop shop = getShop();
        addButton(new BackButton(MenuSlotRegistry.slot("shop-analytics", "back", 36), new EditShopMenu(getPlayer(), shop)));

        long revenue = shop.getLifetimeRevenue();
        long sales = shop.getLifetimeSales();
        long productsSold = shop.getLifetimeProductsSold();
        long impressions = shop.getLifetimeImpressions();
        long stockAdded = shop.getLifetimeStockAdded();
        long stockRemoved = shop.getLifetimeStockRemoved();
        long stockNet = shop.getNetStockChange();
        long internalStock = shop.getCurrentInternalStockAmount();

        long avgRevenuePerSale = sales <= 0 ? 0 : revenue / sales;
        long avgItemsPerSale = sales <= 0 ? 0 : productsSold / sales;
        long stockUtilization = stockAdded <= 0 ? 0 : (productsSold * 100L) / stockAdded;
        long conversionRate = impressions <= 0 ? 0 : (sales * 100L) / impressions;

        List<CoreProtectLogger.HistoryEntry> realBuys = shop.getHistoryEntries().stream()
                .filter(ShopAnalyticsMenu::isRecordedPurchase)
                .toList();
        Set<String> uniqueBuyers = realBuys.stream()
                .map(CoreProtectLogger.HistoryEntry::actor)
                .filter(actor -> actor != null && !actor.isEmpty())
                .collect(Collectors.toSet());
        Map<String, Long> topBuyers = realBuys.stream()
                .collect(Collectors.groupingBy(
                        CoreProtectLogger.HistoryEntry::actor,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        addButton(infoButton(MenuSlotRegistry.slot("shop-analytics", "revenue", 10),
                GuiIcons.material("shop-analytics.revenue", Material.EMERALD), "&a&lLifetime Revenue",
                "&7Total: &a" + fmt(revenue)));
        addButton(infoButton(MenuSlotRegistry.slot("shop-analytics", "sales", 11),
                GuiIcons.material("shop-analytics.sales", Material.GOLD_INGOT), "&6&lTotal Sales",
                "&7Count: &e" + fmt(sales)));
        addButton(infoButton(MenuSlotRegistry.slot("shop-analytics", "products-sold", 12),
                GuiIcons.material("shop-analytics.products-sold", Material.CHEST), "&e&lProducts Sold",
                "&7Units: &f" + fmt(productsSold)));
        addButton(infoButton(MenuSlotRegistry.slot("shop-analytics", "impressions", 13),
                GuiIcons.material("shop-analytics.impressions", Material.ENDER_EYE), "&d&lTotal Impressions",
                "&7Views: &d" + fmt(impressions)));

        addButton(infoButton(MenuSlotRegistry.slot("shop-analytics", "stock-added", 14),
                GuiIcons.material("shop-analytics.stock-added", Material.LIME_DYE), "&a&lStock Added",
                "&7Units: &a+" + fmt(stockAdded)));
        addButton(infoButton(MenuSlotRegistry.slot("shop-analytics", "stock-removed", 15),
                GuiIcons.material("shop-analytics.stock-removed", Material.RED_DYE), "&c&lStock Removed",
                "&7Units: &c-" + fmt(stockRemoved)));
        addButton(infoButton(MenuSlotRegistry.slot("shop-analytics", "internal-stock", 16),
                GuiIcons.material("shop-analytics.internal-stock", Material.HOPPER), "&b&lCurrent Internal Stock",
                "&7Units: &b" + fmt(internalStock)));

        addButton(infoButton(MenuSlotRegistry.slot("shop-analytics", "avg-revenue", 28),
                GuiIcons.material("shop-analytics.avg-revenue", Material.PAPER), "&f&lAvg Revenue / Sale",
                "&7Amount: &f" + fmt(avgRevenuePerSale)));
        addButton(infoButton(MenuSlotRegistry.slot("shop-analytics", "avg-items", 29),
                GuiIcons.material("shop-analytics.avg-items", Material.BOOK), "&f&lAvg Items / Sale",
                "&7Items: &f" + fmt(avgItemsPerSale)));
        addButton(infoButton(MenuSlotRegistry.slot("shop-analytics", "utilization", 30),
                GuiIcons.material("shop-analytics.utilization", Material.CLOCK), "&d&lStock Utilization",
                "&7Sold vs Added: &d" + fmt(stockUtilization) + "%"));
        addButton(infoButton(MenuSlotRegistry.slot("shop-analytics", "conversion", 31),
                GuiIcons.material("shop-analytics.conversion", Material.TARGET), "&a&lImpression -> Buy Ratio",
                "&7Conversion: &a" + fmt(conversionRate) + "%"));

        String netColor = stockNet >= 0 ? "&a+" : "&c";
        addButton(infoButton(MenuSlotRegistry.slot("shop-analytics", "net-stock", 32),
                GuiIcons.material("shop-analytics.net-stock", Material.COMPARATOR), "&9&lNet Stock Change",
                "&7Net: " + netColor + fmt(stockNet)));
        addButton(infoButton(MenuSlotRegistry.slot("shop-analytics", "unique-buyers", 33),
                GuiIcons.material("shop-analytics.unique-buyers", Material.PLAYER_HEAD), "&b&lUnique Buyers",
                "&7Players: &b" + fmt(uniqueBuyers.size())));
        addButton(infoButton(MenuSlotRegistry.slot("shop-analytics", "top-buyers", 34),
                GuiIcons.material("shop-analytics.top-buyers", Material.WRITABLE_BOOK), "&6&lTop Buyers",
                buildTopBuyerLore(topBuyers)));
        addButton(new MarketComparisonButton(MenuSlotRegistry.slot("shop-analytics", "market-compare", 40), shop, buildMarketComparisonLore(shop)));
    }

    @Override
    protected void onAfterPlaceButtons() {
        paintRegisteredLayoutFillers("shop-analytics");
    }

    @Override
    protected boolean enforceManagementAccess() {
        return true;
    }

    /** Classic GUI buys {@code SHOP_BUY}; merchant UI {@code SHOP_BUY_VILLAGER}; admin emulate {@code SHOP_BUY_EMULATED}. */
    private static boolean isRecordedPurchase(CoreProtectLogger.HistoryEntry entry) {
        String a = entry.action();
        return "SHOP_BUY".equals(a) || "SHOP_BUY_VILLAGER".equals(a) || "SHOP_BUY_EMULATED".equals(a);
    }

    private Button infoButton(int slot, Material icon, String title, String line) {
        return infoButton(slot, icon, title, Arrays.asList(line));
    }

    private Button infoButton(int slot, Material icon, String title, List<String> lines) {
        return new Button(slot) {
            private final ItemStack item = createItem(
                    Utils.colorize(title),
                    lines.stream().map(Utils::colorize).toList(),
                    icon,
                    false
            );

            @Override
            public ItemStack getItem() {
                return item;
            }

            @Override
            protected void onClick(org.bukkit.event.inventory.InventoryClickEvent event) {
                event.setCancelled(true);
            }
        };
    }

    private List<String> buildTopBuyerLore(Map<String, Long> topBuyers) {
        if (topBuyers.isEmpty()) {
            return Arrays.asList("&7No buyer data yet.");
        }
        List<String> lore = new java.util.ArrayList<>();
        int rank = 1;
        for (Map.Entry<String, Long> entry : topBuyers.entrySet()) {
            lore.add("&7#" + rank + " &f" + entry.getKey() + ": &e" + fmt(entry.getValue()) + " buys");
            rank++;
        }
        return lore;
    }

    private List<String> buildMarketComparisonLore(ItemShop shop) {
        List<ItemStack> targetProducts = shop.getProducts();
        if (targetProducts.isEmpty()) {
            return Arrays.asList("&7No products set for comparison.");
        }
        List<String> targetKeys = targetProducts.stream()
                .filter(item -> item != null)
                .map(this::itemKey)
                .distinct()
                .toList();
        if (targetKeys.isEmpty()) {
            return Arrays.asList("&7No comparable market data.");
        }

        int comparableShops = 0;
        long totalSales = 0;
        int minPrice = Integer.MAX_VALUE;
        int sumPrice = 0;
        int maxPrice = Integer.MIN_VALUE;

        for (Location location : NormalShops.getInstance().getShopManager().getUUIDMap().keySet()) {
            ItemShop candidate = ItemShop.get(location);
            if (candidate == null || candidate.isDeleted()) continue;
            if (candidate.getLocation().equals(shop.getLocation())) continue;

            boolean sellsAnyTarget = candidate.getProducts().stream()
                    .filter(item -> item != null)
                    .map(this::itemKey)
                    .anyMatch(targetKeys::contains);
            if (!sellsAnyTarget) continue;

            comparableShops++;
            int candidatePrice = candidate.getPrice().getAmount();
            minPrice = Math.min(minPrice, candidatePrice);
            maxPrice = Math.max(maxPrice, candidatePrice);
            sumPrice += candidatePrice;
            totalSales += candidate.getLifetimeSales();
        }

        if (comparableShops == 0) {
            return Arrays.asList("&7No shops selling your products found.");
        }

        int avgPrice = sumPrice / comparableShops;
        List<String> lore = new ArrayList<>();
        lore.add("&7Comparable shops: &d" + fmt(comparableShops));
        lore.add("&7Market price range: &d" + minPrice + " - " + maxPrice);
        lore.add("&7Average market price: &d" + avgPrice);
        lore.add("&7Competitor total sales: &b" + fmt(totalSales));
        return lore;
    }

    private String itemKey(ItemStack item) {
        if (item == null) return "null";
        String displayName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName()
                : item.getType().name();
        return item.getType().name() + "|" + displayName.toLowerCase(Locale.US);
    }

    private class MarketComparisonButton extends Button {
        private final ItemShop shop;
        private final List<String> lore;

        private MarketComparisonButton(int slot, ItemShop shop, List<String> lore) {
            super(slot);
            this.shop = shop;
            this.lore = lore;
        }

        @Override
        public ItemStack getItem() {
            List<String> fullLore = new ArrayList<>(lore);
            fullLore.add("&8 ");
            fullLore.add("&dClick for detailed competitor list");
            return createItem(Utils.colorize("&d&lMarket Comparison"), fullLore.stream().map(Utils::colorize).toList(),
                    Material.SPYGLASS, false);
        }

        @Override
        protected void onClick(org.bukkit.event.inventory.InventoryClickEvent event) {
            event.setCancelled(true);
            new ShopMarketComparisonMenu((Player) event.getWhoClicked(), shop).open();
        }
    }

    private String fmt(long value) {
        return String.format(Locale.US, "%,d", value);
    }
}
