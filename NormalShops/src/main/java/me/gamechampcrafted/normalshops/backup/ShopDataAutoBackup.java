package me.gamechampcrafted.normalshops.backup;

import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.data.Setting;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Periodically snapshots {@code data.yml}, {@code shop-backups.yml} (if present), and every file under
 * {@code shops/} into {@code backups/auto/&lt;timestamp&gt;/}. Flushes live data on the main thread, then copies
 * files in parallel off-thread.
 */
public final class ShopDataAutoBackup {

    private static final String AUTO_FOLDER = "backups" + File.separator + "auto";
    private static final DateTimeFormatter SNAPSHOT_NAME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final NormalShops plugin;
    private final ExecutorService ioPool;
    private final Runnable tickRunnable;

    private volatile int taskId = -1;

    public ShopDataAutoBackup(NormalShops plugin) {
        this.plugin = plugin;
        int threads = Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors()));
        this.ioPool = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "normalshops-data-backup");
            t.setDaemon(true);
            return t;
        });
        this.tickRunnable = () -> {
            if (!plugin.isEnabled()) {
                return;
            }
            plugin.saveData();
            ioPool.execute(this::runBackupOffMainThread);
        };
    }

    /**
     * Schedules or reschedules the repeating backup task from current {@link Setting} values.
     */
    public void schedule() {
        cancelTask();
        if (!Setting.AUTO_DATA_BACKUP_ENABLED.isEnabled()) {
            plugin.getLogger().info("[NormalShops] Automatic shop data backups are disabled (auto-data-backup-enabled).");
            return;
        }
        int minutes = clampInt(Setting.AUTO_DATA_BACKUP_INTERVAL_MINUTES.getInt(), 1, 24 * 60);
        int maxSnaps = clampInt(Setting.AUTO_DATA_BACKUP_MAX_SNAPSHOTS.getInt(), 2, 500);
        long ticks = minutes * 60L * 20L;
        if (ticks < 20L) {
            ticks = 20L;
        }
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, tickRunnable, ticks, ticks);
        plugin.getLogger().info("[NormalShops] Shop data snapshots every " + minutes
                + " min → " + AUTO_FOLDER.replace(File.separatorChar, '/') + "/ (keeping " + maxSnaps + " newest).");
    }

    public void restart() {
        schedule();
    }

    private void cancelTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void runBackupOffMainThread() {
        try {
            File dataRoot = plugin.getResolvedDataFolder();
            Path rootPath = dataRoot.toPath();
            List<Path> sources = collectSources(dataRoot);
            if (sources.isEmpty()) {
                return;
            }

            String stamp = LocalDateTime.now().format(SNAPSHOT_NAME);
            Path destRoot = rootPath.resolve("backups").resolve("auto").resolve(stamp);
            Files.createDirectories(destRoot);

            sources.parallelStream().forEach(src -> {
                try {
                    copyOne(rootPath, destRoot, src);
                } catch (RuntimeException ex) {
                    plugin.getLogger().warning("[NormalShops] Skipped file in snapshot: " + ex.getMessage());
                }
            });

            int maxSnaps = clampInt(Setting.AUTO_DATA_BACKUP_MAX_SNAPSHOTS.getInt(), 2, 500);
            pruneOldSnapshots(dataRoot, maxSnaps);

            plugin.getLogger().info("[NormalShops] Shop data snapshot written to backups/auto/" + stamp + "/");
        } catch (Exception e) {
            plugin.getLogger().warning("[NormalShops] Automatic shop data backup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<Path> collectSources(File dataRoot) {
        List<Path> out = new ArrayList<>();
        Path dataYml = dataRoot.toPath().resolve("data.yml");
        if (Files.isRegularFile(dataYml)) {
            out.add(dataYml);
        }
        Path shopBackups = dataRoot.toPath().resolve("shop-backups.yml");
        if (Files.isRegularFile(shopBackups)) {
            out.add(shopBackups);
        }
        Path shopsDir = dataRoot.toPath().resolve("shops");
        if (Files.isDirectory(shopsDir)) {
            try (Stream<Path> walk = Files.walk(shopsDir)) {
                walk.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".yml")).forEach(out::add);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return out;
    }

    private static void copyOne(Path dataRoot, Path destRoot, Path src) {
        try {
            Path relative = dataRoot.relativize(src);
            Path dst = destRoot.resolve(relative.toString());
            Files.createDirectories(dst.getParent());
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("copy " + src + ": " + e.getMessage(), e);
        }
    }

    private static void pruneOldSnapshots(File dataRoot, int maxSnapshots) {
        File autoRoot = new File(dataRoot, "backups/auto");
        File[] dirs = autoRoot.listFiles(File::isDirectory);
        if (dirs == null || dirs.length <= maxSnapshots) {
            return;
        }
        Arrays.sort(dirs, Comparator.comparing(File::getName));
        int excess = dirs.length - maxSnapshots;
        for (int i = 0; i < excess; i++) {
            deleteRecursiveQuiet(dirs[i]);
        }
    }

    private static void deleteRecursiveQuiet(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File c : children) {
                    deleteRecursiveQuiet(c);
                }
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    private static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /**
     * Stop the timer and shut down the worker pool (plugin disable).
     */
    public void shutdown() {
        cancelTask();
        ioPool.shutdown();
        try {
            if (!ioPool.awaitTermination(15, TimeUnit.SECONDS)) {
                ioPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            ioPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
