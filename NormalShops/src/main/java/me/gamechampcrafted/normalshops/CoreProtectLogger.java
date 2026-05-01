package me.gamechampcrafted.normalshops;

import me.gamechampcrafted.normalshops.Logger;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CoreProtectLogger {

    private static CoreProtectAPI api;

    public static void initialize() {
        try {
            org.bukkit.plugin.Plugin plugin = NormalShops.getInstance().getServer().getPluginManager().getPlugin("CoreProtect");

            if (plugin == null || !(plugin instanceof CoreProtect)) {
                Logger.info("CoreProtect not found - logging disabled");
                return;
            }

            CoreProtectAPI coreProtect = ((CoreProtect) plugin).getAPI();
            if (!coreProtect.isEnabled()) {
                Logger.info("CoreProtect API not enabled - logging disabled");
                return;
            }


            api = coreProtect;
            Logger.info("CoreProtect API v" + coreProtect.APIVersion() + " initialized successfully - logging enabled");
        } catch (Exception e) {
            Logger.warning("CoreProtect initialization failed: " + e.getMessage());
        }
    }

    @Nullable
    public static CoreProtectAPI getAPI() {
        return api;
    }

    public static boolean isAvailable() {
        return api != null;
    }

    /**
     * Test method to verify CoreProtect API is working
     */
    public static void testAPI() {
        if (api == null) {
            Logger.warning("CoreProtect API not available for testing");
            return;
        }
        try {
            api.testAPI();
            Logger.info("CoreProtect API test successful");
        } catch (Exception e) {
            Logger.warning("CoreProtect API test failed: " + e.getMessage());
        }
    }

    public static void logShopCreate(Player player, Location location, ItemStack price, ItemStack product) {
        if (api == null) {
            Logger.info("CoreProtect API not available - skipping shop create log");
            return;
        }
        String message = "Created shop: " + formatItem(product) + " for " + formatItem(price);
        Logger.info("Logging shop create: " + player.getName() + " at " + formatLocation(location));
        logInteraction(player, location, message);
        addShopHistory(location, player.getName(), "SHOP_CREATE",
                "Created listing | receives: " + formatItem(price) + " | gives: " + formatItem(product));
    }

    public static void logShopDelete(Player player, Location location) {
        if (api == null) {
            Logger.info("CoreProtect API not available - skipping shop delete log");
            return;
        }
        String message = "Deleted shop";
        Logger.info("Logging shop delete: " + player.getName() + " at " + formatLocation(location));
        logInteraction(player, location, message);
        addShopHistory(location, player.getName(), "SHOP_DELETE", "Deleted shop");
    }

    public static void logShopBuy(Player buyer, Player owner, Location location, ItemStack price, ItemStack product) {
        if (api == null) {
            Logger.info("CoreProtect API not available - skipping shop buy log");
            return;
        }
        String message = "Bought " + formatItem(product) + " for " + formatItem(price) + " from " + owner.getName();
        Logger.info("Logging shop buy: " + buyer.getName() + " from " + owner.getName() + " at " + formatLocation(location));
        logInteraction(buyer, location, message);
        addShopHistory(location, buyer.getName(), "SHOP_BUY",
                "Paid: " + formatItem(price) + " | Received: " + formatItem(product) + " | Seller: " + owner.getName());
    }

    public static void logShopBuyVillager(Player buyer, String ownerName, Location location, ItemStack price, List<ItemStack> products) {
        if (api == null) {
            Logger.info("CoreProtect API not available - skipping villager shop buy log");
            return;
        }
        String seller = ownerName == null || ownerName.isEmpty() ? "unknown" : ownerName;
        String received = formatItems(products);
        String message = "[VILLAGER] Bought " + received + " for " + formatItem(price) + " from " + seller;
        Logger.info("Logging villager shop buy: " + buyer.getName() + " from " + seller + " at " + formatLocation(location));
        logInteraction(buyer, location, message);
        addShopHistory(location, buyer.getName(), "SHOP_BUY_VILLAGER",
                "Paid: " + formatItem(price) + " | Received: " + received + " | Seller: " + seller);
    }

    public static void logShopBuyEmulated(Player admin, Player owner, Location location, ItemStack price, ItemStack product) {
        if (api == null) {
            Logger.info("CoreProtect API not available - skipping emulated shop buy log");
            return;
        }
        String ownerName = owner != null ? owner.getName() : "unknown";
        String message = "[EMULATED] Bought " + formatItem(product) + " for " + formatItem(price)
                + " from " + ownerName + " (triggered by " + admin.getName() + ")";
        Logger.info("Logging emulated shop buy: " + admin.getName() + " at " + formatLocation(location));
        logInteraction("NormalShops-EmulatedSale", location, message, admin);
        addShopHistory(location, admin.getName(), "SHOP_BUY_EMULATED",
                "Paid: " + formatItem(price) + " | Received: " + formatItem(product) + " | Seller: " + ownerName);
    }

    public static void logShopEdit(Player player, Location location, String editType) {
        if (api == null) {
            Logger.info("CoreProtect API not available - skipping shop edit log");
            return;
        }
        String message = "Edited shop: " + editType;
        Logger.info("Logging shop edit: " + player.getName() + " " + editType + " at " + formatLocation(location));
        logInteraction(player, location, message);
        addShopHistory(location, player.getName(), "SHOP_EDIT", editType);
    }

    public static void logStockpileAdd(Player player, Location shopLocation, Location stockpileLocation) {
        if (api == null) {
            Logger.info("CoreProtect API not available - skipping stockpile add log");
            return;
        }
        String message = "Added stockpile at " + formatLocation(stockpileLocation);
        Logger.info("Logging stockpile add: " + player.getName() + " at " + formatLocation(shopLocation) + " -> " + formatLocation(stockpileLocation));
        logInteraction(player, shopLocation, message);
        addShopHistory(shopLocation, player.getName(), "SHOP_CONNECT_STOCKPILE",
                "Connected stockpile at " + formatLocation(stockpileLocation));
    }

    public static void logStockpileRemove(Player player, Location shopLocation, Location stockpileLocation) {
        if (api == null) {
            Logger.info("CoreProtect API not available - skipping stockpile remove log");
            return;
        }
        String message = "Removed stockpile at " + formatLocation(stockpileLocation);
        Logger.info("Logging stockpile remove: " + player.getName() + " at " + formatLocation(shopLocation) + " -> " + formatLocation(stockpileLocation));
        logInteraction(player, shopLocation, message);
        addShopHistory(shopLocation, player.getName(), "SHOP_DISCONNECT_STOCKPILE",
                "Disconnected stockpile at " + formatLocation(stockpileLocation));
    }

    public static void logEarningsPileCreate(Player player, Location location) {
        if (api == null) {
            Logger.info("CoreProtect API not available - skipping earnings pile create log");
            return;
        }
        String message = "Created earnings pile";
        Logger.info("Logging earnings pile create: " + player.getName() + " at " + formatLocation(location));
        logInteraction(player, location, message);
    }

    public static void logEarningsPileDelete(Player player, Location location) {
        if (api == null) {
            Logger.info("CoreProtect API not available - skipping earnings pile delete log");
            return;
        }
        String message = "Deleted earnings pile";
        Logger.info("Logging earnings pile delete: " + player.getName() + " at " + formatLocation(location));
        logInteraction(player, location, message);
    }

    public static void logEarningsCollect(Player player, Location location, ItemStack price, int amount) {
        if (api == null) {
            Logger.info("CoreProtect API not available - skipping earnings collect log");
            return;
        }
        String message = "Collected " + (amount * price.getAmount()) + " " + formatItem(price);
        Logger.info("Logging earnings collect: " + player.getName() + " at " + formatLocation(location) + " amount=" + amount);
        logInteraction(player, location, message);
        addShopHistory(location, player.getName(), "SHOP_COLLECT_EARNINGS",
                "Collected: " + (amount * price.getAmount()) + " " + formatItem(price));
    }

    public static void logShopConnect(Player player, Location shopLocation, Location targetLocation, String type) {
        if (api == null) {
            Logger.info("CoreProtect API not available - skipping shop connect log");
            return;
        }
        String message = "Connected " + type + " at " + formatLocation(targetLocation);
        Logger.info("Logging shop connect: " + player.getName() + " at " + formatLocation(shopLocation) + " -> " + formatLocation(targetLocation) + " type=" + type);
        logInteraction(player, shopLocation, message);
        addShopHistory(shopLocation, player.getName(), "SHOP_CONNECT_" + type.toUpperCase(),
                "Connected " + type + " at " + formatLocation(targetLocation));
    }

    public static void logShopDisconnect(Player player, Location shopLocation, Location targetLocation, String type) {
        if (api == null) {
            Logger.info("CoreProtect API not available - skipping shop disconnect log");
            return;
        }
        String message = "Disconnected " + type + " at " + formatLocation(targetLocation);
        Logger.info("Logging shop disconnect: " + player.getName() + " at " + formatLocation(shopLocation) + " -> " + formatLocation(targetLocation) + " type=" + type);
        logInteraction(player, shopLocation, message);
        addShopHistory(shopLocation, player.getName(), "SHOP_DISCONNECT_" + type.toUpperCase(),
                "Disconnected " + type + " at " + formatLocation(targetLocation));
    }

    public static List<HistoryEntry> getShopHistory(Location location, int lookbackSeconds) {
        List<HistoryEntry> detailedEntries = getPersistentHistory(location);
        if (!detailedEntries.isEmpty()) {
            return detailedEntries;
        }
        if (api == null || location.getWorld() == null) {
            return detailedEntries;
        }
        try {
            List<String[]> results = api.blockLookup(location.getBlock(), lookbackSeconds);
            if (results == null) {
                return Collections.emptyList();
            }
            List<HistoryEntry> entries = new ArrayList<>();
            for (String[] result : results) {
                CoreProtectAPI.ParseResult parsed = api.parseResult(result);
                if (parsed == null) continue;
                if (!location.getWorld().getName().equals(parsed.worldName())) continue;
                if (parsed.getX() != location.getBlockX()
                        || parsed.getY() != location.getBlockY()
                        || parsed.getZ() != location.getBlockZ()) {
                    continue;
                }
                entries.add(new HistoryEntry(
                        parsed.getTimestamp(),
                        parsed.getPlayer(),
                        parsed.getActionString(),
                        parsed.getType() == null ? "unknown" : parsed.getType().name(),
                        ""
                ));
            }
            entries.sort(Comparator.comparingLong(HistoryEntry::timestamp).reversed());
            return entries;
        } catch (Exception exception) {
            Logger.warning("CoreProtect history lookup failed: " + exception.getMessage());
            return Collections.emptyList();
        }
    }

    private static void logInteraction(Player player, Location location, String message) {
        logInteraction(player.getName(), location, message, player);
    }

    private static void logInteraction(String actorName, Location location, String message, @Nullable Player chatPlayer) {
        if (api == null) return;
        try {
            api.logInteraction(actorName, location);
            Logger.info("CoreProtect log successful: " + message);
        } catch (Exception e) {
            Logger.warning("CoreProtect logging failed: " + e.getMessage());
        }
    }

    private static String formatItem(ItemStack item) {
        if (item == null) return "unknown";
        String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName()
                : item.getType().name().toLowerCase().replace("_", " ");
        return item.getAmount() + "x " + name;
    }

    private static String formatLocation(Location loc) {
        return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private static String formatItems(List<ItemStack> items) {
        if (items == null || items.isEmpty()) return "unknown";
        return items.stream()
                .filter(item -> item != null)
                .map(CoreProtectLogger::formatItem)
                .collect(Collectors.joining(", "));
    }

    private static void addShopHistory(Location location, String actor, String action, String details) {
        ItemShop shop = ItemShop.get(location);
        if (shop == null) return;
        shop.addHistoryEntry(new HistoryEntry(System.currentTimeMillis() / 1000, actor, action, "N/A", details));
    }

    private static List<HistoryEntry> getPersistentHistory(Location location) {
        ItemShop shop = ItemShop.get(location);
        if (shop == null) return Collections.emptyList();
        List<HistoryEntry> copy = new ArrayList<>(shop.getHistoryEntries());
        copy.sort(Comparator.comparingLong(HistoryEntry::timestamp).reversed());
        return copy;
    }

    public record HistoryEntry(long timestamp, String actor, String action, String material, String details) {
    }
}
