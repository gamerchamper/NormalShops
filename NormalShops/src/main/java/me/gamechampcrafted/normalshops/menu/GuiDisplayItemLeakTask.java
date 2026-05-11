package me.gamechampcrafted.normalshops.menu;

import me.gamechampcrafted.normalshops.NormalShops;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
/**
 * Removes tagged GUI “fake” items from player-controlled storage (survival leak / dupe cleanup).
 */
public final class GuiDisplayItemLeakTask implements Runnable {

    @Override
    public void run() {
        NormalShops plugin = NormalShops.getInstance();
        if (plugin == null || !plugin.isEnabled()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            stripPlayerCarrying(player);
        }
    }

    private static void stripPlayerCarrying(Player player) {
        stripInventorySlots(player.getInventory());
        ItemStack cursor = player.getItemOnCursor();
        if (GuiDisplayItem.isGuiItem(cursor)) {
            player.setItemOnCursor(null);
        }
        stripInventorySlots(player.getEnderChest());
    }

    private static void stripInventorySlots(Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && GuiDisplayItem.isGuiItem(item)) {
                inventory.setItem(i, null);
            }
        }
    }
}
