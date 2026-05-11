package me.gamechampcrafted.normalshops.commands;

import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.Setting;
import me.gamechampcrafted.normalshops.menu.view.PublicShopsListMenu;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.utils.ShopItemQueryMatcher;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

public class ShopsCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "normalshops.shops";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
            return true;
        }
        if (!player.hasPermission(PERMISSION)) {
            Message.NO_PERMISSION.send(player);
            return true;
        }
        if (!Setting.SHOPS_COMMAND_ENABLED.isEnabled()) {
            Message.FEATURE_DISABLED.send(player);
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("item")) {
            return handleItemSearch(player, args, true);
        }
        if (args.length >= 1) {
            // /shops diamond — same as /shops item diamond (filter by product)
            return handleItemSearch(player, args, false);
        }

        List<ItemShop> shops = NormalShops.getInstance().getShopManager().listPublicInStockShops();
        if (shops.isEmpty()) {
            Message.SHOPS_MENU_EMPTY.send(player);
            return true;
        }
        new PublicShopsListMenu(player, shops, 0).open();
        return true;
    }

    /**
     * @param itemKeyword when true, args are {@code item <query...>}; when false, args are the whole query ({@code diamond}, {@code iron sword}).
     */
    private boolean handleItemSearch(Player player, String[] args, boolean itemKeyword) {
        String query;
        if (itemKeyword) {
            if (args.length < 2) {
                Message.SHOPS_ITEM_USAGE.send(player);
                return true;
            }
            query = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
        } else {
            query = String.join(" ", args).trim();
        }
        if (query.isEmpty()) {
            Message.SHOPS_ITEM_USAGE.send(player);
            return true;
        }

        List<Material> materials = ShopItemQueryMatcher.resolve(query);
        if (materials.isEmpty()) {
            Message.SHOPS_ITEM_NO_MATCH.parameterizer().put("query", query).send(player);
            return true;
        }

        List<ItemShop> shops = NormalShops.getInstance().getShopManager().listPublicInStockShopsSelling(materials);
        if (shops.isEmpty()) {
            Message.SHOPS_ITEM_NO_SHOPS_SELLING.parameterizer().put("query", query).send(player);
            return true;
        }

        String subtitle = buildSearchSubtitle(materials, query);
        new PublicShopsListMenu(player, shops, 0, subtitle).open();
        return true;
    }

    private static String buildSearchSubtitle(List<Material> materials, String query) {
        if (materials.size() == 1) {
            return Utils.getFormattedName(new ItemStack(materials.get(0)));
        }
        if (materials.size() <= 5) {
            StringJoiner j = new StringJoiner(", ");
            for (Material m : materials) {
                j.add(Utils.getFormattedName(new ItemStack(m)));
            }
            return j.toString();
        }
        return materials.size() + " item types · " + query;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission(PERMISSION)) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            String p = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            if ("item".startsWith(p)) {
                out.add("item");
            }
            return out;
        }
        return Collections.emptyList();
    }
}
