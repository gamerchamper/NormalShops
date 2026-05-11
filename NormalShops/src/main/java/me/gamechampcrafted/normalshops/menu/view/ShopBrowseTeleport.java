package me.gamechampcrafted.normalshops.menu.view;

import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Chest;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Safe stand position in front of the shop container (latch side) and per-player teleport cooldown.
 */
public final class ShopBrowseTeleport {

    private static final ConcurrentHashMap<java.util.UUID, Long> LAST_TELEPORT_MS = new ConcurrentHashMap<>();

    private ShopBrowseTeleport() {
    }

    /**
     * Location centered in the air block in front of the chest/barrel “front” (where the latch texture faces you).
     * Picks a nearby safe spot if the primary cell is blocked.
     */
    public static Location standLocationInFront(ItemShop shop) {
        Location base = shop.getLocation().clone();
        World world = base.getWorld();
        if (world == null) {
            return base.clone().add(0.5, 1, 0.5);
        }
        Block shopBlock = base.getBlock();
        BlockFace outward = resolveStandDirection(shop, shopBlock);

        int cx = base.getBlockX();
        int cy = base.getBlockY();
        int cz = base.getBlockZ();
        int ox = outward.getModX();
        int oz = outward.getModZ();

        BlockFace left = rotateHorizontalCCW(outward);
        int lx = left.getModX();
        int lz = left.getModZ();

        List<long[]> columns = new ArrayList<>();
        columns.add(offset(cx, cz, ox, oz));
        columns.add(offset(cx, cz, ox + lx, oz + lz));
        columns.add(offset(cx, cz, ox - lx, oz - lz));
        columns.add(offset(cx, cz, 2 * ox, 2 * oz));
        columns.add(offset(cx, cz, ox + 2 * lx, oz + 2 * lz));
        columns.add(offset(cx, cz, ox - 2 * lx, oz - 2 * lz));

        Location chestCenter = base.clone().add(0.5, 0.5, 0.5);

        for (long[] col : columns) {
            int bx = (int) col[0];
            int bz = (int) col[1];
            Location found = findVerticalSafeStand(world, bx, bz, cy, chestCenter);
            if (found != null) {
                return found;
            }
        }

        // Fallback: center in front at chest Y even if not ideal
        Location fallback = new Location(world, cx + ox + 0.5, cy, cz + oz + 0.5);
        pointYawAt(fallback, chestCenter);
        return fallback;
    }

    private static long[] offset(int cx, int cz, int dx, int dz) {
        return new long[]{cx + dx, cz + dz};
    }

    /**
     * Horizontal direction from chest block toward where the player should stand (into the block in front of the latch).
     * Chest/Barrel {@link Directional#getFacing()} is where the container faces; the latch/front is on that side,
     * so the player stands on the opposite cardinal neighbor.
     */
    private static BlockFace resolveStandDirection(ItemShop shop, Block block) {
        BlockData data = block.getBlockData();
        BlockFace facing = facingFromData(data);
        if (facing != null) {
            return facing.getOppositeFace();
        }
        String saved = shop.getContainerBlockData();
        if (saved != null && !saved.isEmpty()) {
            try {
                BlockData parsed = Bukkit.createBlockData(saved);
                facing = facingFromData(parsed);
                if (facing != null) {
                    return facing.getOppositeFace();
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return BlockFace.SOUTH;
    }

    @Nullable
    private static BlockFace facingFromData(BlockData data) {
        if (data instanceof Chest chest) {
            return chest.getFacing();
        }
        if (data instanceof Directional dir) {
            BlockFace f = dir.getFacing();
            if (isHorizontal(f)) {
                return f;
            }
        }
        return null;
    }

    private static boolean isHorizontal(BlockFace f) {
        return f == BlockFace.NORTH || f == BlockFace.SOUTH || f == BlockFace.EAST || f == BlockFace.WEST;
    }

    private static BlockFace rotateHorizontalCCW(BlockFace f) {
        return switch (f) {
            case NORTH -> BlockFace.WEST;
            case WEST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.EAST;
            case EAST -> BlockFace.NORTH;
            default -> BlockFace.WEST;
        };
    }

    private static Location findVerticalSafeStand(World world, int bx, int bz, int chestY, Location lookAt) {
        for (int dy = 2; dy >= -6; dy--) {
            int y = chestY + dy;
            Location feet = new Location(world, bx + 0.5, y, bz + 0.5);
            if (canStandHere(world, bx, y, bz)) {
                pointYawAt(feet, lookAt);
                return feet;
            }
        }
        return null;
    }

    private static boolean canStandHere(World world, int x, int feetBlockY, int z) {
        Block feet = world.getBlockAt(x, feetBlockY, z);
        Block head = world.getBlockAt(x, feetBlockY + 1, z);
        Block floor = world.getBlockAt(x, feetBlockY - 1, z);
        if (isHazard(feet.getType()) || isHazard(head.getType()) || isHazard(floor.getType())) {
            return false;
        }
        if (!isSolidFloor(floor.getType())) {
            return false;
        }
        return allowsBody(feet.getType()) && allowsBody(head.getType());
    }

    private static boolean allowsBody(Material m) {
        if (m == Material.AIR || m == Material.CAVE_AIR || m == Material.VOID_AIR) {
            return true;
        }
        if (m == Material.WATER) {
            return true;
        }
        return !m.isSolid();
    }

    private static boolean isSolidFloor(Material m) {
        if (m == Material.LAVA || m == Material.FIRE || m == Material.MAGMA_BLOCK || m == Material.CACTUS) {
            return false;
        }
        return m.isSolid();
    }

    private static boolean isHazard(Material m) {
        return m == Material.LAVA || m == Material.FIRE || m == Material.MAGMA_BLOCK
                || m == Material.CACTUS || m == Material.SWEET_BERRY_BUSH
                || m == Material.WITHER_ROSE || m == Material.POINTED_DRIPSTONE;
    }

    private static void pointYawAt(Location fromFeet, Location target) {
        Vector to = target.toVector().subtract(fromFeet.toVector());
        if (to.lengthSquared() > 1e-6) {
            fromFeet.setDirection(to);
        }
    }

    /**
     * @return remaining cooldown in milliseconds, or 0 if none / expired.
     */
    public static long remainingCooldownMs(Player player, int cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            return 0L;
        }
        Long last = LAST_TELEPORT_MS.get(player.getUniqueId());
        if (last == null) {
            return 0L;
        }
        long elapsed = System.currentTimeMillis() - last;
        long period = cooldownSeconds * 1000L;
        return Math.max(0L, period - elapsed);
    }

    public static void markTeleport(Player player) {
        LAST_TELEPORT_MS.put(player.getUniqueId(), System.currentTimeMillis());
    }
}
