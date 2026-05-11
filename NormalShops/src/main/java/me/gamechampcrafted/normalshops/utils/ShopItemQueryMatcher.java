package me.gamechampcrafted.normalshops.utils;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Resolves loose player input (typos, spaces vs underscores, partial names) to {@link Material} IDs
 * for filtering shops by product type.
 */
public final class ShopItemQueryMatcher {

    private ShopItemQueryMatcher() {}

    /**
     * Returns distinct item materials that match the query. Empty if nothing usable was typed.
     */
    public static List<Material> resolve(String rawQuery) {
        if (rawQuery == null) {
            return List.of();
        }
        String q = rawQuery.trim();
        if (q.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<Material> ordered = new LinkedHashSet<>();

        // 1) Spigot enum / legacy names (with underscores)
        Material direct = Material.matchMaterial(q.replace(' ', '_').replace('-', '_'));
        if (direct != null && isUsableItem(direct)) {
            return List.of(direct);
        }
        direct = Material.matchMaterial(q.replace(' ', '_').replace('-', '_').toUpperCase(Locale.ROOT));
        if (direct != null && isUsableItem(direct)) {
            return List.of(direct);
        }

        String lowerNameQuery = q.toLowerCase(Locale.ROOT);
        String underscoreQuery = lowerNameQuery.replace(' ', '_').replace('-', '_');

        // 2) All query tokens appear as substrings in material enum name (e.g. "iron pick" → IRON_PICKAXE)
        String[] tokens = lowerNameQuery.split("\\s+");
        if (tokens.length > 0 && !(tokens.length == 1 && tokens[0].length() < 2)) {
            for (Material m : Material.values()) {
                if (!isUsableItem(m)) {
                    continue;
                }
                String name = m.name().toLowerCase(Locale.ROOT);
                boolean ok = true;
                boolean anySignificantToken = false;
                for (String t : tokens) {
                    if (t.length() < 2) {
                        continue;
                    }
                    anySignificantToken = true;
                    if (!name.contains(t)) {
                        ok = false;
                        break;
                    }
                }
                if (ok && anySignificantToken) {
                    ordered.add(m);
                }
            }
        }

        // 3) Single blob substring on enum (e.g. "diamond" matches DIAMOND_* )
        if (ordered.isEmpty() && tokens.length == 1 && tokens[0].length() >= 2) {
            String t = tokens[0];
            for (Material m : Material.values()) {
                if (!isUsableItem(m)) {
                    continue;
                }
                if (m.name().toLowerCase(Locale.ROOT).contains(t)) {
                    ordered.add(m);
                }
            }
        }

        // 4) Levenshtein on compact letters (handles "dimond" → diamond-like names)
        if (ordered.isEmpty()) {
            String qLetters = lettersOnly(q);
            if (qLetters.length() >= 3) {
                List<ScoredMaterial> scored = new ArrayList<>();
                for (Material m : Material.values()) {
                    if (!isUsableItem(m)) {
                        continue;
                    }
                    String mLetters = lettersOnly(m.name());
                    int d = levenshtein(qLetters, mLetters);
                    int threshold = Math.min(3, 1 + qLetters.length() / 4);
                    if (d <= threshold) {
                        scored.add(new ScoredMaterial(m, d * 100 + m.name().length()));
                    }
                }
                scored.sort(Comparator.comparingInt(a -> a.score));
                for (ScoredMaterial sm : scored) {
                    ordered.add(sm.material);
                    if (ordered.size() >= 24) {
                        break;
                    }
                }
            }
        }

        // 5) Full enum Levenshtein as last resort (short queries)
        if (ordered.isEmpty() && underscoreQuery.length() >= 3) {
            List<ScoredMaterial> scored = new ArrayList<>();
            for (Material m : Material.values()) {
                if (!isUsableItem(m)) {
                    continue;
                }
                int d = levenshtein(underscoreQuery, m.name().toLowerCase(Locale.ROOT));
                if (d <= 4) {
                    scored.add(new ScoredMaterial(m, d * 1000 + m.name().length()));
                }
            }
            scored.sort(Comparator.comparingInt(a -> a.score));
            for (ScoredMaterial sm : scored) {
                ordered.add(sm.material);
                if (ordered.size() >= 12) {
                    break;
                }
            }
        }

        return List.copyOf(ordered);
    }

    private static boolean isUsableItem(Material m) {
        return m.isItem() && !m.isAir();
    }

    private static String lettersOnly(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetter(c)) {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    private record ScoredMaterial(Material material, int score) {}

    private static int levenshtein(String a, String b) {
        if (a.isEmpty()) {
            return b.length();
        }
        if (b.isEmpty()) {
            return a.length();
        }
        int n = a.length();
        int m = b.length();
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];
        for (int j = 0; j <= m; j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                char cb = b.charAt(j - 1);
                int cost = ca == cb ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[m];
    }
}
