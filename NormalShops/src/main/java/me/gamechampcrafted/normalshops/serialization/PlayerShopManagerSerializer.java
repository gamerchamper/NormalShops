package me.gamechampcrafted.normalshops.serialization;

import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.shop.Pile;
import me.gamechampcrafted.normalshops.shop.PlayerShopManager;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class PlayerShopManagerSerializer extends Serializer<PlayerShopManager> {

    public PlayerShopManagerSerializer(Map<String, Object> map) {
        super(map);
    }

    @Override
    protected @NotNull PlayerShopManager deserializeMap() {
        List<ItemShop> shops = getListOrDefault("shops", new ArrayList<>());
        Map<Location, ItemShop> shopMap = shops.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ItemShop::getLocation, shop -> shop));
        List<Pile> piles = getListOrDefault("piles", new ArrayList<>());
        Map<Location, Pile> pileMap = piles.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Pile::getLocation, pile -> pile));

        return new PlayerShopManager(shopMap, pileMap);
    }

    public static Map<String, Object> serialize(PlayerShopManager manager) {
        Map<String, Object> map = new HashMap<>();
        map.put("v", PlayerShopManager.VERSION);
        map.put("shops", new ArrayList<>(manager.getShops()));
        map.put("piles", new ArrayList<>(manager.getPiles()));
        return map;
    }
}
