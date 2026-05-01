package me.gamechampcrafted.normalshops.events;

import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.Permission;
import me.gamechampcrafted.normalshops.menu.create.CreateShopMenu;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

public class ShopCreateEvent implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        Location location = block.getLocation();

        if (player.isSneaking()) {
            return; //Ignore if sneaking
        }
        if (ItemShop.get(location) != null) {
            return; //Ignore if shop
        }
        if (NormalShops.getInstance().getShopManager().getStockpileOwner(location) != null) {
            return; //Ignore if stockpile
        }
        Material hand = player.getInventory().getItemInMainHand().getType();
        if (!Tag.ALL_SIGNS.isTagged(hand)) {
            return;
        }
        if (!isValidShopHolderAndWarn(block, player)) {
            return;
        }
        //Create Shop
        event.setCancelled(true);
        if (NormalShops.getInstance().getShopManager().isShopLimitReachedForAndWarn(player)) return;
        if (Permission.CREATE.lacksAndNotify(player)) {
            return;
        }
        new CreateShopMenu(player, location).open();
        player.playSound(player, Sound.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, .5f, .8f);
    }

    private boolean isValidShopHolderAndWarn(Block block, Player player) {
        Material mat = block.getType();
        if (mat != Material.CHEST && mat != Material.BARREL) {
            return false;
        }
        Container container = (Container) block.getState();
        InventoryHolder holder = container.getInventory().getHolder();
        if (holder instanceof DoubleChest) {
            Message.CREATE_DOUBLE_CHEST.send(player);
            return false;
        }
        if (!container.getInventory().isEmpty()) {
            Message.CREATE_NOT_EMPTY.send(player);
            return false;
        }
        return holder instanceof Chest || holder instanceof Barrel;
    }
}
