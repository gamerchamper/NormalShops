package me.gamechampcrafted.normalshops.menu;

import me.gamechampcrafted.normalshops.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Loads recipe-style layouts from {@code gui.yml} {@code layouts.<menu-id>}:
 * {@code rows} (list of equal-width strings), {@code role-keys} (role → one character),
 * and optional {@code filler-key} (default {@code X}) for decorative background slots
 * painted via {@code icons.<menu-id>.background} / {@code icons.global.background}.
 * The same character may appear multiple times for one role (e.g. {@code entry: "#"}).
 */
public final class MenuSlotRegistry {

    private static final Map<String, ParsedMenuLayout> MENUS = new HashMap<>();

    private MenuSlotRegistry() {}

    public static void reload(FileConfiguration gui) {
        MENUS.clear();
        if (gui == null) {
            return;
        }
        ConfigurationSection layouts = gui.getConfigurationSection("layouts");
        if (layouts == null) {
            return;
        }
        for (String menuId : layouts.getKeys(false)) {
            String idLower = menuId.toLowerCase(Locale.ROOT);
            if ("trading".equals(idLower) || "owner".equals(idLower)) {
                continue;
            }
            ConfigurationSection sec = layouts.getConfigurationSection(menuId);
            if (sec == null) {
                continue;
            }
            ParsedMenuLayout parsed = ParsedMenuLayout.tryParse(menuId, sec);
            if (parsed != null) {
                MENUS.put(idLower, parsed);
            }
        }
    }

    public static int slot(String menuId, String role, int fallback) {
        ParsedMenuLayout p = MENUS.get(menuId.toLowerCase(Locale.ROOT));
        if (p == null) {
            return fallback;
        }
        return p.slot(role, fallback);
    }

    public static List<Integer> slots(String menuId, String role, List<Integer> fallback) {
        ParsedMenuLayout p = MENUS.get(menuId.toLowerCase(Locale.ROOT));
        if (p == null) {
            return fallback;
        }
        List<Integer> list = p.allSlotsForRole(role);
        return list.isEmpty() ? fallback : list;
    }

    public static boolean hasLayout(String menuId) {
        return MENUS.containsKey(menuId.toLowerCase(Locale.ROOT));
    }

    /** Slots painted with {@code icons.<id>.background} — grid cells matching {@code filler-key}. */
    public static List<Integer> fillerSlots(String menuId) {
        ParsedMenuLayout p = MENUS.get(menuId.toLowerCase(Locale.ROOT));
        return p == null ? List.of() : p.fillerSlots;
    }

    public static final class ParsedMenuLayout {
        private final Map<String, Integer> single = new HashMap<>();
        private final Map<String, List<Integer>> multi = new HashMap<>();
        private List<Integer> fillerSlots = List.of();

        static ParsedMenuLayout tryParse(String menuId, ConfigurationSection sec) {
            List<String> rawRows = sec.getStringList("rows");
            if (rawRows.isEmpty()) {
                return null;
            }
            List<String> rows = new ArrayList<>();
            for (String line : rawRows) {
                if (line == null) {
                    continue;
                }
                String t = line.trim().replace("\t", "");
                if (!t.isEmpty()) {
                    rows.add(t);
                }
            }
            if (rows.isEmpty()) {
                return null;
            }

            String fillerKeyStr = Objects.requireNonNullElse(sec.getString("filler-key"), "X");
            char fillerChar = fillerKeyStr.isEmpty() ? 'X' : fillerKeyStr.charAt(0);

            int cols = rows.get(0).length();
            for (int i = 1; i < rows.size(); i++) {
                String normalized = padOrTrim(rows.get(i), cols, fillerChar);
                rows.set(i, normalized);
                if (normalized.length() != cols) {
                    Logger.warning("[NormalShops] gui layouts." + menuId + ": all rows must have width " + cols + ".");
                    return null;
                }
            }
            rows.set(0, padOrTrim(rows.get(0), cols, fillerChar));

            ConfigurationSection roleKeys = sec.getConfigurationSection("role-keys");
            if (roleKeys == null) {
                Logger.warning("[NormalShops] gui layouts." + menuId + " missing role-keys.");
                return null;
            }

            Map<String, Character> roleToChar = new LinkedHashMap<>();
            for (String role : roleKeys.getKeys(false)) {
                String v = roleKeys.getString(role);
                if (v == null || v.isEmpty()) {
                    continue;
                }
                roleToChar.put(role.toLowerCase(Locale.ROOT), v.charAt(0));
            }
            if (roleToChar.isEmpty()) {
                return null;
            }

            for (Character marker : roleToChar.values()) {
                if (marker == fillerChar) {
                    Logger.warning("[NormalShops] gui layouts." + menuId + ": filler-key '" + fillerChar
                            + "' conflicts with a role marker.");
                    return null;
                }
            }

            Map<Character, List<Integer>> charSlots = new HashMap<>();
            for (int r = 0; r < rows.size(); r++) {
                String row = rows.get(r);
                for (int c = 0; c < cols; c++) {
                    char ch = row.charAt(c);
                    int slot = r * cols + c;
                    charSlots.computeIfAbsent(ch, k -> new ArrayList<>()).add(slot);
                }
            }

            ParsedMenuLayout out = new ParsedMenuLayout();
            for (Map.Entry<String, Character> e : roleToChar.entrySet()) {
                String role = e.getKey();
                Character marker = e.getValue();
                List<Integer> found = charSlots.get(marker);
                if (found == null || found.isEmpty()) {
                    Logger.warning("[NormalShops] gui layouts." + menuId + ": marker for \"" + role + "\" not found.");
                    return null;
                }
                List<Integer> sorted = new ArrayList<>(found);
                Collections.sort(sorted);
                if (sorted.size() == 1) {
                    out.single.put(role, sorted.get(0));
                } else {
                    out.multi.put(role, sorted);
                }
            }

            List<Integer> filler = new ArrayList<>(charSlots.getOrDefault(fillerChar, List.of()));
            Collections.sort(filler);
            out.fillerSlots = List.copyOf(filler);
            return out;
        }

        private static String padOrTrim(String row, int cols, char padChar) {
            String t = row.trim().replace("\t", "");
            if (t.length() >= cols) {
                return t.substring(0, cols);
            }
            StringBuilder sb = new StringBuilder(t);
            while (sb.length() < cols) {
                sb.append(padChar);
            }
            return sb.toString();
        }

        int slot(String role, int fallback) {
            String r = role.toLowerCase(Locale.ROOT);
            Integer s = single.get(r);
            if (s != null) {
                return s;
            }
            List<Integer> m = multi.get(r);
            if (m != null && !m.isEmpty()) {
                return m.get(0);
            }
            return fallback;
        }

        List<Integer> allSlotsForRole(String role) {
            String r = role.toLowerCase(Locale.ROOT);
            if (multi.containsKey(r)) {
                return new ArrayList<>(multi.get(r));
            }
            Integer s = single.get(r);
            return s != null ? List.of(s) : List.of();
        }
    }
}
