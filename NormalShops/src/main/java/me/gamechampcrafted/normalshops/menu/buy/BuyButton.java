package me.gamechampcrafted.normalshops.menu.buy;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.MessageType;
import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.menu.ShopButton;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.shop.Transaction;
import me.gamechampcrafted.normalshops.utils.Parameterizer;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class BuyButton extends ShopButton {

    private boolean stocked;

    private final ItemStack buyButton;
    private final ItemStack outOfStockButton;

    public BuyButton(int slot, ItemShop shop) {
        super(slot, shop);
        stocked = shop.hasStock();
        buyButton = createItem(Message.BUTTON_BUY.toString(),
                Message.BUTTON_BUY.getParameterizedLore(new Parameterizer<>()
                        .put("player", shop.getOwnerName())
                        .put("price", Utils.formatItemWithAmount(shop.getPrice()))),
                Material.LIME_CONCRETE_POWDER, false);
        outOfStockButton =
                createItem(Message.BUTTON_OUT_OF_STOCK, Material.RED_CONCRETE_POWDER, false);
    }

    @Override
    public void clickSound(Player player) {
        if (!stocked) MessageType.FAIL.playSound(player);
    }

    @Override
    public ItemStack getItem() {
        return stocked ? buyButton : outOfStockButton;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemShop shop = getShop();
        if (!shop.hasStock()) {
            // Out of stock / refresh button
            stocked = false;
            event.getInventory().setItem(getSlot(), getItem());
            return;
        }
        //Buy Logic
        if (Permission.BUY.lacks(player)) {
            Message.NO_PERMISSION.send(player);
            player.closeInventory();
            return;
        }
        if (!new Transaction(player, shop).make()) {
            //Unsuccessful
            return;
        }
        //Reset button
        stocked = shop.hasStock();
        event.getInventory().setItem(getSlot(), getItem());
    }
}
