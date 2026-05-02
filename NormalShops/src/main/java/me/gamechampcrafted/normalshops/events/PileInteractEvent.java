package me.gamechampcrafted.normalshops.events;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.menu.pile.EarningsMenu;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.shop.Pile;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;

public class PileInteractEvent extends InteractEvent {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        handleInteract(event);
    }

    @Override
    protected boolean onInteract(Player player, Block block) {
        if (!player.isSneaking()) return false;
        if (block.getType() != Material.ENDER_CHEST && block.getType() != Material.CHEST && block.getType() != Material.BARREL) return false;
        if (ItemShop.get(block.getLocation()) != null) return false;
        Location location = Pile.resolvePileLocation(block.getLocation());
        Pile pile = Pile.get(location);
        if (pile == null) {
            // Only registered earnings piles react here (created via shop editor → connect earnings).
            return false;
        }
        if (!pile.isOwner(player)) {
            Message.PILE_NOT_OWNER.send(player);
            return true;
        }
        if (Permission.EARNINGS_PILE.lacksAndNotify(player)) {
            return true;
        }

        new EarningsMenu(player, pile).open();
        Sound openSound = block.getType() == Material.ENDER_CHEST ? Sound.BLOCK_ENDER_CHEST_OPEN : Sound.BLOCK_CHEST_OPEN;
        player.playSound(player, openSound, SoundCategory.BLOCKS, .5f, .8f);
        return true;
    }
}
