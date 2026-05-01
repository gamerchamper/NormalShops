package me.gamechampcrafted.normalshops.menu.edit;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.MessageType;
import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.menu.ShopButton;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.shop.connector.PileConnector;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ConnectPileButton extends ShopButton {

    public ConnectPileButton(int slot, ItemShop shop) {
        super(slot, shop);
    }

    private final ItemStack item =
            createItem(Message.BUTTON_CONNECT_EARNINGS, Material.ENDER_CHEST, false);

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        player.closeInventory();
        if (Permission.EARNINGS_PILE.lacksAndNotify(player)) {
            return;
        }

        try {
            new PileConnector(getShop(), player);
        } catch (IllegalArgumentException exception) {
            MessageType.FAIL.playSound(player);
        }
    }
}
