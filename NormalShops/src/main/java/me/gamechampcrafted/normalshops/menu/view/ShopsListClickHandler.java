package me.gamechampcrafted.normalshops.menu.view;

import me.gamechampcrafted.normalshops.menu.ClickHandler;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Read-only /shops (and similar) lists: shift-clicks must still trigger buttons, but every interaction
 * that could move display icons must be cancelled. {@link ClickHandler}'s default shift rule skips button
 * handlers for top-inventory shift-clicks; double-click collect can pull matching types from browse slots
 * because {@link Menu#editableSlots()} does not include those slots.
 */
public class ShopsListClickHandler extends ClickHandler {

    @Override
    protected boolean isIllegalShiftClick(InventoryClickEvent event) {
        if (!event.getClick().isShiftClick()) {
            return false;
        }
        if (event.getClickedInventory() != null
                && event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return false;
        }
        return super.isIllegalShiftClick(event);
    }

    @Override
    protected boolean isIllegalDoubleClick(InventoryClickEvent event) {
        if (event.getClick() == ClickType.DOUBLE_CLICK
                && event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            return true;
        }
        return super.isIllegalDoubleClick(event);
    }
}
