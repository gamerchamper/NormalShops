package me.gamechampcrafted.normalshops.menu.edit;

import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.menu.BackButton;
import me.gamechampcrafted.normalshops.menu.Button;
import me.gamechampcrafted.normalshops.menu.ClickHandler;
import me.gamechampcrafted.normalshops.menu.MenuColor;
import me.gamechampcrafted.normalshops.menu.ShopMenu;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ShopMarketComparisonMenu extends ShopMenu {

    private static final int MAX_PRODUCT_LORE_LINES = 8;

    private static final List<Integer> LIST_SLOTS = List.of(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    );
    private static final int PAGE_SIZE = LIST_SLOTS.size();
    private final int page;

    public ShopMarketComparisonMenu(Player player, ItemShop shop) {
        this(player, shop, 0);
    }

    public ShopMarketComparisonMenu(Player player, ItemShop shop, int page) {
        super(player, shop, 45, new ClickHandler(), MenuColor.BLUE, Utils.colorize("&dMarket Competitors"));
        this.page = Math.max(0, page);
    }

    @Override
    protected void setupButtons() {
        ItemShop shop = getShop();
        addButton(new BackButton(36, new ShopAnalyticsMenu(getPlayer(), shop)));

        List<ComparableShop> comparable = getComparableShops(shop);
        comparable.sort(Comparator
                .comparingLong(ComparableShop::lifetimeSales).reversed()
                .thenComparingInt(ComparableShop::priceAmount));

        int totalPages = Math.max(1, (int) Math.ceil((double) comparable.size() / PAGE_SIZE));
        int currentPage = Math.min(page, totalPages - 1);
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, comparable.size());

        for (int i = start; i < end; i++) {
            addButton(new CompetitorButton(LIST_SLOTS.get(i - start), comparable.get(i), shop));
        }

        addButton(pageControl(40, Material.BOOK, "&bPage " + (currentPage + 1) + "/" + totalPages, null));
        if (currentPage > 0) {
            addButton(pageControl(37, Material.ARROW, "&ePrevious Page",
                    event -> new ShopMarketComparisonMenu(getPlayer(), shop, currentPage - 1).open()));
        }
        if (currentPage + 1 < totalPages) {
            addButton(pageControl(43, Material.ARROW, "&eNext Page",
                    event -> new ShopMarketComparisonMenu(getPlayer(), shop, currentPage + 1).open()));
        }
    }

    @Override
    protected boolean enforceManagementAccess() {
        return true;
    }

    private List<ComparableShop> getComparableShops(ItemShop currentShop) {
        List<ComparableShop> list = new ArrayList<>();
        List<ItemStack> targetProducts = currentShop.getProducts();

        for (Location location : NormalShops.getInstance().getShopManager().getUUIDMap().keySet()) {
            ItemShop candidate = ItemShop.get(location);
            if (candidate == null || candidate.isDeleted()) continue;
            if (candidate.getLocation().equals(currentShop.getLocation())) continue;

            Set<String> matchedProducts = new LinkedHashSet<>();
            for (ItemStack target : targetProducts) {
                if (target == null) continue;
                String targetKey = itemKey(target);
                boolean match = candidate.getProducts().stream()
                        .filter(item -> item != null)
                        .anyMatch(item -> itemKey(item).equals(targetKey));
                if (match) {
                    matchedProducts.add(Utils.getFormattedName(target));
                }
            }
            if (matchedProducts.isEmpty()) continue;

            int priceDeltaVsYours = candidate.getPrice().getAmount() - currentShop.getPrice().getAmount();
            String customName = candidate.hasCustomName() ? candidate.getCustomName() : null;

            list.add(new ComparableShop(
                    candidate.getOwnerName() == null ? "unknown" : candidate.getOwnerName(),
                    candidate.getLocation(),
                    candidate.getPrice().getAmount(),
                    Utils.formatItemWithAmount(candidate.getPrice()),
                    candidate.getLifetimeSales(),
                    candidate.getLifetimeRevenue(),
                    candidate.getLifetimeProductsSold(),
                    candidate.getLifetimeImpressions(),
                    candidate.hasStock(),
                    candidate.isAdminShop(),
                    customName,
                    distanceLabel(currentShop.getLocation(), candidate.getLocation()),
                    priceDeltaVsYours,
                    new ArrayList<>(matchedProducts)
            ));
        }
        return list;
    }

    private static String distanceLabel(Location from, Location to) {
        if (from.getWorld() == null || to.getWorld() == null) {
            return "?";
        }
        if (!from.getWorld().equals(to.getWorld())) {
            return "Other world";
        }
        double d = from.distance(to);
        if (d >= 1000.0) {
            return String.format(Locale.US, "%.1fk blocks", d / 1000.0);
        }
        return String.format(Locale.US, "%.0f blocks", d);
    }

    private String itemKey(ItemStack item) {
        if (item == null) return "null";
        String displayName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName()
                : item.getType().name();
        return item.getType().name() + "|" + displayName.toLowerCase(Locale.US);
    }

    private static class CompetitorButton extends Button {
        private final ComparableShop data;
        private final ItemShop referenceShop;

        private CompetitorButton(int slot, ComparableShop data, ItemShop referenceShop) {
            super(slot);
            this.data = data;
            this.referenceShop = referenceShop;
        }

        @Override
        public ItemStack getItem() {
            String title = (data.customName() != null && !data.customName().isBlank())
                    ? "&f&l" + data.customName()
                    : "&dCompetitor Shop";

            List<String> lore = new ArrayList<>();
            lore.add(Utils.colorize("&7Owner: &f" + data.ownerName()));
            if (data.adminShop()) {
                lore.add(Utils.colorize("&bAdmin shop"));
            }
            lore.add(Utils.colorize("&7Price (per purchase): &e" + data.priceBundleFormatted()));
            lore.add(Utils.colorize(priceDeltaLine(data.priceDeltaVsYours())));
            lore.add(Utils.colorize("&7Lifetime revenue: &a" + fmt(data.lifetimeRevenue())));
            lore.add(Utils.colorize("&7Sales (checkouts): &b" + fmt(data.lifetimeSales())));
            lore.add(Utils.colorize("&7Units sold: &f" + fmt(data.lifetimeProductsSold())));
            lore.add(Utils.colorize("&7Shop views: &d" + fmt(data.lifetimeImpressions())));
            lore.add(Utils.colorize(stockStatusLine(data)));
            lore.add(Utils.colorize("&7Distance: &f" + data.distanceLabel()));
            lore.add(Utils.colorize("&7Your price: &e" + Utils.formatItemWithAmount(referenceShop.getPrice())));
            lore.add(Utils.colorize("&7Their coords: &f" + Utils.formatLocation(data.location())));
            lore.add(Utils.colorize("&8 "));
            lore.add(Utils.colorize("&7Overlapping products:"));
            List<String> products = data.matchedProducts();
            int shown = Math.min(MAX_PRODUCT_LORE_LINES, products.size());
            for (int i = 0; i < shown; i++) {
                lore.add(Utils.colorize("&8 - &7" + products.get(i)));
            }
            if (products.size() > MAX_PRODUCT_LORE_LINES) {
                int more = products.size() - MAX_PRODUCT_LORE_LINES;
                lore.add(Utils.colorize("&8 - &7... +" + more + " more"));
            }

            Material icon = pickIcon(data);
            return createItem(Utils.colorize(title), lore, icon, false);
        }

        private static String priceDeltaLine(int delta) {
            if (delta == 0) {
                return "&7vs your price: &eSame bundle cost";
            }
            int abs = Math.abs(delta);
            if (delta < 0) {
                return "&7vs your price: &aThey charge &f" + fmt(abs) + " &aless per bundle";
            }
            return "&7vs your price: &cThey charge &f" + fmt(abs) + " &cmore per bundle";
        }

        private static String stockStatusLine(ComparableShop data) {
            if (data.adminShop()) {
                return "&7Stock: &bUnlimited (admin)";
            }
            return data.hasStock()
                    ? "&7Stock: &aIn stock"
                    : "&7Stock: &cOut of stock";
        }

        private static Material pickIcon(ComparableShop data) {
            if (data.adminShop()) {
                return Material.NETHER_STAR;
            }
            return data.hasStock() ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
        }

        private static String fmt(long value) {
            return String.format(Locale.US, "%,d", value);
        }

        @Override
        protected void onClick(org.bukkit.event.inventory.InventoryClickEvent event) {
            event.setCancelled(true);
        }
    }

    private Button pageControl(int slot, Material icon, String title, java.util.function.Consumer<InventoryClickEvent> onClick) {
        return new Button(slot) {
            @Override
            public ItemStack getItem() {
                return createItem(Utils.colorize(title), List.of(), icon, false);
            }

            @Override
            protected void onClick(InventoryClickEvent event) {
                event.setCancelled(true);
                if (onClick != null) {
                    onClick.accept(event);
                }
            }
        };
    }

    private record ComparableShop(
            String ownerName,
            Location location,
            int priceAmount,
            String priceBundleFormatted,
            long lifetimeSales,
            long lifetimeRevenue,
            long lifetimeProductsSold,
            long lifetimeImpressions,
            boolean hasStock,
            boolean adminShop,
            String customName,
            String distanceLabel,
            int priceDeltaVsYours,
            List<String> matchedProducts
    ) {
    }
}
