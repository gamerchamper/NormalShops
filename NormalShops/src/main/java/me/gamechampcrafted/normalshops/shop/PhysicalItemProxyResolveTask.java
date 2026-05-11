package me.gamechampcrafted.normalshops.shop;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Periodically converts physical proxy items to real stacks when ledger totals are consistent, or trims duplicate mass.
 */
public final class PhysicalItemProxyResolveTask implements Runnable {

    @Override
    public void run() {
        if (!PhysicalItemProxy.isProxyEnabled()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            PhysicalItemProxy.resolveCarriedProxies(player);
        }
    }
}
