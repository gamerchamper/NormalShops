package me.gamechampcrafted.normalshops.shop.display;

import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.shop.DisplayHandler;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.*;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public abstract class ShopDisplay extends DisplayHandler implements ConfigurationSerializable {

    /**
     * “No stock” line — below {@link GlassDisplay} sale title ({@code TEXT_TRANSFORMATION} at {@code y=.7f})
     * so it stays visible when sale text is also shown (same slot would overlap).
     */
    private static final Transformation STOCK_STATUS_EMPTY_TRANSFORMATION = new Transformation(
            new Vector3f(0f, .52f, 0f),
            ZERO_AXIS_4F,
            new Vector3f(1f, 1f, 1f),
            ZERO_AXIS_4F
    );

    /** Under the sale title line when both sale text and “Low on stock” could show. */
    private static final Transformation STOCK_STATUS_LOW_TRANSFORMATION = new Transformation(
            new Vector3f(0f, .52f, 0f),
            ZERO_AXIS_4F,
            new Vector3f(1f, 1f, 1f),
            ZERO_AXIS_4F
    );

    private ItemShop shop;
    private Location location;
    private final ShopDisplayType type;
    @Nullable
    private UUID lowStockHintUUID;

    protected ShopDisplay(@Nullable ItemShop shop, ShopDisplayType type) {
        this.type = type;
        setShop(shop);
    }

    public final void update() {
        if (shop == null) {
            NormalShops.getInstance().getLogger().warning("Shop not set: Can't update display.");
            return;
        }
        if (getShop().isBedrockOutOfStockStorefront()) {
            applyOutOfStockVisual(true);
            refreshLowStockHint();
            return;
        }
        prepareDisplays();
        updateDisplay();
        refreshLowStockHint();
    }

    protected abstract void updateDisplay();

    /**
     * Ensures that all displays necessary in updateDisplay() exists and sets their UUID.
     */
    protected abstract void prepareDisplays();

    @NotNull
    public ItemShop getShop() {
        if (shop == null) throw new IllegalStateException("Shop not set!");
        return shop;
    }

    public void setShop(ItemShop shop) {
        this.shop = shop;
        if (shop != null) {
            this.location = shop.getLocation().add(.5, 1, .5);
        }
    }

    public ShopDisplayType getType() {
        return type;
    }

    /**
     * When {@code true}, temporarily removes hologram / display entities used for the stocked storefront.
     * When {@code false}, subclasses may no-op; callers typically invoke {@link #update()} next.
     */
    public void applyOutOfStockVisual(boolean outOfStock) {
    }

    /**
     * Same calls as {@link GlassDisplay#setSaleText}: {@link TextDisplay}, vertical billboard, legacy-colored text.
     * “No stock” when empty; “Low on stock” when still selling but the bottleneck line is ≤20% of one bundle.
     */
    public void refreshLowStockHint() {
        if (shop == null) {
            return;
        }
        if (shop.isAdminShop()) {
            clearLowStockHint();
            return;
        }
        boolean empty = !shop.hasStock();
        boolean low = shop.hasStock() && shop.getMinimumStockFulfillmentRatio() <= 0.2;
        if (!empty && !low) {
            clearLowStockHint();
            return;
        }
        UUID beforeHintUuid = lowStockHintUUID;
        lowStockHintUUID = createTextDisplayIfNotExists(lowStockHintUUID).getUniqueId();
        TextDisplay text = getTextDisplay(lowStockHintUUID);
        text.setTransformation(empty ? STOCK_STATUS_EMPTY_TRANSFORMATION : STOCK_STATUS_LOW_TRANSFORMATION);
        text.setText(Utils.colorize(empty ? "&c&lNo stock" : "&e&lLow on stock"));
        text.setBillboard(Display.Billboard.VERTICAL);
        // Spawn pipeline uses 0.1×DISPLAY_VIEW_RANGE (~0.2 mult); sale title uses same — bump so this line matches vanilla visibility.
        text.setViewRange(1.0f);
        if (!Objects.equals(beforeHintUuid, lowStockHintUUID)) {
            shop.saveData();
        }
    }

    protected void clearLowStockHint() {
        UUID removed = lowStockHintUUID;
        removeDisplayIfExists(lowStockHintUUID);
        lowStockHintUUID = null;
        if (removed != null && shop != null) {
            shop.saveData();
        }
    }

    /**
     * Persists the stock-status TextDisplay across plugin reloads so we can remove/update the same entity
     * instead of leaving orphans or stacking duplicates.
     */
    protected void readStockHintFromMap(Map<String, Object> map) {
        Object raw = map.get("stock-hint");
        if (raw == null) {
            lowStockHintUUID = null;
            return;
        }
        try {
            lowStockHintUUID = UUID.fromString(String.valueOf(raw));
        } catch (IllegalArgumentException e) {
            lowStockHintUUID = null;
        }
    }

    protected void putStockHintIntoMap(Map<String, Object> map) {
        if (lowStockHintUUID != null) {
            map.put("stock-hint", lowStockHintUUID.toString());
        }
    }

    @NotNull
    protected Location getLocation() {
        if (location == null) throw new IllegalStateException("Shop not set!");
        return location;
    }

    protected BlockDisplay createBlockDisplayIfNotExists(@Nullable UUID uuid) {
        return (BlockDisplay) createDisplayIfNotExists(uuid, EntityType.BLOCK_DISPLAY);
    }

    protected ItemDisplay createItemDisplayIfNotExists(@Nullable UUID uuid) {
        return (ItemDisplay) createDisplayIfNotExists(uuid, EntityType.ITEM_DISPLAY);
    }

    protected TextDisplay createTextDisplayIfNotExists(@Nullable UUID uuid) {
        return (TextDisplay) createDisplayIfNotExists(uuid, EntityType.TEXT_DISPLAY);
    }

    private Display createDisplayIfNotExists(@Nullable UUID uuid, EntityType type) {
        if (exists(uuid)) return (Display) Bukkit.getEntity(uuid);
        return spawnDisplay(getLocation(), type);
    }
}
