package me.gamechampcrafted.normalshops.data;

import me.gamechampcrafted.normalshops.EmbeddedBundledDefaults;
import me.gamechampcrafted.normalshops.Logger;
import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.menu.MenuSlotRegistry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.IOException;
import java.io.StringReader;

/**
 * Loads {@code gui.yml} (layouts + icon materials). Merged from jar defaults when keys are missing.
 */
public final class GuiConfigManager {

    private static final int VERSION = 1;

    private static final String FILE_NAME = "gui";

    private static YAMLDataManager dataManager;

    public static void initialize() throws IOException {
        NormalShops plugin = NormalShops.getInstance();
        dataManager = new YAMLDataManager(plugin, plugin.getResolvedDataFolder(), FILE_NAME, plugin.getResolvedDataFolder());

        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new StringReader(EmbeddedBundledDefaults.guiYml()));
        FileConfiguration user = dataManager.getConfig();
        boolean added = MessageManager.mergeMissingKeys(user, defaults);
        int userVer = parseVersion(user.get("gui-config-version"));
        boolean versionBump = userVer < VERSION;
        if (versionBump) {
            user.set("gui-config-version", VERSION);
        }
        if (added || versionBump) {
            dataManager.saveConfig();
        }

        MenuSlotRegistry.reload(user);
    }

    public static void reload() throws IOException {
        initialize();
    }

    private static int parseVersion(Object version) {
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

    public static FileConfiguration getConfig() {
        if (dataManager == null) {
            Logger.warning("[NormalShops] GuiConfigManager not initialized.");
            return null;
        }
        return dataManager.getConfig();
    }

    public static void save() {
        if (dataManager != null) {
            dataManager.saveConfig();
        }
    }
}
