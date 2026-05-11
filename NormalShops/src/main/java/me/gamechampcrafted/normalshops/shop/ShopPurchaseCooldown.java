package me.gamechampcrafted.normalshops.shop;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player, per-shop-block cooldown between purchases (classic buy GUI + villager trades).
 */
public final class ShopPurchaseCooldown {

    private static final ConcurrentHashMap<String, Long> LAST_PURCHASE_MS = new ConcurrentHashMap<>();

    private ShopPurchaseCooldown() {}

    /**
     * @return seconds remaining (at least 1) if still cooling down, or {@code null} if a purchase is allowed
     */
    public static Integer blockingRemaining(Player player, Location shopBlock, int cooldownSeconds) {
        if (cooldownSeconds <= 0 || player == null || shopBlock == null) {
            return null;
        }
        String key = key(player, shopBlock);
        Long last = LAST_PURCHASE_MS.get(key);
        if (last == null) {
            return null;
        }
        long needMs = cooldownSeconds * 1000L;
        long elapsed = System.currentTimeMillis() - last;
        if (elapsed >= needMs) {
            return null;
        }
        int sec = (int) Math.ceil((needMs - elapsed) / 1000.0);
        return Math.max(1, sec);
    }

    public static void record(Player player, Location shopBlock) {
        if (player == null || shopBlock == null) {
            return;
        }
        LAST_PURCHASE_MS.put(key(player, shopBlock), System.currentTimeMillis());
    }

    private static String key(Player player, Location shopBlock) {
        Location b = shopBlock.getBlock().getLocation();
        World w = b.getWorld();
        if (w == null) {
            return player.getUniqueId().toString();
        }
        return player.getUniqueId() + "|" + w.getName() + "|" + b.getBlockX() + "|" + b.getBlockY() + "|" + b.getBlockZ();
    }
}
