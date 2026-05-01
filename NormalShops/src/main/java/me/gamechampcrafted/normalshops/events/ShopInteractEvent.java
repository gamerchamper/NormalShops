package me.gamechampcrafted.normalshops.events;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.Debug;
import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.data.Setting;
import me.gamechampcrafted.normalshops.menu.buy.BuyMenu;
import me.gamechampcrafted.normalshops.menu.delete.DeleteShopMenu;
import me.gamechampcrafted.normalshops.menu.edit.EditShopMenu;
import me.gamechampcrafted.normalshops.menu.edit.ShopAccessChoiceMenu;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;

public class ShopInteractEvent extends InteractEvent {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        handleInteract(event);
    }

    @Override
    protected boolean onInteract(Player player, Block block) {
        ItemShop shop = ItemShop.get(block.getLocation());
        if (shop == null) return false;
        boolean sneaking = player.isSneaking();
        // Admin/Delete Menu (always takes priority on shift-right-click)
        if (Permission.DELETE.has(player) && sneaking) {
            new DeleteShopMenu(player, shop).open();
            CoreProtectLogger.logShopEdit(player, shop.getLocation(), "opened delete menu");
            return true;
        }
        // Owner/trusted can choose between editor and trade view
        if (shop.isOwner(player) || shop.isTrusted(player)) {
            new ShopAccessChoiceMenu(player, shop).open();
            player.playSound(player, Sound.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, .5f, .8f);
            CoreProtectLogger.logShopEdit(player, shop.getLocation(), "opened owner/trusted access menu");
            return true;
        }
        //Buy Menu
        if (Permission.BUY.lacksAndNotify(player)) {
            return true;
        }
        shop.incrementImpressions();
        shop.saveData();
        if (Setting.VILLAGER_TRADING_MENU.isEnabled()) {
            NormalShops.getInstance().getTradingMenuManager().openTradingMenu(player, shop);
        } else {
            new BuyMenu(player, shop).open();
            player.playSound(player, Sound.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, .5f, .8f);
        }
        CoreProtectLogger.logShopEdit(player, shop.getLocation(), "opened buy menu");
        return true;
    }
}
