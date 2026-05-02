package me.gamechampcrafted.normalshops.shop;

import me.gamechampcrafted.normalshops.Logger;
import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.data.LegacySerializationYaml;
import me.gamechampcrafted.normalshops.serialization.ItemShopSerializer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Persists a snapshot of every shop when it is removed from the live manager (delete, transfer, etc.)
 * so it can be restored later at a block with {@link #findLatestShopDataForBlock(Location)}.
 */
public final class ShopBackupService {

    private static final int MAX_BACKUP_ENTRIES = 2500;
    private static final String ROOT_KEY = "backups";

    private final File file;
    private FileConfiguration config;

    public ShopBackupService(Plugin plugin) throws IOException {
        File dir = plugin instanceof NormalShops
                ? ((NormalShops) plugin).getResolvedDataFolder()
                : plugin.getDataFolder();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create plugin data folder");
        }
        this.file = new File(dir, "shop-backups.yml");
        if (!file.exists() && !file.createNewFile()) {
            throw new IOException("Could not create shop-backups.yml");
        }
        this.config = LegacySerializationYaml.loadConfiguration(file);
    }

    /**
     * Records a full serialized snapshot of the shop before it is unregistered.
     * Safe to call from the main thread only.
     */
    public void recordRemoval(ItemShop shop) {
        try {
            Location loc = shop.getLocation().getBlock().getLocation();
            World world = loc.getWorld();
            if (world == null) {
                return;
            }
            Map<String, Object> entry = new HashMap<>();
            entry.put("saved-at", System.currentTimeMillis());
            entry.put("reason", "removed");
            entry.put("world", world.getName());
            entry.put("x", loc.getBlockX());
            entry.put("y", loc.getBlockY());
            entry.put("z", loc.getBlockZ());
            entry.put("shop", ItemShopSerializer.serialize(shop));

            List<Map<String, Object>> backups = getBackupListMutable();
            backups.add(entry);
            trimOldest(backups);
            config.set(ROOT_KEY, backups);
            config.save(file);
        } catch (Exception e) {
            Logger.warning("Failed to save shop backup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Most recent backup whose original shop block matches this world/block position.
     */
    @SuppressWarnings("unchecked")
    public @Nullable Map<String, Object> findLatestShopDataForBlock(Location blockLocation) {
        reload();
        World world = blockLocation.getWorld();
        if (world == null) {
            return null;
        }
        String w = world.getName();
        int x = blockLocation.getBlockX();
        int y = blockLocation.getBlockY();
        int z = blockLocation.getBlockZ();

        List<Map<?, ?>> raw = config.getMapList(ROOT_KEY);
        for (int i = raw.size() - 1; i >= 0; i--) {
            Map<?, ?> entry = raw.get(i);
            if (!Objects.equals(w, String.valueOf(entry.get("world")))) {
                continue;
            }
            if (asInt(entry.get("x")) != x || asInt(entry.get("y")) != y || asInt(entry.get("z")) != z) {
                continue;
            }
            Object shopObj = entry.get("shop");
            if (shopObj instanceof Map) {
                return (Map<String, Object>) shopObj;
            }
        }
        return null;
    }

    public void reload() {
        try {
            config = LegacySerializationYaml.loadConfiguration(file);
        } catch (IOException e) {
            Logger.warning("Could not reload shop-backups.yml: " + e.getMessage());
            config = YamlConfiguration.loadConfiguration(file);
        }
    }

    private List<Map<String, Object>> getBackupListMutable() {
        List<Map<String, Object>> out = new ArrayList<>();
        List<Map<?, ?>> raw = config.getMapList(ROOT_KEY);
        for (Map<?, ?> m : raw) {
            Map<String, Object> copy = new HashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) {
                copy.put(String.valueOf(e.getKey()), e.getValue());
            }
            out.add(copy);
        }
        return out;
    }

    private void trimOldest(List<Map<String, Object>> backups) {
        while (backups.size() > MAX_BACKUP_ENTRIES) {
            backups.remove(0);
        }
    }

    private static int asInt(Object o) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }

    public File getFile() {
        return file;
    }
}
