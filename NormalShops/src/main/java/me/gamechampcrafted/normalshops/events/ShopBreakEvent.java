package me.gamechampcrafted.normalshops.events;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class ShopBreakEvent implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        ItemShop shop = ItemShop.get(event.getBlock().getLocation());
        if (shop == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        // Break by owner
        if (shop.isOwner(player)) {
            CoreProtectLogger.logShopEdit(player, shop.getLocation(), "attempted to break shop");
            Message.BREAK_CONFIRM.send(player);
            return;
        }
        // No permissions
        if (Permission.DELETE.lacks(player)) {
            CoreProtectLogger.logShopEdit(player, shop.getLocation(), "attempted to break shop without permission");
            Message.SHOP_NO_BREAK.send(player);
            return;
        }
        // Break by operator
        CoreProtectLogger.logShopEdit(player, shop.getLocation(), "attempted to break shop as operator");
        Message.BREAK_OPERATOR_CONFIRM.send(player);
    }
}
