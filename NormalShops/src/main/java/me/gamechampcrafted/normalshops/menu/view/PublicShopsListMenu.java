package me.gamechampcrafted.normalshops.menu.view;

import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.Setting;
import me.gamechampcrafted.normalshops.menu.Button;
import me.gamechampcrafted.normalshops.menu.GuiIcons;
import me.gamechampcrafted.normalshops.menu.GuiDisplayItem;
import me.gamechampcrafted.normalshops.menu.Menu;
import me.gamechampcrafted.normalshops.menu.MenuColor;
import me.gamechampcrafted.normalshops.menu.MenuSlotRegistry;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Paginated browse menu: 36 shop slots + bottom row (standard 45-slot chest; pagination always visible).
 */
public class PublicShopsListMenu extends Menu {

    /** Shop icons — rows 0–3 (slots 0–35). Bottom row 36–44 is pagination. */
    private static final int PAGE_SIZE = 36;
    private static final List<Integer> DEFAULT_SHOP_ENTRY_SLOTS =
            IntStream.range(0, PAGE_SIZE).boxed().collect(Collectors.toList());
    private static final int BOTTOM_ROW_FIRST = 36;
    private static final int BOTTOM_ROW_LAST = 44;
    private static int prevSlot() {
        return MenuSlotRegistry.slot("public-shops", "prev-page", 36);
    }

    private static int infoSlot() {
        return MenuSlotRegistry.slot("public-shops", "pagination-info", 40);
    }

    private static int nextSlot() {
        return MenuSlotRegistry.slot("public-shops", "next-page", 44);
    }

    private final List<ItemShop> shops;
    private final int page;
    /** Optional subtitle after page indicator (e.g. item search summary). */
    private final String menuSubtitle;

    public PublicShopsListMenu(Player player, List<ItemShop> shops, int page) {
        this(player, shops, page, null);
    }

    public PublicShopsListMenu(Player player, List<ItemShop> shops, int page, String menuSubtitle) {
        super(player, player.getLocation(), Menu.MAX_SIZE, new PublicShopsListClickHandler(), MenuColor.BLACK,
                buildTitle(shops, page, menuSubtitle));
        this.shops = shops;
        this.page = Math.max(0, page);
        this.menuSubtitle = menuSubtitle;
    }

    private static String buildTitle(List<ItemShop> shops, int page, String menuSubtitle) {
        int totalPages = Math.max(1, (int) Math.ceil((double) shops.size() / PAGE_SIZE));
        int currentPage = Math.min(Math.max(0, page), totalPages - 1);
        String base = Message.MENU_PUBLIC_SHOPS.toString() + ChatColor.DARK_GRAY + " [" + (currentPage + 1) + "/" + totalPages + "]";
        if (menuSubtitle != null && !menuSubtitle.isBlank()) {
            base += ChatColor.DARK_GRAY + " · " + ChatColor.GRAY + menuSubtitle;
        }
        return base;
    }

    @Override
    protected void setupButtons() {
        int totalPages = Math.max(1, (int) Math.ceil((double) shops.size() / PAGE_SIZE));
        int currentPage = Math.min(page, totalPages - 1);
        int start = currentPage * PAGE_SIZE;

        List<Integer> shopSlots = MenuSlotRegistry.slots("public-shops", "shop-entry", DEFAULT_SHOP_ENTRY_SLOTS);
        for (int i = 0; i < PAGE_SIZE && start + i < shops.size(); i++) {
            addButton(new PublicShopEntryButton(shopSlots.get(i), shops.get(start + i)));
        }

        addButton(navButton(prevSlot(),
                GuiIcons.material("public-shops.nav-enabled", Material.ARROW),
                ChatColor.YELLOW + "Previous Page",
                currentPage > 0,
                e -> new PublicShopsListMenu(getPlayer(), shops, currentPage - 1, menuSubtitle).open()));

        int showingFrom = shops.isEmpty() ? 0 : start + 1;
        int showingTo = Math.min(shops.size(), start + PAGE_SIZE);
        String rangeInfo = shops.isEmpty()
                ? ChatColor.GRAY + "No shops"
                : ChatColor.GOLD + String.valueOf(showingFrom) + ChatColor.DARK_GRAY + " – "
                + ChatColor.GOLD + String.valueOf(showingTo) + ChatColor.GRAY + " / " + shops.size();
        addButton(navButton(infoSlot(),
                GuiIcons.material("public-shops.info", Material.BOOK),
                ChatColor.GOLD + "Page " + (currentPage + 1) + "/" + totalPages,
                List.of(rangeInfo),
                true, null));

        addButton(navButton(nextSlot(),
                GuiIcons.material("public-shops.nav-enabled", Material.ARROW),
                ChatColor.YELLOW + "Next Page",
                currentPage + 1 < totalPages,
                e -> new PublicShopsListMenu(getPlayer(), shops, currentPage + 1, menuSubtitle).open()));

        for (int s = BOTTOM_ROW_FIRST; s <= BOTTOM_ROW_LAST; s++) {
            if (s == prevSlot() || s == infoSlot() || s == nextSlot()) {
                continue;
            }
            addButton(glass(s));
        }
    }

    private static Button glass(int slot) {
        return new Button(slot) {
            @Override
            public ItemStack getItem() {
                Material paneMat = GuiIcons.material("global.pagination-glass", Material.GRAY_STAINED_GLASS_PANE);
                return GuiDisplayItem.paperIconWithMeta(paneMat, " ", List.of());
            }

            @Override
            protected void onClick(InventoryClickEvent event) {
                event.setCancelled(true);
            }
        };
    }

    private Button navButton(int slot, Material mat, String title, boolean enabled,
                             java.util.function.Consumer<InventoryClickEvent> onClick) {
        return navButton(slot, mat, title, List.of(), enabled, onClick);
    }

    /**
     * {@code enabled} only gates clicks — icon is always {@code mat}. (Using glass when disabled hid the
     * info BOOK because it was non-clickable, and turned arrows into panes on the first/last page.)
     */
    private Button navButton(int slot, Material mat, String title, List<String> lore, boolean enabled,
                             java.util.function.Consumer<InventoryClickEvent> onClick) {
        return new Button(slot) {
            @Override
            public ItemStack getItem() {
                return GuiDisplayItem.paperIconWithMeta(mat, title, lore.isEmpty() ? List.of() : lore);
            }

            @Override
            protected void onClick(InventoryClickEvent event) {
                event.setCancelled(true);
                if (!enabled || onClick == null) {
                    return;
                }
                onClick.accept(event);
            }
        };
    }

    private static class PublicShopEntryButton extends Button {
        private final ItemShop shop;

        PublicShopEntryButton(int slot, ItemShop shop) {
            super(slot);
            this.shop = shop;
        }

        @Override
        public ItemStack getItem() {
            ItemStack icon = barePaperStackForProduct();
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(displayTitle());
                meta.setLore(loreLines());
                icon.setItemMeta(meta);
            }
            ItemStack first = firstProduct();
            NamespacedKey modelKey = first != null && first.getType() != Material.AIR
                    ? first.getType().getKey()
                    : Material.CHEST.getKey();
            GuiDisplayItem.applyVanillaItemModelToPaper(icon, modelKey);
            return icon;
        }

        /** Amount-only {@link Material#PAPER}; {@link GuiDisplayItem#applyVanillaItemModelToPaper} runs after lore. */
        private ItemStack barePaperStackForProduct() {
            List<ItemStack> products = shop.getProducts();
            if (products != null) {
                for (ItemStack p : products) {
                    if (p != null && p.getType() != Material.AIR) {
                        int amt = Math.max(1, Math.min(64, p.getAmount()));
                        return new ItemStack(Material.PAPER, amt);
                    }
                }
            }
            return new ItemStack(Material.PAPER, 1);
        }

        private String displayTitle() {
            if (shop.hasCustomName() && shop.getCustomName() != null && !shop.getCustomName().isEmpty()) {
                return Utils.colorize("&f" + shop.getCustomName());
            }
            ItemStack first = firstProduct();
            if (first != null) {
                return Utils.colorize("&f" + Utils.formatItemWithAmount(first));
            }
            return Utils.colorize("&fShop");
        }

        private ItemStack firstProduct() {
            List<ItemStack> products = shop.getProducts();
            if (products == null) {
                return null;
            }
            for (ItemStack p : products) {
                if (p != null && p.getType() != Material.AIR) {
                    return p;
                }
            }
            return null;
        }

        private List<String> loreLines() {
            List<String> lore = new ArrayList<>();
            String owner = shop.getOwnerName();
            lore.add(Utils.colorize("&7Owner: &f" + (owner != null ? owner : "Unknown")));
            lore.add(Utils.colorize("&7Price: &f" + formatDeal()));
            Location loc = shop.getLocation();
            if (loc.getWorld() != null) {
                lore.add(Utils.colorize("&7World: &f" + loc.getWorld().getName()));
            } else {
                lore.add(Utils.colorize("&7World: &c(unloaded)"));
            }
            lore.add(Utils.colorize("&7Location: &f" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()));
            lore.add(Utils.colorize("&aClick to teleport"));
            return lore;
        }

        private String formatDeal() {
            ItemStack price = shop.getPrice();
            String pricePart = price != null && price.getType() != Material.AIR
                    ? Utils.formatItemWithAmount(price)
                    : "?";
            String productsPart = formatProductsList();
            return pricePart + " &7for &f" + productsPart;
        }

        private String formatProductsList() {
            List<ItemStack> products = shop.getProducts();
            if (products == null || products.isEmpty()) {
                return "?";
            }
            List<String> parts = new ArrayList<>();
            for (ItemStack p : products) {
                if (p != null && p.getType() != Material.AIR) {
                    parts.add(Utils.formatItemWithAmount(p));
                }
            }
            return parts.isEmpty() ? "?" : String.join("&7, &f", parts);
        }

        @Override
        protected void onClick(InventoryClickEvent event) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            int cdSec = Setting.SHOPS_MENU_TELEPORT_COOLDOWN_SECONDS.getInt();
            long remaining = ShopBrowseTeleport.remainingCooldownMs(player, cdSec);
            if (remaining > 0) {
                long secs = (remaining + 999L) / 1000L;
                Message.SHOPS_MENU_TELEPORT_COOLDOWN.parameterizer()
                        .put("seconds", secs)
                        .send(player);
                return;
            }
            ItemShop fresh = ItemShop.get(shop.getLocation());
            if (fresh == null || fresh.isDeleted()) {
                Message.SHOP_ERROR.send(player);
                player.closeInventory();
                return;
            }
            if (!fresh.hasStock()) {
                Message.BUY_NO_STOCK.send(player);
                player.closeInventory();
                return;
            }
            if (!ShopsMenuRegions.allowsShopAt(fresh.getLocation())) {
                Message.SHOPS_MENU_REGION_BLOCKED.send(player);
                player.closeInventory();
                return;
            }
            Location dest = ShopBrowseTeleport.standLocationInFront(fresh);
            if (dest.getWorld() == null || Bukkit.getWorld(dest.getWorld().getUID()) == null) {
                Message.SHOPS_MENU_WORLD_UNAVAILABLE.send(player);
                player.closeInventory();
                return;
            }
            player.closeInventory();
            Bukkit.getScheduler().runTask(NormalShops.getInstance(), () -> {
                player.teleport(dest);
                ShopBrowseTeleport.markTeleport(player);
                Message.SHOPS_MENU_TELEPORTED.send(player);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.4f, 1.2f);
            });
        }
    }
}
