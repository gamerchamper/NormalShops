package me.gamechampcrafted.normalshops.menu;

import me.gamechampcrafted.normalshops.Logger;
import me.gamechampcrafted.normalshops.data.GuiConfigManager;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;

/**
 * Icon materials from {@code gui.yml} under {@code icons.<path>}. Uses dotted paths, e.g. {@code trading.buy-in-stock}.
 */
public final class GuiIcons {

    private GuiIcons() {}

    public static Material material(String dotPath, Material fallback) {
        FileConfiguration cfg = GuiConfigManager.getConfig();
        if (cfg == null) {
            return fallback;
        }
        String name = cfg.getString("icons." + dotPath);
        if (name == null || name.isBlank()) {
            return fallback;
        }
        Material m = Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));
        if (m == null) {
            Logger.warning("[NormalShops] gui.yml icons." + dotPath + ": unknown material \"" + name + "\" — using default.");
            return fallback;
        }
        return m;
    }

    /**
     * Decorative glass for layout {@code filler-key} slots: tries {@code icons.<menuId>.background},
     * then {@code icons.global.background}.
     */
    public static Material menuBackground(String menuId, Material fallback) {
        FileConfiguration cfg = GuiConfigManager.getConfig();
        if (cfg == null) {
            return fallback;
        }
        String name = cfg.getString("icons." + menuId + ".background");
        if (name == null || name.isBlank()) {
            name = cfg.getString("icons.global.background");
        }
        if (name == null || name.isBlank()) {
            return fallback;
        }
        Material m = Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));
        if (m == null) {
            Logger.warning("[NormalShops] gui.yml icons." + menuId + ".background / global.background: unknown \""
                    + name + "\" — using default.");
            return fallback;
        }
        return m;
    }
}
