package me.gamechampcrafted.normalshops.shop.display;

import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;

public class GlassDisplay extends ShopDisplay {

    private static final int MAX_SALE_TEXT_LENGTH = 26;

    public static final Set<Material> GLASS_OPTIONS = new HashSet<>(Arrays.asList(Material.GLASS, Material.TINTED_GLASS, Material.LIGHT_GRAY_STAINED_GLASS, Material.BLACK_STAINED_GLASS,
            Material.BLUE_STAINED_GLASS, Material.CYAN_STAINED_GLASS, Material.PURPLE_STAINED_GLASS, Material.RED_STAINED_GLASS, Material.ORANGE_STAINED_GLASS,
            Material.MAGENTA_STAINED_GLASS, Material.LIME_STAINED_GLASS, Material.GREEN_STAINED_GLASS, Material.WHITE_STAINED_GLASS,
            Material.GRAY_STAINED_GLASS, Material.BROWN_STAINED_GLASS, Material.YELLOW_STAINED_GLASS, Material.LIGHT_BLUE_STAINED_GLASS, Material.PINK_STAINED_GLASS));

    private static final Material DEFAULT_GLASS = Material.GLASS;
    private static final Material DEFAULT_BASE = Material.STRIPPED_OAK_LOG;

    private static final Transformation TEXT_TRANSFORMATION = new Transformation(
            new Vector3f(0f, .7f, 0f),
            ZERO_AXIS_4F,
            new Vector3f(1f, 1f, 1f),
            ZERO_AXIS_4F
    );

    private UUID glassUUID;
    private UUID baseUUID;
    private UUID itemUUID;
    private UUID textUUID;

    /** Plain sale line (persisted); entities are torn down during out-of-stock visuals and rebuilt from this. */
    @Nullable
    private String saleTextMessage;

    /** Custom glass block for the display shell; {@code null} means {@link #DEFAULT_GLASS}. */
    @Nullable
    private Material persistedGlassMaterial;

    /** Custom base/plinth block; {@code null} means {@link #DEFAULT_BASE}. */
    @Nullable
    private Material persistedBaseMaterial;

    public GlassDisplay(ItemShop shop) throws IllegalArgumentException {
        super(shop, ShopDisplayType.GLASS);
        Location location = shop.getLocation();

        Material blockAbove = location.getBlock().getRelative(BlockFace.UP).getType();
        if (blockAbove != Material.AIR && blockAbove != Material.LIGHT) {
            throw new IllegalArgumentException("Block above must be air or light for glass displays!");
        }
    }

    private GlassDisplay(
            UUID glassUUID,
            UUID baseUUID,
            UUID itemUUID,
            UUID textUUID,
            @Nullable String saleTextMessage,
            @Nullable Material persistedGlassMaterial,
            @Nullable Material persistedBaseMaterial
    ) {
        super(null, ShopDisplayType.GLASS);
        this.glassUUID = glassUUID;
        this.baseUUID = baseUUID;
        this.itemUUID = itemUUID;
        this.textUUID = textUUID;
        this.saleTextMessage = saleTextMessage;
        this.persistedGlassMaterial = persistedGlassMaterial;
        this.persistedBaseMaterial = persistedBaseMaterial;
    }

    @Override
    protected void updateDisplay() {
        boolean isChest = getShop().getLocation().getBlock().getType() == Material.CHEST;

        Material glassMat = effectiveGlassMaterial();
        Material baseMat = effectiveBaseMaterial();

        BlockDisplay glass = getBlockDisplay(glassUUID);
        glass.setTransformation(getGlassTransformation(isChest));
        glass.setBlock(glassMat.createBlockData());

        BlockDisplay base = getBlockDisplay(baseUUID);
        base.setTransformation(getGlassTransformation(isChest));
        base.setBlock(baseMat.createBlockData());
        base.setTransformation(getBaseTransformation(isChest));

        ItemDisplay item = getItemDisplay(itemUUID);
        ItemStack product = getShop().getProducts().get(0).clone();
        item.setTransformation(getItemTransformation(product.getType(), isChest));

        item.setItemStack(product);
        item.setBillboard(Display.Billboard.VERTICAL);

        applySaleTextEntity();
    }

    @Override
    protected void prepareDisplays() {
        glassUUID = createBlockDisplayIfNotExists(glassUUID).getUniqueId();
        baseUUID = createBlockDisplayIfNotExists(baseUUID).getUniqueId();
        itemUUID = createItemDisplayIfNotExists(itemUUID).getUniqueId();
    }

    @Override
    public void applyOutOfStockVisual(boolean outOfStock) {
        if (!outOfStock) {
            // True path removed every display UUID and entities; callers often invoke update() next, but if anything
            // short-circuits we must rebuild shell + item + sale hologram from persisted saleTextMessage here.
            if (getShop() != null && !getShop().isBedrockOutOfStockStorefront()) {
                prepareDisplays();
                updateDisplay();
            }
            return;
        }
        boolean migrated = false;
        if (persistedGlassMaterial == null && glassUUID != null && exists(glassUUID)) {
            Material m = getMaterial(glassUUID);
            if (m != DEFAULT_GLASS) {
                persistedGlassMaterial = m;
                migrated = true;
            }
        }
        if (persistedBaseMaterial == null && baseUUID != null && exists(baseUUID)) {
            Material m = getMaterial(baseUUID);
            if (m != DEFAULT_BASE) {
                persistedBaseMaterial = m;
                migrated = true;
            }
        }
        if (migrated) {
            getShop().saveData();
        }
        removeDisplayIfExists(itemUUID);
        removeDisplayIfExists(textUUID);
        removeDisplayIfExists(glassUUID);
        removeDisplayIfExists(baseUUID);
        itemUUID = null;
        textUUID = null;
        glassUUID = null;
        baseUUID = null;
    }

    public boolean hasSaleText() {
        return saleTextMessage != null && !saleTextMessage.isEmpty();
    }

    public void setSaleText(@Nullable String saleText) {
        if (saleText == null) {
            saleTextMessage = null;
            removeDisplayIfExists(textUUID);
            textUUID = null;
            getShop().saveData();
            return;
        }
        saleTextMessage = saleText;
        if (!shouldDeferSaleTextWhileOutOfStock()) {
            applySaleTextEntity();
        } else {
            removeDisplayIfExists(textUUID);
            textUUID = null;
        }
        getShop().saveData();
    }

    /** While the shop is in bedrock/out-of-stock storefront mode, keep the message but do not spawn the hologram. */
    private boolean shouldDeferSaleTextWhileOutOfStock() {
        return getShop().isBedrockOutOfStockStorefront();
    }

    /** Recreates the sale hologram from {@link #saleTextMessage} (used after restock or load). */
    private void applySaleTextEntity() {
        if (saleTextMessage == null || saleTextMessage.isEmpty()) {
            return;
        }
        if (shouldDeferSaleTextWhileOutOfStock()) {
            removeDisplayIfExists(textUUID);
            textUUID = null;
            return;
        }
        textUUID = createTextDisplayIfNotExists(textUUID).getUniqueId();
        TextDisplay text = getTextDisplay(textUUID);
        text.setTransformation(TEXT_TRANSFORMATION);
        text.setText(Utils.rainbow(saleTextMessage, true));
        text.setBillboard(Display.Billboard.VERTICAL);
        text.setViewRange(1.0f);
    }

    @Override
    public void clear() {
        clearLowStockHint();
        saleTextMessage = null;
        persistedGlassMaterial = null;
        persistedBaseMaterial = null;
        Block above = getShop().getLocation().getBlock().getRelative(BlockFace.UP);
        if (above.getType() == Material.LIGHT) {
            above.setType(Material.AIR);
        }
        removeDisplayIfExists(glassUUID);
        removeDisplayIfExists(baseUUID);
        removeDisplayIfExists(itemUUID);
        removeDisplayIfExists(textUUID);
    }

    /**
     * @param material valid glass variant
     * @return if material was valid
     */
    public boolean setGlassMaterial(Material material) {
        if (!GLASS_OPTIONS.contains(material)) return false;
        persistedGlassMaterial = material;
        setMaterial(glassUUID, material);
        getShop().saveData();
        return true;
    }

    public void setBaseMaterial(Material material) {
        persistedBaseMaterial = material;
        setMaterial(baseUUID, material);
        getShop().saveData();
    }

    public Material getGlassMaterial() {
        if (persistedGlassMaterial != null) {
            return persistedGlassMaterial;
        }
        if (!exists(glassUUID)) return DEFAULT_GLASS;
        return getMaterial(glassUUID);
    }

    public Material getBaseMaterial() {
        if (persistedBaseMaterial != null) {
            return persistedBaseMaterial;
        }
        if (!exists(baseUUID)) return DEFAULT_BASE;
        return getMaterial(baseUUID);
    }

    private Material effectiveGlassMaterial() {
        return persistedGlassMaterial != null ? persistedGlassMaterial : DEFAULT_GLASS;
    }

    private Material effectiveBaseMaterial() {
        return persistedBaseMaterial != null ? persistedBaseMaterial : DEFAULT_BASE;
    }

    private Material getMaterial(UUID uuid) {
        return getBlockDisplay(uuid).getBlock().getMaterial();
    }

    private void setMaterial(UUID uuid, Material material) {
        if (!exists(uuid)) return;
        getBlockDisplay(uuid).setBlock(material.createBlockData());
    }

    private Transformation getGlassTransformation(boolean isChest) {
        return isChest
                ? new Transformation(
                new Vector3f(-.3f, -0.05f, -.3f), ZERO_AXIS_4F,
                new Vector3f(0.6f, 0.6f, 0.6f), ZERO_AXIS_4F)
                : new Transformation(
                new Vector3f(-.3f, .075f, -.3f), ZERO_AXIS_4F,
                new Vector3f(0.6f, 0.6f, 0.6f), ZERO_AXIS_4F);
    }

    private Transformation getBaseTransformation(boolean isChest) {
        return isChest
                ? new Transformation(
                new Vector3f(-.35f, -.15f, -.35f), ZERO_AXIS_4F,
                new Vector3f(0.7f, 0.1f, 0.7f), ZERO_AXIS_4F)
                : new Transformation(
                new Vector3f(-.4f, -.025f, -.4f), ZERO_AXIS_4F,
                new Vector3f(.8f, .1f, .8f), ZERO_AXIS_4F);
    }

    private Vector3f getItemScale(Material material) {
        if (material == Material.WITHER_SKELETON_SKULL) {
            return new Vector3f(.5f, .5f, .5f);
        }
        if (material.isBlock()) {
            return new Vector3f(.3f, .3f, .3f);
        }
        return new Vector3f(.4f, .4f, .4f);
    }

    private Transformation getItemTransformation(Material material, boolean isChest) {
        Vector3f scale = getItemScale(material);
        return isChest
                ? new Transformation(new Vector3f(0f, .25f, 0f), ZERO_AXIS_4F, scale, ZERO_AXIS_4F)
                : new Transformation(new Vector3f(0f, .375f, 0f), ZERO_AXIS_4F, scale, ZERO_AXIS_4F);
    }

    public static boolean isValidSaleText(String saleText) {
        return saleText.length() <= MAX_SALE_TEXT_LENGTH;
    }

    public static GlassDisplay deserialize(Map<String, Object> map) {
        String glass = (String) map.get("glass");
        String base = (String) map.get("base");
        String item = (String) map.get("item");
        String text = (String) map.get("text");
        String saleTextPersisted = (String) map.get("sale-text");
        UUID glassUUID = glass != null ? UUID.fromString(glass) : null;
        UUID baseUUID = base != null ? UUID.fromString(base) : null;
        UUID itemUUID = item != null ? UUID.fromString(item) : null;
        UUID textUUID = text != null ? UUID.fromString(text) : null;
        GlassDisplay display = new GlassDisplay(
                glassUUID,
                baseUUID,
                itemUUID,
                textUUID,
                saleTextPersisted,
                parsePersistedMaterial((String) map.get("glass-material")),
                parsePersistedMaterial((String) map.get("base-material"))
        );
        display.readStockHintFromMap(map);
        return display;
    }

    private static @Nullable Material parsePersistedMaterial(@Nullable String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        try {
            return Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        if (glassUUID != null) map.put("glass", glassUUID.toString());
        if (baseUUID != null) map.put("base", baseUUID.toString());
        if (itemUUID != null) map.put("item", itemUUID.toString());
        if (textUUID != null) map.put("text", textUUID.toString());
        if (saleTextMessage != null) map.put("sale-text", saleTextMessage);
        if (persistedGlassMaterial != null) map.put("glass-material", persistedGlassMaterial.name());
        if (persistedBaseMaterial != null) map.put("base-material", persistedBaseMaterial.name());
        putStockHintIntoMap(map);
        return map;
    }
}
