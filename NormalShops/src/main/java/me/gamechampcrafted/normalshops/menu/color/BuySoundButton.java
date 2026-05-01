package me.gamechampcrafted.normalshops.menu.color;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.menu.ShopButton;
import me.gamechampcrafted.normalshops.shop.BuySound;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class BuySoundButton extends ShopButton {

    private final BuySound buySound;

    public BuySoundButton(int slot, ItemShop shop, BuySound buySound) {
        super(slot, shop);
        this.buySound = buySound;
        Material mat = buySound.getIcon();
        boolean selected = mat == shop.getBuySound().getIcon();
        String name = selected
                ? Message.BUTTON_SOUND_SELECTED.toString()
                : Message.BUTTON_SOUND.toString();
        this.item = createItem(
                name, Message.BUTTON_SOUND.getLore(), mat, selected
        );
    }

    private final ItemStack item;

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public void clickSound(Player player) {
        buySound.playSound(player);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemShop shop = getShop();
        shop.setBuySound(buySound);
        CoreProtectLogger.logShopEdit(player, shop.getLocation(), "changed buy sound to " + buySound.name());
        new ColorMenu(player, shop).open();
    }
}
