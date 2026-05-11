package me.gamechampcrafted.normalshops.menu.change;

import me.gamechampcrafted.normalshops.menu.Button;
import me.gamechampcrafted.normalshops.shop.PhysicalItemProxy;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ItemButton extends Button {

    private final ItemStack item;

    public ItemButton(int slot, Material mat) {
        super(slot);
        if (mat == null || mat.isAir()) {
            this.item = new ItemStack(Material.AIR);
        } else {
            this.item = new ItemStack(mat, 1);
        }
    }

    @Override
    public void clickSound(Player player) {
    }

    public ItemButton(int slot, ItemStack item) {
        super(slot);
        if (item == null || item.getType().isAir()) {
            this.item = new ItemStack(Material.AIR);
        } else {
            ItemStack src = PhysicalItemProxy.isProxy(item)
                    ? PhysicalItemProxy.unwrapForShop(item)
                    : item;
            if (src == null || src.getType().isAir()) {
                this.item = new ItemStack(Material.AIR);
            } else {
                this.item = src.clone();
            }
        }
    }

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
