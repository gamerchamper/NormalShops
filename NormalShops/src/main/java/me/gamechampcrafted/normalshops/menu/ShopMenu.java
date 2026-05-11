package me.gamechampcrafted.normalshops.menu;

import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.menu.change.ItemButton;
import me.gamechampcrafted.normalshops.menu.create.PriceButton;
import me.gamechampcrafted.normalshops.menu.create.ProductButton;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class ShopMenu extends Menu {

    private final ItemShop shop;
    private static final Map<String, UUID> ACTIVE_EDITORS = new HashMap<>();

    public ShopMenu(Player player, ItemShop shop, int size, ClickHandler clickHandler, Message title) {
        this(player, shop, size, clickHandler, title.toString());
    }

    public ShopMenu(Player player, ItemShop shop, int size, ClickHandler clickHandler, String title) {
        this(player, shop, size, clickHandler, shop.getColor(), title);
    }

    public ShopMenu(Player player, ItemShop shop, int size, ClickHandler clickHandler, MenuColor color, String title) {
        super(player, shop.getLocation(), size, clickHandler, color, title);
        this.shop = shop;
    }

    protected ItemShop getShop() {
        return shop;
    }

    protected boolean enforceManagementAccess() {
        return false;
    }

    protected boolean enforceEditorSessionLock() {
        return enforceManagementAccess();
    }

    protected boolean hasManagementAccess(Player player) {
        return shop.isOwner(player) || shop.isTrusted(player) || Permission.DELETE.has(player);
    }

    @Override
    public void open() {
        if (enforceEditorSessionLock()) {
            Player player = getPlayer();
            if (!acquireEditorLock(player)) {
                player.sendMessage(ChatColor.RED + "This shop editor is already in use.");
                return;
            }
        }
        super.open();
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        if (enforceManagementAccess()) {
            Player player = (Player) event.getWhoClicked();
            if (!hasManagementAccess(player)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You no longer have access to this shop.");
                player.closeInventory();
                return;
            }
        }
        super.onClick(event);
    }

    @Override
    protected void onClose(InventoryCloseEvent event) {
        if (enforceEditorSessionLock() && event.getPlayer() instanceof Player player) {
            releaseEditorLock(player);
        }
    }

    private boolean acquireEditorLock(Player player) {
        String key = getShopKey();
        synchronized (ACTIVE_EDITORS) {
            UUID active = ACTIVE_EDITORS.get(key);
            if (active != null && !active.equals(player.getUniqueId())) {
                return false;
            }
            ACTIVE_EDITORS.put(key, player.getUniqueId());
            return true;
        }
    }

    private void releaseEditorLock(Player player) {
        String key = getShopKey();
        synchronized (ACTIVE_EDITORS) {
            UUID active = ACTIVE_EDITORS.get(key);
            if (active != null && active.equals(player.getUniqueId())) {
                ACTIVE_EDITORS.remove(key);
            }
        }
    }

    private String getShopKey() {
        var location = shop.getLocation();
        String world = location.getWorld() == null ? "unknown" : location.getWorld().getName();
        return world + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    protected void addPriceAndProductButtons(boolean productButtons) {
        List<ItemStack> products = shop.getProducts();
        ItemStack price = shop.getPrice();

        //Display Price
        int ps = Menu.priceSlot();
        if (price != null) {
            addButton(new ItemButton(ps, price));
        } else {
            addButton(new PriceButton(ps));
        }

        //Display Products
        List<Integer> slots = Menu.productSlots();
        for (int i = 0; i < slots.size(); i++) {
            int slot = slots.get(i);
            if (i < products.size()) {
                ItemStack item = products.get(i);
                if (item != null) {
                    addButton(new ItemButton(slot, item));
                    continue;
                }
            }
            if (productButtons) {
                addButton(new ProductButton(slot));
            } else {
                addButton(new ItemButton(slot, Material.AIR));
            }
        }
    }
}
