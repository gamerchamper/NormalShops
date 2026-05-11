package me.gamechampcrafted.normalshops;

import me.gamechampcrafted.normalshops.commands.NormalShopsCommand;
import me.gamechampcrafted.normalshops.commands.SetPointCommand;
import me.gamechampcrafted.normalshops.commands.ShopsCommand;
import me.gamechampcrafted.normalshops.shop.TradingMenuManager;
import me.gamechampcrafted.normalshops.data.DataManager;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.Setting;
import me.gamechampcrafted.normalshops.menu.GuiDisplayItemLeakTask;
import me.gamechampcrafted.normalshops.data.YAMLDataManager;
import me.gamechampcrafted.normalshops.events.*;
import me.gamechampcrafted.normalshops.menu.MenuListener;
import me.gamechampcrafted.normalshops.menu.delete.ShopHistoryMenuManager;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.shop.Pile;
import me.gamechampcrafted.normalshops.shop.PlayerShopManager;
import me.gamechampcrafted.normalshops.shop.ShopBackupService;
import me.gamechampcrafted.normalshops.shop.PrivateShopStatsHologramManager;
import me.gamechampcrafted.normalshops.shop.ShopManager;
import me.gamechampcrafted.normalshops.shop.EarningsChestManager;
import me.gamechampcrafted.normalshops.shop.StockChestManager;
import me.gamechampcrafted.normalshops.shop.PhysicalItemProxyResolveTask;
import me.gamechampcrafted.normalshops.shop.StockpileViewManager;
import me.gamechampcrafted.normalshops.backup.ShopDataAutoBackup;
import me.gamechampcrafted.normalshops.shop.connector.ConnectorManager;
import me.gamechampcrafted.normalshops.shop.display.FrameDisplay;
import me.gamechampcrafted.normalshops.shop.display.GlassDisplay;
import me.gamechampcrafted.normalshops.shop.display.HintOnlyDisplay;
import me.gamechampcrafted.normalshops.update.ModrinthUpdateService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.jetbrains.annotations.Nullable;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;

public final class NormalShops extends JavaPlugin {

    static {
        ConfigurationSerialization.registerClass(ItemShop.class, "ItemShop");
        ConfigurationSerialization.registerClass(Pile.class, "Pile");
        ConfigurationSerialization.registerClass(PlayerShopManager.class, "PlayerShopManager");
        ConfigurationSerialization.registerClass(ShopManager.class, "ShopManager");
        ConfigurationSerialization.registerClass(FrameDisplay.class, "FrameDisplay");
        ConfigurationSerialization.registerClass(GlassDisplay.class, "GlassDisplay");
        ConfigurationSerialization.registerClass(HintOnlyDisplay.class, "HintOnlyDisplay");
    }

    private static NormalShops plugin;

    public static NormalShops getInstance() {
        return plugin;
    }

    /**
     * All plugin YAML/config/backups should use this path: if {@code plugins/ClickShop} already exists
     * (legacy ClickShop migration), it is used instead of {@link #getDataFolder()} ({@code plugins/NormalShops}).
     */
    public File getResolvedDataFolder() {
        File defaultFolder = super.getDataFolder();
        File pluginsDir = defaultFolder.getParentFile();
        if (pluginsDir == null) {
            return defaultFolder;
        }
        File legacy = new File(pluginsDir, "ClickShop");
        if (legacy.exists() && legacy.isDirectory()) {
            return legacy;
        }
        return defaultFolder;
    }

    @Override
    public void onEnable() {
        plugin = this;

        loadData();
        if (!isEnabled()) {
            return;
        }
        shopDataAutoBackup = new ShopDataAutoBackup(this);
        shopDataAutoBackup.schedule();
        initializeAllManagers();
        registerAllEvents();
        registerCommands();
        CoreProtectLogger.initialize();

        if (Setting.CHECK_UPDATE.isEnabled()) {
            ModrinthUpdateService.get().checkAsync(this);
        }

        Runnable refreshOutOfStockDisplays = () -> {
            if (shopManager != null) {
                shopManager.refreshAllOutOfStockDisplays();
            }
        };
        // Immediate pass after managers exist; delayed passes catch shops whose chunks were not ready yet.
        Bukkit.getScheduler().runTask(this, refreshOutOfStockDisplays);
        Bukkit.getScheduler().runTaskLater(this, refreshOutOfStockDisplays, 20L);
        Bukkit.getScheduler().runTaskLater(this, refreshOutOfStockDisplays, 100L);

        int guiLeakSweepTicks = Setting.GUI_ITEM_LEAK_SWEEP_TICKS.getInt();
        if (guiLeakSweepTicks > 0) {
            guiDisplayItemLeakTask = Bukkit.getScheduler().runTaskTimer(
                    this, new GuiDisplayItemLeakTask(), guiLeakSweepTicks, guiLeakSweepTicks);
        }

        int physicalProxyTicks = Setting.PHYSICAL_ITEM_PROXY_RESOLVE_TICKS.getInt();
        if (physicalProxyTicks > 0) {
            physicalItemProxyResolveTask = Bukkit.getScheduler().runTaskTimer(
                    this, new PhysicalItemProxyResolveTask(), physicalProxyTicks, physicalProxyTicks);
        }

        getLogger().info("NormalShops enabled.");
    }

    @Override
    public void onDisable() {
        if (menuListener != null) menuListener.closeActiveMenus();
        if (connectorManager != null) connectorManager.cancelAllConnections();
        if (tradingMenuManager != null) tradingMenuManager.closeAllSessions();
        if (stockpileViewManager != null) stockpileViewManager.closeAllSessions();
        if (stockChestManager != null) stockChestManager.closeAllSessions();
        if (earningsChestManager != null) earningsChestManager.closeAllSessions();
        if (shopHistoryMenuManager != null) shopHistoryMenuManager.closeAllSessions();
        if (privateShopStatsHologramManager != null) {
            privateShopStatsHologramManager.shutdown();
        }
        if (shopDataAutoBackup != null) {
            shopDataAutoBackup.shutdown();
        }
        if (guiDisplayItemLeakTask != null) {
            guiDisplayItemLeakTask.cancel();
            guiDisplayItemLeakTask = null;
        }
        if (physicalItemProxyResolveTask != null) {
            physicalItemProxyResolveTask.cancel();
            physicalItemProxyResolveTask = null;
        }
        saveData();
        getLogger().info("NormalShops disabled.");
    }

    private void initializeAllManagers() {
        menuListener = new MenuListener();
        connectorManager = new ConnectorManager();
        chatInputListener = new ChatInputListener(this);
        tradingMenuManager = new TradingMenuManager();
        stockpileViewManager = new StockpileViewManager(this);
        stockChestManager = new StockChestManager();
        earningsChestManager = new EarningsChestManager();
        shopHistoryMenuManager = new ShopHistoryMenuManager();
        privateShopStatsHologramManager = new PrivateShopStatsHologramManager(this);
        privateShopStatsHologramManager.start();
    }

    private void registerCommands() {
        NormalShopsCommand command = new NormalShopsCommand();
        getCommand("normalshops").setExecutor(command);
        getCommand("normalshops").setTabCompleter(command);
        getCommand("viewshops").setExecutor(command);
        getCommand("viewshops").setTabCompleter(command);
        ShopsCommand shopsCommand = new ShopsCommand();
        getCommand("shops").setExecutor(shopsCommand);
        getCommand("shops").setTabCompleter(shopsCommand);
        SetPointCommand setPointCommand = new SetPointCommand();
        getCommand("setpoint").setExecutor(setPointCommand);
    }

    private void registerAllEvents() {
        registerEvents(
                menuListener,
                tradingMenuManager,
                stockpileViewManager,
                stockChestManager,
                earningsChestManager,
                shopHistoryMenuManager,
                connectorManager,
                chatInputListener,
                new ShopInteractEvent(),
                new PileInteractEvent(),
                new PileBreakEvent(),
                new ShopBreakEvent(),
                new ShopCreateEvent(),
                new PlaceEvent(),
                new StockpileBreakEvent(),
                new StockpileInventoryListener(),
                new JoinEvent(),
                new ExplodeEvent(),
                privateShopStatsHologramManager
        );

        if (Setting.PROTECT_STOCKPILES.isEnabled()) {
            registerEvents(new StockpileGriefEvent());
        }

        if (Setting.BLOCK_PISTON.isEnabled()) {
            registerEvents(new PistonEvent());
        }
    }

    private void registerEvents(Listener... listeners) {
        PluginManager pluginManager = Bukkit.getPluginManager();
        for (Listener listener : listeners) {
            pluginManager.registerEvents(listener, this);
        }
    }

    private DataManager data;

    private void loadData() {
        // Initialize enums
        try {
            Setting.initialize();
        } catch (IOException exception) {
            getLogger().severe("Couldn't load config.yml! Default settings will be used.");
            getLogger().severe("Cause: " + exception.getMessage());
            exception.printStackTrace();
        }

        try {
            Message.initialize();
        } catch (IOException exception) {
            getLogger().severe("Couldn't load message.yml! Messages will be empty.");
            getLogger().severe("Cause: " + exception.getMessage());
            exception.printStackTrace();
        }

        try {
            File dataRoot = getResolvedDataFolder();
            if (!dataRoot.equals(getDataFolder())) {
                getLogger().info("Using ClickShop data folder: " + dataRoot.getAbsolutePath());
            }
            data = new YAMLDataManager(this, dataRoot, "data", dataRoot);
        } catch (IOException exception) {
            getLogger().severe("Couldn't load data.yml! Disabling plugin...");
            getLogger().severe("Cause: " + exception.getMessage());
            exception.printStackTrace();
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (data.getConfig().contains("shop-manager")) {
            shopManager = (ShopManager) data.getConfig().get("shop-manager");
        } else {
            shopManager = new ShopManager();
        }
        if (Setting.RECOVER_SHOP_FILES.isEnabled()) {
            shopManager.recoverAllManagers();
            Logger.warning("Loaded and recovered all shop files. Restart server again to save memory!");
            Setting.RECOVER_SHOP_FILES.set(false);
            Setting.saveSettings();
        }

        try {
            shopBackupService = new ShopBackupService(this);
        } catch (IOException exception) {
            getLogger().severe("Could not initialize shop-backups.yml — automatic backups are disabled.");
            getLogger().severe("Cause: " + exception.getMessage());
            exception.printStackTrace();
            shopBackupService = null;
        }
    }

    public void saveData() {
        if (data == null) {
            return;
        }
        if (shopManager != null) {
            data.getConfig().set("shop-manager", shopManager);
            shopManager.saveAll();
        }
        data.saveConfig();
    }

    private ShopManager shopManager;

    @Nullable
    private ShopBackupService shopBackupService;

    public ShopManager getShopManager() {
        return shopManager;
    }

    @Nullable
    public ShopBackupService getShopBackupService() {
        return shopBackupService;
    }

    private ConnectorManager connectorManager;

    public ConnectorManager getConnectorManager() {
        return connectorManager;
    }

    private ShopDataAutoBackup shopDataAutoBackup;

    public ShopDataAutoBackup getShopDataAutoBackup() {
        return shopDataAutoBackup;
    }

    private MenuListener menuListener;

    public MenuListener getMenuListener() {
        return menuListener;
    }

    private ChatInputListener chatInputListener;

    public ChatInputListener getChatInputListener() {
        return chatInputListener;
    }

    private TradingMenuManager tradingMenuManager;

    public TradingMenuManager getTradingMenuManager() {
        return tradingMenuManager;
    }

    private StockpileViewManager stockpileViewManager;

    public StockpileViewManager getStockpileViewManager() {
        return stockpileViewManager;
    }

    private StockChestManager stockChestManager;

    public StockChestManager getStockChestManager() {
        return stockChestManager;
    }

    private EarningsChestManager earningsChestManager;

    public EarningsChestManager getEarningsChestManager() {
        return earningsChestManager;
    }

    private ShopHistoryMenuManager shopHistoryMenuManager;

    public ShopHistoryMenuManager getShopHistoryMenuManager() {
        return shopHistoryMenuManager;
    }

    private PrivateShopStatsHologramManager privateShopStatsHologramManager;

    public PrivateShopStatsHologramManager getPrivateShopStatsHologramManager() {
        return privateShopStatsHologramManager;
    }

    @Nullable
    private BukkitTask guiDisplayItemLeakTask;

    @Nullable
    private BukkitTask physicalItemProxyResolveTask;
}
