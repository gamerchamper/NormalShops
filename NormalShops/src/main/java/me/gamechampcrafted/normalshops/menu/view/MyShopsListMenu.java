package me.gamechampcrafted.normalshops.menu.view;

import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.menu.Button;
import me.gamechampcrafted.normalshops.menu.GuiDisplayItem;
import me.gamechampcrafted.normalshops.menu.GuiIcons;
import me.gamechampcrafted.normalshops.menu.Menu;
import me.gamechampcrafted.normalshops.menu.MenuColor;
import me.gamechampcrafted.normalshops.menu.MenuSlotRegistry;
import me.gamechampcrafted.normalshops.menu.edit.ShopAccessChoiceMenu;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Paginated list of shops the player owns or is trusted on; opens {@link ShopAccessChoiceMenu} for remote management.
 */
public class MyShopsListMenu extends Menu {

    /** Shop icons — rows 0–3 (slots 0–35). Bottom row 36–44 is pagination ({@link Menu#MAX_SIZE}). */
    private static final int PAGE_SIZE = 36;
    private static final List<Integer> DEFAULT_SHOP_ENTRY_SLOTS =
            IntStream.range(0, PAGE_SIZE).boxed().collect(Collectors.toList());
    private static final int BOTTOM_ROW_FIRST = 36;
    private static final int BOTTOM_ROW_LAST = 44;

    private static int prevSlot() {
        return MenuSlotRegistry.slot("my-shops", "prev-page", 36);
    }

    private static int infoSlot() {
        return MenuSlotRegistry.slot("my-shops", "pagination-info", 40);
    }

    private static int nextSlot() {
        return MenuSlotRegistry.slot("my-shops", "next-page", 44);
    }

    private final List<ItemShop> allShops;
    private final int page;

    public MyShopsListMenu(Player player, List<ItemShop> allShops, int page) {
        super(player, player.getLocation(), Menu.MAX_SIZE, new ShopsListClickHandler(), MenuColor.BLACK, buildTitle(allShops, page));
        this.allShops = allShops;
        this.page = Math.max(0, page);
    }

    private static String buildTitle(List<ItemShop> allShops, int page) {
        int totalPages = Math.max(1, (int) Math.ceil((double) allShops.size() / PAGE_SIZE));
        int currentPage = Math.min(Math.max(0, page), totalPages - 1);
        return Message.MENU_MY_SHOPS.toString() + ChatColor.DARK_GRAY + " [" + (currentPage + 1) + "/" + totalPages + "]";
    }

    @Override
    protected void setupButtons() {
        int totalPages = Math.max(1, (int) Math.ceil((double) allShops.size() / PAGE_SIZE));
        int currentPage = Math.min(page, totalPages - 1);
        int start = currentPage * PAGE_SIZE;

        List<Integer> shopSlots = MenuSlotRegistry.slots("my-shops", "shop-entry", DEFAULT_SHOP_ENTRY_SLOTS);
        for (int i = 0; i < PAGE_SIZE && start + i < allShops.size(); i++) {
            addButton(new ShopEntryButton(shopSlots.get(i), getPlayer(), allShops.get(start + i)));
        }

        addButton(navButton(prevSlot(),
                GuiIcons.material("my-shops.nav-enabled", Material.ARROW),
                ChatColor.YELLOW + "Previous Page",
                currentPage > 0,
                e -> new MyShopsListMenu(getPlayer(), allShops, currentPage - 1).open()));

        int showingFrom = allShops.isEmpty() ? 0 : start + 1;
        int showingTo = Math.min(allShops.size(), start + PAGE_SIZE);
        String rangeInfo = allShops.isEmpty()
                ? ChatColor.GRAY + "No shops"
                : ChatColor.GOLD + String.valueOf(showingFrom) + ChatColor.DARK_GRAY + " – "
                + ChatColor.GOLD + String.valueOf(showingTo) + ChatColor.GRAY + " / " + allShops.size();
        addButton(navButton(infoSlot(),
                GuiIcons.material("my-shops.info", Material.BOOK),
                ChatColor.GOLD + "Page " + (currentPage + 1) + "/" + totalPages,
                List.of(rangeInfo),
                true, null));

        addButton(navButton(nextSlot(),
                GuiIcons.material("my-shops.nav-enabled", Material.ARROW),
                ChatColor.YELLOW + "Next Page",
                currentPage + 1 < totalPages,
                e -> new MyShopsListMenu(getPlayer(), allShops, currentPage + 1).open()));

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

    private Button navButton(int slot, Material mat, String title, boolean enabled, java.util.function.Consumer<InventoryClickEvent> onClick) {
        return navButton(slot, mat, title, List.of(), enabled, onClick);
    }

    /** {@code enabled} only gates clicks — icon is always {@code mat}. */
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

    private static class ShopEntryButton extends Button {
        private final Player menuPlayer;
        private final ItemShop shop;

        ShopEntryButton(int slot, Player menuPlayer, ItemShop shop) {
            super(slot);
            this.menuPlayer = menuPlayer;
            this.shop = shop;
        }

        @Override
        public ItemStack getItem() {
            Block block = shop.getLocation().getBlock();
            Material mat = block.getType();
            if (mat == Material.AIR || !mat.isItem()) {
                mat = Material.CHEST;
            }
            ItemStack icon = new ItemStack(Material.PAPER);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(displayTitle());
                meta.setLore(loreLines());
                icon.setItemMeta(meta);
            }
            GuiDisplayItem.applyVanillaItemModelToPaper(icon, mat.getKey());
            return icon;
        }

        private String displayTitle() {
            if (shop.hasCustomName() && shop.getCustomName() != null && !shop.getCustomName().isEmpty()) {
                return Utils.colorize("&f" + shop.getCustomName());
            }
            Location loc = shop.getLocation();
            String w = loc.getWorld() != null ? loc.getWorld().getName() : "?";
            return Utils.colorize("&fShop &8(" + w + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + ")");
        }

        private List<String> loreLines() {
            List<String> lore = new ArrayList<>();
            lore.add(Utils.colorize("&7" + Utils.formatLocation(shop.getLocation())));
            if (!shop.isOwner(menuPlayer)) {
                String ownerName = shop.getOwnerName();
                lore.add(Utils.colorize("&6Trusted on &f" + (ownerName != null ? ownerName : "unknown") + "&6's shop"));
            }
            if (!shop.getProducts().isEmpty() && shop.getProducts().get(0) != null) {
                lore.add(Utils.colorize("&7Sells: &f" + Utils.formatItemWithAmount(shop.getProducts().get(0))));
            }
            if (shop.isAdminShop()) {
                lore.add(Utils.colorize("&bStock: &funlimited &8(admin)"));
            } else if (shop.hasStock()) {
                lore.add(Message.MENU_SHOP_OPEN.toString());
            } else {
                lore.add(Message.MENU_SHOP_CLOSED.toString());
            }
            lore.add(Utils.colorize("&aClick to open"));
            return lore;
        }

        @Override
        protected void onClick(InventoryClickEvent event) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            ItemShop fresh = ItemShop.get(shop.getLocation());
            if (fresh == null || fresh.isDeleted()) {
                Message.SHOP_ERROR.send(player);
                player.closeInventory();
                return;
            }
            if (!fresh.isOwner(player) && !fresh.isTrusted(player)) {
                Message.VIEW_SHOPS_CANNOT_OPEN.send(player);
                player.closeInventory();
                return;
            }
            player.closeInventory();
            Bukkit.getScheduler().runTask(NormalShops.getInstance(), () -> {
                new ShopAccessChoiceMenu(player, fresh).open();
                player.playSound(player, Sound.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, .5f, .8f);
            });
        }
    }
}
