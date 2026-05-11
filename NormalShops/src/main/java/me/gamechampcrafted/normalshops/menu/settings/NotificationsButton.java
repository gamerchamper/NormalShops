package me.gamechampcrafted.normalshops.menu.settings;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.MessageType;
import me.gamechampcrafted.normalshops.menu.GuiIcons;
import me.gamechampcrafted.normalshops.menu.ShopButton;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class NotificationsButton extends ShopButton {

    public NotificationsButton(int slot, ItemShop shop) {
        super(slot, shop);
        List<String> lore = Message.BUTTON_NOTIFICATIONS_ENABLED.getLore();
        enabled = createItem(
                Message.BUTTON_NOTIFICATIONS_ENABLED.toString(), lore,
                GuiIcons.material("settings.notifications-enabled", Material.EMERALD), false
        );
        disabled = createItem(
                Message.BUTTON_NOTIFICATIONS_DISABLED.toString(), lore,
                GuiIcons.material("settings.notifications-disabled", Material.GRAY_DYE), false
        );
    }

    @Override
    public void clickSound(Player player) {
    }

    private final ItemStack enabled;
    private final ItemStack disabled;

    @Override
    public ItemStack getItem() {
        if (getShop().isNotificationsEnabled()) {
            return enabled;
        }
        return disabled;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemShop shop = getShop();
        shop.setNotificationsEnabled(!shop.isNotificationsEnabled());
        CoreProtectLogger.logShopEdit(player, shop.getLocation(), "toggled notifications to " + shop.isNotificationsEnabled());
        if (shop.isNotificationsEnabled()) {
            MessageType.CONFIRM.playSound(player);
        } else {
            MessageType.WARN.playSound(player);
        }
        // Update item
        event.getInventory().setItem(getSlot(), getItem());
    }
}
