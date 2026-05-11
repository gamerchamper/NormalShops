package me.gamechampcrafted.normalshops.menu.pile;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.menu.GuiIcons;
import me.gamechampcrafted.normalshops.menu.PileButton;
import me.gamechampcrafted.normalshops.shop.Pile;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class PileCollectButton extends PileButton {

    public PileCollectButton(int slot, Pile pile) {
        super(slot, pile);
    }

    @Override
    public ItemStack getItem() {
        return createItem(Message.BUTTON_PILE_COLLECT,
                GuiIcons.material("earnings-pile.collect", Material.DIAMOND), false);
    }

    public void clickSound(Player player) {
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        Pile pile = getPile();
        if (pile != null) {
            pile.collectEarnings(player);
        }

        player.closeInventory();
    }
}
