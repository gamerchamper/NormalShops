package me.gamechampcrafted.normalshops.menu.create;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.menu.ChangeShopButton;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CreateShopButton extends ChangeShopButton {

    private final Location location;

    public CreateShopButton(int slot, Location location) {
        super(slot, null);
        this.location = location;
    }

    private final ItemStack item = createItem(
            Message.BUTTON_CREATE_SHOP, Material.ANVIL, false
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

        player.closeInventory();

        if (Permission.CREATE.lacksAndNotify(player)) {
            return;
        }
        if (products == null || price == null) {
            Message.CREATE_INVALID.send(player);
            return;
        }
        // Fail if shop already exists at location
        if (ItemShop.get(location) != null) {
            Message.CREATE_SHOP_EXIST.send(player);
            return;
        }

        ItemShop shop = new ItemShop(location, player, price, products);
        NormalShops.getInstance().getShopManager().registerShop(shop);
        shop.saveData();

        CoreProtectLogger.logShopCreate(player, location, price, products.get(0));

        Message.CREATE_SHOP.sendSilently(player);
        player.playSound(location, Sound.BLOCK_ANVIL_DESTROY, SoundCategory.BLOCKS, .5f, 1f);
    }
}
