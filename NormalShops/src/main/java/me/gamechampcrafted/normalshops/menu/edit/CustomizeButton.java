package me.gamechampcrafted.normalshops.menu.edit;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.menu.ShopButton;
import me.gamechampcrafted.normalshops.menu.customize.CustomizeShopMenu;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class CustomizeButton extends ShopButton {

    public CustomizeButton(int slot, ItemShop shop) {
        super(slot, shop);
    }

    private final ItemStack item = createItem(
            rainbow(Message.BUTTON_CUSTOMIZE.toString()),
            Message.BUTTON_CUSTOMIZE.getLore(),
            Material.BRUSH, false);

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        if (Permission.CUSTOMIZE.lacksAndNotify(player)) {
            player.closeInventory();
            return;
        }

        new CustomizeShopMenu(player, getShop()).open();
    }
}
