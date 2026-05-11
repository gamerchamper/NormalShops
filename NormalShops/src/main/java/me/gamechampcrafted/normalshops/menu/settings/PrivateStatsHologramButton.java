package me.gamechampcrafted.normalshops.menu.settings;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.MessageType;
import me.gamechampcrafted.normalshops.data.Setting;
import me.gamechampcrafted.normalshops.menu.GuiIcons;
import me.gamechampcrafted.normalshops.menu.ShopButton;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PrivateStatsHologramButton extends ShopButton {

    public PrivateStatsHologramButton(int slot, ItemShop shop) {
        super(slot, shop);
        List<String> lore = Message.BUTTON_STATS_HOLOGRAM_ENABLED.getLore();
        enabled = createItem(
                Message.BUTTON_STATS_HOLOGRAM_ENABLED.toString(), lore,
                GuiIcons.material("settings.stats-hologram-enabled", Material.ENDER_EYE), false
        );
        disabled = createItem(
                Message.BUTTON_STATS_HOLOGRAM_DISABLED.toString(), lore,
                GuiIcons.material("settings.stats-hologram-disabled", Material.GRAY_DYE), false
        );
    }

    @Override
    public void clickSound(Player player) {
    }

    private final ItemStack enabled;
    private final ItemStack disabled;

    @Override
    public ItemStack getItem() {
        if (getShop().isPrivateStatsHologramEnabled()) {
            return enabled;
        }
        return disabled;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemShop shop = getShop();
        shop.setPrivateStatsHologramEnabled(!shop.isPrivateStatsHologramEnabled());
        CoreProtectLogger.logShopEdit(player, shop.getLocation(),
                "toggled private stats hologram to " + shop.isPrivateStatsHologramEnabled());
        if (shop.isPrivateStatsHologramEnabled()) {
            MessageType.CONFIRM.playSound(player);
        } else {
            MessageType.WARN.playSound(player);
        }
        event.getInventory().setItem(getSlot(), getItem());
    }
}
