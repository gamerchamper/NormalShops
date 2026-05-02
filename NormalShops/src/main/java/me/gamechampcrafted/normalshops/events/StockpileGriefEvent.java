package me.gamechampcrafted.normalshops.events;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.shop.ShopManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class StockpileGriefEvent implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.HOPPER) return;
        Location above = event.getBlock().getRelative(BlockFace.UP).getLocation();
        Player player = event.getPlayer();
        if (Permission.BYPASS_STOCKPILE.has(player)) return;
        if (shouldDenyStockpileAccess(above, player)) {
            CoreProtectLogger.logShopEdit(player, above, "attempted to place hopper on stockpile");
            Message.STOCKPILE_NO_HOPPER.send(player);
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onOpen(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null) return;
        Location location = block.getLocation();
        if (block.getType() != Material.CHEST && block.getType() != Material.BARREL) return;
        if (Permission.BYPASS_STOCKPILE.has(player)) return;
        if (shouldDenyStockpileAccess(location, player)) {
            CoreProtectLogger.logShopEdit(player, location, "attempted to open stockpile without permission");
            Message.STOCKPILE_NOT_OWNER.send(player);
            event.setCancelled(true);
        }
    }

    private boolean shouldDenyStockpileAccess(Location location, Player player) {
        ShopManager shopManager = NormalShops.getInstance().getShopManager();
        UUID owner = shopManager.getStockpileOwner(location);
        if (owner == null) {
            return false;
        }
        return !shopManager.playerMayAccessRegisteredStockpile(location, player);
    }
}
