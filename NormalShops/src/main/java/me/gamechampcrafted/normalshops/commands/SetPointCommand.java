package me.gamechampcrafted.normalshops.commands;

import me.gamechampcrafted.normalshops.data.Message;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetPointCommand implements CommandExecutor {

    private static final String PERMISSION = "normalshops.region";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        if (!player.hasPermission(PERMISSION)) {
            Message.NO_PERMISSION.send(player);
            return true;
        }
        ShopsRegionEditor.AddCornerResult r = ShopsRegionEditor.addCorner(player);
        switch (r) {
            case NO_SESSION -> {
                player.sendMessage(ChatColor.RED + "Start with " + ChatColor.YELLOW + "/normalshops region make"
                        + ChatColor.RED + " first.");
                return true;
            }
            case WRONG_WORLD -> {
                player.sendMessage(ChatColor.RED + "Both corners must be in the same world as point 1.");
                return true;
            }
            case RECORDED_NEED_MORE -> {
                int done = ShopsRegionEditor.sessionCornerCount(player);
                player.sendMessage(ChatColor.GREEN + "Corner " + ChatColor.AQUA + done + "/2"
                        + ChatColor.GREEN + " set at your feet. Stand at the next corner and run "
                        + ChatColor.YELLOW + "/setpoint" + ChatColor.GREEN + ".");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 0.5f, 1.2f);
                return true;
            }
            case COMPLETE_SAVED -> {
                player.sendMessage(ChatColor.GREEN + "Region saved to " + ChatColor.YELLOW + "config.yml"
                        + ChatColor.GREEN + " under " + ChatColor.AQUA + "shops-menu-regions"
                        + ChatColor.GREEN + ".");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.3f, 1f);
                return true;
            }
            default -> {
                return true;
            }
        }
    }
}
