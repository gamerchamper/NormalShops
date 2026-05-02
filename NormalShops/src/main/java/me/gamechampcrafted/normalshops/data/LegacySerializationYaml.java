package me.gamechampcrafted.normalshops.data;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * ClickShop stored {@link org.bukkit.configuration.serialization.ConfigurationSerializable}
 * type tags as {@code ==: me.clickism.clickshop...}; NormalShops uses {@code me.gamechampcrafted.normalshops...}.
 */
public final class LegacySerializationYaml {

    private static final String LEGACY_PACKAGE_PREFIX = "me.clickism.clickshop";

    private LegacySerializationYaml() {
    }

    public static String migrateRegisteredClassNames(String yamlBody) {
        if (!yamlBody.contains(LEGACY_PACKAGE_PREFIX)) {
            return yamlBody;
        }
        return yamlBody.replace(LEGACY_PACKAGE_PREFIX, "me.gamechampcrafted.normalshops");
    }

    /**
     * Loads a YAML file, rewriting legacy serialization class names so Bukkit can deserialize into this plugin's classes.
     */
    public static YamlConfiguration loadConfiguration(File file) throws IOException {
        String raw = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        String migrated = migrateRegisteredClassNames(raw);
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(migrated);
        } catch (InvalidConfigurationException e) {
            throw new IOException("Failed to parse " + file.getPath(), e);
        }
        return yaml;
    }
}
