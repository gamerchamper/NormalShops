package me.gamechampcrafted.normalshops.menu;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.shop.Pile;
import org.bukkit.entity.Player;

public abstract class PileMenu extends Menu {

    private final Pile pile;

    public PileMenu(Player player, Pile pile, int size, ClickHandler clickHandler, Message title) {
        super(player, pile.getLocation(), size, clickHandler, MenuColor.PINK, title);
        this.pile = pile;
    }

    protected Pile getPile() {
        return pile;
    }
}
