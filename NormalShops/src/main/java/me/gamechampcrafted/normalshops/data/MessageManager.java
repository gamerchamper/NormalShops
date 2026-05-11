package me.gamechampcrafted.normalshops.data;

import me.gamechampcrafted.normalshops.Debug;
import me.gamechampcrafted.normalshops.EmbeddedBundledDefaults;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class MessageManager {

    /**
     * Bump when adding or renaming keys in {@code resources/messages/*.yml}.
     * On startup (and reload), each locale file on disk is merged with the jar:
     * keys you added stay in the jar; missing keys are filled in without overwriting custom text.
     */
    public static final int VERSION = 7;

    private static final String DIRECTORY_NAME = "messages";

    /**
     * Locale codes that ship inside the jar (see {@code src/main/resources/messages/}).
     */
    private static final String[] EMBEDDED_LANGUAGES = {
            "en_US", "de_DE", "tr_TR"
    };

    private final Plugin plugin;
    private final File dataRoot;
    private final DataManager dataManager;

    public MessageManager(Plugin plugin, String languageCode, File dataRoot) throws IOException {
        this.plugin = plugin;
        this.dataRoot = dataRoot;
        File messagesDir = new File(dataRoot, DIRECTORY_NAME);
        if (!messagesDir.exists() && !messagesDir.mkdirs()) {
            throw new IOException("Could not create directory: " + messagesDir.getPath());
        }

        if (Debug.OVERRIDE_MESSAGES) {
            extractEmbeddedLanguagesOverwrite(messagesDir);
        } else {
            for (String lang : EMBEDDED_LANGUAGES) {
                syncEmbeddedLanguageFile(messagesDir, lang);
            }
        }

        ensureActiveLocaleFile(messagesDir, languageCode);

        dataManager = new YAMLDataManager(plugin, messagesDir, languageCode, dataRoot);
    }

    /** Dev-only: replace on-disk locales with embedded copies (drops local edits). */
    private void extractEmbeddedLanguagesOverwrite(File messagesDir) {
        for (String lang : EMBEDDED_LANGUAGES) {
            File outFile = new File(messagesDir, lang + ".yml");
            String embedded = EmbeddedBundledDefaults.messagesForLocale(lang);
            if (embedded == null) {
                plugin.getLogger().warning("[NormalShops] No embedded messages for: " + lang);
                continue;
            }
            try {
                Files.writeString(outFile.toPath(), embedded, StandardCharsets.UTF_8);
            } catch (IOException e) {
                plugin.getLogger().warning("[NormalShops] Could not write messages/" + lang + ".yml: " + e.getMessage());
            }
        }
    }

    private void syncEmbeddedLanguageFile(File messagesDir, String lang) throws IOException {
        String embedded = EmbeddedBundledDefaults.messagesForLocale(lang);
        if (embedded == null) {
            plugin.getLogger().warning("[NormalShops] No embedded messages for: " + lang);
            return;
        }
        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new StringReader(embedded));
        File outFile = new File(messagesDir, lang + ".yml");
        YamlConfiguration user = new YamlConfiguration();
        if (outFile.exists()) {
            user = YamlConfiguration.loadConfiguration(outFile);
        }

        int userVer = user.getInt("messages-version", 0);
        if (userVer >= VERSION) {
            return;
        }

        mergeMissingKeys(user, defaults);
        user.set("messages-version", VERSION);
        user.save(outFile);
    }

    /**
     * Copies missing keys from {@code defaults} into {@code user}. Existing keys in {@code user} are kept.
     *
     * @return true if at least one key was added
     */
    public static boolean mergeMissingKeys(FileConfiguration user, FileConfiguration defaults) {
        boolean added = false;
        for (String key : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(key)) {
                continue;
            }
            if (!user.isSet(key)) {
                user.set(key, defaults.get(key));
                added = true;
            }
        }
        return added;
    }

    /**
     * If {@code language.yml} is missing (custom locale), create it from English bundled defaults.
     */
    private void ensureActiveLocaleFile(File messagesDir, String languageCode) throws IOException {
        File target = new File(messagesDir, languageCode + ".yml");
        if (target.exists()) {
            return;
        }
        String embedded = EmbeddedBundledDefaults.messagesForLocale(languageCode);
        if (embedded != null) {
            Files.writeString(target.toPath(), embedded, StandardCharsets.UTF_8);
            return;
        }

        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                new StringReader(EmbeddedBundledDefaults.messagesEnUs()));
        YamlConfiguration user = new YamlConfiguration();
        mergeMissingKeys(user, defaults);
        user.set("messages-version", VERSION);
        user.save(target);
        plugin.getLogger().info("[NormalShops] Created messages/" + languageCode + ".yml from embedded English defaults — translate or edit as you like.");
    }

    /**
     * @param path key of message
     * @return colorized string
     */
    @Nullable
    public String get(String path) {
        String message = (String) dataManager.getConfig().get(path);
        if (message == null) {
            return null;
        }
        return Utils.colorize(message);
    }

    public List<String> getLore(String pathToButton) {
        List<String> lore = new ArrayList<>();
        for (int i = 1; i < 10; i++) {
            String line = get(pathToButton + "-lore-" + i);
            if (line == null) break;
            lore.add(line);
        }
        return lore;
    }
}
