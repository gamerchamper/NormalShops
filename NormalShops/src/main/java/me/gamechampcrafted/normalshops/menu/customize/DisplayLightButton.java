package me.gamechampcrafted.normalshops.menu.customize;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.MessageType;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.shop.display.ShopDisplayType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class DisplayLightButton extends ModifyDisplayButton {

    @Override
    public void clickSound(Player player) {
    }

    private final Block blockAbove;

    public DisplayLightButton(int slot, ItemShop shop) {
        super(slot, shop, ShopDisplayType.GLASS);
        blockAbove = getShop().getLocation().getBlock().getRelative(BlockFace.UP);
    }

    private final ItemStack removeLightButton =
            createItem(Message.BUTTON_DISPLAY_REMOVE_LIGHT, Material.GLOWSTONE_DUST, false);
    private final ItemStack addLightButton =
            createItem(Message.BUTTON_DISPLAY_ADD_LIGHT, Material.GUNPOWDER, false);

    @Override
    public ItemStack getItem() {
        if (blockAbove.getType() == Material.LIGHT) {
            return removeLightButton;
        }
        return addLightButton;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        if (blockAbove.getType() == Material.LIGHT) {
            // Remove light
            blockAbove.setType(Material.AIR);
            CoreProtectLogger.logShopEdit(player, getShop().getLocation(), "removed display light");
            MessageType.WARN.playSound(player);
            sendDisplayParticle(player);
        } else if (blockAbove.getType() == Material.AIR) {
            // Add light
            blockAbove.setType(Material.LIGHT);
            CoreProtectLogger.logShopEdit(player, getShop().getLocation(), "added display light");
            MessageType.CONFIRM.playSound(player);
            sendDisplayParticle(player);
        } else {
            // Not empty
            Message.DISPLAY_GLASS_INVALID_LIGHT.send(player);
            return;
        }

        event.setCurrentItem(getItem());
    }
}
