package me.gamechampcrafted.normalshops.commands;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.Logger;
import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.backup.ShopDataAutoBackup;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.data.Setting;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.shop.ShopBackupService;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NormalShopsCommand implements CommandExecutor, TabCompleter {

    /** Max raycast distance for /normalshops delete (solid block line-of-sight). */
    private static final int DELETE_LOOK_MAX_DISTANCE = 12;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }

        if (args[0].equalsIgnoreCase("delete")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
                return true;
            }
            if (Permission.DELETE.lacksAndNotify(player)) {
                return true;
            }
            Block targeted = player.getTargetBlockExact(DELETE_LOOK_MAX_DISTANCE, FluidCollisionMode.NEVER);
            if (targeted == null || targeted.getType().isAir()) {
                Message.COMMAND_DELETE_LOOK_NOTHING.send(player);
                return true;
            }
            ItemShop shop = ItemShop.get(targeted.getLocation());
            if (shop == null || shop.isDeleted()) {
                Message.COMMAND_DELETE_LOOK_NOT_SHOP.send(player);
                return true;
            }
            CoreProtectLogger.logShopDelete(player, shop.getLocation());
            shop.delete(player);
            Message.SHOP_BREAK_OPERATOR.parameterizer()
                    .put("owner", shop.getOwnerName())
                    .send(player);
            Logger.info("Shop deleted via /normalshops delete by " + player.getName() + " at " + shop.getLocation().getBlockX()
                    + " " + shop.getLocation().getBlockY() + " " + shop.getLocation().getBlockZ());
            return true;
        }

        if (args[0].equalsIgnoreCase("restore")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
                return true;
            }
            if (Permission.RESTORE.lacksAndNotify(player)) {
                return true;
            }
            ShopBackupService backups = NormalShops.getInstance().getShopBackupService();
            if (backups == null) {
                sender.sendMessage(ChatColor.RED + "Shop backups are not available (failed to load shop-backups.yml).");
                return true;
            }
            Block targeted = player.getTargetBlockExact(DELETE_LOOK_MAX_DISTANCE, FluidCollisionMode.NEVER);
            if (targeted == null || targeted.getType().isAir()) {
                Message.COMMAND_DELETE_LOOK_NOTHING.send(player);
                return true;
            }
            if (!isStandaloneChestOrBarrel(targeted)) {
                Message.COMMAND_RESTORE_BLOCK_INVALID.send(player);
                return true;
            }
            org.bukkit.Location shopBlockLoc = targeted.getLocation();
            if (ItemShop.get(shopBlockLoc) != null) {
                Message.COMMAND_RESTORE_ALREADY_SHOP.send(player);
                return true;
            }
            Map<String, Object> shopData = backups.findLatestShopDataForBlock(shopBlockLoc);
            if (shopData == null) {
                Message.COMMAND_RESTORE_NO_BACKUP.send(player);
                return true;
            }
            HashMap<String, Object> copy = new HashMap<>(shopData);
            copy.put("location", shopBlockLoc.getBlock().getLocation());
            copy.remove("display");
            ItemShop restored;
            try {
                restored = ItemShop.deserialize(copy);
            } catch (Exception e) {
                Logger.severe("Failed to deserialize shop backup: " + e.getMessage());
                e.printStackTrace();
                Message.SHOP_ERROR.send(player);
                return true;
            }
            NormalShops.getInstance().getShopManager().registerShop(restored);
            Message.COMMAND_RESTORE_SUCCESS.parameterizer()
                    .put("owner", restored.getOwnerName())
                    .put("location", Utils.formatLocation(restored.getLocation()))
                    .send(player);
            Logger.info("Shop restored via /normalshops restore by " + player.getName()
                    + " at " + shopBlockLoc.getBlockX() + " " + shopBlockLoc.getBlockY() + " " + shopBlockLoc.getBlockZ());
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("normalshops.reload")) {
                Message.NO_PERMISSION.send(sender);
                return true;
            }

            try {
                Setting.reload();
                Message.reload();
                if (NormalShops.getInstance().getShopManager() != null) {
                    NormalShops.getInstance().getShopManager().refreshAllOutOfStockDisplays();
                }
                ShopBackupService svc = NormalShops.getInstance().getShopBackupService();
                if (svc != null) {
                    svc.reload();
                }
                ShopDataAutoBackup dab = NormalShops.getInstance().getShopDataAutoBackup();
                if (dab != null) {
                    dab.restart();
                }
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

    private static boolean isStandaloneChestOrBarrel(Block block) {
        Material type = block.getType();
        if (type == Material.BARREL) {
            return true;
        }
        if (type != Material.CHEST) {
            return false;
        }
        BlockState state = block.getState();
        if (!(state instanceof Chest chest)) {
            return false;
        }
        return !(chest.getInventory().getHolder() instanceof DoubleChest);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("normalshops.reload")) {
                completions.add("reload");
            }
            if (sender.hasPermission("normalshops.delete")) {
                completions.add("delete");
            }
            if (sender.hasPermission("normalshops.restore")) {
                completions.add("restore");
            }
        }
        return completions;
    }
}
