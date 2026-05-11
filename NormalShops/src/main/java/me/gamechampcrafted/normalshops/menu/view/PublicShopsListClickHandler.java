package me.gamechampcrafted.normalshops.menu.view;

import me.gamechampcrafted.normalshops.menu.MenuSlotRegistry;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * /shops browse GUI: no inventory manipulation except clicking shop rows (teleport) and pagination.
 * Player inventory clicks/drags are always cancelled; top filler glass is non-interactive by slot whitelist.
 */
public final class PublicShopsListClickHandler extends ShopsListClickHandler {

    private static final int PAGE_SIZE = 36;
    private static final Set<Integer> INTERACTIVE_TOP_SLOTS = buildInteractiveTopSlots();

    private static Set<Integer> buildInteractiveTopSlots() {
        List<Integer> defaults = IntStream.range(0, PAGE_SIZE).boxed().collect(Collectors.toList());
        Set<Integer> s = new HashSet<>(MenuSlotRegistry.slots("public-shops", "shop-entry", defaults));
        s.add(MenuSlotRegistry.slot("public-shops", "prev-page", 36));
        s.add(MenuSlotRegistry.slot("public-shops", "pagination-info", 40));
        s.add(MenuSlotRegistry.slot("public-shops", "next-page", 44));
        return Set.copyOf(s);
    }

    @Override
    public boolean isValidClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) {
            return false;
        }
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return false;
        }
        if (!INTERACTIVE_TOP_SLOTS.contains(event.getRawSlot())) {
            return false;
        }
        ClickType ct = event.getClick();
        if (ct == ClickType.UNKNOWN
                || ct == ClickType.NUMBER_KEY
                || ct == ClickType.DROP
                || ct == ClickType.CONTROL_DROP
                || ct == ClickType.SWAP_OFFHAND
                || ct == ClickType.WINDOW_BORDER_LEFT
                || ct == ClickType.WINDOW_BORDER_RIGHT
                || ct.isCreativeAction()) {
            return false;
        }
        return super.isValidClick(event);
    }

    @Override
    public void handleDrag(InventoryDragEvent event) {
        event.setCancelled(true);
    }
}
