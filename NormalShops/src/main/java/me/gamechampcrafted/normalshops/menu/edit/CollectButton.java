package me.gamechampcrafted.normalshops.menu.edit;

import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.menu.ShopButton;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.utils.Parameterizer;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CollectButton extends ShopButton {

    public CollectButton(int slot, ItemShop shop) {
        super(slot, shop);
        this.item = createItem(
                Message.BUTTON_COLLECT.toString(),
                getEarningsLore(), Material.DIAMOND, false);
    }

    private final ItemStack item;

    @Override
    public ItemStack getItem() {
        return item;
    }

    public void clickSound(Player player) {
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        NormalShops.getInstance().getEarningsChestManager().open(player, getShop());
    }

    private List<String> getEarningsLore() {
        ItemShop shop = getShop();
        int total = shop.getEarnings() * shop.getPrice().getAmount();

        return Message.BUTTON_COLLECT.getParameterizedLore(new Parameterizer<>()
                .put("earnings", total > 0
                        ? total + " " + Utils.getFormattedName(shop.getPrice())
                        : Message.BUTTON_NONE));
    }
}
