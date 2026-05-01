package me.gamechampcrafted.normalshops;

import me.gamechampcrafted.normalshops.commands.NormalShopsCommand;
import me.gamechampcrafted.normalshops.shop.TradingMenuManager;
import me.gamechampcrafted.normalshops.data.DataManager;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.Setting;
import me.gamechampcrafted.normalshops.data.YAMLDataManager;
import me.gamechampcrafted.normalshops.events.*;
import me.gamechampcrafted.normalshops.menu.MenuListener;
import me.gamechampcrafted.normalshops.menu.delete.ShopHistoryMenuManager;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.shop.Pile;
import me.gamechampcrafted.normalshops.shop.PlayerShopManager;
import me.gamechampcrafted.normalshops.shop.ShopManager;
import me.gamechampcrafted.normalshops.shop.EarningsChestManager;
import me.gamechampcrafted.normalshops.shop.StockChestManager;
import me.gamechampcrafted.normalshops.shop.connector.ConnectorManager;
import me.gamechampcrafted.normalshops.shop.display.FrameDisplay;
import me.gamechampcrafted.normalshops.shop.display.GlassDisplay;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class NormalShops extends JavaPlugin {

    static {
        ConfigurationSerialization.registerClass(ItemShop.class, "ItemShop");
        ConfigurationSerialization.registerClass(Pile.class, "Pile");
        ConfigurationSerialization.registerClass(PlayerShopManager.class, "PlayerShopManager");
        ConfigurationSerialization.registerClass(ShopManager.class, "ShopManager");
        ConfigurationSerialization.registerClass(FrameDisplay.class, "FrameDisplay");
        ConfigurationSerialization.registerClass(GlassDisplay.class, "GlassDisplay");
    }

    private static NormalShops plugin;

    public static NormalShops getInstance() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;

        loadData();
        initializeAllManagers();
        registerAllEvents();
        registerCommands();
        CoreProtectLogger.initialize();

        getLogger().info("NormalShops enabled.");
    }

    @Override
    public void onDisable() {
        if (menuListener != null) menuListener.closeActiveMenus();
        if (connectorManager != null) connectorManager.cancelAllConnections();
        if (tradingMenuManager != null) tradingMenuManager.closeAllSessions();
        if (stockChestManager != null) stockChestManager.closeAllSessions();
        if (earningsChestManager != null) earningsChestManager.closeAllSessions();
        if (shopHistoryMenuManager != null) shopHistoryMenuManager.closeAllSessions();
        saveData();
        getLogger().info("NormalShops disabled.");
    }

    private void initializeAllManagers() {
        menuListener = new MenuListener();
        connectorManager = new ConnectorManager();
        chatInputListener = new ChatInputListener(this);
        tradingMenuManager = new TradingMenuManager();
        stockChestManager = new StockChestManager();
        earningsChestManager = new EarningsChestManager();
        shopHistoryMenuManager = new ShopHistoryMenuManager();
    }

    private void registerCommands() {
        NormalShopsCommand command = new NormalShopsCommand();
        getCommand("normalshops").setExecutor(command);
        getCommand("normalshops").setTabCompleter(command);
    }

    private void registerAllEvents() {
        registerEvents(
                menuListener,
                tradingMenuManager,
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
                new JoinEvent(),
                new ExplodeEvent()
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
            data = new YAMLDataManager(this, getDataFolder(), "data");
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
    }

    public void saveData() {
        data.getConfig().set("shop-manager", shopManager);
        data.saveConfig();
        shopManager.saveAll();
    }

    private ShopManager shopManager;

    public ShopManager getShopManager() {
        return shopManager;
    }

    private ConnectorManager connectorManager;

    public ConnectorManager getConnectorManager() {
        return connectorManager;
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
}
