package me.gamechampcrafted.normalshops.shop;

import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.data.Setting;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitTask;

import org.bukkit.command.CommandSender;
import org.bukkit.Color;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

/**
 * Floating {@link TextDisplay} stats above shops, visible only to the owner and trusted players
 * via {@link Player#hideEntity(org.bukkit.plugin.Plugin, Entity)} / {@link Player#showEntity}.
 */
public final class PrivateShopStatsHologramManager implements Listener {

    /** Match vanilla-ish interaction reach so the hologram appears when the targeted block is near the shop. */
    private static final int LOOK_MAX_DISTANCE = 12;

    /** Raycast hit block must be within this Chebyshev distance (same/adjacent blocks) of the shop anchor block. */
    private static final int LOOK_SHOP_BLOCK_BUFFER = 1;

    /** Spawn offset above the shop block (block center); must stay in sync with {@link #spawnDisplay}. */
    private static final double STATS_HOLOGRAM_OFFSET_Y = 2.18;
    private static final float STATS_HOLOGRAM_VIEW_RANGE = 1.25f;
    private static final int STATS_HOLOGRAM_BACKGROUND_ARGB = 0x78000000;

    private final NormalShops plugin;
    private final Map<String, UUID> holoByShopKey = new ConcurrentHashMap<>();
    private BukkitTask task;
    @Nullable
    private BukkitTask cleanupTask;

    public PrivateShopStatsHologramManager(NormalShops plugin) {
        this.plugin = plugin;
    }

    /** Starts the refresh task (idempotent). */
    public void start() {
        if (task != null) {
            restartCleanupTask();
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        restartCleanupTask();
    }

    /** Schedules periodic removal of stacked/drifted stats holograms (interval from {@link Setting#STATS_HOLOGRAM_AUTO_CLEANUP_TICKS}). */
    private void restartCleanupTask() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        int ticks = Setting.STATS_HOLOGRAM_AUTO_CLEANUP_TICKS.getInt();
        if (ticks <= 0) {
            return;
        }
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupOrphanStatsDisplaysNearAllShops, ticks, ticks);
    }

    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        if (task != null) {
            task.cancel();
            task = null;
        }
        purgeAllStatsHolograms();
    }

    /** Called after {@link Setting#reload()}; removes tracked entities and orphans at shop positions. */
    public void reload() {
        purgeAllStatsHolograms();
        restartCleanupTask();
    }

    /**
     * Removes every tracked stats hologram entity, clears bookkeeping, then deletes stray
     * {@link TextDisplay}s at our spawn offset/fingerprint near each registered shop (e.g. after PlugMan reload).
     */
    public void purgeAllStatsHolograms() {
        removeAllEntities();
        holoByShopKey.clear();
        cleanupOrphanStatsDisplaysNearAllShops();
    }

    /**
     * Removes stacked / drifted stock–earnings stats {@link TextDisplay}s near each shop (loaded chunks only),
     * clears the in-memory UUID for each cleaned shop so the next tick spawns a single fresh hologram.
     *
     * @return how many entities were removed
     */
    public int cleanupOrphanStatsDisplaysNearAllShops() {
        ShopManager sm = plugin.getShopManager();
        if (sm == null) {
            return 0;
        }
        int removed = 0;
        for (Location loc : new ArrayList<>(sm.getUUIDMap().keySet())) {
            World w = loc.getWorld();
            if (w == null || !loc.getChunk().isLoaded()) {
                continue;
            }
            removed += removeStatsHologramCandidatesNearShopBlock(loc);
            holoByShopKey.remove(locationKey(loc));
        }
        return removed;
    }

    /**
     * Admin: list tracked UUIDs and scan loaded shops for TextDisplays matching the stock/earnings hologram fingerprint.
     */
    public void sendStatsHologramReport(CommandSender sender) {
        sender.sendMessage(org.bukkit.ChatColor.GOLD + "=== Private stats holograms (Stock / Earnings) ===");
        sender.sendMessage(org.bukkit.ChatColor.GRAY + "Tracked registry entries: " + holoByShopKey.size());
        ShopManager sm = plugin.getShopManager();
        for (Map.Entry<String, UUID> e : holoByShopKey.entrySet()) {
            Entity ent = Bukkit.getEntity(e.getValue());
            String extra;
            if (ent instanceof TextDisplay td) {
                boolean fingerprint = matchesPrivateStatsHologramFingerprint(td);
                extra = org.bukkit.ChatColor.GREEN + "loaded at "
                        + formatCoords(ent.getLocation())
                        + (fingerprint ? "" : org.bukkit.ChatColor.RED + " (fingerprint mismatch!)");
            } else {
                extra = org.bukkit.ChatColor.RED + "not loaded / dead";
            }
            sender.sendMessage(org.bukkit.ChatColor.YELLOW + e.getKey()
                    + org.bukkit.ChatColor.GRAY + " -> " + e.getValue() + " " + extra);
        }
        if (sm == null) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "ShopManager unavailable; skipping world scan.");
            return;
        }
        sender.sendMessage(org.bukkit.ChatColor.GOLD + "Scanning loaded shop chunks for matching TextDisplays…");
        int matches = 0;
        int orphans = 0;
        for (Location shopLoc : new ArrayList<>(sm.getUUIDMap().keySet())) {
            World w = shopLoc.getWorld();
            if (w == null || !shopLoc.getChunk().isLoaded()) {
                continue;
            }
            Location spawn = statsHologramSpawnLocation(shopLoc);
            if (spawn == null) {
                continue;
            }
            Collection<Entity> nearby = w.getNearbyEntities(spawn, 0.6, 0.6, 0.6);
            String key = locationKey(shopLoc);
            UUID trackedId = holoByShopKey.get(key);
            for (Entity en : nearby) {
                if (!(en instanceof TextDisplay td)) {
                    continue;
                }
                if (!matchesPrivateStatsHologramFingerprint(td)) {
                    continue;
                }
                matches++;
                if (trackedId == null || !trackedId.equals(td.getUniqueId())) {
                    orphans++;
                    sender.sendMessage(org.bukkit.ChatColor.RED + "Untracked / mismatched UUID at shop "
                            + key + " → entity " + td.getUniqueId() + " @ " + formatCoords(td.getLocation()));
                }
            }
        }
        sender.sendMessage(org.bukkit.ChatColor.GRAY + "Fingerprint matches near shops: " + matches
                + "; untracked or UUID mismatch: " + orphans);
        sender.sendMessage(org.bukkit.ChatColor.GRAY + "Run " + org.bukkit.ChatColor.YELLOW + "/normalshops stats-holograms cleanup"
                + org.bukkit.ChatColor.GRAY + " to remove stacked/stray displays (shop storefront sale lines use VERTICAL billboard and are kept).");
    }

    private static String formatCoords(Location loc) {
        return loc.getWorld() != null ? loc.getWorld().getName() + " "
                + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()
                : "?";
    }

    /**
     * Removes stacked or drifted stock/earnings stats {@link TextDisplay}s above this shop.
     * <ul>
     *     <li>Strict fingerprint match (normal case)</li>
     *     <li>{@link Display.Billboard#CENTER} + non-persistent — catches entities whose view range / background drifted
     *     off the fingerprint (these show as “mismatch” in list). Glass sale text uses {@link Display.Billboard#VERTICAL}
     *     and is outside this volume / not removed.</li>
     * </ul>
     */
    private int removeStatsHologramCandidatesNearShopBlock(Location shopBlock) {
        World w = shopBlock.getWorld();
        if (w == null || !shopBlock.getChunk().isLoaded()) {
            return 0;
        }
        Location spawn = statsHologramSpawnLocation(shopBlock);
        if (spawn == null) {
            return 0;
        }
        int removed = 0;
        for (Entity e : new ArrayList<>(w.getNearbyEntities(spawn, 0.6, 0.6, 0.6))) {
            if (!(e instanceof TextDisplay td)) {
                continue;
            }
            if (matchesPrivateStatsHologramFingerprint(td)) {
                td.remove();
                removed++;
                continue;
            }
            if (td.getBillboard() == Display.Billboard.CENTER && !td.isPersistent()) {
                td.remove();
                removed++;
            }
        }
        return removed;
    }

    private static Location statsHologramSpawnLocation(Location shopBlock) {
        Location block = shopBlock.getBlock().getLocation();
        World world = block.getWorld();
        if (world == null) {
            return null;
        }
        return block.clone().add(0.5, STATS_HOLOGRAM_OFFSET_Y, 0.5);
    }

    /**
     * Identifies our stock/earnings {@link TextDisplay}s (distinct from storefront sale text: VERTICAL billboard).
     */
    private static boolean matchesPrivateStatsHologramFingerprint(TextDisplay td) {
        if (!td.isValid()) {
            return false;
        }
        if (td.getBillboard() != Display.Billboard.CENTER) {
            return false;
        }
        if (!td.isSeeThrough()) {
            return false;
        }
        if (td.isPersistent()) {
            return false;
        }
        if (Math.abs(td.getViewRange() - STATS_HOLOGRAM_VIEW_RANGE) > 0.06f) {
            return false;
        }
        Color bg = td.getBackgroundColor();
        if (bg == null || colorArgb(bg) != STATS_HOLOGRAM_BACKGROUND_ARGB) {
            return false;
        }
        return true;
    }

    private static int colorArgb(Color c) {
        return (c.getAlpha() << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }

    /**
     * Remove hologram when a shop is unregistered so we do not orphan entities.
     */
    public void removeShop(Location shopBlockLocation) {
        String key = locationKey(shopBlockLocation);
        UUID id = holoByShopKey.remove(key);
        removeEntityIfPresent(id);
    }

    /** Called when the owner toggles per-shop stats hologram off/on in settings. */
    public void onShopPrivateStatsHologramSettingChanged(ItemShop shop) {
        if (!shop.isPrivateStatsHologramEnabled()) {
            removeShop(shop.getLocation());
        }
    }

    private void tick() {
        if (!Setting.PRIVATE_SHOP_STATS_HOLOGRAMS.isEnabled()) {
            if (!holoByShopKey.isEmpty()) {
                removeAllEntities();
                holoByShopKey.clear();
            }
            return;
        }

        ShopManager sm = plugin.getShopManager();
        if (sm == null) {
            return;
        }

        for (Location loc : new ArrayList<>(sm.getUUIDMap().keySet())) {
            World lw = loc.getWorld();
            if (lw == null || !lw.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                continue;
            }
            ItemShop shop = sm.getShop(loc);
            if (shop == null) {
                continue;
            }
            if (shop.isDeleted()) {
                removeShop(loc);
                continue;
            }
            if (!shop.isPrivateStatsHologramEnabled()) {
                removeShop(loc);
                continue;
            }

            String key = locationKey(loc);
            UUID existingId = holoByShopKey.get(key);
            TextDisplay display = existingId == null ? null : asTextDisplay(existingId);

            if (display == null || display.isDead()) {
                removeEntityIfPresent(existingId);
                holoByShopKey.remove(key);
                display = spawnDisplay(shop);
                if (display != null) {
                    holoByShopKey.put(key, display.getUniqueId());
                }
            } else {
                updateText(display, shop);
                applyVisibility(display, shop);
            }
        }

        Iterator<Map.Entry<String, UUID>> it = holoByShopKey.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, UUID> e = it.next();
            Location shopLoc = parseKey(e.getKey());
            if (shopLoc == null || sm.getShop(shopLoc) == null) {
                removeEntityIfPresent(e.getValue());
                it.remove();
            }
        }
    }

    private TextDisplay spawnDisplay(ItemShop shop) {
        Location base = statsHologramSpawnLocation(shop.getLocation());
        if (base == null) {
            return null;
        }
        World world = base.getWorld();
        TextDisplay td = (TextDisplay) world.spawnEntity(base, EntityType.TEXT_DISPLAY);
        td.setPersistent(false);
        td.setBillboard(Display.Billboard.CENTER);
        td.setAlignment(TextDisplay.TextAlignment.CENTER);
        td.setSeeThrough(true);
        td.setShadowed(true);
        td.setViewRange(STATS_HOLOGRAM_VIEW_RANGE);
        td.setBackgroundColor(Color.fromARGB(STATS_HOLOGRAM_BACKGROUND_ARGB));
        // Display entities may broadcast to all viewers before per-player hide/show runs; hide from
        // everyone first, then allow only owner/trusted with applyVisibility.
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.hideEntity(plugin, td);
        }
        trySetVisibleByDefaultFalse(td);
        updateText(td, shop);
        applyVisibility(td, shop);
        return td;
    }

    /**
     * Paper (and newer Spigot): entity is not sent to clients until {@link Player#showEntity};
     * avoids one tick where everyone could see the display. No-op on older APIs.
     */
    private static void trySetVisibleByDefaultFalse(Entity entity) {
        try {
            Method m = Entity.class.getMethod("setVisibleByDefault", boolean.class);
            m.invoke(entity, Boolean.FALSE);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static TextDisplay asTextDisplay(UUID id) {
        Entity e = Bukkit.getEntity(id);
        return e instanceof TextDisplay td ? td : null;
    }

    private void updateText(TextDisplay display, ItemShop shop) {
        String stockLine;
        if (shop.isAdminShop()) {
            stockLine = "&aStock: &7(unlimited)";
        } else {
            stockLine = "&aStock: &f" + shop.formatStatsStockSummary();
        }
        String earnLine;
        if (shop.isAdminShop()) {
            earnLine = "&eEarnings: &7—";
        } else {
            earnLine = "&eEarnings: &f" + shop.formatStatsEarningsSummary();
        }
        display.setText(Utils.colorize(stockLine + "\n" + earnLine));
    }

    private void applyVisibility(TextDisplay entity, ItemShop shop) {
        World entityWorld = entity.getWorld();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().equals(entityWorld)) {
                p.hideEntity(plugin, entity);
                continue;
            }
            if (!allowed(p, shop)) {
                p.hideEntity(plugin, entity);
                continue;
            }
            if (shouldSeeHologram(p, shop)) {
                p.showEntity(plugin, entity);
            } else {
                p.hideEntity(plugin, entity);
            }
        }
    }

    private boolean shouldSeeHologram(Player player, ItemShop shop) {
        return shop.isPrivateStatsHologramEnabled() && isLookingAtShopBlock(player, shop);
    }

    /** True if the block the player is looking at is the shop block or within {@link #LOOK_SHOP_BLOCK_BUFFER} on each axis (inclusive). */
    private static boolean isLookingAtShopBlock(Player player, ItemShop shop) {
        if (!player.getWorld().equals(shop.getLocation().getWorld())) {
            return false;
        }
        Block hit = player.getTargetBlockExact(LOOK_MAX_DISTANCE, FluidCollisionMode.NEVER);
        if (hit == null) {
            return false;
        }
        Location sb = shop.getLocation().getBlock().getLocation();
        if (!hit.getWorld().equals(sb.getWorld())) {
            return false;
        }
        int dx = Math.abs(hit.getX() - sb.getBlockX());
        int dy = Math.abs(hit.getY() - sb.getBlockY());
        int dz = Math.abs(hit.getZ() - sb.getBlockZ());
        return dx <= LOOK_SHOP_BLOCK_BUFFER && dy <= LOOK_SHOP_BLOCK_BUFFER && dz <= LOOK_SHOP_BLOCK_BUFFER;
    }

    private static boolean allowed(Player player, ItemShop shop) {
        return shop.isOwner(player) || shop.isTrusted(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!Setting.PRIVATE_SHOP_STATS_HOLOGRAMS.isEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> syncPlayer(player));
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        if (!Setting.PRIVATE_SHOP_STATS_HOLOGRAMS.isEnabled()) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> syncPlayer(event.getPlayer()));
    }

    private void syncPlayer(Player player) {
        ShopManager sm = plugin.getShopManager();
        if (sm == null) {
            return;
        }
        for (Map.Entry<String, UUID> e : holoByShopKey.entrySet()) {
            Location shopLoc = parseKey(e.getKey());
            Entity ent = Bukkit.getEntity(e.getValue());
            if (!(ent instanceof TextDisplay td)) {
                continue;
            }
            if (!player.getWorld().equals(td.getWorld())) {
                player.hideEntity(plugin, td);
                continue;
            }
            ItemShop shop = shopLoc == null ? null : sm.getShop(shopLoc);
            if (shop == null || shop.isDeleted()) {
                player.hideEntity(plugin, td);
                continue;
            }
            if (!allowed(player, shop)) {
                player.hideEntity(plugin, td);
                continue;
            }
            if (shouldSeeHologram(player, shop)) {
                player.showEntity(plugin, td);
            } else {
                player.hideEntity(plugin, td);
            }
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        int cx = event.getChunk().getX();
        int cz = event.getChunk().getZ();
        World world = event.getWorld();

        Iterator<Map.Entry<String, UUID>> it = holoByShopKey.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, UUID> e = it.next();
            Location shopLoc = parseKey(e.getKey());
            if (shopLoc == null || shopLoc.getWorld() == null || !shopLoc.getWorld().equals(world)) {
                continue;
            }
            if ((shopLoc.getBlockX() >> 4) != cx || (shopLoc.getBlockZ() >> 4) != cz) {
                continue;
            }
            removeEntityIfPresent(e.getValue());
            it.remove();
        }
    }

    private void removeEntityIfPresent(UUID id) {
        if (id == null) {
            return;
        }
        Entity e = Bukkit.getEntity(id);
        if (e != null) {
            e.remove();
        }
    }

    private void removeAllEntities() {
        for (UUID id : holoByShopKey.values()) {
            removeEntityIfPresent(id);
        }
    }

    private static String locationKey(Location loc) {
        Location b = blockLoc(loc);
        World w = b.getWorld();
        if (w == null) {
            return "";
        }
        return w.getName() + "|" + b.getBlockX() + "|" + b.getBlockY() + "|" + b.getBlockZ();
    }

    private static Location blockLoc(Location loc) {
        return loc.getBlock().getLocation();
    }

    private static Location parseKey(String key) {
        String[] p = key.split("\\|");
        if (p.length != 4) {
            return null;
        }
        World w = Bukkit.getWorld(p[0]);
        if (w == null) {
            return null;
        }
        try {
            int x = Integer.parseInt(p[1]);
            int y = Integer.parseInt(p[2]);
            int z = Integer.parseInt(p[3]);
            return new Location(w, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
