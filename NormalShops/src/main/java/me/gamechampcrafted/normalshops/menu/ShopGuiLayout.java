package me.gamechampcrafted.normalshops.menu;

import me.gamechampcrafted.normalshops.Logger;
import me.gamechampcrafted.normalshops.data.GuiConfigManager;
import me.gamechampcrafted.normalshops.data.Setting;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Slot layouts for shop chest GUIs (45 slots: indices 0–44).
 * Primary format: five lines of nine characters each under {@code shop-gui.trading-layout}
 * and {@code shop-gui.owner-layout} (recipe-style). Legacy numeric slots remain supported
 * when {@code trading-layout} / {@code owner-layout} are absent.
 */
public final class ShopGuiLayout {

    private static final int ROWS = 5;
    private static final int COLS = 9;
    private static final int CHEST_SIZE = ROWS * COLS;
    private static final int PRODUCT_COUNT = 9;

    /** Classic trading grid: price + 3×3 products + buy; save / delete / back for change listing. */
    private static final List<String> DEFAULT_TRADING_GRID = List.of(
            "XXXXXXXXX",
            "XXXSXOOOX",
            "XPXBXOOOX",
            "XXXDXOOOX",
            "KXXXXXXXX"
    );

    /** Owner / trusted management panel. */
    private static final List<String> DEFAULT_OWNER_GRID = List.of(
            "XXXXXXXXX",
            "XLXXEXXUX",
            "XFXXHXXGX",
            "XXXXXXXXX",
            "TAXXXXXXI"
    );

    private static final List<Integer> DEFAULT_PRODUCT_SLOTS =
            List.of(14, 15, 16, 23, 24, 25, 32, 33, 34);

    private static final OwnerKeyMap DEFAULT_OWNER_KEYS = OwnerKeyMap.fromSection(null, null);

    private static final OwnerSlots DEFAULT_OWNER = Objects.requireNonNullElseGet(
            parseOwnerFromGrid(DEFAULT_OWNER_GRID, DEFAULT_OWNER_KEYS),
            () -> new OwnerSlots(10, 13, 16, 19, 22, 25, 29, 31, 33)
    );

    private static final List<Integer> DEFAULT_OWNER_FILLERS =
            ownerFillerSlotsFromGrid(DEFAULT_OWNER_GRID, DEFAULT_OWNER_KEYS);

    private static final ParsedTrading DEFAULT_TRADING_PARSED = Objects.requireNonNull(
            parseTradingGrid(DEFAULT_TRADING_GRID, TradingKeyMap.defaults()),
            "default trading grid"
    );

    private static final ShopGuiLayout DEFAULT = buildFromParsed(
            DEFAULT_TRADING_PARSED,
            DEFAULT_OWNER,
            DEFAULT_OWNER_FILLERS
    );

    private static ShopGuiLayout instance = DEFAULT;

    private final int priceSlot;
    private final int buySlot;
    private final int createShopSlot;
    private final List<Integer> productSlots;
    private final int changeShopSaveSlot;
    private final int changeShopDeleteSlot;
    private final int changeShopBackSlot;
    private final List<Integer> editableSlots;
    private final OwnerSlots owner;
    private final List<Integer> tradingFillerSlots;
    private final List<Integer> ownerFillerSlots;

    private ShopGuiLayout(
            int priceSlot,
            int buySlot,
            int createShopSlot,
            List<Integer> productSlots,
            int changeShopSaveSlot,
            int changeShopDeleteSlot,
            int changeShopBackSlot,
            OwnerSlots owner,
            List<Integer> tradingFillerSlots,
            List<Integer> ownerFillerSlots
    ) {
        this.priceSlot = priceSlot;
        this.buySlot = buySlot;
        this.createShopSlot = createShopSlot;
        this.productSlots = List.copyOf(productSlots);
        this.changeShopSaveSlot = changeShopSaveSlot;
        this.changeShopDeleteSlot = changeShopDeleteSlot;
        this.changeShopBackSlot = changeShopBackSlot;
        this.owner = owner;
        this.tradingFillerSlots = List.copyOf(tradingFillerSlots);
        this.ownerFillerSlots = List.copyOf(ownerFillerSlots);
        LinkedHashSet<Integer> editable = new LinkedHashSet<>();
        editable.add(priceSlot);
        editable.addAll(productSlots);
        List<Integer> ed = new ArrayList<>(editable);
        Collections.sort(ed);
        this.editableSlots = List.copyOf(ed);
    }

    private static ShopGuiLayout buildFromParsed(ParsedTrading t, OwnerSlots ownerSlots, List<Integer> ownerFillers) {
        return new ShopGuiLayout(
                t.priceSlot,
                t.buySlot,
                t.createShopSlot,
                t.productSlots,
                t.saveSlot,
                t.deleteSlot,
                t.backSlot,
                ownerSlots,
                t.fillerSlots(),
                ownerFillers
        );
    }

    public List<Integer> tradingFillerSlots() {
        return tradingFillerSlots;
    }

    public List<Integer> ownerFillerSlots() {
        return ownerFillerSlots;
    }

    public static ShopGuiLayout get() {
        return instance;
    }

    public static void reload() {
        try {
            ConfigurationSection tradingSec = tradingSection();
            if (tradingSec == null) {
                instance = DEFAULT;
                Logger.info("[NormalShops] No trading layout in gui.yml (layouts.price-product / layouts.trading) or config shop-gui — using defaults.");
                return;
            }
            ParsedTrading trading = parseTradingSection(tradingSec);
            if (trading == null) {
                instance = DEFAULT;
                return;
            }

            OwnerResolution own = resolveOwnerLayout();
            OwnerSlots ownerSlots = own.slots();
            List<Integer> ownerFillers = own.fillerSlots();
            if (!validateOwnerDistinct(ownerSlots)) {
                Logger.warning("[NormalShops] owner layout: nine unique action slots required — using default owner grid.");
                ownerSlots = DEFAULT_OWNER;
                ownerFillers = DEFAULT_OWNER_FILLERS;
            }

            ShopGuiLayout candidate = buildFromParsed(trading, ownerSlots, ownerFillers);
            if (!validateTrading(candidate)) {
                Logger.warning("[NormalShops] trading + owner layout invalid — using defaults.");
                instance = DEFAULT;
                return;
            }
            instance = candidate;
        } catch (Exception e) {
            Logger.warning("[NormalShops] Failed to parse shop layouts: " + e.getMessage());
            instance = DEFAULT;
        }
    }

    /**
     * Prefer {@code gui.yml} {@code layouts.price-product} (change price/products GUI), then {@code layouts.trading}
     * (YAML alias / legacy); else {@code config.yml} {@code shop-gui}.
     */
    private static ConfigurationSection tradingSection() {
        FileConfiguration gui = GuiConfigManager.getConfig();
        if (gui != null) {
            ConfigurationSection pp = gui.getConfigurationSection("layouts.price-product");
            if (pp != null && (!pp.getStringList("trading-layout").isEmpty()
                    || pp.isSet("price-slot"))) {
                return pp;
            }
            ConfigurationSection lt = gui.getConfigurationSection("layouts.trading");
            if (lt != null && (!lt.getStringList("trading-layout").isEmpty()
                    || lt.isSet("price-slot"))) {
                return lt;
            }
        }
        FileConfiguration cfg = Setting.getConfig();
        if (cfg != null) {
            ConfigurationSection sg = cfg.getConfigurationSection("shop-gui");
            if (sg != null && (!sg.getStringList("trading-layout").isEmpty() || sg.isSet("price-slot"))) {
                return sg;
            }
        }
        return null;
    }

    private record OwnerResolution(OwnerSlots slots, List<Integer> fillerSlots) {}

    private static OwnerResolution resolveOwnerLayout() {
        FileConfiguration gui = GuiConfigManager.getConfig();
        if (gui != null) {
            ConfigurationSection lo = gui.getConfigurationSection("layouts.owner");
            if (lo != null) {
                List<String> rows = readGrid(lo, "owner-layout");
                if (rows != null) {
                    OwnerKeyMap ok = OwnerKeyMap.fromSection(lo.getConfigurationSection("owner-keys"), lo);
                    OwnerSlots fromGrid = parseOwnerFromGrid(rows, ok);
                    if (fromGrid != null && validateOwnerDistinct(fromGrid)) {
                        return new OwnerResolution(fromGrid, ownerFillerSlotsFromGrid(rows, ok));
                    }
                    Logger.warning("[NormalShops] gui.yml layouts.owner invalid — trying config shop-gui.");
                } else {
                    ConfigurationSection oc = lo.getConfigurationSection("owner-control-menu");
                    if (oc != null && hasNumericOwner(oc)) {
                        return new OwnerResolution(parseOwnerLegacy(oc), List.of());
                    }
                }
            }
        }
        FileConfiguration cfg = Setting.getConfig();
        if (cfg != null) {
            ConfigurationSection sg = cfg.getConfigurationSection("shop-gui");
            if (sg != null) {
                List<String> rows = readGrid(sg, "owner-layout");
                if (rows != null) {
                    OwnerKeyMap ok = OwnerKeyMap.fromSection(sg.getConfigurationSection("owner-keys"), sg);
                    OwnerSlots fromGrid = parseOwnerFromGrid(rows, ok);
                    if (fromGrid != null && validateOwnerDistinct(fromGrid)) {
                        return new OwnerResolution(fromGrid, ownerFillerSlotsFromGrid(rows, ok));
                    }
                }
                ConfigurationSection oc = sg.getConfigurationSection("owner-control-menu");
                if (oc != null && hasNumericOwner(oc)) {
                    return new OwnerResolution(parseOwnerLegacy(oc), List.of());
                }
            }
        }
        return new OwnerResolution(DEFAULT_OWNER, DEFAULT_OWNER_FILLERS);
    }

    private static boolean hasNumericOwner(ConfigurationSection oc) {
        return oc.isSet("collect-slot") || oc.isSet("change-listing-slot");
    }

    private static ParsedTrading parseTradingSection(ConfigurationSection sec) {
        List<String> gridRows = readGrid(sec, "trading-layout");
        if (gridRows != null) {
            TradingKeyMap keys = TradingKeyMap.fromSection(sec.getConfigurationSection("trading-keys"), sec);
            ParsedTrading t = parseTradingGrid(gridRows, keys);
            if (t == null) {
                Logger.warning("[NormalShops] trading-layout could not be parsed.");
            }
            return t;
        }
        if (sec.isSet("price-slot") && sec.isSet("buy-slot") && sec.contains("product-slots")) {
            return parseTradingLegacy(sec);
        }
        return parseTradingGrid(DEFAULT_TRADING_GRID, TradingKeyMap.defaults());
    }

    private static List<String> readGrid(ConfigurationSection sec, String key) {
        List<String> raw = sec.getStringList(key);
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        List<String> rows = new ArrayList<>();
        for (String line : raw) {
            if (line == null) {
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            rows.add(trimmed.replace("\t", ""));
        }
        if (rows.isEmpty()) {
            return null;
        }
        return rows;
    }

    private record ParsedTrading(
            int priceSlot,
            int buySlot,
            int createShopSlot,
            List<Integer> productSlots,
            int saveSlot,
            int deleteSlot,
            int backSlot,
            List<Integer> fillerSlots
    ) {}

    private static ParsedTrading parseTradingGrid(List<String> rows, TradingKeyMap k) {
        if (rows.size() != ROWS) {
            Logger.warning("[NormalShops] trading-layout must have exactly " + ROWS + " rows (got " + rows.size() + ").");
            return null;
        }
        int price = -1;
        int buy = -1;
        int create = -1;
        int save = -1;
        int del = -1;
        int back = -1;
        List<Integer> products = new ArrayList<>();
        List<Integer> tradingFillers = new ArrayList<>();

        for (int r = 0; r < ROWS; r++) {
            String row = normalizeGridRow(rows.get(r), k.fillerPad());
            if (row.length() != COLS) {
                Logger.warning("[NormalShops] trading-layout row " + (r + 1) + " must be " + COLS + " characters (after trimming).");
                return null;
            }
            for (int c = 0; c < COLS; c++) {
                char ch = row.charAt(c);
                int slot = r * COLS + c;
                TradingRole role = k.roleOf(ch);
                if (role == TradingRole.FILLER) {
                    tradingFillers.add(slot);
                    continue;
                }
                if (role == TradingRole.UNKNOWN) {
                    Logger.warning("[NormalShops] trading-layout: unknown character '" + ch + "' at row " + (r + 1) + " col " + (c + 1)
                            + ". Allowed: fillers " + k.fillerSummary() + " or keys from trading-keys.");
                    return null;
                }
                switch (role) {
                    case PRICE -> price = assignUnique("price", price, slot);
                    case PRODUCT -> products.add(slot);
                    case BUY -> {
                        int nb = assignUnique("buy", buy, slot);
                        if (nb == -2) {
                            buy = -2;
                        } else {
                            buy = nb;
                            if (create < 0) {
                                create = nb;
                            }
                        }
                    }
                    case CREATE -> create = assignUnique("create-shop", create, slot);
                    case SAVE -> save = assignUnique("save", save, slot);
                    case DELETE -> del = assignUnique("delete", del, slot);
                    case BACK -> back = assignUnique("back", back, slot);
                    default -> {
                    }
                }
                if (price == -2 || buy == -2 || create == -2 || save == -2 || del == -2 || back == -2) {
                    return null;
                }
            }
        }

        if (price < 0) {
            Logger.warning("[NormalShops] trading-layout: missing price marker (" + k.price + ").");
            return null;
        }
        if (buy < 0) {
            Logger.warning("[NormalShops] trading-layout: missing buy marker (" + k.buy + ").");
            return null;
        }
        if (create < 0) {
            create = buy;
        }
        if (save < 0 || del < 0 || back < 0) {
            Logger.warning("[NormalShops] trading-layout: need save (" + k.save + "), delete (" + k.delete + "), back (" + k.back + ").");
            return null;
        }
        if (products.size() != PRODUCT_COUNT) {
            Logger.warning("[NormalShops] trading-layout: need exactly " + PRODUCT_COUNT + " product markers (" + k.product + "), got " + products.size() + ".");
            return null;
        }

        Collections.sort(products);
        Collections.sort(tradingFillers);
        return new ParsedTrading(price, buy, create, List.copyOf(products), save, del, back, List.copyOf(tradingFillers));
    }

    private static int assignUnique(String name, int current, int slot) {
        if (current >= 0 && current != slot) {
            Logger.warning("[NormalShops] trading-layout: duplicate " + name + " slot.");
            return -2;
        }
        return slot;
    }

    private static String normalizeGridRow(String row, char padChar) {
        String t = row.trim();
        if (t.length() > COLS) {
            return t.substring(0, COLS);
        }
        if (t.length() < COLS) {
            StringBuilder sb = new StringBuilder(t);
            while (sb.length() < COLS) {
                sb.append(padChar);
            }
            return sb.toString();
        }
        return t;
    }

    private static List<Integer> ownerFillerSlotsFromGrid(List<String> rows, OwnerKeyMap k) {
        List<Integer> out = new ArrayList<>();
        for (int r = 0; r < ROWS; r++) {
            String row = normalizeGridRow(rows.get(r), k.fillerPad());
            for (int c = 0; c < COLS; c++) {
                if (k.roleOf(row.charAt(c)) == OwnerRole.FILLER) {
                    out.add(r * COLS + c);
                }
            }
        }
        Collections.sort(out);
        return List.copyOf(out);
    }

    private static ParsedTrading parseTradingLegacy(ConfigurationSection sec) {
        int price = sec.getInt("price-slot");
        int buy = sec.getInt("buy-slot");
        int create = sec.contains("create-shop-slot") ? sec.getInt("create-shop-slot") : buy;
        List<Integer> products = sec.getIntegerList("product-slots");
        ConfigurationSection ch = sec.getConfigurationSection("change-shop-menu");
        int save = ch != null ? ch.getInt("save-slot") : 12;
        int del = ch != null ? ch.getInt("delete-slot") : 30;
        int back = ch != null ? ch.getInt("back-slot") : 36;
        return new ParsedTrading(price, buy, create, products, save, del, back, List.of());
    }

    private enum TradingRole {
        FILLER,
        UNKNOWN,
        PRICE,
        PRODUCT,
        BUY,
        CREATE,
        SAVE,
        DELETE,
        BACK
    }

    private static final class TradingKeyMap {
        final char price;
        final char product;
        final char buy;
        final char create;
        final char save;
        final char delete;
        final char back;
        final Set<Character> fillers;
        final char fillerPad;

        private TradingKeyMap(
                char price,
                char product,
                char buy,
                char create,
                char save,
                char delete,
                char back,
                Set<Character> fillers,
                char fillerPad
        ) {
            this.price = price;
            this.product = product;
            this.buy = buy;
            this.create = create;
            this.save = save;
            this.delete = delete;
            this.back = back;
            this.fillers = fillers;
            this.fillerPad = fillerPad;
        }

        char fillerPad() {
            return fillerPad;
        }

        static TradingKeyMap defaults() {
            return fromSection(null, null);
        }

        static TradingKeyMap fromSection(ConfigurationSection tradingKeys, ConfigurationSection layoutTrading) {
            ConfigurationSection sec = tradingKeys;
            char price = sec != null && sec.contains("price") ? single(sec, "price", 'P') : 'P';
            char product = sec != null && sec.contains("product") ? single(sec, "product", 'O') : 'O';
            char buy = sec != null && sec.contains("buy") ? single(sec, "buy", 'B') : 'B';
            char create = sec != null && sec.contains("create-shop") ? single(sec, "create-shop", '\0') : '\0';
            char save = sec != null && sec.contains("save") ? single(sec, "save", 'S') : 'S';
            char delete = sec != null && sec.contains("delete") ? single(sec, "delete", 'D') : 'D';
            char back = sec != null && sec.contains("back") ? single(sec, "back", 'K') : 'K';

            char fillerPad = 'X';
            if (layoutTrading != null) {
                String fk = layoutTrading.getString("filler-key");
                if (fk != null && !fk.isEmpty()) {
                    fillerPad = fk.charAt(0);
                }
            }

            Set<Character> fillers = new LinkedHashSet<>();
            fillers.add(fillerPad);
            fillers.add('.');
            fillers.add(' ');
            if (sec != null && sec.isList("filler")) {
                for (Object o : Objects.requireNonNull(sec.getList("filler"))) {
                    String s = String.valueOf(o);
                    if (!s.isEmpty()) {
                        fillers.add(s.charAt(0));
                    }
                }
            }

            return new TradingKeyMap(price, product, buy, create, save, delete, back, fillers, fillerPad);
        }

        private static char single(ConfigurationSection sec, String key, char def) {
            String s = sec.getString(key);
            if (s == null || s.isEmpty()) {
                return def;
            }
            return s.charAt(0);
        }

        TradingRole roleOf(char raw) {
            char ch = raw;
            if (fillers.contains(ch)) {
                return TradingRole.FILLER;
            }
            char u = Character.toUpperCase(ch);
            if (u == Character.toUpperCase(price)) {
                return TradingRole.PRICE;
            }
            if (u == Character.toUpperCase(product) || ch == 'o') {
                return TradingRole.PRODUCT;
            }
            if (u == Character.toUpperCase(buy)) {
                return TradingRole.BUY;
            }
            if (create != '\0' && u == Character.toUpperCase(create)) {
                return TradingRole.CREATE;
            }
            if (u == Character.toUpperCase(save)) {
                return TradingRole.SAVE;
            }
            if (u == Character.toUpperCase(delete)) {
                return TradingRole.DELETE;
            }
            if (u == Character.toUpperCase(back)) {
                return TradingRole.BACK;
            }
            return TradingRole.UNKNOWN;
        }

        String fillerSummary() {
            return "X . space";
        }
    }

    private static OwnerSlots parseOwnerFromGrid(List<String> rows, OwnerKeyMap k) {
        if (rows.size() != ROWS) {
            Logger.warning("[NormalShops] owner-layout must have exactly " + ROWS + " rows.");
            return null;
        }
        int collect = -1;
        int changeListing = -1;
        int customize = -1;
        int settings = -1;
        int stockpile = -1;
        int stockChest = -1;
        int trusted = -1;
        int analytics = -1;
        int info = -1;

        for (int r = 0; r < ROWS; r++) {
            String row = normalizeGridRow(rows.get(r), k.fillerPad());
            if (row.length() != COLS) {
                Logger.warning("[NormalShops] owner-layout row " + (r + 1) + " must be " + COLS + " characters.");
                return null;
            }
            for (int c = 0; c < COLS; c++) {
                char ch = row.charAt(c);
                int slot = r * COLS + c;
                OwnerRole role = k.roleOf(ch);
                if (role == OwnerRole.FILLER) {
                    continue;
                }
                if (role == OwnerRole.UNKNOWN) {
                    Logger.warning("[NormalShops] owner-layout: unknown character '" + ch + "' at row " + (r + 1) + " col " + (c + 1) + ".");
                    return null;
                }
                switch (role) {
                    case COLLECT -> collect = assignUnique("collect", collect, slot);
                    case CHANGE_LISTING -> changeListing = assignUnique("change-listing", changeListing, slot);
                    case CUSTOMIZE -> customize = assignUnique("customize", customize, slot);
                    case SETTINGS -> settings = assignUnique("settings", settings, slot);
                    case STOCKPILE -> stockpile = assignUnique("stockpile", stockpile, slot);
                    case STOCK_CHEST -> stockChest = assignUnique("stock-chest", stockChest, slot);
                    case TRUSTED -> trusted = assignUnique("trusted", trusted, slot);
                    case ANALYTICS -> analytics = assignUnique("analytics", analytics, slot);
                    case INFO -> info = assignUnique("shop-info", info, slot);
                }
                if (collect == -2 || changeListing == -2 || customize == -2 || settings == -2
                        || stockpile == -2 || stockChest == -2 || trusted == -2 || analytics == -2 || info == -2) {
                    return null;
                }
            }
        }

        if (collect < 0 || changeListing < 0 || customize < 0 || settings < 0 || stockpile < 0
                || stockChest < 0 || trusted < 0 || analytics < 0 || info < 0) {
            Logger.warning("[NormalShops] owner-layout: each action letter must appear exactly once (E L U G F H T A I).");
            return null;
        }

        return new OwnerSlots(collect, changeListing, customize, settings, stockpile, stockChest, trusted, analytics, info);
    }

    private enum OwnerRole {
        FILLER,
        UNKNOWN,
        COLLECT,
        CHANGE_LISTING,
        CUSTOMIZE,
        SETTINGS,
        STOCKPILE,
        STOCK_CHEST,
        TRUSTED,
        ANALYTICS,
        INFO
    }

    private static final class OwnerKeyMap {
        final char collect;
        final char changeListing;
        final char customize;
        final char settings;
        final char stockpile;
        final char stockChest;
        final char trusted;
        final char analytics;
        final char info;
        final Set<Character> fillers;
        final char fillerPad;

        private OwnerKeyMap(
                char collect,
                char changeListing,
                char customize,
                char settings,
                char stockpile,
                char stockChest,
                char trusted,
                char analytics,
                char info,
                Set<Character> fillers,
                char fillerPad
        ) {
            this.collect = collect;
            this.changeListing = changeListing;
            this.customize = customize;
            this.settings = settings;
            this.stockpile = stockpile;
            this.stockChest = stockChest;
            this.trusted = trusted;
            this.analytics = analytics;
            this.info = info;
            this.fillers = fillers;
            this.fillerPad = fillerPad;
        }

        char fillerPad() {
            return fillerPad;
        }

        static OwnerKeyMap fromSection(ConfigurationSection ownerKeys, ConfigurationSection layoutOwner) {
            ConfigurationSection sec = ownerKeys;
            char e = sec != null && sec.contains("collect") ? single(sec, "collect", 'E') : 'E';
            char l = sec != null && sec.contains("change-listing") ? single(sec, "change-listing", 'L') : 'L';
            char u = sec != null && sec.contains("customize") ? single(sec, "customize", 'U') : 'U';
            char g = sec != null && sec.contains("settings") ? single(sec, "settings", 'G') : 'G';
            char f = sec != null && sec.contains("connect-stockpile") ? single(sec, "connect-stockpile", 'F') : 'F';
            char h = sec != null && sec.contains("stock-chest") ? single(sec, "stock-chest", 'H') : 'H';
            char t = sec != null && sec.contains("trusted-players") ? single(sec, "trusted-players", 'T') : 'T';
            char a = sec != null && sec.contains("analytics") ? single(sec, "analytics", 'A') : 'A';
            char i = sec != null && sec.contains("shop-info") ? single(sec, "shop-info", 'I') : 'I';

            char fillerPad = 'X';
            if (layoutOwner != null) {
                String fk = layoutOwner.getString("filler-key");
                if (fk != null && !fk.isEmpty()) {
                    fillerPad = fk.charAt(0);
                }
            }

            Set<Character> fillers = new LinkedHashSet<>();
            fillers.add(fillerPad);
            fillers.add('.');
            fillers.add(' ');
            if (sec != null && sec.isList("filler")) {
                for (Object o : Objects.requireNonNull(sec.getList("filler"))) {
                    String s = String.valueOf(o);
                    if (!s.isEmpty()) {
                        fillers.add(s.charAt(0));
                    }
                }
            }

            return new OwnerKeyMap(e, l, u, g, f, h, t, a, i, fillers, fillerPad);
        }

        private static char single(ConfigurationSection sec, String key, char def) {
            String s = sec.getString(key);
            if (s == null || s.isEmpty()) {
                return def;
            }
            return s.charAt(0);
        }

        OwnerRole roleOf(char raw) {
            if (fillers.contains(raw)) {
                return OwnerRole.FILLER;
            }
            char u = Character.toUpperCase(raw);
            if (u == Character.toUpperCase(collect)) {
                return OwnerRole.COLLECT;
            }
            if (u == Character.toUpperCase(changeListing)) {
                return OwnerRole.CHANGE_LISTING;
            }
            if (u == Character.toUpperCase(customize)) {
                return OwnerRole.CUSTOMIZE;
            }
            if (u == Character.toUpperCase(settings)) {
                return OwnerRole.SETTINGS;
            }
            if (u == Character.toUpperCase(stockpile)) {
                return OwnerRole.STOCKPILE;
            }
            if (u == Character.toUpperCase(stockChest)) {
                return OwnerRole.STOCK_CHEST;
            }
            if (u == Character.toUpperCase(trusted)) {
                return OwnerRole.TRUSTED;
            }
            if (u == Character.toUpperCase(analytics)) {
                return OwnerRole.ANALYTICS;
            }
            if (u == Character.toUpperCase(info)) {
                return OwnerRole.INFO;
            }
            return OwnerRole.UNKNOWN;
        }
    }

    private static OwnerSlots parseOwnerLegacy(ConfigurationSection oc) {
        return new OwnerSlots(
                oc.getInt("collect-slot", DEFAULT_OWNER.collect),
                oc.getInt("change-listing-slot", DEFAULT_OWNER.changeListing),
                oc.getInt("customize-slot", DEFAULT_OWNER.customize),
                oc.getInt("settings-slot", DEFAULT_OWNER.settings),
                oc.getInt("connect-stockpile-slot", DEFAULT_OWNER.connectStockpile),
                oc.getInt("stock-chest-slot", DEFAULT_OWNER.stockChest),
                oc.getInt("trusted-players-slot", DEFAULT_OWNER.trustedPlayers),
                oc.getInt("analytics-slot", DEFAULT_OWNER.analytics),
                oc.getInt("shop-info-slot", DEFAULT_OWNER.shopInfo)
        );
    }

    private static boolean validateTrading(ShopGuiLayout g) {
        if (!inChest(g.priceSlot) || !inChest(g.buySlot) || !inChest(g.createShopSlot)) {
            return false;
        }
        if (!inChest(g.changeShopSaveSlot) || !inChest(g.changeShopDeleteSlot) || !inChest(g.changeShopBackSlot)) {
            return false;
        }
        if (g.productSlots.size() != PRODUCT_COUNT) {
            return false;
        }
        Set<Integer> seen = new LinkedHashSet<>();
        for (int s : g.productSlots) {
            if (!inChest(s) || !seen.add(s)) {
                return false;
            }
        }
        if (seen.contains(g.priceSlot)) {
            return false;
        }
        if (seen.contains(g.buySlot) || g.priceSlot == g.buySlot) {
            return false;
        }
        if (seen.contains(g.createShopSlot) || g.priceSlot == g.createShopSlot) {
            return false;
        }
        if (g.changeShopSaveSlot == g.priceSlot || seen.contains(g.changeShopSaveSlot)
                || g.changeShopSaveSlot == g.buySlot || g.changeShopSaveSlot == g.createShopSlot) {
            return false;
        }
        if (g.changeShopDeleteSlot == g.priceSlot || seen.contains(g.changeShopDeleteSlot)
                || g.changeShopDeleteSlot == g.buySlot || g.changeShopDeleteSlot == g.createShopSlot) {
            return false;
        }
        if (g.changeShopBackSlot == g.priceSlot || seen.contains(g.changeShopBackSlot)
                || g.changeShopBackSlot == g.buySlot || g.changeShopBackSlot == g.createShopSlot) {
            return false;
        }
        if (g.changeShopSaveSlot == g.changeShopDeleteSlot
                || g.changeShopSaveSlot == g.changeShopBackSlot
                || g.changeShopDeleteSlot == g.changeShopBackSlot) {
            return false;
        }
        return true;
    }

    private static boolean validateOwnerDistinct(OwnerSlots o) {
        Set<Integer> set = new LinkedHashSet<>();
        for (int s : o.allSlots()) {
            if (!inChest(s) || !set.add(s)) {
                return false;
            }
        }
        return set.size() == 9;
    }

    private static boolean inChest(int slot) {
        return slot >= 0 && slot < CHEST_SIZE;
    }

    public int priceSlot() {
        return priceSlot;
    }

    public int buySlot() {
        return buySlot;
    }

    public int createShopSlot() {
        return createShopSlot;
    }

    public List<Integer> productSlots() {
        return productSlots;
    }

    public int changeShopSaveSlot() {
        return changeShopSaveSlot;
    }

    public int changeShopDeleteSlot() {
        return changeShopDeleteSlot;
    }

    public int changeShopBackSlot() {
        return changeShopBackSlot;
    }

    public List<Integer> editableSlots() {
        return editableSlots;
    }

    public int ownerCollectSlot() {
        return owner.collect;
    }

    public int ownerChangeListingSlot() {
        return owner.changeListing;
    }

    public int ownerCustomizeSlot() {
        return owner.customize;
    }

    public int ownerSettingsSlot() {
        return owner.settings;
    }

    public int ownerConnectStockpileSlot() {
        return owner.connectStockpile;
    }

    public int ownerStockChestSlot() {
        return owner.stockChest;
    }

    public int ownerTrustedPlayersSlot() {
        return owner.trustedPlayers;
    }

    public int ownerAnalyticsSlot() {
        return owner.analytics;
    }

    public int ownerShopInfoSlot() {
        return owner.shopInfo;
    }

    private record OwnerSlots(
            int collect,
            int changeListing,
            int customize,
            int settings,
            int connectStockpile,
            int stockChest,
            int trustedPlayers,
            int analytics,
            int shopInfo
    ) {
        int[] allSlots() {
            return new int[]{
                    collect, changeListing, customize, settings, connectStockpile,
                    stockChest, trustedPlayers, analytics, shopInfo
            };
        }
    }
}
