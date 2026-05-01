package me.gamechampcrafted.normalshops.menu.edit;

import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.menu.ShopButton;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class StockChestButton extends ShopButton {

    public StockChestButton(int slot, ItemShop shop) {
        super(slot, shop);
    }

    @Override
    public void clickSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, .5f, 1f);
    }

    private final ItemStack item =
            createItem(Message.BUTTON_STOCK_CHEST, Material.CHEST, false);

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        NormalShops.getInstance().getStockChestManager().open(player, getShop());
    }
}
