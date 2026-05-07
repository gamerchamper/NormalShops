package me.gamechampcrafted.normalshops.menu.delete;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.menu.ShopButton;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ForceChangeOwnerButton extends ShopButton {

    public ForceChangeOwnerButton(int slot, ItemShop shop) {
        super(slot, shop);
    }

    @Override
    public ItemStack getItem() {
        List<String> lore = Arrays.asList(
                Utils.colorize("&7Force transfer this shop to another player."),
                Utils.colorize("&7After clicking, type a username in chat.")
        );
        return createItem(Utils.colorize("&c👤 &lFORCE CHANGE OWNER"), lore, Material.PLAYER_HEAD, false);
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player admin = (Player) event.getWhoClicked();
        if (AdminGuiSecurity.denyUnlessAdminTools(admin, "force change shop owner (admin GUI click)")) {
            return;
        }
        ItemShop currentShop = getShop();
        admin.closeInventory();
        admin.sendMessage(ChatColor.YELLOW + "Type the new owner username in chat (10s).");

        NormalShops.getInstance().getChatInputListener().addChatCallback(admin, input -> {
            Bukkit.getScheduler().runTask(NormalShops.getInstance(), () -> {
                if (AdminGuiSecurity.denyUnlessAdminTools(admin, "force change shop owner (chat confirm)")) {
                    return;
                }
                ItemShop shop = ItemShop.get(currentShop.getLocation());
                if (shop == null || shop.isDeleted()) {
                    admin.sendMessage(ChatColor.RED + "Shop no longer exists.");
                    return;
                }

                String username = input == null ? "" : input.trim();
                if (username.isEmpty()) {
                    admin.sendMessage(ChatColor.RED + "Username cannot be empty.");
                    return;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(username);
                UUID targetUUID = target.getUniqueId();
                if (targetUUID == null) {
                    admin.sendMessage(ChatColor.RED + "Could not resolve that username.");
                    return;
                }
                if (shop.getOwnerUUID().equals(targetUUID)) {
                    admin.sendMessage(ChatColor.RED + "That player already owns this shop.");
                    return;
                }

                String oldOwner = shop.getOwnerName();
                String targetName = target.getName() != null ? target.getName() : username;
                ItemShop transferred = NormalShops.getInstance().getShopManager().transferShopOwnership(shop, targetUUID);
                CoreProtectLogger.logShopEdit(admin, transferred.getLocation(), "force changed owner from "
                        + oldOwner + " to " + targetName);
                admin.sendMessage(ChatColor.GREEN + "Transferred shop at "
                        + Utils.formatLocation(transferred.getLocation()) + ChatColor.GRAY
                        + " from " + oldOwner + " to " + targetName + ".");
            });
        }, 200, null);
    }
}
