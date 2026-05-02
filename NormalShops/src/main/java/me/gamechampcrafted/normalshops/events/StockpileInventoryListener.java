package me.gamechampcrafted.normalshops.events;

import me.gamechampcrafted.normalshops.NormalShops;
import org.bukkit.Location;
import org.bukkit.block.Barrel;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Physical stockpile chest/barrels are edited outside the plugin stock GUI; this refreshes linked shops'
 * out-of-stock visuals when those inventories change (players or hoppers).
 */
public class StockpileInventoryListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        notifyStockpileChange(event.getInventory());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        notifyStockpileChange(event.getSource());
        notifyStockpileChange(event.getDestination());
    }

    private static void notifyStockpileChange(Inventory inventory) {
        if (inventory == null) {
            return;
        }
        NormalShops plugin = NormalShops.getInstance();
        if (plugin == null || plugin.getShopManager() == null) {
            return;
        }
        for (Location loc : blockLocationsOfHolder(inventory.getHolder())) {
            plugin.getShopManager().refreshShopsUsingStockpile(loc);
        }
    }

    private static List<Location> blockLocationsOfHolder(InventoryHolder holder) {
        if (holder instanceof DoubleChest dc) {
            List<Location> list = new ArrayList<>(2);
            if (dc.getLeftSide() instanceof Chest left) {
                list.add(left.getBlock().getLocation());
            }
            if (dc.getRightSide() instanceof Chest right) {
                list.add(right.getBlock().getLocation());
            }
            return list;
        }
        if (holder instanceof Chest chest) {
            return Collections.singletonList(chest.getBlock().getLocation());
        }
        if (holder instanceof Barrel barrel) {
            return Collections.singletonList(barrel.getBlock().getLocation());
        }
        return Collections.emptyList();
    }
}
