package me.gamechampcrafted.normalshops.menu.edit;

import me.gamechampcrafted.normalshops.menu.ShopButton;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ShopInfoButton extends ShopButton {

    public ShopInfoButton(int slot, ItemShop shop) {
        super(slot, shop);
    }

    @Override
    public ItemStack getItem() {
        ItemShop shop = getShop();
        long internalStock = shop.getCurrentInternalStockAmount();
        long lifetimeRevenue = shop.getLifetimeRevenue();
        long lifetimeSales = shop.getLifetimeSales();
        long lifetimeImpressions = shop.getLifetimeImpressions();
        long soldProducts = shop.getLifetimeProductsSold();
        long stockAdded = shop.getLifetimeStockAdded();
        long stockRemoved = shop.getLifetimeStockRemoved();
        long netStock = shop.getNetStockChange();

        List<String> lore = Arrays.asList(
                Utils.colorize("&7Current internal stock: &e" + format(internalStock)),
                Utils.colorize("&7Lifetime earnings: &a" + format(lifetimeRevenue)),
                Utils.colorize("&7Lifetime sales: &b" + format(lifetimeSales)),
                Utils.colorize("&7Total impressions: &d" + format(lifetimeImpressions)),
                Utils.colorize("&7Products sold: &b" + format(soldProducts)),
                Utils.colorize("&7Stock added: &a+" + format(stockAdded)),
                Utils.colorize("&7Stock removed: &c-" + format(stockRemoved)),
                Utils.colorize("&7Net stock change: " + (netStock >= 0 ? "&a+" : "&c") + format(netStock))
        );
        return createItem(Utils.colorize("&6📊 &lSHOP STATS"), lore, Material.BOOK, false);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    private String format(long number) {
        return String.format(Locale.US, "%,d", number);
    }
}
