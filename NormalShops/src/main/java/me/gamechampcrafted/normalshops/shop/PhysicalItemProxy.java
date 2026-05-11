package me.gamechampcrafted.normalshops.shop;

import me.gamechampcrafted.normalshops.NormalShops;
import me.gamechampcrafted.normalshops.data.Setting;
import me.gamechampcrafted.normalshops.menu.GuiDisplayItem;
import me.gamechampcrafted.normalshops.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Optional paper stand-ins for real items in shop stock/earnings GUIs and items granted to players (buys, earnings).
 * Each minted stack carries a ledger id + issued total + serialized template for dupe checks and conversion back to
 * real {@link ItemStack}s once totals are consistent across splits (no duplicate ledger mass).
 */
public final class PhysicalItemProxy {

    private static final String K_LEDGER = "physical_proxy_ledger";
    private static final String K_ISSUED = "physical_proxy_issued";
    private static final String K_PAYLOAD = "physical_proxy_payload";

    private static NamespacedKey keyLedger;
    private static NamespacedKey keyIssued;
    private static NamespacedKey keyPayload;

    private PhysicalItemProxy() {}

    public static boolean isProxyEnabled() {
        return Setting.PHYSICAL_ITEM_PROXY_ENABLED.isEnabled();
    }

    private static void ensureKeys(NormalShops plugin) {
        if (keyLedger == null) {
            keyLedger = new NamespacedKey(plugin, K_LEDGER);
            keyIssued = new NamespacedKey(plugin, K_ISSUED);
            keyPayload = new NamespacedKey(plugin, K_PAYLOAD);
        }
    }

    public static boolean isProxy(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return false;
        }
        NormalShops plugin = NormalShops.getInstance();
        if (plugin == null) {
            return false;
        }
        ensureKeys(plugin);
        return stack.getItemMeta().getPersistentDataContainer().has(keyLedger, PersistentDataType.STRING);
    }

    /**
     * Show in shop-managed inventories (stock/earnings pages). Does not split stacks.
     */
    public static ItemStack wrapForShopDisplay(ItemStack real) {
        if (!isProxyEnabled() || real == null || real.getType().isAir()) {
            return real == null ? null : real.clone();
        }
        return mintProxy(real.clone());
    }

    /**
     * Restore data-layer item from a slot (unwrap proxy or clone real).
     */
    public static ItemStack unwrapForShop(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }
        if (!isProxy(stack)) {
            return stack.clone();
        }
        return toReal(stack);
    }

    /**
     * Merchant recipe result. Always the real stack (clone): vanilla merchant trade UIs do not render {@code item_model}
     * on outputs, so paper proxies would appear as plain paper. Stock and fulfillment still use {@link ItemShop}
     * product templates, not this stack’s type.
     */
    public static ItemStack wrapMerchantResult(ItemStack real) {
        return real == null ? null : real.clone();
    }

    /**
     * Splits large withdrawals into multiple proxy stacks capped by {@link Setting#PHYSICAL_ITEM_PROXY_MAX_UNITS_PER_STACK}.
     */
    public static List<ItemStack> wrapPlayerGrant(ItemStack real) {
        List<ItemStack> out = new ArrayList<>();
        if (real == null || real.getType().isAir()) {
            return out;
        }
        if (!isProxyEnabled()) {
            out.add(real.clone());
            return out;
        }
        int maxUnits = Math.max(1, Setting.PHYSICAL_ITEM_PROXY_MAX_UNITS_PER_STACK.getInt());
        int left = real.getAmount();
        while (left > 0) {
            int chunk = Math.min(left, maxUnits);
            ItemStack piece = real.clone();
            piece.setAmount(chunk);
            out.add(mintProxy(piece));
            left -= chunk;
        }
        return out;
    }

    private static ItemStack mintProxy(ItemStack real) {
        NormalShops plugin = NormalShops.getInstance();
        if (plugin == null) {
            return real.clone();
        }
        ensureKeys(plugin);

        UUID ledger = UUID.randomUUID();
        int issued = real.getAmount();

        ItemStack templateOne = real.clone();
        templateOne.setAmount(1);
        String payload = encodePayload(templateOne);

        ItemStack paper = new ItemStack(Material.PAPER, issued);
        GuiDisplayItem.applyVanillaItemModelToPaper(paper, real.getType().getKey(), false);

        ItemMeta meta = paper.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RESET + Utils.formatItemWithAmount(real));
            ItemMeta srcMeta = real.getItemMeta();
            if (srcMeta != null && srcMeta.hasLore() && srcMeta.getLore() != null && !srcMeta.getLore().isEmpty()) {
                meta.setLore(new ArrayList<>(srcMeta.getLore()));
            }
            meta.getPersistentDataContainer().set(keyLedger, PersistentDataType.STRING, ledger.toString());
            meta.getPersistentDataContainer().set(keyIssued, PersistentDataType.INTEGER, issued);
            meta.getPersistentDataContainer().set(keyPayload, PersistentDataType.STRING, payload);
            paper.setItemMeta(meta);
        }
        return paper;
    }

    private static ItemStack toReal(ItemStack proxyStack) {
        NormalShops plugin = NormalShops.getInstance();
        if (plugin == null || !proxyStack.hasItemMeta()) {
            return proxyStack.clone();
        }
        ensureKeys(plugin);
        ItemMeta meta = proxyStack.getItemMeta();
        String payload = meta.getPersistentDataContainer().get(keyPayload, PersistentDataType.STRING);
        if (payload == null || payload.isEmpty()) {
            return proxyStack.clone();
        }
        ItemStack template = decodePayload(payload);
        if (template == null) {
            return proxyStack.clone();
        }
        ItemStack out = template.clone();
        out.setAmount(proxyStack.getAmount());
        return out;
    }

    private static String encodePayload(ItemStack templateAmountOne) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos)) {
                boos.writeObject(templateAmountOne.clone());
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static ItemStack decodePayload(String b64) {
        try {
            byte[] raw = Base64.getDecoder().decode(b64);
            try (BukkitObjectInputStream bio = new BukkitObjectInputStream(new ByteArrayInputStream(raw))) {
                Object o = bio.readObject();
                if (o instanceof ItemStack stack) {
                    return stack.clone();
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * Converts validated proxy stacks to real items; trims stacks when duplicated ledger totals exceed issued amount.
     */
    public static void resolveCarriedProxies(Player player) {
        if (!isProxyEnabled() || player == null) {
            return;
        }
        Map<UUID, LedgerAgg> map = new HashMap<>();
        accumulateInventory(player.getInventory(), map);
        accumulateInventory(player.getEnderChest(), map);
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && isProxy(cursor)) {
            accumulateStack(player, cursor, map, true);
        }
        for (Map.Entry<UUID, LedgerAgg> e : map.entrySet()) {
            LedgerAgg agg = e.getValue();
            if (!agg.issuedConsistent()) {
                continue;
            }
            int issued = agg.expectedIssued;
            if (agg.sum > issued) {
                int excess = agg.sum - issued;
                for (int idx = agg.refs.size() - 1; idx >= 0 && excess > 0; idx--) {
                    SlotRef ref = agg.refs.get(idx);
                    ItemStack stack = ref.getStack();
                    if (stack == null || !isProxy(stack)) {
                        continue;
                    }
                    int take = Math.min(stack.getAmount(), excess);
                    int remain = stack.getAmount() - take;
                    excess -= take;
                    ref.setAmount(remain <= 0 ? null : cloneAmount(stack, remain));
                }
            } else if (agg.sum == issued) {
                for (SlotRef ref : agg.refs) {
                    ItemStack stack = ref.getStack();
                    if (stack == null || !isProxy(stack)) {
                        continue;
                    }
                    ref.place(toReal(stack));
                }
            }
        }
    }

    private static ItemStack cloneAmount(ItemStack proto, int amount) {
        ItemStack c = proto.clone();
        c.setAmount(amount);
        return c;
    }

    private static void accumulateInventory(Inventory inventory, Map<UUID, LedgerAgg> map) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack s = inventory.getItem(i);
            if (s == null || !isProxy(s)) {
                continue;
            }
            accumulateStack(inventory, i, s, map);
        }
    }

    private static void accumulateStack(Object invOrPlayer, ItemStack stack, Map<UUID, LedgerAgg> map, boolean cursor) {
        UUID ledger = readLedger(stack);
        if (ledger == null) {
            return;
        }
        LedgerAgg agg = map.computeIfAbsent(ledger, k -> new LedgerAgg());
        int issued = readIssued(stack);
        agg.registerIssued(issued);
        agg.sum += stack.getAmount();
        if (cursor && invOrPlayer instanceof Player p) {
            agg.refs.add(new SlotRef(p));
        }
    }

    private static void accumulateStack(Inventory inventory, int slot, ItemStack stack, Map<UUID, LedgerAgg> map) {
        UUID ledger = readLedger(stack);
        if (ledger == null) {
            return;
        }
        LedgerAgg agg = map.computeIfAbsent(ledger, k -> new LedgerAgg());
        agg.registerIssued(readIssued(stack));
        agg.sum += stack.getAmount();
        agg.refs.add(new SlotRef(inventory, slot));
    }

    private static UUID readLedger(ItemStack stack) {
        if (!stack.hasItemMeta()) {
            return null;
        }
        NormalShops plugin = NormalShops.getInstance();
        if (plugin == null) {
            return null;
        }
        ensureKeys(plugin);
        String s = stack.getItemMeta().getPersistentDataContainer().get(keyLedger, PersistentDataType.STRING);
        if (s == null) {
            return null;
        }
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static int readIssued(ItemStack stack) {
        if (!stack.hasItemMeta()) {
            return 0;
        }
        NormalShops plugin = NormalShops.getInstance();
        if (plugin == null) {
            return 0;
        }
        ensureKeys(plugin);
        Integer v = stack.getItemMeta().getPersistentDataContainer().get(keyIssued, PersistentDataType.INTEGER);
        return v == null ? 0 : v;
    }

    private static final class LedgerAgg {
        int sum;
        int expectedIssued = -1;
        boolean issuedConflict;
        final List<SlotRef> refs = new ArrayList<>();

        void registerIssued(int issued) {
            if (issuedConflict) {
                return;
            }
            if (expectedIssued < 0) {
                expectedIssued = issued;
            } else if (expectedIssued != issued) {
                issuedConflict = true;
            }
        }

        boolean issuedConsistent() {
            return !issuedConflict && expectedIssued >= 0;
        }
    }

    /**
     * Cursor uses {@link Player}; chest slots use {@link Inventory}+slot index.
     */
    private static final class SlotRef {
        @Nullable
        private final Inventory inventory;
        private final int slot;
        @Nullable
        private final Player cursorPlayer;

        SlotRef(Inventory inventory, int slot) {
            this.inventory = inventory;
            this.slot = slot;
            this.cursorPlayer = null;
        }

        SlotRef(Player cursorPlayer) {
            this.inventory = null;
            this.slot = -1;
            this.cursorPlayer = cursorPlayer;
        }

        void setAmount(@Nullable ItemStack stack) {
            if (cursorPlayer != null) {
                cursorPlayer.setItemOnCursor(stack);
            } else {
                inventory.setItem(slot, stack);
            }
        }

        void place(ItemStack real) {
            setAmount(real);
        }

        ItemStack getStack() {
            if (cursorPlayer != null) {
                return cursorPlayer.getItemOnCursor();
            }
            return inventory.getItem(slot);
        }
    }
}
