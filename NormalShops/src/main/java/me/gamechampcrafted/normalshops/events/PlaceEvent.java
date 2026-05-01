package me.gamechampcrafted.normalshops.events;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.shop.display.GlassDisplay;
import me.gamechampcrafted.normalshops.shop.display.ShopDisplay;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class PlaceEvent implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Location location = event.getBlock().getLocation().add(0, -1, 0);
        ItemShop shop = ItemShop.get(location);
        if (shop == null) return;
        ShopDisplay display = shop.getDisplay();
        if (display == null) return;

        if (display instanceof GlassDisplay) {
            event.setCancelled(true);
            Message.BLOCK_PLACE.send(event.getPlayer());
        }
    }
}
