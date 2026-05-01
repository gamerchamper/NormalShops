package me.gamechampcrafted.normalshops.shop.connector;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.shop.Pile;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class ShopConnector extends Connector {

    private final Pile pile;

    public ShopConnector(Pile pile, Player player) throws IllegalArgumentException {
        super(Message.CONNECT_SHOP, pile.getLocation(), player);
        this.pile = pile;
        addCovers();
    }

    @Override
    public void handleConnection(Location target) {
        ItemShop shop = ItemShop.get(target);
        Player player = getPlayer();
        if (pile == null || shop == null) {
            Message.CONNECTOR_INVALID.send(player);
            return;
        }
        if (pile.hasShop(shop)) {
            pile.removeShop(shop);
            Message.CONNECTOR_REMOVE_SHOP.sendSilently(player);
            disconnectEffect(player);
            CoreProtectLogger.logShopDisconnect(player, target, pile.getLocation(), "earnings pile");
        } else {
            if (pile.isShopLimitReachedAndWarn(player)) return;
            pile.addShop(shop);
            Message.CONNECTOR_SHOP.sendSilently(player);
            connectEffect(player, target);
            CoreProtectLogger.logShopConnect(player, target, pile.getLocation(), "earnings pile");
        }
    }

    @Override
    public void addCovers() {
        if (pile == null) return;

        for (Location location : pile.getShops()) {
            addCover(new Cover(location, Material.LIGHT_BLUE_STAINED_GLASS));
        }
    }
}
