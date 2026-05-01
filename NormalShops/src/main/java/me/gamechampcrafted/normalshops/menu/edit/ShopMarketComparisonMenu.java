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
            addButton(new CompetitorButton(LIST_SLOTS.get(i - start), comparable.get(i)));
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

            list.add(new ComparableShop(
                    candidate.getOwnerName() == null ? "unknown" : candidate.getOwnerName(),
                    candidate.getLocation(),
                    candidate.getPrice().getAmount(),
                    Utils.getFormattedName(candidate.getPrice()),
                    candidate.getLifetimeSales(),
                    new ArrayList<>(matchedProducts)
            ));
        }
        return list;
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

        private CompetitorButton(int slot, ComparableShop data) {
            super(slot);
            this.data = data;
        }

        @Override
        public ItemStack getItem() {
            List<String> lore = new ArrayList<>();
            lore.add(Utils.colorize("&7Owner: &f" + data.ownerName()));
            lore.add(Utils.colorize("&7Price: &e" + data.priceAmount() + " " + data.priceName()));
            lore.add(Utils.colorize("&7Lifetime sales: &b" + data.lifetimeSales()));
            lore.add(Utils.colorize("&7Location: &f" + Utils.formatLocation(data.location())));
            lore.add(Utils.colorize("&7Matched products:"));
            for (String product : data.matchedProducts()) {
                lore.add(Utils.colorize("&8 - &7" + product));
            }
            return createItem(Utils.colorize("&dCompetitor Shop"), lore, Material.PAPER, false);
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
            String priceName,
            long lifetimeSales,
            List<String> matchedProducts
    ) {
    }
}
