package me.gamechampcrafted.normalshops.events;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public abstract class InteractEvent implements Listener {

    /**
     * Handles the player interact event on a clicked block.
     *
     * @param player player
     * @param block  clicked block
     * @return <code>true</code> if event is processed and should be cancelled
     */
    protected abstract boolean onInteract(Player player, Block block);

    protected void handleInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        // Return if player wants to place block or hanging item frame (not covered by Material#isBlock)
        Player player = event.getPlayer();
        Material hand = player.getInventory().getItemInMainHand().getType();
        if (player.isSneaking() && hand != Material.AIR
                && (hand.isBlock() || hand == Material.ITEM_FRAME || hand == Material.GLOW_ITEM_FRAME)) {
            return;
        }

        if (onInteract(player, block)) {
            event.setCancelled(true);
        }
    }
}
