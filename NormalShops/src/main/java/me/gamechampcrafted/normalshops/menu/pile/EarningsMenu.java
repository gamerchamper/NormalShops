package me.gamechampcrafted.normalshops.menu.pile;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.menu.ClickHandler;
import me.gamechampcrafted.normalshops.menu.PileMenu;
import me.gamechampcrafted.normalshops.shop.Pile;
import org.bukkit.entity.Player;

public class EarningsMenu extends PileMenu {

    public EarningsMenu(Player player, Pile pile) {
        super(player, pile, 27, new ClickHandler(), Message.MENU_EARNINGS);
    }

    @Override
    protected void setupButtons() {
        addButton(new ConnectShopButton(12, getPile()));
        addButton(new PileCollectButton(14, getPile()));
    }
}
