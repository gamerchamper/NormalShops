package me.gamechampcrafted.normalshops.events;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.shop.Pile;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class PileBreakEvent implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Pile pile = Pile.get(event.getBlock().getLocation());
        if (pile == null) return;
        // Break by owner
        Player player = event.getPlayer();
        if (pile.isOwner(player)) {
            CoreProtectLogger.logEarningsPileDelete(player, pile.getLocation());
            pile.delete();
            Message.PILE_BREAK_OWNER.send(player);
            return;
        }
        // No permission
        if (Permission.DELETE.lacks(player)) {
            event.setCancelled(true);
            Message.PILE_NO_BREAK.send(player);
            return;
        }
        // Break by operator
        CoreProtectLogger.logEarningsPileDelete(player, pile.getLocation());
        pile.delete();
        Message.PILE_BREAK_OPERATOR.parameterizer()
                .put("owner", pile.getOwnerName())
                .send(player);
    }
}
