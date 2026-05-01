package me.gamechampcrafted.normalshops.shop.connector;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.shop.Pile;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class PileConnector extends Connector {

    private final ItemShop shop;

    public PileConnector(ItemShop shop, Player player) throws IllegalArgumentException {
        super(Message.CONNECT_EARNINGS, shop.getLocation(), player);
        this.shop = shop;
        addCovers();
    }

    @Override
    public void handleConnection(Location target) {
        Location resolvedTarget = Pile.resolvePileLocation(target);
        Player player = getPlayer();
        Pile pile = Pile.get(resolvedTarget);
        // Create earnings pile if it doesn't exist
        if (pile == null) {
            Material mat = resolvedTarget.getBlock().getType();
            if (mat == Material.ENDER_CHEST || mat == Material.CHEST || mat == Material.BARREL) {
                if (Pile.isPileLimitReachedForAndWarn(player)) return;
                pile = new Pile(resolvedTarget, player);
                CoreProtectLogger.logEarningsPileCreate(player, resolvedTarget);
            }
        }
        if (shop == null || pile == null) {
            Message.CONNECTOR_INVALID.send(player);
            return;
        }
        if (pile.hasShop(shop)) {
            pile.removeShop(shop);
            Message.CONNECTOR_REMOVE_EARNINGS.sendSilently(player);
            disconnectEffect(player);
            CoreProtectLogger.logShopDisconnect(player, shop.getLocation(), resolvedTarget, "earnings pile");
        } else {
            if (pile.isShopLimitReachedAndWarn(player)) return;
            pile.addShop(shop);
            Message.CONNECTOR_EARNINGS.sendSilently(player);
            connectEffect(player, resolvedTarget);
            CoreProtectLogger.logShopConnect(player, shop.getLocation(), resolvedTarget, "earnings pile");
        }
    }

    @Override
    public void addCovers() {
    }
}
