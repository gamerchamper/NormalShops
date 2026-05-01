package me.gamechampcrafted.normalshops.menu.customize;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.menu.ShopButton;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class CustomNameButton extends ShopButton {

    private boolean hasCustomName() {
        return getShop().getCustomName() != null;
    }

    public CustomNameButton(int slot, ItemShop shop) {
        super(slot, shop);
    }

    private final ItemStack addName =
            createItem(Message.BUTTON_ADD_CUSTOM_NAME, Material.NAME_TAG, false);
    private final ItemStack removeName =
            createItem(Message.BUTTON_REMOVE_CUSTOM_NAME, Material.NAME_TAG, true);

    @Override
    public ItemStack getItem() {
        return hasCustomName() ? removeName : addName;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemShop shop = getShop();

        if (hasCustomName()) {
            shop.setCustomName(null);
            CoreProtectLogger.logShopEdit(player, shop.getLocation(), "removed custom name");
            event.getInventory().setItem(getSlot(), getItem());
            Message.CUSTOM_NAME_REMOVE.send(player);
            return;
        }

        player.closeInventory();
        Message.CUSTOM_NAME_TYPE.send(player);
        NormalShops.getInstance().getChatInputListener().addChatCallback(player, (String message) -> {
            if (shop != null && !shop.isDeleted()) {
                if (!ItemShop.isValidCustomName(message)) {
                    Message.CUSTOM_NAME_INVALID.send(player);
                    return;
                }
                shop.setCustomName(message);
                CoreProtectLogger.logShopEdit(player, shop.getLocation(), "set custom name to " + message);
                Message.CUSTOM_NAME_SET.parameterizer()
                        .setColorizeParameters(false)
                        .put("name", message)
                        .send(player);
            }
        }, 200, Message.CUSTOM_NAME_CANCEL);
    }
}
