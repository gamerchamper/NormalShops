package me.gamechampcrafted.normalshops.menu.view;

import me.gamechampcrafted.normalshops.Logger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Loaded from {@code shops-menu-regions} in config.yml. When the list is empty, no region filter is applied.
 * Each region is a horizontal rectangle: two corners define min/max X and Z on the ground plane; Y is ignored.
 */
public final class ShopsMenuRegions {

    private static List<Region> regions = List.of();

    private ShopsMenuRegions() {
    }

    public static void reload(FileConfiguration cfg) {
        List<Map<?, ?>> raw = cfg.getMapList("shops-menu-regions");
        if (raw == null || raw.isEmpty()) {
            regions = List.of();
            return;
        }
        List<Region> next = new ArrayList<>();
        for (Map<?, ?> m : raw) {
            if (m == null) {
                continue;
            }
            Region fromCorners = regionFromCorners(m.get("corners"));
            if (fromCorners != null) {
                next.add(fromCorners);
                continue;
            }
            Region legacy = regionFromLegacyXZ(m);
            if (legacy != null) {
                next.add(legacy);
            }
        }
        regions = Collections.unmodifiableList(next);
    }

    /**
     * Two or more corner maps ({@code world}, {@code x}, {@code z}; {@code y} optional, ignored) define the XZ rectangle.
     * Older entries may list four corners — all are projected onto XZ (height never restricts inclusion).
     */
    private static Region regionFromCorners(Object cornersObj) {
        if (!(cornersObj instanceof List<?> cornersList) || cornersList.isEmpty()) {
            return null;
        }
        List<XzPoint> points = new ArrayList<>();
        for (Object o : cornersList) {
            if (!(o instanceof Map<?, ?> cm)) {
                continue;
            }
            XzPoint p = parseCorner(cm);
            if (p != null) {
                points.add(p);
            }
        }
        if (points.size() < 2) {
            Logger.warning("[NormalShops] shops-menu-regions: each entry needs at least 2 corners (world, x, z). Got "
                    + points.size() + " valid corner(s); skipping that entry.");
            return null;
        }
        String world = points.get(0).world;
        for (XzPoint p : points) {
            if (!world.equals(p.world)) {
                Logger.warning("[NormalShops] shops-menu-regions: all corners must use the same world ("
                        + world + " vs " + p.world + "); skipping that entry.");
                return null;
            }
        }
        int minX = points.get(0).x;
        int maxX = minX;
        int minZ = points.get(0).z;
        int maxZ = minZ;
        for (XzPoint p : points) {
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minZ = Math.min(minZ, p.z);
            maxZ = Math.max(maxZ, p.z);
        }
        return new Region(world, minX, maxX, minZ, maxZ);
    }

    private static XzPoint parseCorner(Map<?, ?> cm) {
        String world = stringVal(cm.get("world"));
        Integer x = intVal(cm.get("x"));
        Integer z = intVal(cm.get("z"));
        if (world == null || world.isEmpty() || x == null || z == null) {
            return null;
        }
        return new XzPoint(world, x, z);
    }

    private static Region regionFromLegacyXZ(Map<?, ?> m) {
        String world = stringVal(m.get("world"));
        if (world == null || world.isEmpty()) {
            return null;
        }
        Integer minX = intVal(m.get("min-x"));
        Integer maxX = intVal(m.get("max-x"));
        Integer minZ = intVal(m.get("min-z"));
        Integer maxZ = intVal(m.get("max-z"));
        if (minX == null || maxX == null || minZ == null || maxZ == null) {
            return null;
        }
        int ax = Math.min(minX, maxX);
        int bx = Math.max(minX, maxX);
        int az = Math.min(minZ, maxZ);
        int bz = Math.max(minZ, maxZ);
        return new Region(world, ax, bx, az, bz);
    }

    /**
     * @return true when there is no whitelist, or the shop block's X/Z lies inside at least one region (same world).
     */
    public static boolean allowsShopAt(Location location) {
        if (regions.isEmpty()) {
            return true;
        }
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        String worldName = world.getName();
        int x = location.getBlockX();
        int z = location.getBlockZ();
        for (Region r : regions) {
            if (!r.world.equals(worldName)) {
                continue;
            }
            if (x >= r.minX && x <= r.maxX && z >= r.minZ && z <= r.maxZ) {
                return true;
            }
        }
        return false;
    }

    private static String stringVal(Object o) {
        return o == null ? null : String.valueOf(o).trim();
    }

    private static Integer intVal(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record XzPoint(String world, int x, int z) {
    }

    private record Region(String world, int minX, int maxX, int minZ, int maxZ) {
    }
}
