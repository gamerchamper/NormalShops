package me.gamechampcrafted.normalshops.update;

import me.gamechampcrafted.normalshops.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Checks Modrinth (project NormalShops) for newer releases using API v3. */
public final class ModrinthUpdateService {

    public static final String PROJECT_PAGE_URL = "https://modrinth.com/plugin/normalshops";
    private static final String PROJECT_ID = "DNLfdXJm";
    private static final String VERSIONS_API = "https://api.modrinth.com/v3/project/" + PROJECT_ID + "/version?include_changelog=false";

    private static final Pattern VERSION_NUMBER_PATTERN = Pattern.compile("\"version_number\"\\s*:\\s*\"([^\"]+)\"");
    /** e.g. {@code 26.1.2-v1.0.8} → 1.0.8 */
    private static final Pattern SUFFIX_V_SEMVER = Pattern.compile("-v(\\d+)\\.(\\d+)\\.(\\d+)\\s*$");
    /** Plain {@code 1.0.4} release rows */
    private static final Pattern PLAIN_SEMVER = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)\\s*$");

    private static ModrinthUpdateService instance;

    private volatile UpdateSnapshot snapshot = UpdateSnapshot.initial();

    private ModrinthUpdateService() {}

    public static ModrinthUpdateService get() {
        if (instance == null) {
            instance = new ModrinthUpdateService();
        }
        return instance;
    }

    public UpdateSnapshot getSnapshot() {
        return snapshot;
    }

    /**
     * Runs an HTTP request asynchronously; calls {@link #notifyStaffIfBehind(JavaPlugin)} on the main thread when done.
     */
    public void checkAsync(JavaPlugin plugin) {
        String userAgent = "NormalShops/" + pluginVersion(plugin)
                + " (" + PROJECT_PAGE_URL + ")";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(VERSIONS_API))
                .header("Accept", "application/json")
                .header("User-Agent", userAgent)
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> {
                    if (error != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> applyFailure(plugin, error));
                        return;
                    }
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        Bukkit.getScheduler().runTask(plugin, () -> applyFailure(plugin,
                                new IllegalStateException("HTTP " + response.statusCode())));
                        return;
                    }
                    Bukkit.getScheduler().runTask(plugin, () -> applySuccess(plugin, response.body()));
                });
    }

    private void applyFailure(JavaPlugin plugin, Throwable error) {
        String ver = pluginVersion(plugin);
        String err = error == null ? "unknown error" : error.getMessage();
        if (err == null || err.isEmpty()) {
            err = error == null ? "unknown error" : error.getClass().getSimpleName();
        }
        snapshot = new UpdateSnapshot(true, -1, "", ver, err);
        Logger.warning("Modrinth update check failed: " + err);
    }

    private void applySuccess(JavaPlugin plugin, String body) {
        String current = pluginVersion(plugin);
        List<String> remote = parseVersionNumbers(body);
        int[] currentParts = parseReleaseTriple(current);
        int behind = computeVersionsBehind(currentParts, remote);
        String latest = remote.isEmpty() ? "" : remote.get(0);

        snapshot = new UpdateSnapshot(true, behind, latest, current, null);

        if (behind > 0) {
            Logger.info("NormalShops update available: " + behind + " release(s) newer than this server (latest Modrinth: "
                    + latest + "). See " + PROJECT_PAGE_URL);
            notifyStaffIfBehind(plugin);
        } else if (behind == 0 && currentParts != null) {
            Logger.info("NormalShops is up to date with Modrinth (release " + formatTriple(currentParts) + ").");
        }
    }

    /**
     * Notifies players who are ops or have {@code normalshops.update-notify}.
     */
    public void notifyStaffIfBehind(JavaPlugin plugin) {
        UpdateSnapshot s = snapshot;
        if (!s.checked() || s.behind() <= 0) {
            return;
        }
        String msg = ChatColor.GOLD + "[NormalShops] " + ChatColor.YELLOW + "Update available: "
                + ChatColor.WHITE + s.behind() + ChatColor.YELLOW + " release(s) newer than "
                + ChatColor.GRAY + s.runningVersion() + ChatColor.YELLOW + ". Latest: "
                + ChatColor.GREEN + s.latestModrinthVersion() + ChatColor.YELLOW + ". "
                + ChatColor.AQUA + PROJECT_PAGE_URL;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isOp() || p.hasPermission("normalshops.update-notify")) {
                p.sendMessage(msg);
            }
        }
    }

    public void notifyPlayerIfBehind(Player player) {
        UpdateSnapshot s = snapshot;
        if (!s.checked() || s.behind() <= 0) {
            return;
        }
        if (!player.isOp() && !player.hasPermission("normalshops.update-notify")) {
            return;
        }
        player.sendMessage(ChatColor.GOLD + "[NormalShops] " + ChatColor.YELLOW + "Update available: "
                + ChatColor.WHITE + s.behind() + ChatColor.YELLOW + " release(s) newer (latest "
                + ChatColor.GREEN + s.latestModrinthVersion() + ChatColor.YELLOW + "). "
                + ChatColor.AQUA + PROJECT_PAGE_URL);
    }

    static List<String> parseVersionNumbers(String json) {
        List<String> out = new ArrayList<>();
        Matcher m = VERSION_NUMBER_PATTERN.matcher(json);
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    /**
     * {@code null} if the jar version string does not contain a parsable {@code x.y.z} release (see Modrinth {@code version_number}).
     */
    static int[] parseReleaseTriple(String versionNumber) {
        if (versionNumber == null || versionNumber.isEmpty()) {
            return null;
        }
        Matcher mv = SUFFIX_V_SEMVER.matcher(versionNumber.trim());
        if (mv.find()) {
            return new int[]{Integer.parseInt(mv.group(1)), Integer.parseInt(mv.group(2)), Integer.parseInt(mv.group(3))};
        }
        Matcher mp = PLAIN_SEMVER.matcher(versionNumber.trim());
        if (mp.matches()) {
            return new int[]{Integer.parseInt(mp.group(1)), Integer.parseInt(mp.group(2)), Integer.parseInt(mp.group(3))};
        }
        return null;
    }

    static String formatTriple(int[] t) {
        return t[0] + "." + t[1] + "." + t[2];
    }

    /**
     * Counts Modrinth releases strictly newer than this server's parsed semver.
     */
    static int computeVersionsBehind(int[] current, List<String> modrinthVersionNumbers) {
        if (current == null) {
            return -1;
        }
        int c = 0;
        for (String vn : modrinthVersionNumbers) {
            int[] r = parseReleaseTriple(vn);
            if (r != null && compare(r, current) > 0) {
                c++;
            }
        }
        return c;
    }

    private static String pluginVersion(JavaPlugin plugin) {
        return plugin.getDescription().getVersion();
    }

    static int compare(int[] a, int[] b) {
        for (int i = 0; i < 3; i++) {
            int d = a[i] - b[i];
            if (d != 0) {
                return Integer.signum(d);
            }
        }
        return 0;
    }

    public record UpdateSnapshot(
            boolean checked,
            /** Strictly newer Modrinth releases; {@code -1} if running semver could not be parsed. */
            int behind,
            /** Newest {@code version_number} from API (first entry). */
            String latestModrinthVersion,
            String runningVersion,
            String errorMessage
    ) {
        static UpdateSnapshot initial() {
            return new UpdateSnapshot(false, -1, "", "", null);
        }
    }
}
