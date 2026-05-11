package me.gamechampcrafted.normalshops.data;

import me.gamechampcrafted.normalshops.EmbeddedBundledDefaults;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class YAMLDataManager implements DataManager {

    private final Plugin plugin;

    /** Prefix stripped from {@link #file} paths when copying defaults from the jar (often the plugin data root). */
    private final File pathTrimBase;

    private final File file;
    private FileConfiguration config;

    public YAMLDataManager(Plugin plugin, @NotNull File directory, String fileName) throws IOException {
        this(plugin, directory, fileName, plugin.getDataFolder());
    }

    /**
     * @param pathTrimBase directory prefix stripped when resolving the bundled jar path (see {@link #getTrimmedPath()})
     */
    public YAMLDataManager(Plugin plugin, @NotNull File directory, String fileName, @NotNull File pathTrimBase) throws IOException {
        this.plugin = plugin;
        this.pathTrimBase = pathTrimBase;
        createDirectoryIfNotExists(directory);
        file = new File(directory, fileName + ".yml");

        if (file.exists()) {
            config = LegacySerializationYaml.loadConfiguration(file);
            return;
        }

        saveDefaultConfig();
    }

    @Override
    public FileConfiguration getConfig() {
        return config;
    }

    @Override
    public void reloadConfig() {
        try {
            config = LegacySerializationYaml.loadConfiguration(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not reload \"" + file.getName() + "\": " + exception.getMessage());
            config = YamlConfiguration.loadConfiguration(file);
        }
    }

    @Override
    public void saveConfig() {
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("\"" + file.getName() + "\" config couldn't be saved.");
        }
    }

    private void saveDefaultConfig() throws IOException {
        String path = getTrimmedPath();
        String embedded = EmbeddedBundledDefaults.yamlDefaultForPath(path);
        if (embedded != null) {
            Files.writeString(file.toPath(), embedded, StandardCharsets.UTF_8);
            config = YamlConfiguration.loadConfiguration(file);
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new StringReader(embedded));
            config.setDefaults(defaultConfig);
            saveConfig();
            return;
        }

        plugin.getLogger().warning("[NormalShops] No embedded default for \"" + path + "\" — creating empty file.");
        if (!file.createNewFile()) {
            throw new IOException("Failed to create file: " + file.getPath());
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    private String getTrimmedPath() {
        String basePath = pathTrimBase.getPath();
        String filePath = file.getPath();
        if (filePath.startsWith(basePath)) {
            return filePath.substring(basePath.length() + 1); // Trim plugin data root
        }
        return filePath;
    }

    private static void createDirectoryIfNotExists(File directory) throws IOException {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Failed to create directory " + directory.getPath());
        }
    }
}
