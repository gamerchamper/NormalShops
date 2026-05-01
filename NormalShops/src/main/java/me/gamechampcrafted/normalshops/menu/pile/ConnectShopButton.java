package me.gamechampcrafted.normalshops.menu.pile;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.MessageType;
import me.gamechampcrafted.normalshops.menu.PileButton;
import me.gamechampcrafted.normalshops.shop.Pile;
import me.gamechampcrafted.normalshops.shop.connector.ShopConnector;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ConnectShopButton extends PileButton {

    public ConnectShopButton(int slot, Pile pile) {
        super(slot, pile);
    }

    private final ItemStack item =
            createItem(Message.BUTTON_CONNECT_SHOP, Material.HOPPER, false);

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        player.closeInventory();

        try {
            new ShopConnector(getPile(), player);
        } catch (IllegalArgumentException exception) {
            MessageType.FAIL.playSound(player);
        }
    }
}
