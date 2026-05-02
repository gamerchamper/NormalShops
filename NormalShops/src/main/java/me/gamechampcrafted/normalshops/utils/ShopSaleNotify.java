package me.gamechampcrafted.normalshops.utils;

import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.shop.ItemShop;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Chat notifications when someone buys from a shop — owner messages include a clickable link to open the shop remotely.
 */
public final class ShopSaleNotify {

    private static final String HOVER_LINE_FORMAT = "&f%dx &a%s\n";

    private ShopSaleNotify() {
    }

    public static String remoteOpenCommand(@NotNull ItemShop shop) {
        Location loc = shop.getLocation();
        World w = loc.getWorld();
        if (w == null) {
            return "/viewshops";
        }
        UUID worldUid = w.getUID();
        return "/viewshops open " + worldUid + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
    }

    public static void sendOwnerSellNotification(Player owner, Player buyer, ItemShop shop) {
        List<ItemStack> products = shop.getProducts();
        ItemStack price = shop.getPrice();
        ItemStack product = products.get(0);

        StringBuilder boughtProducts = new StringBuilder();
        products.forEach(item -> boughtProducts.append(Utils.formatItemWithAmount(HOVER_LINE_FORMAT, item)));

        boolean hasCustomName = shop.hasCustomName();
        String customName = shop.getCustomName() != null ? shop.getCustomName() : "";
        String shopAt = new MessageParametizer()
                .setMessage(hasCustomName ? Message.SHOP_AT_CUSTOM : Message.SHOP_AT)
                .put("name", customName)
                .put("location", Utils.formatLocation(shop.getLocation()))
                .toString();
        String shopNotifyHover = shopAt + "\n" + Message.HOVER_YOUR_SHOP_REMOTE_HINT;
        BaseComponent[] productsHover = getHoverText(
                Message.HOVER_VARIOUS_PRODUCTS.toString(),
                Utils.colorize(boughtProducts.toString()));
        BaseComponent[] shopHover = getHoverText(
                Message.HOVER_YOUR_SHOP_NOTIFY.toString(),
                Utils.colorize(shopNotifyHover));

        HoverableMessageParametizer parameterizer = Message.SELL_SINGLE.parameterizer()
                .put("buyer", buyer.getDisplayName())
                .put("price", Utils.formatItemWithAmount(price))
                .put("product", Utils.formatItemWithAmount(product))
                .hoverable()
                .putHover("hoverVariousProducts", productsHover)
                .putHover("hoverYourShop", shopHover)
                .putClick("hoverYourShop", remoteOpenCommand(shop));

        Message ownerMessage = products.size() <= 1 ? Message.SELL_SINGLE : Message.SELL_MULTIPLE;
        parameterizer.setMessage(ownerMessage).sendSilently(owner);
    }

    private static BaseComponent[] getHoverText(@NotNull String message, @NotNull String hoverText) {
        BaseComponent[] messageComponents = TextComponent.fromLegacyText(message);
        BaseComponent[] hoverComponents = TextComponent.fromLegacyText(hoverText);
        for (BaseComponent messageComponent : messageComponents) {
            messageComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverComponents)));
        }
        return messageComponents;
    }
}
