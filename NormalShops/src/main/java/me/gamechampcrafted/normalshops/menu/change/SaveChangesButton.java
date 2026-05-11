package me.gamechampcrafted.normalshops.menu.change;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.menu.ChangeShopButton;
import me.gamechampcrafted.normalshops.menu.GuiIcons;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class SaveChangesButton extends ChangeShopButton {

    public SaveChangesButton(int slot, ItemShop shop) {
        super(slot, shop);
    }

    private final ItemStack item = createItem(
            Message.BUTTON_SAVE_CHANGES,
            GuiIcons.material("trading.save-changes", Material.ANVIL), false
    );

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public void clickSound(Player player) {
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        //Shop logic
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        List<ItemStack> products = getProducts(event.getInventory());
        ItemStack price = getPrice(event.getInventory());

        if (products == null || price == null) {
            player.closeInventory();
            Message.CREATE_INVALID.send(player);
            return;
        }

        ItemShop shop = getShop();

        if (shop.getEarnings() > 0) {
            shop.collectEarnings(player, true);
        }

        shop.setPrice(price);
        shop.setProducts(products);
        shop.saveData();

        CoreProtectLogger.logShopEdit(player, shop.getLocation(), "price and products");

        player.closeInventory();
        Message.CHANGE_SHOP.sendSilently(player);
        player.playSound(shop.getLocation(), Sound.BLOCK_ANVIL_DESTROY, SoundCategory.BLOCKS, .5f, 1f);
    }
}
