package me.gamechampcrafted.normalshops.menu.delete;

import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.menu.ShopButton;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class ShopHistoryButton extends ShopButton {

    public ShopHistoryButton(int slot, ItemShop shop) {
        super(slot, shop);
    }

    @Override
    public ItemStack getItem() {
        List<String> lore = Arrays.asList(
                Utils.colorize("&7View logged CoreProtect history"),
                Utils.colorize("&7for this shop location.")
        );
        return createItem(Utils.colorize("&b📜 &lSHOP HISTORY"), lore, Material.WRITABLE_BOOK, false);
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        NormalShops.getInstance().getShopHistoryMenuManager().open(player, getShop());
    }
}
