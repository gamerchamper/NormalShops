package me.gamechampcrafted.normalshops.menu.edit;

import me.gamechampcrafted.normalshops.menu.ShopButton;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class AnalyticsDashboardButton extends ShopButton {

    public AnalyticsDashboardButton(int slot, ItemShop shop) {
        super(slot, shop);
    }

    @Override
    public ItemStack getItem() {
        List<String> lore = Arrays.asList(
                Utils.colorize("&7View shop sales analytics dashboard."),
                Utils.colorize("&7Revenue, stock flow, and averages.")
        );
        return createItem(Utils.colorize("&b📈 &lANALYTICS"), lore, Material.COMPASS, false);
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        new ShopAnalyticsMenu((org.bukkit.entity.Player) event.getWhoClicked(), getShop()).open();
    }
}
