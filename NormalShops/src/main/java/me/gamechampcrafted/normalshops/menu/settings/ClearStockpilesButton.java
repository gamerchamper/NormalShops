package me.gamechampcrafted.normalshops.menu.settings;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.menu.GuiIcons;
import me.gamechampcrafted.normalshops.menu.ShopButton;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class ClearStockpilesButton extends ShopButton {

    public ClearStockpilesButton(int slot, ItemShop shop) {
        super(slot, shop);
    }

    @Override
    public void clickSound(Player player) {
    }

    private final ItemStack item = createItem(
            Message.BUTTON_CLEAR_STOCKPILES,
            GuiIcons.material("settings.clear-stockpiles", Material.TNT_MINECART), false
    );

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemShop shop = getShop();
        new ArrayList<>(shop.getStockpileSet())
                .forEach(location -> {
                    shop.removeStockpile(location);
                    CoreProtectLogger.logStockpileRemove(player, shop.getLocation(), location);
                });
        Message.CLEAR_STOCKPILES.send(player);
        player.closeInventory();
    }
}
