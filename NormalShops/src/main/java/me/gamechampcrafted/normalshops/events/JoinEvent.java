package me.gamechampcrafted.normalshops.events;

import me.gamechampcrafted.normalshops.NormalShops;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinEvent implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(NormalShops.getInstance(), task -> {
            NormalShops.getInstance().getShopManager().sendWarning(event.getPlayer());
        }, 20L);
    }
}
