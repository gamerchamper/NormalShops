package me.gamechampcrafted.normalshops.menu.settings;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.MessageType;
import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.menu.GuiIcons;
import me.gamechampcrafted.normalshops.menu.ShopButton;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class UnlimitedStockButton extends ShopButton {

    public UnlimitedStockButton(int slot, ItemShop shop) {
        super(slot, shop);
        List<String> lore = Message.BUTTON_UNLIMITED_ENABLED.getLore();
        enabled = createItem(
                Message.BUTTON_UNLIMITED_ENABLED.toString(), lore,
                GuiIcons.material("settings.unlimited-enabled", Material.TOTEM_OF_UNDYING), true
        );
        disabled = createItem(
                Message.BUTTON_UNLIMITED_DISABLED.toString(), lore,
                GuiIcons.material("settings.unlimited-disabled", Material.TOTEM_OF_UNDYING), false
        );
    }

    @Override
    public void clickSound(Player player) {
    }

    private final ItemStack enabled;
    private final ItemStack disabled;

    @Override
    public ItemStack getItem() {
        if (getShop().isAdminShop()) {
            return enabled;
        }
        return disabled;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        if (Permission.UNLIMITED_STOCK.lacksAndNotify(player)) {
            player.closeInventory();
            return;
        }

        ItemShop shop = getShop();
        shop.setAdminShop(!shop.isAdminShop());
        CoreProtectLogger.logShopEdit(player, shop.getLocation(), "toggled admin shop to " + shop.isAdminShop());
        if (shop.isAdminShop()) {
            MessageType.CONFIRM.playSound(player);
        } else {
            MessageType.WARN.playSound(player);
        }
        // Update item
        event.getInventory().setItem(getSlot(), getItem());
    }
}
