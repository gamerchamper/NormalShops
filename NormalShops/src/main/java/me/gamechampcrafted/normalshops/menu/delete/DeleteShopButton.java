package me.gamechampcrafted.normalshops.menu.delete;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.menu.ShopButton;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class DeleteShopButton extends ShopButton {

    public DeleteShopButton(int slot, ItemShop shop) {
        super(slot, shop);
    }

    private final ItemStack item =
            createItem(Message.BUTTON_DELETE_SHOP, Material.BARRIER, false);

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemShop shop = getShop();
        if (shop.isOwner(player)) {
            player.closeInventory();
            Message.DELETE_SHOP.send(player);
            CoreProtectLogger.logShopDelete(player, shop.getLocation());
            shop.delete(player);
            return;
        }

        if (AdminGuiSecurity.denyUnlessAdminTools(player, "delete another player's shop (admin GUI)")) {
            return;
        }

        player.closeInventory();
        CoreProtectLogger.logShopDelete(player, shop.getLocation());
        shop.delete(player);
        Message.SHOP_BREAK_OPERATOR.parameterizer()
                .put("owner", shop.getOwnerName())
                .send(player);
    }
}
