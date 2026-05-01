package me.gamechampcrafted.normalshops.events;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.shop.ShopManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.UUID;

public class StockpileBreakEvent implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        ShopManager shopManager = NormalShops.getInstance().getShopManager();
        // Stockpile has owner
        UUID owner = shopManager.getStockpileOwner(location);
        if (owner == null) return;
        event.setCancelled(true);
        if (!owner.equals(player.getUniqueId())) {
            Message.STOCKPILE_NO_BREAK.send(player);
            return;
        }
        CoreProtectLogger.logStockpileRemove(player, location, location);
        Message.STOCKPILE_REMOVE_FIRST.send(player);
    }
}
