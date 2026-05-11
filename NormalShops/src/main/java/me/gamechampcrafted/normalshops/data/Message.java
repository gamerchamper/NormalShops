package me.gamechampcrafted.normalshops.data;

import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.utils.MessageParametizer;
import me.gamechampcrafted.normalshops.utils.Parameterizer;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public enum Message {

    // GENERAL
    UPDATE(MessageType.WARN),
    NO_PERMISSION(MessageType.FAIL),
    FEATURE_DISABLED(MessageType.FAIL),
    SHOP_NO_BREAK(MessageType.FAIL),
    SHOP_BREAK_OWNER(MessageType.WARN),
    SHOP_BREAK_OPERATOR(MessageType.WARN),
    PILE_NO_BREAK(MessageType.FAIL),
    PILE_BREAK_OWNER(MessageType.WARN),
    PILE_BREAK_OPERATOR(MessageType.WARN),
    BLOCK_PLACE(MessageType.FAIL),
    SHOP_ERROR(MessageType.FAIL),
    PILE_NOT_OWNER(MessageType.FAIL),
    COOLDOWN(MessageType.FAIL),
    BREAK_CONFIRM(MessageType.WARN),
    BREAK_OPERATOR_CONFIRM(MessageType.WARN),

    COMMAND_DELETE_LOOK_NOTHING(MessageType.FAIL),
    COMMAND_DELETE_LOOK_NOT_SHOP(MessageType.FAIL),

    COMMAND_RESTORE_NO_BACKUP(MessageType.FAIL),
    COMMAND_RESTORE_BLOCK_INVALID(MessageType.FAIL),
    COMMAND_RESTORE_ALREADY_SHOP(MessageType.FAIL),
    COMMAND_RESTORE_SUCCESS(MessageType.CONFIRM),

    VIEW_SHOPS_EMPTY(MessageType.FAIL),
    VIEW_SHOPS_CANNOT_OPEN(MessageType.FAIL),

    SHOPS_MENU_EMPTY(MessageType.FAIL),
    SHOPS_MENU_TELEPORT_COOLDOWN(MessageType.FAIL),
    SHOPS_MENU_TELEPORTED(MessageType.CONFIRM),
    SHOPS_MENU_REGION_BLOCKED(MessageType.FAIL),
    SHOPS_MENU_WORLD_UNAVAILABLE(MessageType.FAIL),

    SHOPS_ITEM_USAGE(MessageType.FAIL),
    SHOPS_ITEM_NO_MATCH(MessageType.FAIL),
    SHOPS_ITEM_NO_SHOPS_SELLING(MessageType.FAIL),

    STOCKPILE_NOT_OWNER(MessageType.FAIL),
    STOCKPILE_NO_BREAK(MessageType.FAIL),
    STOCKPILE_REMOVE_FIRST(MessageType.WARN),
    STOCKPILE_NO_HOPPER(MessageType.FAIL),

    SHOP_LIMIT(MessageType.FAIL),
    PILE_LIMIT(MessageType.FAIL),
    STOCKPILE_LIMIT(MessageType.FAIL),
    PILE_SHOP_LIMIT(MessageType.FAIL),

    STOCK_WARNING(MessageType.STOCK_WARNING),

    // TRANSACTIONS
    BUY_NO_STOCK(MessageType.FAIL),
    BUY_NO_MONEY(MessageType.FAIL),
    BUY_COOLDOWN(MessageType.FAIL),
    SHOP_AT,
    SHOP_AT_CUSTOM,
    SHOP_DETAILS,
    HOVER_VARIOUS_PRODUCTS,
    HOVER_YOUR_SHOP,
    HOVER_YOUR_SHOP_NOTIFY,
    HOVER_YOUR_SHOP_REMOTE_HINT,
    BUY_MULTIPLE(MessageType.TRANSACTION),
    BUY_MULTIPLE_CUSTOM(MessageType.TRANSACTION),
    BUY_SINGLE(MessageType.TRANSACTION),
    BUY_SINGLE_CUSTOM(MessageType.TRANSACTION),
    SELL_MULTIPLE(MessageType.TRANSACTION),
    SELL_SINGLE(MessageType.TRANSACTION),

    // CONNECTIONS
    CONNECT_STOCKPILE,
    CONNECT_EARNINGS,
    CONNECT_SHOP,

    CONNECTOR_STOCKPILE(MessageType.CONFIRM),
    CONNECTOR_EARNINGS(MessageType.CONFIRM),
    CONNECTOR_SHOP(MessageType.CONFIRM),
    CONNECTOR_CANCEL(MessageType.WARN),
    CONNECTOR_INVALID(MessageType.FAIL),

    CONNECTOR_REMOVE_STOCKPILE(MessageType.WARN),
    CONNECTOR_REMOVE_EARNINGS(MessageType.WARN),
    CONNECTOR_REMOVE_SHOP(MessageType.WARN),

    CLEAR_STOCKPILES(MessageType.WARN),

    // MENU INTERACTIONS
    COLLECT_NO_EARNINGS(MessageType.WARN),
    COLLECT_EARNINGS(MessageType.CONFIRM),
    COLLECT_PILE_EARNINGS(MessageType.CONFIRM),
    COLLECT_PILE_NO_EARNINGS(MessageType.WARN),

    CREATE_DOUBLE_CHEST(MessageType.FAIL),
    CREATE_NOT_EMPTY(MessageType.FAIL),
    CREATE_SHOP_EXIST(MessageType.FAIL),
    CREATE_INVALID(MessageType.FAIL),
    CREATE_SHOP(MessageType.CONFIRM),

    CHANGE_SHOP(MessageType.CONFIRM),
    DELETE_SHOP(MessageType.WARN),

    CUSTOM_NAME_TYPE(MessageType.WARN),
    CUSTOM_NAME_SET(MessageType.CONFIRM),
    CUSTOM_NAME_CANCEL(MessageType.FAIL),
    CUSTOM_NAME_REMOVE(MessageType.FAIL),
    CUSTOM_NAME_INVALID(MessageType.FAIL),

    SALE_TEXT_TYPE(MessageType.WARN),
    SALE_TEXT_SET(MessageType.CONFIRM),
    SALE_TEXT_CANCEL(MessageType.FAIL),
    SALE_TEXT_REMOVE(MessageType.FAIL),
    SALE_TEXT_INVALID(MessageType.FAIL),

    NOT_BLOCK(MessageType.FAIL),
    GLASS_NOT_GLASS(MessageType.FAIL),
    GLASS_CHANGE_GLASS(MessageType.CONFIRM),
    GLASS_CHANGE_BASE(MessageType.CONFIRM),

    FRAME_ADD_FRAME(MessageType.CONFIRM),
    FRAME_REMOVE_FRAME(MessageType.WARN),

    BLOCK_CHANGE(MessageType.CONFIRM),

    CUSTOMIZE_REQUIRES_STOCK(MessageType.FAIL),

    DISPLAY_CLEAR(MessageType.WARN),

    DISPLAY_GLASS_BUILD(MessageType.CONFIRM),
    DISPLAY_GLASS_INVALID_BLOCK(MessageType.FAIL),
    DISPLAY_GLASS_INVALID_LIGHT(MessageType.FAIL),

    DISPLAY_FRAME_BUILD(MessageType.CONFIRM),
    DISPLAY_FRAME_INVALID_CHEST(MessageType.FAIL),

    // MENU TITLES
    MENU_CHANGE_SHOP,
    MENU_CREATE_SHOP,
    MENU_SHOP_OPEN,
    MENU_SHOP_CLOSED,
    MENU_CUSTOMIZE,
    MENU_BUY,
    MENU_BUY_CUSTOM,
    MENU_STOCK_CHEST,
    MENU_SETTINGS,
    MENU_STOCKPILE,
    /** Linked stockpile picker opened from the stock chest nav row */
    MENU_STOCKPILE_BROWSER,
    /** Read-only mirror of one linked stockpile inventory */
    MENU_STOCKPILE_VIEW_READONLY,
    /** Browse grid row item; YAML uses placeholders {@code {index}} and {@code {location}}. */
    MENU_STOCKPILE_BROWSER_ENTRY,
    /** Back row in linked-stockpile browser (flower pattern). */
    BUTTON_STOCKPILE_BROWSER_NAV_BACK,
    MENU_EARNINGS,
    MENU_DELETE,
    MENU_MY_SHOPS,
    MENU_PUBLIC_SHOPS,
    MENU_COLOR,

    // BUTTONS
    BUTTON_BACK,

    // CREATE MENU
    BUTTON_PRICE,
    BUTTON_PRODUCT,
    BUTTON_CREATE_SHOP,

    // CHANGE MENU
    BUTTON_SAVE_CHANGES,
    BUTTON_CHANGE_SHOP,
    BUTTON_DELETE_SHOP,
    BUTTON_DELETE_REDIRECT,
    BUTTON_UNLIMITED_ENABLED,
    BUTTON_UNLIMITED_DISABLED,

    // EDIT MENU
    BUTTON_COLLECT,
    BUTTON_NONE,
    BUTTON_STOCK_CHEST,
    BUTTON_SETTINGS,
    BUTTON_CUSTOMIZE,
    BUTTON_CONNECT_STOCK,
    BUTTON_STOCKPILE_BROWSER,
    BUTTON_CONNECT_EARNINGS,

    STOCKPILE_VIEW_IN_USE(MessageType.FAIL),
    STOCKPILE_NOT_LOADED(MessageType.FAIL),

    // BUY MENU
    BUTTON_BUY,
    BUTTON_OUT_OF_STOCK,

    // CUSTOMIZE MENU
    BUTTON_COLOR,
    BUTTON_COLOR_SELECTED,
    BUTTON_SOUND,
    BUTTON_SOUND_SELECTED,
    BUTTON_THEME,
    BUTTON_CHANGE_BLOCK,
    BUTTON_CLEAR_DISPLAY,
    BUTTON_ADD_CUSTOM_NAME,
    BUTTON_REMOVE_CUSTOM_NAME,
    BUTTON_GLASS_DISPLAY,
    BUTTON_DISPLAY_CHANGE_GLASS,
    BUTTON_DISPLAY_CHANGE_BASE,
    BUTTON_DISPLAY_ADD_LIGHT,
    BUTTON_DISPLAY_REMOVE_LIGHT,
    BUTTON_DISPLAY_ADD_SALE,
    BUTTON_DISPLAY_REMOVE_SALE,
    BUTTON_FRAME_DISPLAY,
    BUTTON_DISPLAY_ADD_FRAME,
    BUTTON_DISPLAY_REMOVE_FRAME,
    BUTTON_DISPLAY_MOVE_TO_TOP,

    // EARNINGS MENU
    BUTTON_PILE_COLLECT,
    BUTTON_CONNECT_SHOP,

    // SETTINGS MENU
    BUTTON_NOTIFICATIONS_ENABLED,
    BUTTON_NOTIFICATIONS_DISABLED,
    BUTTON_STOCK_WARNING_ENABLED,
    BUTTON_STOCK_WARNING_DISABLED,
    BUTTON_STATS_HOLOGRAM_ENABLED,
    BUTTON_STATS_HOLOGRAM_DISABLED,
    BUTTON_CLEAR_STOCKPILES,

    BLANK;

    private static MessageManager dataManager;
    private static boolean initialized = false;

    public static void initialize() throws IOException {
        if (initialized) return;
        Setting.initialize(); // To access LANGUAGE settings
        dataManager = new MessageManager(NormalShops.getInstance(), Setting.LANGUAGE.getString(),
                NormalShops.getInstance().getResolvedDataFolder());
        initialized = true;
    }

    public static void reload() throws IOException {
        Setting.reload();
        dataManager = new MessageManager(NormalShops.getInstance(), Setting.LANGUAGE.getString(),
                NormalShops.getInstance().getResolvedDataFolder());
    }

    final MessageType type;

    Message() {
        this(null);
    }

    Message(MessageType type) {
        this.type = type;
    }

    public void send(CommandSender player) {
        if (type == null) throw new IllegalArgumentException("Not a sendable message");
        type.send(player, toString(), true);
    }

    public void sendSilently(CommandSender player) {
        if (type == null) throw new IllegalArgumentException("Not a sendable message");
        type.send(player, toString(), false);
    }

    @Override
    public String toString() {
        if (dataManager == null) {
            return name().replace('_', ' ');
        }
        String text = dataManager.get(getPath());
        if (text != null) {
            return text;
        }
        return Utils.colorize("&7" + getPath().replace('-', ' '));
    }

    public List<String> getLore() {
        if (type != null) throw new IllegalArgumentException("Not a button");
        if (dataManager == null) {
            return Collections.emptyList();
        }
        return dataManager.getLore(getPath());
    }

    public List<String> getParameterizedLore(Parameterizer<?> parameterizer) {
        return getLore().stream()
                .map(line -> parameterizer.setString(line).toString())
                .collect(Collectors.toList());
    }

    public MessageType getType() {
        return type;
    }

    private String path;

    private String getPath() {
        if (path == null) {
            path = name().toLowerCase().replace("_", "-");
        }
        return path;
    }

    public MessageParametizer parameterizer() {
        return new MessageParametizer(this);
    }
}
