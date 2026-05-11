package me.gamechampcrafted.normalshops.menu;

import me.gamechampcrafted.normalshops.NormalShops;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * GUI “fake items”: stack type is always {@link Material#PAPER}; clients render the vanilla model for another item via
 * {@code item_model} (Paper 1.21+). Used across menus so icons are uniform while still looking like chests, arrows, etc.
 * <p>
 * Each stack is tagged with a unique id in {@link org.bukkit.persistence.PersistentDataContainer} (serializes with
 * item NBT as custom data) so leaked copies can be stripped from player inventories.
 */
public final class GuiDisplayItem {

    private static final String PDC_KEY_ID = "gui_display_item_id";

    private static NamespacedKey cachedIdKey;

    private GuiDisplayItem() {}

    public static ItemStack paperIconForProduct(ItemStack productTemplate) {
        if (productTemplate == null || productTemplate.getType().isAir()) {
            return paperIconForMaterial(Material.CHEST);
        }
        int amt = Math.max(1, Math.min(64, productTemplate.getAmount()));
        ItemStack icon = new ItemStack(Material.PAPER, amt);
        applyVanillaItemModelToPaper(icon, productTemplate.getType().getKey());
        return icon;
    }

    public static ItemStack paperIconForMaterial(Material material) {
        Material m = material != null && !material.isAir() ? material : Material.CHEST;
        ItemStack icon = new ItemStack(Material.PAPER, 1);
        applyVanillaItemModelToPaper(icon, m.getKey());
        return icon;
    }

    /**
     * Paper stack with display name / lore, then the visual model for {@code visualMaterial}.
     * Call after all meta mutations are done (same rules as {@link #applyVanillaItemModelToPaper}).
     */
    public static ItemStack paperIconWithMeta(Material visualMaterial, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (displayName != null) {
                meta.setDisplayName(displayName);
            }
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        Material v = visualMaterial != null && !visualMaterial.isAir() ? visualMaterial : Material.PAPER;
        applyVanillaItemModelToPaper(item, v.getKey());
        return item;
    }

    /**
     * Applies the vanilla item model for {@code minecraft:&lt;item&gt;} while keeping stack type {@link Material#PAPER}.
     * Prefer calling <strong>after</strong> display name / lore / enchant meta when possible.
     */
    public static void applyVanillaItemModelToPaper(ItemStack paperStack, NamespacedKey vanillaItemKey) {
        applyVanillaItemModelToPaper(paperStack, vanillaItemKey, true);
    }

    /**
     * @param stampGuiItem when false, skips {@link #stampGuiItem} so callers (e.g. physical proxies) can use their own PDC.
     */
    public static void applyVanillaItemModelToPaper(ItemStack paperStack, NamespacedKey vanillaItemKey, boolean stampGuiItem) {
        String keyString = vanillaItemKey.getNamespace() + ":" + vanillaItemKey.getKey();
        Object adventureKey = tryCreateAdventureKey(keyString);
        ItemMeta meta = paperStack.getItemMeta();
        if (meta != null && invokeSetItemModel(meta, vanillaItemKey, adventureKey)) {
            paperStack.setItemMeta(meta);
            if (stampGuiItem) {
                GuiDisplayItem.stampGuiItem(paperStack);
            }
            return;
        }
        tryDataComponentsItemModel(paperStack, vanillaItemKey, adventureKey);
        if (stampGuiItem) {
            GuiDisplayItem.stampGuiItem(paperStack);
        }
    }

    /**
     * Tags a stack as a NormalShops GUI display item (unique id in PDC). Use when a paper icon is built without
     * calling {@link #applyVanillaItemModelToPaper} (e.g. plain paper + meta only).
     */
    public static void stampGuiItem(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return;
        }
        NormalShops plugin = NormalShops.getInstance();
        if (plugin == null) {
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        NamespacedKey idKey = guiItemIdKey(plugin);
        if (idKey == null) {
            return;
        }
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        stack.setItemMeta(meta);
    }

    /**
     * True if this stack was created as a NormalShops GUI display item (has our PDC id).
     */
    public static boolean isGuiItem(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return false;
        }
        NormalShops plugin = NormalShops.getInstance();
        if (plugin == null) {
            return false;
        }
        NamespacedKey idKey = guiItemIdKey(plugin);
        return idKey != null
                && stack.getItemMeta().getPersistentDataContainer().has(idKey, PersistentDataType.STRING);
    }

    private static NamespacedKey guiItemIdKey(NormalShops plugin) {
        if (cachedIdKey == null) {
            cachedIdKey = new NamespacedKey(plugin, PDC_KEY_ID);
        }
        return cachedIdKey;
    }

    private static boolean tryDataComponentsItemModel(ItemStack stack, NamespacedKey nk, Object adventureKey) {
        Object componentType;
        try {
            Class<?> typesClass = Class.forName("io.papermc.paper.datacomponent.DataComponentTypes");
            componentType = typesClass.getField("ITEM_MODEL").get(null);
        } catch (Throwable ignored) {
            return false;
        }
        for (Method m : stack.getClass().getMethods()) {
            if (!"setData".equals(m.getName()) || m.getParameterCount() != 2 || Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            Class<?> p0 = m.getParameterTypes()[0];
            if (!p0.getName().contains("DataComponentType")) {
                continue;
            }
            Object[] attempts = adventureKey != null
                    ? new Object[]{adventureKey, nk}
                    : new Object[]{nk};
            for (Object value : attempts) {
                if (value == null) {
                    continue;
                }
                try {
                    m.invoke(stack, componentType, value);
                    return true;
                } catch (Throwable ignored) {
                }
            }
        }
        return false;
    }

    private static Object tryCreateAdventureKey(String keyString) {
        try {
            Class<?> keyClass = Class.forName("net.kyori.adventure.key.Key");
            Method factory = keyClass.getMethod("key", String.class);
            return factory.invoke(null, keyString);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean invokeSetItemModel(ItemMeta meta, NamespacedKey nk, Object adventureKey) {
        for (Method method : collectSetItemModelMethods(meta.getClass())) {
            Class<?> p = method.getParameterTypes()[0];
            Object arg = null;
            if (p == NamespacedKey.class) {
                arg = nk;
            } else if (adventureKey != null && p.isAssignableFrom(adventureKey.getClass())) {
                arg = adventureKey;
            } else if (adventureKey != null && "net.kyori.adventure.key.Key".equals(p.getName())) {
                arg = adventureKey;
            }
            if (arg == null) {
                continue;
            }
            try {
                method.setAccessible(true);
                method.invoke(meta, arg);
                return true;
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private static List<Method> collectSetItemModelMethods(Class<?> start) {
        Set<Method> seen = new LinkedHashSet<>();
        List<Method> out = new ArrayList<>();
        for (Class<?> c = start; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (!"setItemModel".equals(m.getName()) || m.getParameterCount() != 1) {
                    continue;
                }
                if (seen.add(m)) {
                    out.add(m);
                }
            }
        }
        for (Class<?> iface : start.getInterfaces()) {
            for (Method m : iface.getMethods()) {
                if (!"setItemModel".equals(m.getName()) || m.getParameterCount() != 1) {
                    continue;
                }
                if (seen.add(m)) {
                    out.add(m);
                }
            }
        }
        for (Method m : start.getMethods()) {
            if (!"setItemModel".equals(m.getName()) || m.getParameterCount() != 1) {
                continue;
            }
            if (seen.add(m)) {
                out.add(m);
            }
        }
        return out;
    }
}
