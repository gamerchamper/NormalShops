package me.gamechampcrafted.normalshops.menu.customize;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.shop.display.FrameDisplay;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class BuildFrameDisplayButton extends DisplayButton {

    public BuildFrameDisplayButton(int slot, ItemShop shop) {
        super(slot, shop);
    }

    @Override
    public void clickSound(Player player) {
    }

    private final ItemStack item = createItem(
            Message.BUTTON_FRAME_DISPLAY, Material.PAINTING, false
    );

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemShop shop = getShop();

        if (shop.getDisplay() instanceof FrameDisplay) {
            Message.DISPLAY_FRAME_BUILD.send(player);
            shop.updateDisplay();
            return;
        }
        if (Permission.DISPLAY.lacksAndNotify(player)) {
            player.closeInventory();
            return;
        }

        try {
            FrameDisplay display = new FrameDisplay(shop);
            shop.setDisplay(display);
            CoreProtectLogger.logShopEdit(player, shop.getLocation(), "built frame display");
            new CustomizeShopMenu(player, shop).open();
            Message.DISPLAY_FRAME_BUILD.sendSilently(player);
            playBuildDisplaySound(player);
            sendDisplayParticle(player);
        } catch (IllegalArgumentException exception) {
            // Shop block is a chest
            Message.DISPLAY_FRAME_INVALID_CHEST.send(player);
        }
    }
}
