package me.gamechampcrafted.normalshops.menu.delete;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.menu.ShopButton;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class EmulateSaleButton extends ShopButton {

    public EmulateSaleButton(int slot, ItemShop shop) {
        super(slot, shop);
    }

    @Override
    public ItemStack getItem() {
        List<String> lore = Arrays.asList(
                Utils.colorize("&7Simulate one purchase on this shop."),
                Utils.colorize("&7Consumes stock and adds earnings."),
                Utils.colorize("&7Buyer is logged as the admin who clicked.")
        );
        return createItem(Utils.colorize("&d🧪 &lEMULATE SALE"), lore, Material.EXPERIENCE_BOTTLE, false);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player admin = (Player) event.getWhoClicked();
        if (AdminGuiSecurity.denyUnlessAdminTools(admin, "emulate sale (admin GUI)")) {
            return;
        }
        ItemShop shop = getShop();

        if (!shop.hasStock()) {
            admin.sendMessage(ChatColor.RED + "Shop is out of stock.");
            return;
        }

        List<ItemStack> products = shop.getProducts();
        for (ItemStack product : products) {
            if (shop.removeInternalStock(product, product.getAmount())) {
                shop.recordProductsSold(product.getAmount());
                continue;
            }
            Inventory stockInv = shop.getNextStockedInventory();
            if (stockInv != null && ItemShop.removeStockFromInventory(stockInv, product, product.getAmount())) {
                shop.recordProductsSold(product.getAmount());
                continue;
            }
            admin.sendMessage(ChatColor.RED + "Failed to emulate sale: insufficient stock.");
            return;
        }

        shop.incrementEarnings();
        shop.saveData();

        Player owner = Bukkit.getPlayer(shop.getOwnerUUID());
        CoreProtectLogger.logShopBuyEmulated(admin, owner, shop.getLocation(), shop.getPrice(), products.get(0));

        int total = shop.getPrice().getAmount();
        admin.sendMessage(ChatColor.GREEN + "Emulated sale at " + Utils.formatLocation(shop.getLocation())
                + ChatColor.GRAY + " | +" + total + " " + Utils.getFormattedName(shop.getPrice()));

        if (!shop.hasStock()) {
            NormalShops.getInstance().getShopManager().addWarning(shop);
        }
    }
}
