package me.gamechampcrafted.normalshops.menu;

import me.gamechampcrafted.normalshops.shop.Pile;

public abstract class PileButton extends Button {

    private final Pile pile;

    public PileButton(int slot, Pile pile) {
        super(slot);
        this.pile = pile;
    }

    protected Pile getPile() {
        return pile;
    }

}
