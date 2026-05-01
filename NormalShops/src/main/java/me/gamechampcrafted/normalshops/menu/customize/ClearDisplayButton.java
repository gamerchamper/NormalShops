package me.gamechampcrafted.normalshops.menu.customize;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.MessageType;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ClearDisplayButton extends DisplayButton {

    public ClearDisplayButton(int slot, ItemShop shop) {
        super(slot, shop);
    }

    private final ItemStack item = createItem(
            Message.BUTTON_CLEAR_DISPLAY, Material.BARRIER, false
    );

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public void clickSound(Player player) {
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemShop shop = getShop();

        if (shop == null) return;
        if (shop.getDisplay() == null) {
            MessageType.FAIL.playSound(player);
            return;
        }

        try {
            shop.setDisplay(null);
            CoreProtectLogger.logShopEdit(player, shop.getLocation(), "cleared display");
            player.closeInventory();
            Message.DISPLAY_CLEAR.send(player);
            sendDisplayParticle(player);
        } catch (Exception ignored) {
            // Can't build display
            MessageType.FAIL.playSound(player);
        }
    }
}
