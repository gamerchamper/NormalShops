package me.gamechampcrafted.normalshops.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class SettingManager {

    /**
     * Bump when the bundled {@code config.yml} gains new keys. Older files get those keys merged in on load.
     */
    private static final int VERSION = 3;

    private static final String FILE_NAME = "config";

    private final Plugin plugin;
    private final DataManager dataManager;

    public SettingManager(Plugin plugin, File dataRoot) throws IOException {
        this.plugin = plugin;
        dataManager = new YAMLDataManager(plugin, dataRoot, FILE_NAME, dataRoot);

        YamlConfiguration defaults = loadBundledDefaults();
        if (defaults != null) {
            FileConfiguration user = dataManager.getConfig();
            boolean added = MessageManager.mergeMissingKeys(user, defaults);
            int userVer = parseConfigVersion(user.get("config-version"));
            boolean versionBump = userVer < VERSION;
            if (versionBump) {
                user.set("config-version", VERSION);
            }
            if (added || versionBump) {
                dataManager.saveConfig();
            }
        }
    }

    private YamlConfiguration loadBundledDefaults() {
        try (InputStream in = plugin.getResource(FILE_NAME + ".yml")) {
            if (in == null) {
                plugin.getLogger().severe("[NormalShops] Missing " + FILE_NAME + ".yml in the plugin jar.");
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            plugin.getLogger().severe("[NormalShops] Could not read bundled " + FILE_NAME + ".yml: " + e.getMessage());
            return null;
        }
    }

    private static int parseConfigVersion(Object version) {
        if (version == null) {
            return 0;
        }
        if (version instanceof Number) {
            return ((Number) version).intValue();
        }
        if (version instanceof String) {
            try {
                return Integer.parseInt(((String) version).trim());
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    @Nullable
    public Object get(String path) {
        return dataManager.getConfig().get(path);
    }

    public void set(String path, Object object) {
        dataManager.getConfig().set(path, object);
    }
}
