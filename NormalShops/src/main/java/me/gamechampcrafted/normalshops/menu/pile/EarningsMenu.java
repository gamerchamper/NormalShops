package me.gamechampcrafted.normalshops.menu.pile;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.menu.ClickHandler;
import me.gamechampcrafted.normalshops.menu.MenuSlotRegistry;
import me.gamechampcrafted.normalshops.menu.PileMenu;
import me.gamechampcrafted.normalshops.shop.Pile;
import org.bukkit.entity.Player;

public class EarningsMenu extends PileMenu {

    public EarningsMenu(Player player, Pile pile) {
        super(player, pile, 27, new ClickHandler(), Message.MENU_EARNINGS);
    }

    @Override
    protected void setupButtons() {
        addButton(new ConnectShopButton(MenuSlotRegistry.slot("earnings-pile", "connect-shop", 12), getPile()));
        addButton(new PileCollectButton(MenuSlotRegistry.slot("earnings-pile", "collect", 14), getPile()));
    }
}
