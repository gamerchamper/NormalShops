package me.gamechampcrafted.normalshops.menu.customize;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.shop.display.FrameDisplay;
import me.gamechampcrafted.normalshops.shop.display.ShopDisplayType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class DisplayMoveToTopButton extends ModifyDisplayButton {

    public DisplayMoveToTopButton(int slot, ItemShop shop) {
        super(slot, shop, ShopDisplayType.FRAME);
    }

    private final ItemStack item =
            createItem(
                    Message.BUTTON_DISPLAY_MOVE_TO_TOP,
                    Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE,
                    false);

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        FrameDisplay display = (FrameDisplay) getShop().getDisplay();
        if (display == null) return;
        display.moveToTop();
        CoreProtectLogger.logShopEdit(player, getShop().getLocation(), "moved display to top");
        Message.DISPLAY_FRAME_BUILD.sendSilently(player);
        playBuildDisplaySound(player);
        sendDisplayParticle(player);
    }
}
