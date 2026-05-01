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

public class DisplayRemoveFrameButton extends ModifyDisplayButton {

    public DisplayRemoveFrameButton(int slot, ItemShop shop) {
        super(slot, shop, ShopDisplayType.FRAME);
    }

    private final ItemStack item =
            createItem(Message.BUTTON_DISPLAY_REMOVE_FRAME, Material.STRUCTURE_VOID, false);

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
        FrameDisplay display = (FrameDisplay) shop.getDisplay();
        if (display == null) return;
        display.setFrame(Material.AIR);
        CoreProtectLogger.logShopEdit(player, shop.getLocation(), "removed frame");
        Message.FRAME_REMOVE_FRAME.send(player);
        new CustomizeShopMenu(player, shop).open();

    }
}
