package me.gamechampcrafted.normalshops.menu;

import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.data.Message;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class Menu {

    /** Slots where price + products are edited (create / change listing). From {@code config.yml} {@code shop-gui}. */
    public static List<Integer> editableSlots() {
        return ShopGuiLayout.get().editableSlots();
    }

    public static int priceSlot() {
        return ShopGuiLayout.get().priceSlot();
    }

    public static List<Integer> productSlots() {
        return ShopGuiLayout.get().productSlots();
    }

    public static int buySlot() {
        return ShopGuiLayout.get().buySlot();
    }

    public static int createShopSlot() {
        return ShopGuiLayout.get().createShopSlot();
    }

    public static int changeShopSaveSlot() {
        return ShopGuiLayout.get().changeShopSaveSlot();
    }

    public static int changeShopDeleteSlot() {
        return ShopGuiLayout.get().changeShopDeleteSlot();
    }

    public static int changeShopBackSlot() {
        return ShopGuiLayout.get().changeShopBackSlot();
    }

    /** Owner / trusted shop control panel ({@code EditShopMenu}) — from {@code shop-gui.owner-control-menu}. */
    public static int ownerCollectSlot() {
        return ShopGuiLayout.get().ownerCollectSlot();
    }

    public static int ownerChangeListingSlot() {
        return ShopGuiLayout.get().ownerChangeListingSlot();
    }

    public static int ownerCustomizeSlot() {
        return ShopGuiLayout.get().ownerCustomizeSlot();
    }

    public static int ownerSettingsSlot() {
        return ShopGuiLayout.get().ownerSettingsSlot();
    }

    public static int ownerConnectStockpileSlot() {
        return ShopGuiLayout.get().ownerConnectStockpileSlot();
    }

    public static int ownerStockChestSlot() {
        return ShopGuiLayout.get().ownerStockChestSlot();
    }

    public static int ownerTrustedPlayersSlot() {
        return ShopGuiLayout.get().ownerTrustedPlayersSlot();
    }

    public static int ownerAnalyticsSlot() {
        return ShopGuiLayout.get().ownerAnalyticsSlot();
    }

    public static int ownerShopInfoSlot() {
        return ShopGuiLayout.get().ownerShopInfoSlot();
    }

    /** Single chest (5 rows): list GUIs use rows 0–3 for content + row 4 for pagination controls. */
    public static final int MAX_SIZE = 45;

    private final Player player;
    private final Location location;
    private final int size;
    private final ClickHandler clickHandler;
    private final MenuColor color;
    private final String title;

    private Inventory inventory;

    public Menu(Player player, Location location, int size, ClickHandler clickHandler,
                MenuColor color, Message title) {
        this(player, location, size, clickHandler, color, title.toString());
    }

    public Menu(Player player, Location location, int size, ClickHandler clickHandler,
                MenuColor color, String title) {
        if (size > MAX_SIZE) throw new IllegalArgumentException("Menu size must not be higher than " + MAX_SIZE);
        this.player = player;
        this.location = location;

        this.size = size;
        this.clickHandler = clickHandler;
        this.color = color;
        this.title = inventoryTitleOrFallback(title);
    }

    /** Bukkit 1.21+ rejects null (and some builds reject blank) custom inventory titles. */
    private static String inventoryTitleOrFallback(String title) {
        if (title != null && !title.isEmpty()) {
            return title;
        }
        return ChatColor.DARK_GRAY + "Shop";
    }

    public void open() {
        inventory = Bukkit.createInventory(null, size, title);
        decorate();
        NormalShops.getInstance().getMenuListener().registerActiveMenu(inventory, this);
        player.openInventory(inventory);
    }

    protected abstract void setupButtons();

    private final Map<Integer, Button> buttonMap = new HashMap<>();

    protected void addButton(Button button) {
        buttonMap.put(button.getSlot(), button);
    }

    protected void decorate() {
        for (int i = 0; i < size; i++) {
            addButton(color.getBackgroundButton(i));
        }
        setupButtons();
        placeButtons();
        onAfterPlaceButtons();
    }

    /** Override to paint gui.yml {@code filler-key} slots or other post-layout tweaks. */
    protected void onAfterPlaceButtons() {
    }

    /**
     * Paint slots matching {@code filler-key} in gui.yml {@code layouts.<menuId>} using
     * {@code icons.<menuId>.background} / {@code icons.global.background}.
     */
    protected final void paintRegisteredLayoutFillers(String yamlMenuId) {
        paintLayoutFillers(yamlMenuId, MenuSlotRegistry.fillerSlots(yamlMenuId));
    }

    protected final void paintLayoutFillers(String yamlMenuIdForIcons, List<Integer> slots) {
        if (slots == null || slots.isEmpty()) {
            return;
        }
        Material mat = GuiIcons.menuBackground(yamlMenuIdForIcons, Material.GRAY_STAINED_GLASS_PANE);
        ItemStack pane = BlankButton.fillerStack(mat);
        Inventory inv = getInventory();
        int max = getMenuSize();
        for (int slot : slots) {
            if (slot >= 0 && slot < max) {
                inv.setItem(slot, pane.clone());
            }
        }
    }

    private void placeButtons() {
        buttonMap.forEach((slot, button) -> inventory.setItem(slot, button.getItem()));
    }

    protected Inventory getInventory() {
        return inventory;
    }

    protected int getMenuSize() {
        return size;
    }

    protected void onClick(InventoryClickEvent event) {
        if (!clickHandler.isValidClick(event)) {
            event.setCancelled(true);
            return;
        }
        Button button = buttonMap.get(event.getRawSlot());
        if (button == null) {
            int raw = event.getRawSlot();
            if (raw >= 0 && raw < getMenuSize()
                    && event.getView().getTopInventory().equals(inventory)) {
                event.setCancelled(true);
            }
            return;
        }
        if (!clickHandler.handleClick(event)) {
            return;
        }
        button.handleClick(event);
    }

    protected void onDrag(InventoryDragEvent event) {
        clickHandler.handleDrag(event);
    }

    protected void onClose(InventoryCloseEvent event) {
    }

    public Player getPlayer() {
        return player;
    }

    public Location getLocation() {
        return location;
    }

}
