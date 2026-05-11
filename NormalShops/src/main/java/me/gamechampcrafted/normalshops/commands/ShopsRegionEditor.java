package me.gamechampcrafted.normalshops.commands;

import me.gamechampcrafted.normalshops.Logger;
import me.gamechampcrafted.normalshops.data.Setting;
import me.gamechampcrafted.normalshops.menu.view.ShopsMenuRegions;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Admin workflow: {@code /normalshops region make} then two {@code /setpoint} uses (XZ rectangle; Y ignored).
 */
public final class ShopsRegionEditor {

    private static final ConcurrentHashMap<UUID, List<Map<String, Object>>> SESSIONS = new ConcurrentHashMap<>();

    private ShopsRegionEditor() {
    }

    public static boolean hasSession(Player player) {
        return SESSIONS.containsKey(player.getUniqueId());
    }

    public static void start(Player player) {
        SESSIONS.put(player.getUniqueId(), new ArrayList<>());
    }

    public static void cancel(Player player) {
        SESSIONS.remove(player.getUniqueId());
    }

    /** Corners recorded so far (0–2), or 0 if no session. */
    public static int sessionCornerCount(Player player) {
        List<Map<String, Object>> corners = SESSIONS.get(player.getUniqueId());
        return corners == null ? 0 : corners.size();
    }

    public enum AddCornerResult {
        NO_SESSION,
        WRONG_WORLD,
        RECORDED_NEED_MORE,
        COMPLETE_SAVED
    }

    public static AddCornerResult addCorner(Player player) {
        List<Map<String, Object>> corners = SESSIONS.get(player.getUniqueId());
        if (corners == null) {
            return AddCornerResult.NO_SESSION;
        }
        Location loc = player.getLocation().getBlock().getLocation();
        World world = loc.getWorld();
        if (world == null) {
            return AddCornerResult.NO_SESSION;
        }
        if (!corners.isEmpty()) {
            String firstWorld = String.valueOf(corners.get(0).get("world"));
            if (!firstWorld.equals(world.getName())) {
                return AddCornerResult.WRONG_WORLD;
            }
        }
        Map<String, Object> pt = new LinkedHashMap<>();
        pt.put("world", world.getName());
        pt.put("x", loc.getBlockX());
        pt.put("y", loc.getBlockY());
        pt.put("z", loc.getBlockZ());
        corners.add(pt);
        if (corners.size() < 2) {
            return AddCornerResult.RECORDED_NEED_MORE;
        }
        persistNewRegion(new ArrayList<>(corners));
        SESSIONS.remove(player.getUniqueId());
        return AddCornerResult.COMPLETE_SAVED;
    }

    private static void persistNewRegion(List<Map<String, Object>> cornersTwo) {
        FileConfiguration cfg = Setting.getConfig();
        if (cfg == null) {
            Logger.severe("Cannot append shops-menu-regions: config not loaded.");
            return;
        }
        List<Map<String, Object>> regions = shallowCopyRegions(cfg);
        Map<String, Object> region = new LinkedHashMap<>();
        List<Map<String, Object>> cornerList = new ArrayList<>();
        for (Map<String, Object> c : cornersTwo) {
            cornerList.add(new LinkedHashMap<>(c));
        }
        region.put("corners", cornerList);
        regions.add(region);
        cfg.set("shops-menu-regions", regions);
        Setting.saveSettings();
        ShopsMenuRegions.reload(cfg);
    }

    private static List<Map<String, Object>> shallowCopyRegions(FileConfiguration cfg) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<?, ?> raw : cfg.getMapList("shops-menu-regions")) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : raw.entrySet()) {
                copy.put(String.valueOf(e.getKey()), e.getValue());
            }
            out.add(copy);
        }
        return out;
    }
}
