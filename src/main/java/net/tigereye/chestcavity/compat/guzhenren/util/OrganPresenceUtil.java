package net.tigereye.chestcavity.compat.guzhenren.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Utility helpers to query organs present in a chest cavity inventory.
 */
public final class OrganPresenceUtil {

    private OrganPresenceUtil() {
    }

    public static boolean has(ChestCavityInstance cc, Item item) {
        return item != null && has(cc, stack -> stack.is(item));
    }

    public static boolean has(ChestCavityInstance cc, ResourceLocation itemId) {
        if (itemId == null) {
            return false;
        }
        return has(cc, stack -> {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return Objects.equals(id, itemId);
        });
    }

    public static boolean has(ChestCavityInstance cc, Predicate<ItemStack> predicate) {
        if (cc == null || cc.inventory == null || predicate == null) {
            return false;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (predicate.test(stack)) {
                return true;
            }
        }
        return false;
    }

    public static ItemStack findFirst(ChestCavityInstance cc, Predicate<ItemStack> predicate) {
        if (cc == null || cc.inventory == null || predicate == null) {
            return ItemStack.EMPTY;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (predicate.test(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
