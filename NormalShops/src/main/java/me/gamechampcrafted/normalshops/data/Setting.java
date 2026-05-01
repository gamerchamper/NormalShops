package me.gamechampcrafted.normalshops.data;

import me.gamechampcrafted.normalshops.Logger;
import me.gamechampcrafted.normalshops.NormalShops;

import java.io.IOException;

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

    RECOVER_SHOP_FILES(false);

    private static SettingManager manager;
    private final Object defaultValue;

    Setting(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public static void initialize() throws IOException {
        if (manager == null) {
            manager = new SettingManager(NormalShops.getInstance());
        }
    }

    public static void reload() throws IOException {
        manager = new SettingManager(NormalShops.getInstance());
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

    private <T> T get(Class<T> type) {
        Object value = null;
        if (manager != null) {
            value = manager.get(getPath());
        } else {
            Logger.severe("SettingManager is null.");
        }
        if (!type.isInstance(value)) {
            Logger.warning("Invalid value for \"" + getPath() + "\" in config.yml. Default value " + defaultValue + " is used instead.");
            value = defaultValue;
        }
        return type.cast(value);
    }

    public int getInt() {
        return get(Integer.class);
    }

    public String getString() {
        return get(String.class);
    }

    public boolean isEnabled() {
        return get(Boolean.class);
    }

    private String path;

    private String getPath() {
        if (path == null) {
            path = name().toLowerCase().replace("_", "-");
        }
        return path;
    }

}
