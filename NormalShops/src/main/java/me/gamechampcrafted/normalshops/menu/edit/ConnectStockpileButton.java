package me.gamechampcrafted.normalshops.menu.edit;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.MessageType;
import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.menu.ShopButton;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.shop.connector.StockpileConnector;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ConnectStockpileButton extends ShopButton {

    public ConnectStockpileButton(int slot, ItemShop shop) {
        super(slot, shop);
    }

    private final ItemStack item =
            createItem(Message.BUTTON_CONNECT_STOCK, Material.HOPPER, false);

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        player.closeInventory();
        if (Permission.STOCKPILE.lacksAndNotify(player)) {
            return;
        }

        try {
            new StockpileConnector(getShop(), player);
        } catch (IllegalArgumentException exception) {
            MessageType.FAIL.playSound(player);
        }
    }
}
