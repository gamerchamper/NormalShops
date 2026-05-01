package me.gamechampcrafted.normalshops.commands;

import me.gamechampcrafted.normalshops.Logger;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.Setting;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class NormalShopsCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("normalshops.reload")) {
                Message.NO_PERMISSION.send(sender);
                return true;
            }

            try {
                Setting.reload();
                Message.reload();
                sender.sendMessage(ChatColor.GREEN + "NormalShops config and messages reloaded.");
                Logger.info("Config and messages reloaded by " + sender.getName());
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "Failed to reload NormalShops: " + e.getMessage());
                Logger.severe("Failed to reload: " + e.getMessage());
                e.printStackTrace();
            }
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("normalshops.reload")) {
                completions.add("reload");
            }
        }
        return completions;
    }
}
