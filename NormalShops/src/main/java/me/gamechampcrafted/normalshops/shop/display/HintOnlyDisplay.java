package me.gamechampcrafted.normalshops.shop.display;

import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Placeholder display used only so {@link ShopDisplay#refreshLowStockHint()} / backup serialization can run
 * when a shop has no glass/frame storefront yet (new shops that start empty).
 */
public class HintOnlyDisplay extends ShopDisplay {

    public HintOnlyDisplay(ItemShop shop) {
        super(shop, ShopDisplayType.NONE);
    }

    private HintOnlyDisplay() {
        super(null, ShopDisplayType.NONE);
    }

    @Override
    protected void updateDisplay() {
    }

    @Override
    protected void prepareDisplays() {
    }

    @Override
    public void clear() {
        clearLowStockHint();
    }

    public static HintOnlyDisplay deserialize(Map<String, Object> map) {
        HintOnlyDisplay display = new HintOnlyDisplay();
        display.readStockHintFromMap(map);
        return display;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        putStockHintIntoMap(map);
        return map;
    }
}
