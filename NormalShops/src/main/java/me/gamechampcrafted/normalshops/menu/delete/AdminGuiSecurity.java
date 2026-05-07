package me.gamechampcrafted.normalshops.menu.delete;

import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.data.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Guards admin-only controls in {@link DeleteShopMenu} and related views.
 * Requires {@link Permission#DELETE} and {@linkplain Player#isOp() operator} status.
 */
public final class AdminGuiSecurity {

    private AdminGuiSecurity() {
    }

    public static boolean canUseAdminTools(Player player) {
        return Permission.DELETE.has(player) && player.isOp();
    }

    /**
     * @return true if the action must be denied (player failed the gate)
     */
    public static boolean denyUnlessAdminTools(Player player, String actionDescription) {
        if (canUseAdminTools(player)) {
            return false;
        }
        logExploitAttempt(player, actionDescription);
        return true;
    }

    public static void logExploitAttempt(Player player, String actionDescription) {
        String details = player.getName() + " (" + player.getUniqueId() + ") — " + actionDescription;
        String line = ChatColor.RED + "[NormalShops] Exploit detected: " + details
                + " (requires normalshops.delete + operator).";
        NormalShops.getInstance().getLogger().severe(ChatColor.stripColor(line));
        for (Player op : Bukkit.getOnlinePlayers()) {
            if (op.isOp()) {
                op.sendMessage(line);
            }
        }
    }
}
