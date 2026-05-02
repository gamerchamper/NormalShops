package me.gamechampcrafted.normalshops.data;

import me.gamechampcrafted.normalshops.Logger;
import me.gamechampcrafted.normalshops.NormalShops;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public enum Setting {
    CONFIG_VERSION(0),

    LANGUAGE("en_US"),

    PROTECT_STOCKPILES(false),

    OPERATOR_DELETE_SHOP_COLLECT(false),
    VISUALIZE_TETHERS(true),

    DISPLAY_VIEW_RANGE(2),
    MAX_CONNECTION_DISTANCE(100),

    SHOP_LIMIT_PER_PLAYER(30),
    STOCKPILE_LIMIT_PER_SHOP(10),

    EARNINGS_PILE_LIMIT_PER_PLAYER(10),
    SHOP_LIMIT_PER_EARNINGS_PILE(10),

    BLOCK_PISTON(true),

    FULL_NBT_CHECK(false),

    VILLAGER_TRADING_MENU(true),

    /**
     * When true, empty shops show as bedrock and hide item/sale displays until restocked.
     */
    DISPLAY_OUT_OF_STOCK(false),

    RECOVER_SHOP_FILES(false),

    /** Copies {@code data.yml}, {@code shops/*.yml}, and {@code shop-backups.yml} on a timer (see {@code backups/auto/}). */
    AUTO_DATA_BACKUP_ENABLED(true),

    /** Minutes between snapshots (1–1440). */
    AUTO_DATA_BACKUP_INTERVAL_MINUTES(10),

    /** Number of timestamp folders under {@code backups/auto/} to retain. */
    AUTO_DATA_BACKUP_MAX_SNAPSHOTS(48);

    private static SettingManager manager;
    private final Object defaultValue;

    Setting(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public static void initialize() throws IOException {
        if (manager == null) {
            manager = new SettingManager(NormalShops.getInstance(), NormalShops.getInstance().getResolvedDataFolder());
        }
    }

    public static void reload() throws IOException {
        manager = new SettingManager(NormalShops.getInstance(), NormalShops.getInstance().getResolvedDataFolder());
    }

    public static void saveSettings() {
        if (manager == null) {
            Logger.warning("Couldn't save settings config. SettingManager is null.");
            return;
        }
        manager.getDataManager().saveConfig();
    }

    public void set(Object object) {
        if (manager == null) {
            Logger.warning("Couldn't save setting " + name() + " to config. SettingManager is null.");
            return;
        }
        manager.set(getPath(), object);
    }

    /** Used for string settings only; booleans and ints use {@link FileConfiguration} helpers. */
    private <T> T get(Class<T> type) {
        Object value = null;
        if (manager != null) {
            value = manager.get(getPath());
        } else {
            Logger.severe("SettingManager is null.");
        }
        Object coerced = coerce(value, type);
        if (coerced != null && type.isInstance(coerced)) {
            return type.cast(coerced);
        }
        return type.cast(defaultValue);
    }

    /**
     * Legacy configs and YAML edge cases may yield strings for booleans or doubles for integers.
     */
    private static Object coerce(Object value, Class<?> type) {
        if (value == null) {
            return null;
        }
        if (type == Boolean.class) {
            if (value instanceof Boolean) {
                return value;
            }
            if (value instanceof String) {
                String s = ((String) value).trim().toLowerCase(Locale.ROOT);
                if (s.isEmpty()) {
                    return null;
                }
                if ("true".equals(s) || "yes".equals(s) || "on".equals(s) || "1".equals(s)) {
                    return Boolean.TRUE;
                }
                if ("false".equals(s) || "no".equals(s) || "off".equals(s) || "0".equals(s)) {
                    return Boolean.FALSE;
                }
                return null;
            }
            if (value instanceof Character) {
                char c = (Character) value;
                if (c == '1' || c == 'y' || c == 'Y' || c == 't' || c == 'T') {
                    return Boolean.TRUE;
                }
                if (c == '0' || c == 'n' || c == 'N' || c == 'f' || c == 'F') {
                    return Boolean.FALSE;
                }
                return null;
            }
            if (value instanceof Number) {
                return ((Number) value).doubleValue() != 0.0;
            }
            if (value instanceof List<?>) {
                List<?> list = (List<?>) value;
                if (list.size() == 1) {
                    return coerce(list.get(0), type);
                }
                return null;
            }
            if (value instanceof ConfigurationSection) {
                ConfigurationSection sec = (ConfigurationSection) value;
                Set<String> keys = sec.getKeys(false);
                if (keys.size() == 1) {
                    return coerce(sec.get(keys.iterator().next()), type);
                }
                return null;
            }
            if (value instanceof Map<?, ?>) {
                Map<?, ?> map = (Map<?, ?>) value;
                if (map.size() == 1) {
                    return coerce(map.values().iterator().next(), type);
                }
                return null;
            }
            return null;
        }
        if (type == Integer.class) {
            if (value instanceof Integer) {
                return value;
            }
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            if (value instanceof String) {
                try {
                    return Integer.parseInt(((String) value).trim());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            return null;
        }
        if (type == String.class) {
            if (value instanceof String) {
                return value;
            }
            return String.valueOf(value);
        }
        return null;
    }

    public int getInt() {
        if (manager == null) {
            Logger.severe("SettingManager is null.");
            return (Integer) defaultValue;
        }
        FileConfiguration cfg = manager.getDataManager().getConfig();
        String path = getPath();
        int def = (Integer) defaultValue;
        if (!cfg.isSet(path)) {
            return def;
        }
        Object raw = cfg.get(path);
        Object coerced = coerce(raw, Integer.class);
        if (coerced instanceof Integer) {
            return (Integer) coerced;
        }
        return cfg.getInt(path, def);
    }

    public String getString() {
        return get(String.class);
    }

    public boolean isEnabled() {
        if (manager == null) {
            Logger.severe("SettingManager is null.");
            return (Boolean) defaultValue;
        }
        FileConfiguration cfg = manager.getDataManager().getConfig();
        String path = getPath();
        boolean def = (Boolean) defaultValue;
        if (!cfg.isSet(path)) {
            return def;
        }
        Object raw = cfg.get(path);
        Object coerced = coerce(raw, Boolean.class);
        if (coerced instanceof Boolean) {
            return (Boolean) coerced;
        }
        return cfg.getBoolean(path, def);
    }

    private String path;

    private String getPath() {
        if (path == null) {
            path = name().toLowerCase().replace("_", "-");
        }
        return path;
    }

}
