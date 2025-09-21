package net.tigereye.chestcavity.util.retention;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for organs that should be retained on death when keepInventory is enabled.
 */
public final class OrganRetentionRules {

    private static final Set<ResourceLocation> RETAINED_ITEMS = ConcurrentHashMap.newKeySet();
    private static final Set<String> RETAINED_NAMESPACES = ConcurrentHashMap.newKeySet();

    private OrganRetentionRules() {
    }

    public static void registerItem(ResourceLocation itemId) {
        if (itemId != null) {
            RETAINED_ITEMS.add(itemId);
        }
    }

    public static void registerNamespace(String namespace) {
        if (namespace != null && !namespace.isEmpty()) {
            RETAINED_NAMESPACES.add(namespace);
        }
    }

    public static boolean shouldRetain(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key == null) {
            return false;
        }
        return RETAINED_ITEMS.contains(key) || RETAINED_NAMESPACES.contains(key.getNamespace());
    }
}

