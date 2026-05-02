package me.gamechampcrafted.normalshops.shop;

import me.gamechampcrafted.normalshops.CoreProtectLogger;
import me.gamechampcrafted.normalshops.Debug;
import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.data.Message;
import me.gamechampcrafted.normalshops.data.MessageType;
import me.gamechampcrafted.normalshops.utils.HoverableMessageParametizer;
import me.gamechampcrafted.normalshops.utils.MessageParametizer;
import me.gamechampcrafted.normalshops.utils.Utils;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class TradingMenuManager implements Listener {

    private final Map<UUID, TradingSession> sessions = new HashMap<>();

    public void openTradingMenu(Player player, ItemShop shop) {
        if (NormalShops.getInstance().getStockChestManager().hasActiveStockViewersForShopByOther(shop, player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Shop stock is currently being edited by another player.");
            MessageType.FAIL.playSound(player);
            return;
        }
        boolean outOfStock = !shop.hasStock();
        if (outOfStock) {
            MessageType.FAIL.playSound(player);
        }
        List<ItemStack> products = shop.getProducts();
        ItemStack price = shop.getPrice();

        Merchant merchant = Bukkit.createMerchant(getTitle(shop));
        List<MerchantRecipe> recipes = new ArrayList<>();

        if (products.size() == 1) {
            ItemStack product = products.get(0);
            int maxUses = shop.isAdminShop() ? Integer.MAX_VALUE : (outOfStock ? 0 : calculateMaxUses(shop, product));
            MerchantRecipe recipe = new MerchantRecipe(product.clone(), maxUses);
            recipe.addIngredient(price.clone());
            recipes.add(recipe);
        } else {
            ItemStack bundle = createBundle(products);
            int maxUses = shop.isAdminShop() ? Integer.MAX_VALUE : (outOfStock ? 0 : calculateBundleMaxUses(shop, products));
            MerchantRecipe bundleRecipe = new MerchantRecipe(bundle, maxUses);
            bundleRecipe.addIngredient(price.clone());
            recipes.add(bundleRecipe);

            for (ItemStack product : products) {
                ItemStack previewItem = createPreviewItem(product);
                MerchantRecipe previewRecipe = new MerchantRecipe(previewItem, 0);
                previewRecipe.addIngredient(createPreviewPriceBarrier());
                recipes.add(previewRecipe);
            }
        }

        int[] initialUses = new int[recipes.size()];
        merchant.setRecipes(recipes);
        player.openMerchant(merchant, true);
        sessions.put(player.getUniqueId(), new TradingSession(player, shop, merchant, initialUses, products.size() > 1));
    }

    private ItemStack createBundle(List<ItemStack> products) {
        ItemStack chestItem = new ItemStack(Material.CHEST);
        BlockStateMeta meta = (BlockStateMeta) chestItem.getItemMeta();
        Chest chest = (Chest) meta.getBlockState();
        for (ItemStack product : products) {
            chest.getBlockInventory().addItem(product.clone());
        }
        meta.setBlockState(chest);
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + ">> SHOP BUNDLE <<");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Main purchase option");
        lore.add(ChatColor.DARK_GRAY + " ");
        lore.add(ChatColor.GREEN + "Select this recipe to buy the full bundle.");
        meta.setLore(lore);
        meta.addEnchant(Enchantment.LURE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        chestItem.setItemMeta(meta);
        return chestItem;
    }

    private ItemStack createPreviewItem(ItemStack product) {
        ItemStack preview = product.clone();
        ItemMeta meta = preview.getItemMeta();
        String name = meta.hasDisplayName() ? meta.getDisplayName() : Utils.getFormattedName(product);
        meta.setDisplayName(ChatColor.GRAY + "[Preview] " + ChatColor.WHITE + name);
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        lore.add(0, ChatColor.GRAY + "Included in bundle");
        meta.setLore(lore);
        preview.setItemMeta(meta);
        return preview;
    }

    private ItemStack createPreviewPriceBarrier() {
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta meta = barrier.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Preview Only");
            meta.setLore(List.of(ChatColor.GRAY + "Not purchasable"));
            barrier.setItemMeta(meta);
        }
        return barrier;
    }

    private int calculateBundleMaxUses(ItemShop shop, List<ItemStack> products) {
        int min = Integer.MAX_VALUE;
        for (ItemStack product : products) {
            int uses = calculateMaxUses(shop, product);
            if (uses < min) min = uses;
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    private int calculateMaxUses(ItemShop shop, ItemStack product) {
        int productAmount = product.getAmount();
        if (productAmount <= 0) return 0;
        int total = shop.countInternalStock(product);
        Inventory stockedInv = shop.getNextStockedInventory();
        if (stockedInv != null) {
            String displayName = getDisplayName(product);
            for (ItemStack stack : stockedInv.getContents()) {
                if (stack != null && displayName.equals(getDisplayName(stack))) {
                    total += stack.getAmount();
                }
            }
        }
        return total / productAmount;
    }

    private String getDisplayName(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return item == null ? "" : item.getType().name();
        }
        var meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() ? meta.getDisplayName() : item.getType().name();
    }

    private String getTitle(ItemShop shop) {
        String owner = shop.getOwnerName();
        if (shop.hasStock()) {
            return ChatColor.DARK_GREEN + "In Stock " + ChatColor.GRAY + "| " + ChatColor.RESET + owner;
        }
        return ChatColor.DARK_RED + "Out of Stock " + ChatColor.GRAY + "| " + ChatColor.RESET + owner;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTradeClick(InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.MERCHANT) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        TradingSession session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (event.getSlot() != 2) return;

        Bukkit.getScheduler().runTask(NormalShops.getInstance(), () -> reconcileTrades(session));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getType() != InventoryType.MERCHANT) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        TradingSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;
        reconcileTrades(session);
    }

    private void reconcileTrades(TradingSession session) {
        Player buyer = session.player;
        ItemShop shop = session.shop;
        Merchant merchant = session.merchant;
        List<MerchantRecipe> recipes = merchant.getRecipes();
        List<ItemStack> products = shop.getProducts();
        ItemStack price = shop.getPrice();

        int totalTrades = 0;

        if (session.isBundle) {
            MerchantRecipe bundleRecipe = recipes.get(0);
            int previousUses = session.previousUses[0];
            int currentUses = bundleRecipe.getUses();
            int tradesCompleted = currentUses - previousUses;
            session.previousUses[0] = currentUses;

            for (int t = 0; t < tradesCompleted; t++) {
                boolean fulfilled = true;
                if (!shop.isAdminShop() && shop.hasStock()) {
                    for (ItemStack product : products) {
                        if (shop.removeInternalStock(product, product.getAmount())) {
                            shop.recordProductsSold(product.getAmount());
                            continue;
                        }
                        Inventory stockInv = shop.getNextStockedInventory();
                        if (stockInv != null && ItemShop.removeMatchingItems(stockInv, product, product.getAmount())) {
                            shop.recordProductsSold(product.getAmount());
                            continue;
                        }
                        fulfilled = false;
                        break;
                    }
                } else if (!shop.isAdminShop()) {
                    fulfilled = false;
                }
                if (!fulfilled) {
                    break;
                }
                if (!shop.isAdminShop() && fulfilled) {
                    shop.incrementEarnings();
                }
                shop.saveData();
                totalTrades++;

                CoreProtectLogger.logShopBuyVillager(
                        buyer,
                        shop.getOwnerName(),
                        shop.getLocation(),
                        price,
                        products
                );
            }

            if (!shop.isAdminShop()) {
                int remainingMaxUses = calculateBundleMaxUses(shop, products);
                bundleRecipe.setMaxUses(currentUses + remainingMaxUses);
            }
        } else {
            for (int i = 0; i < recipes.size() && i < products.size(); i++) {
                MerchantRecipe recipe = recipes.get(i);
                ItemStack product = products.get(i);
                int previousUses = session.previousUses[i];
                int currentUses = recipe.getUses();
                int tradesCompleted = currentUses - previousUses;
                session.previousUses[i] = currentUses;
                totalTrades += tradesCompleted;

                for (int t = 0; t < tradesCompleted; t++) {
                    boolean fulfilled = true;
                    if (!shop.isAdminShop() && shop.hasStock()) {
                        if (!shop.removeInternalStock(product, product.getAmount())) {
                            Inventory stockInv = shop.getNextStockedInventory();
                            if (stockInv != null && ItemShop.removeMatchingItems(stockInv, product, product.getAmount())) {
                                shop.recordProductsSold(product.getAmount());
                            } else {
                                fulfilled = false;
                            }
                        } else {
                            shop.recordProductsSold(product.getAmount());
                        }
                    } else if (!shop.isAdminShop()) {
                        fulfilled = false;
                    }
                    if (!fulfilled) {
                        break;
                    }
                    if (!shop.isAdminShop() && fulfilled) {
                        shop.incrementEarnings();
                    }
                    shop.saveData();
                    totalTrades++;

                    CoreProtectLogger.logShopBuyVillager(
                            buyer,
                            shop.getOwnerName(),
                            shop.getLocation(),
                            price,
                            List.of(product)
                    );
                }

                if (!shop.isAdminShop()) {
                    int remainingMaxUses = calculateMaxUses(shop, product);
                    recipe.setMaxUses(currentUses + remainingMaxUses);
                }
            }
        }

        if (totalTrades == 0) return;

        sendBuyMessages(buyer, shop);

        Player owner = Bukkit.getPlayer(shop.getOwnerUUID());
        if (owner != null && shop.isNotificationsEnabled()) {
            if (!shop.isAdminShop() || Debug.SEND_ADMIN_SHOP_NOTIFICATIONS) {
                sendSellMessages(owner, buyer, shop);
                shop.getBuySound().playSound(owner);
            }
        }

        if (!shop.hasStock()) {
            NormalShops.getInstance().getShopManager().addWarning(shop);
        }
    }

    private void sendBuyMessages(Player buyer, ItemShop shop) {
        ItemStack price = shop.getPrice();
        ItemStack product = shop.getProducts().get(0);
        boolean hasCustomName = shop.hasCustomName();
        String customName = hasCustomName ? (shop.getCustomName() != null ? shop.getCustomName() : "") : "";

        BaseComponent[] productsHover = getHoverText(
                Message.HOVER_VARIOUS_PRODUCTS.toString(),
                Utils.colorize(Utils.formatItemWithAmount(shop.getProducts().get(0))));

        String shopAt = new MessageParametizer()
                .setMessage(hasCustomName ? Message.SHOP_AT_CUSTOM : Message.SHOP_AT)
                .put("name", customName)
                .put("location", Utils.formatLocation(shop.getLocation()))
                .toString();
        BaseComponent[] shopHover = getHoverText(Message.HOVER_YOUR_SHOP.toString(), shopAt);

        String underlinedCustomName = Utils.colorize("&a&l&n" + customName);
        String shopDetails = Message.SHOP_DETAILS.parameterizer()
                .put("location", Utils.formatLocation(shop.getLocation()))
                .put("owner", shop.getOwnerName())
                .toString();
        BaseComponent[] customNameHover = getHoverText(underlinedCustomName, shopDetails);

        HoverableMessageParametizer parameterizer = Message.BUY_SINGLE.parameterizer()
                .put("seller", shop.getOwnerName())
                .put("buyer", buyer.getDisplayName())
                .put("price", Utils.formatItemWithAmount(price))
                .put("product", Utils.formatItemWithAmount(product))
                .hoverable()
                .putHover("hoverVariousProducts", productsHover)
                .putHover("hoverYourShop", shopHover)
                .putHover("hoverCustomName", customNameHover);

        Message buyerMessage = hasCustomName ? Message.BUY_SINGLE_CUSTOM : Message.BUY_SINGLE;
        parameterizer.setMessage(buyerMessage).sendSilently(buyer);
        shop.getBuySound().playSound(buyer);
    }

    private void sendSellMessages(Player owner, Player buyer, ItemShop shop) {
        ItemStack price = shop.getPrice();
        ItemStack product = shop.getProducts().get(0);

        HoverableMessageParametizer parameterizer = Message.SELL_SINGLE.parameterizer()
                .put("seller", owner.getDisplayName())
                .put("buyer", buyer.getDisplayName())
                .put("price", Utils.formatItemWithAmount(price))
                .put("product", Utils.formatItemWithAmount(product))
                .hoverable();

        parameterizer.setMessage(Message.SELL_SINGLE).sendSilently(owner);
    }

    private BaseComponent[] getHoverText(String message, String hoverText) {
        BaseComponent[] messageComponents = TextComponent.fromLegacyText(message);
        BaseComponent[] hoverComponents = TextComponent.fromLegacyText(hoverText);
        for (BaseComponent messageComponent : messageComponents) {
            messageComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverComponents)));
        }
        return messageComponents;
    }

    public void closeAllSessions() {
        new ArrayList<>(sessions.keySet()).forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.closeInventory();
                sessions.remove(uuid);
            }
        });
    }

    public boolean hasActiveSessionForShopByOther(ItemShop shop, UUID playerUUID) {
        return sessions.values().stream()
                .anyMatch(session -> !session.player.getUniqueId().equals(playerUUID)
                        && session.shop.getLocation().equals(shop.getLocation()));
    }

    private static class TradingSession {
        final Player player;
        final ItemShop shop;
        final Merchant merchant;
        final int[] previousUses;
        final boolean isBundle;

        TradingSession(Player player, ItemShop shop, Merchant merchant, int[] previousUses, boolean isBundle) {
            this.player = player;
            this.shop = shop;
            this.merchant = merchant;
            this.previousUses = previousUses;
            this.isBundle = isBundle;
        }
    }
}