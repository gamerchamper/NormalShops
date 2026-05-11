package me.gamechampcrafted.normalshops.menu;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class ChangeShopButton extends ShopButton {

    /**
     * Drops editor placeholders accidentally persisted as listing rows (see {@link #getProducts}).
     */
    public static List<ItemStack> filterListingProducts(List<ItemStack> raw) {
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack p : raw) {
            if (p != null && !p.getType().isAir() && !isProductSlotPlaceholder(p)) {
                out.add(p);
            }
        }
        return out;
    }

    public ChangeShopButton(int slot, ItemShop shop) {
        super(slot, shop);
    }

    /**
     * Empty product slots use {@link Button#createItem} paper icons tagged with {@link GuiDisplayItem#stampGuiItem}.
     * Comparing with {@code new ProductButton().getItem()} never worked: each placeholder gets a new random PDC id,
     * so {@link ItemStack#isSimilar} was always false and placeholders were saved as real products ("PRODUCT" paper).
     */
    public static boolean isProductSlotPlaceholder(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        if (GuiDisplayItem.isGuiItem(item)) {
            return true;
        }
        // Legacy unstamped placeholders are always paper; avoid matching real items named "PRODUCT".
        if (item.getType() != Material.PAPER) {
            return false;
        }
        return matchesButtonDisplayName(item, Message.BUTTON_PRODUCT);
    }

    public static boolean isPriceSlotPlaceholder(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        if (GuiDisplayItem.isGuiItem(item)) {
            return true;
        }
        if (item.getType() != Material.PAPER) {
            return false;
        }
        return matchesButtonDisplayName(item, Message.BUTTON_PRICE);
    }

    private static boolean matchesButtonDisplayName(ItemStack item, Message button) {
        if (!item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        String got = ChatColor.stripColor(meta.getDisplayName());
        String expected = ChatColor.stripColor(button.toString());
        return got.equals(expected);
    }

    @Nullable
    public static List<ItemStack> getProducts(Inventory inv) {
        List<ItemStack> product = new ArrayList<>();
        Menu.productSlots().forEach(slot -> {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                if (!isProductSlotPlaceholder(item)) {
                    product.add(item);
                }
            }
        });
        if (product.isEmpty()) return null;
        return product;
    }

    @Nullable
    public static ItemStack getPrice(Inventory inv) {
        ItemStack item = inv.getItem(Menu.priceSlot());
        if (item == null) return null;
        if (item.getType() == Material.AIR) return null;
        if (isPriceSlotPlaceholder(item)) return null;

        return item;
    }

}
