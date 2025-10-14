package net.tigereye.chestcavity.guzhenren.resource;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.registries.BuiltInRegistries;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.OptionalDouble;

/**
 * Utilities for reading and modifying the Yuan Lao Gu item’s stored Yuan Shi amount.
 * This tool operates directly on the ItemStack’s CustomData keys used by Guzhenren.
 */
public final class YuanLaoGuHelper {

    /** Key: current stored amount of Yuan Shi inside the Yuan Lao Gu item. */
    public static final String KEY_AMOUNT = "元老蛊内元石数量";
    /** Key: capacity cap stored on the item; some variants set this every tick. */
    public static final String KEY_CAPACITY = "元老蛊内元石数量上限";

    private static final DecimalFormat DISPLAY_FORMAT = new DecimalFormat("##.##");

    private YuanLaoGuHelper() {}

    /** Returns true if the stack是元老蛊基础形态。 */
    public static boolean isYuanLaoGu(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        if (key == null) return false;
        if (!"guzhenren".equals(key.getNamespace())) return false;
        String path = key.getPath().toLowerCase(Locale.ROOT);
        // Known ids include: yuan_lao_gu_1 / 4 / 5 等
        return path.startsWith("yuan_lao_gu_");
    }

    /** Returns true when the stack is the 二转元老蛊（e_yuanlaogurzhuan). */
    public static boolean isSecondTierYuanLaoGu(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        if (key == null) return false;
        return "guzhenren".equals(key.getNamespace())
                && "e_yuanlaogurzhuan".equals(key.getPath().toLowerCase(Locale.ROOT));
    }

    /** Returns true when the stack is 三转元老蛊（sanzhuanyuanlaogu). */
    public static boolean isThirdTierYuanLaoGu(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        if (key == null) return false;
        return "guzhenren".equals(key.getNamespace())
                && "sanzhuanyuanlaogu".equals(key.getPath().toLowerCase(Locale.ROOT));
    }

    /** Returns true when the stack is 四转元老蛊（yuan_lao_gu_4). */
    public static boolean isFourthTierYuanLaoGu(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        if (key == null) return false;
        return "guzhenren".equals(key.getNamespace())
                && "yuan_lao_gu_4".equals(key.getPath().toLowerCase(Locale.ROOT));
    }

    /** Returns true when the stack is 五转元老蛊（yuan_lao_gu_5). */
    public static boolean isFifthTierYuanLaoGu(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        if (key == null) return false;
        return "guzhenren".equals(key.getNamespace())
                && "yuan_lao_gu_5".equals(key.getPath().toLowerCase(Locale.ROOT));
    }

    /** Reads the current stored amount. Absent key yields 0. */
    public static double readAmount(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getDouble(KEY_AMOUNT);
    }

    /** Reads the capacity cap if present. Returns OptionalDouble.empty() when not set. */
    public static OptionalDouble readCapacity(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (!data.copyTag().contains(KEY_CAPACITY)) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(data.copyTag().getDouble(KEY_CAPACITY));
    }

    /**
     * Sets the stored amount, clamped to [0, capacity] when capacity key exists.
     * Returns the value actually written.
     */
    public static double writeAmountClamped(ItemStack stack, double newAmount) {
        double clamped = Math.max(0.0, newAmount);
        OptionalDouble cap = readCapacity(stack);
        if (cap.isPresent()) {
            clamped = Math.min(clamped, cap.getAsDouble());
        }
        final double value = clamped;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putDouble(KEY_AMOUNT, value));
        return value;
    }

    /** Attempts to add delta (can be negative). Respects capacity when present; never goes below 0. */
    public static boolean adjustAmount(ItemStack stack, double delta) {
        double current = readAmount(stack);
        double target = current + delta;
        OptionalDouble cap = readCapacity(stack);
        if (target < 0.0) return false;
        if (cap.isPresent() && target > cap.getAsDouble()) return false;
        writeAmountClamped(stack, target);
        return true;
    }

    /** Subtracts amount if possible; returns true on success, false if insufficient balance. */
    public static boolean consume(ItemStack stack, double amount) {
        if (amount <= 0.0) return true;
        double current = readAmount(stack);
        if (current + 1e-9 < amount) return false;
        writeAmountClamped(stack, current - amount);
        return true;
    }

    /** Adds amount if capacity allows; returns true on success. */
    public static boolean deposit(ItemStack stack, double amount) {
        if (amount <= 0.0) return true;
        return adjustAmount(stack, amount);
    }

    /**
     * Updates the item’s custom display name to mirror Guzhenren’s format
     * (e.g., “元老蛊 (元石数量:XXX)”). Purely cosmetic; optional.
     */
    public static void updateDisplayName(ItemStack stack) {
        double amt = readAmount(stack);
        String label = "元老蛊 (元石数量:***)".replace("***", DISPLAY_FORMAT.format(amt));
        stack.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(label));
    }
}
