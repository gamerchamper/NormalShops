package me.gamechampcrafted.normalshops.menu.delete;

import me.gamechampcrafted.normalshops.menu.ShopButton;
import me.gamechampcrafted.normalshops.menu.edit.EditShopMenu;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class ForceEditButton extends ShopButton {

    public ForceEditButton(int slot, ItemShop shop) {
        super(slot, shop);
    }

    @Override
    public ItemStack getItem() {
        List<String> lore = Arrays.asList(
                Utils.colorize("&7Force-open the owner shop editor."),
                Utils.colorize("&7You can modify all shop settings.")
        );
        return createItem(Utils.colorize("&6🛠 &lFORCE EDIT SHOP"), lore, Material.COMMAND_BLOCK, false);
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        if (AdminGuiSecurity.denyUnlessAdminTools(player, "force edit shop (admin GUI)")) {
            return;
        }
        new EditShopMenu(player, getShop()).open();
    }
}
