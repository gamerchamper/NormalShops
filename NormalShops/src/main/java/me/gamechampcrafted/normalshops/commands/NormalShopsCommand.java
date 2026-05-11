package me.gamechampcrafted.normalshops.commands;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.Logger;
import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.backup.ShopDataAutoBackup;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.data.Setting;
import me.gamechampcrafted.normalshops.menu.edit.ShopAccessChoiceMenu;
import me.gamechampcrafted.normalshops.menu.view.MyShopsListMenu;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.shop.PrivateShopStatsHologramManager;
import me.gamechampcrafted.normalshops.shop.ShopBackupService;
import me.gamechampcrafted.normalshops.update.ModrinthUpdateService;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Bukkit;
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
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NormalShopsCommand implements CommandExecutor, TabCompleter {

    /** Max raycast distance for /normalshops delete (solid block line-of-sight). */
    private static final int DELETE_LOOK_MAX_DISTANCE = 12;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();
        if ("viewshops".equals(cmdName)) {
            return handleViewShops(sender, args);
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("view")) {
            if (args.length != 1) {
                return false;
            }
            return handleViewShops(sender, new String[0]);
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("region")) {
            return handleRegionCommand(sender, args);
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("stats-holograms")) {
            return handleStatsHolograms(sender, args);
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("version")) {
            return handleVersion(sender);
        }

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
                if (NormalShops.getInstance().getPrivateShopStatsHologramManager() != null) {
                    NormalShops.getInstance().getPrivateShopStatsHologramManager().reload();
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

    private boolean handleVersion(CommandSender sender) {
        if (!sender.hasPermission("normalshops.version")) {
            Message.NO_PERMISSION.send(sender);
            return true;
        }
        NormalShops plugin = NormalShops.getInstance();
        String running = plugin.getDescription().getVersion();
        sender.sendMessage(ChatColor.GOLD + "NormalShops " + ChatColor.WHITE + running);

        ModrinthUpdateService.UpdateSnapshot snap = ModrinthUpdateService.get().getSnapshot();
        if (snap.errorMessage() != null && !snap.errorMessage().isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Update check failed: " + snap.errorMessage());
            sender.sendMessage(ChatColor.GRAY + "Downloads: " + ChatColor.AQUA + ModrinthUpdateService.PROJECT_PAGE_URL);
            return true;
        }
        if (!snap.checked()) {
            sender.sendMessage(ChatColor.GRAY + "Modrinth status: not checked yet (runs shortly after startup if check-update is enabled).");
            sender.sendMessage(ChatColor.GRAY + "Downloads: " + ChatColor.AQUA + ModrinthUpdateService.PROJECT_PAGE_URL);
            return true;
        }
        if (snap.latestModrinthVersion().isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Modrinth did not return any versions.");
            return true;
        }
        sender.sendMessage(ChatColor.GRAY + "Latest on Modrinth: " + ChatColor.GREEN + snap.latestModrinthVersion());
        int behind = snap.behind();
        if (behind < 0) {
            sender.sendMessage(ChatColor.YELLOW + "Cannot compare to Modrinth: use a release-style plugin version such as "
                    + ChatColor.WHITE + "26.1.2-v1.0.8" + ChatColor.YELLOW + " (see Modrinth version list).");
        } else if (behind == 0) {
            sender.sendMessage(ChatColor.GREEN + "You are running the latest Modrinth release (for parsed semver).");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Approximately " + ChatColor.WHITE + behind + ChatColor.YELLOW
                    + " Modrinth release(s) newer than this jar (semver compare).");
        }
        sender.sendMessage(ChatColor.GRAY + "Downloads: " + ChatColor.AQUA + ModrinthUpdateService.PROJECT_PAGE_URL);
        return true;
    }

    /**
     * {@code /normalshops stats-holograms list|cleanup} — inspect or remove stock/earnings {@link org.bukkit.entity.TextDisplay}s.
     */
    private boolean handleStatsHolograms(CommandSender sender, String[] args) {
        if (!sender.hasPermission("normalshops.stats-holograms")) {
            Message.NO_PERMISSION.send(sender);
            return true;
        }
        PrivateShopStatsHologramManager mgr = NormalShops.getInstance().getPrivateShopStatsHologramManager();
        if (mgr == null) {
            sender.sendMessage(ChatColor.RED + "Private stats hologram manager is not available.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.GRAY + "Usage: /normalshops stats-holograms <list|cleanup>");
            return true;
        }
        String sub = args[1].toLowerCase();
        if (sub.equals("list")) {
            mgr.sendStatsHologramReport(sender);
            return true;
        }
        if (sub.equals("cleanup")) {
            int removed = mgr.cleanupOrphanStatsDisplaysNearAllShops();
            sender.sendMessage(ChatColor.GREEN + "Removed " + removed
                    + " stats hologram TextDisplay(s) near loaded shops (including drifted / stacked duplicates). "
                    + "Registry cleared per cleaned shop; fresh holograms spawn on the next tick.");
            return true;
        }
        sender.sendMessage(ChatColor.GRAY + "Usage: /normalshops stats-holograms <list|cleanup>");
        return true;
    }

    /**
     * {@code /normalshops region make|cancel} — two corners via {@code /setpoint} (XZ area).
     */
    private boolean handleRegionCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
            return true;
        }
        if (!player.hasPermission("normalshops.region")) {
            Message.NO_PERMISSION.send(player);
            return true;
        }
        String sub = args[1].toLowerCase();
        if (sub.equals("make")) {
            if (args.length != 2) {
                return false;
            }
            ShopsRegionEditor.start(player);
            player.sendMessage(ChatColor.GREEN + "Stand at " + ChatColor.YELLOW + "two opposite corners"
                    + ChatColor.GREEN + " of the allowed /shops floor area (same world; height ignored). Run "
                    + ChatColor.AQUA + "/setpoint" + ChatColor.GREEN + " twice.");
            player.sendMessage(ChatColor.GRAY + "Cancel anytime: " + ChatColor.WHITE + "/normalshops region cancel");
            return true;
        }
        if (sub.equals("cancel")) {
            if (args.length != 2) {
                return false;
            }
            ShopsRegionEditor.cancel(player);
            player.sendMessage(ChatColor.YELLOW + "Cancelled shops menu region setup.");
            return true;
        }
        return false;
    }

    /** Lists shops or opens one remotely ({@code /viewshops}, {@code /normalshops view}, chat click). */
    private boolean handleViewShops(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
            return true;
        }
        if (!player.hasPermission("normalshops.viewshops")) {
            Message.NO_PERMISSION.send(player);
            return true;
        }
        if (args.length == 0) {
            openMyShopsGui(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("open") && args.length == 5) {
            try {
                UUID worldUid = UUID.fromString(args[1]);
                int x = Integer.parseInt(args[2]);
                int y = Integer.parseInt(args[3]);
                int z = Integer.parseInt(args[4]);
                World world = Bukkit.getWorld(worldUid);
                if (world == null) {
                    Message.VIEW_SHOPS_CANNOT_OPEN.send(player);
                    return true;
                }
                Location loc = new Location(world, x, y, z).getBlock().getLocation();
                ItemShop shop = ItemShop.get(loc);
                if (shop == null || shop.isDeleted()) {
                    Message.VIEW_SHOPS_CANNOT_OPEN.send(player);
                    return true;
                }
                if (!shop.isOwner(player) && !shop.isTrusted(player)) {
                    Message.VIEW_SHOPS_CANNOT_OPEN.send(player);
                    return true;
                }
                new ShopAccessChoiceMenu(player, shop).open();
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, .5f, .8f);
            } catch (IllegalArgumentException e) {
                Message.VIEW_SHOPS_CANNOT_OPEN.send(player);
            }
            return true;
        }
        return false;
    }

    private void openMyShopsGui(Player player) {
        List<ItemShop> shops = NormalShops.getInstance().getShopManager().listShopsAccessibleTo(player);
        if (shops.isEmpty()) {
            Message.VIEW_SHOPS_EMPTY.send(player);
            return;
        }
        new MyShopsListMenu(player, shops, 0).open();
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
        if ("viewshops".equalsIgnoreCase(command.getName())) {
            return List.of();
        }

        List<String> completions = new ArrayList<>();
        String partial = args.length > 0 ? args[args.length - 1].toLowerCase() : "";
        if (args.length == 1) {
            if (sender.hasPermission("normalshops.viewshops") && "view".startsWith(partial)) {
                completions.add("view");
            }
            if (sender.hasPermission("normalshops.reload") && "reload".startsWith(partial)) {
                completions.add("reload");
            }
            if (sender.hasPermission("normalshops.delete") && "delete".startsWith(partial)) {
                completions.add("delete");
            }
            if (sender.hasPermission("normalshops.restore") && "restore".startsWith(partial)) {
                completions.add("restore");
            }
            if (sender.hasPermission("normalshops.region") && "region".startsWith(partial)) {
                completions.add("region");
            }
            if (sender.hasPermission("normalshops.stats-holograms") && "stats-holograms".startsWith(partial)) {
                completions.add("stats-holograms");
            }
            if (sender.hasPermission("normalshops.version") && "version".startsWith(partial)) {
                completions.add("version");
            }
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("stats-holograms") && sender.hasPermission("normalshops.stats-holograms")) {
            if ("list".startsWith(partial)) {
                completions.add("list");
            }
            if ("cleanup".startsWith(partial)) {
                completions.add("cleanup");
            }
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("region") && sender.hasPermission("normalshops.region")) {
            if ("make".startsWith(partial)) {
                completions.add("make");
            }
            if ("cancel".startsWith(partial)) {
                completions.add("cancel");
            }
        }
        return completions;
    }
}
