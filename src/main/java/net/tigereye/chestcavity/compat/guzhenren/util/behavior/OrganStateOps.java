package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.util.NetworkUtil;
import net.minecraft.world.item.ItemStack;

import java.util.function.DoubleUnaryOperator;
import java.util.function.IntUnaryOperator;
import java.util.function.LongUnaryOperator;

/**
 * Convenience setters for OrganState that auto-sync slot updates when changed.
 */
public final class OrganStateOps {

    private OrganStateOps() {}

    public static OrganState.Change<Integer> setIntSync(ChestCavityInstance cc, ItemStack stack, String rootKey,
                                                        String key, int value, IntUnaryOperator clamp, int defaultValue) {
        OrganState state = OrganState.of(stack, rootKey);
        OrganState.Change<Integer> change = state.setInt(key, value, clamp, defaultValue);
        if (change.changed()) {
            NetworkUtil.sendOrganSlotUpdate(cc, stack);
        }
        return change;
    }

    public static OrganState.Change<Long> setLongSync(ChestCavityInstance cc, ItemStack stack, String rootKey,
                                                      String key, long value, LongUnaryOperator clamp, long defaultValue) {
        OrganState state = OrganState.of(stack, rootKey);
        OrganState.Change<Long> change = state.setLong(key, value, clamp, defaultValue);
        if (change.changed()) {
            NetworkUtil.sendOrganSlotUpdate(cc, stack);
        }
        return change;
    }

    public static OrganState.Change<Double> setDoubleSync(ChestCavityInstance cc, ItemStack stack, String rootKey,
                                                          String key, double value, DoubleUnaryOperator clamp, double defaultValue) {
        OrganState state = OrganState.of(stack, rootKey);
        OrganState.Change<Double> change = state.setDouble(key, value, clamp, defaultValue);
        if (change.changed()) {
            NetworkUtil.sendOrganSlotUpdate(cc, stack);
        }
        return change;
    }

    public static OrganState.Change<Boolean> setBooleanSync(ChestCavityInstance cc, ItemStack stack, String rootKey,
                                                            String key, boolean value, boolean defaultValue) {
        OrganState state = OrganState.of(stack, rootKey);
        OrganState.Change<Boolean> change = state.setBoolean(key, value, defaultValue);
        if (change.changed()) {
            NetworkUtil.sendOrganSlotUpdate(cc, stack);
        }
        return change;
    }
    public static OrganState.Change<Integer> setInt(OrganState state, ChestCavityInstance cc, ItemStack stack,
                                                     String key, int value, IntUnaryOperator clamp, int defaultValue) {
        if (state == null) {
            throw new IllegalArgumentException("state is null");
        }
        OrganState.Change<Integer> change = state.setInt(key, value, clamp, defaultValue);
        if (change.changed()) {
            NetworkUtil.sendOrganSlotUpdate(cc, stack);
        }
        return change;
    }

    public static OrganState.Change<Long> setLong(OrganState state, ChestCavityInstance cc, ItemStack stack,
                                                  String key, long value, LongUnaryOperator clamp, long defaultValue) {
        if (state == null) {
            throw new IllegalArgumentException("state is null");
        }
        OrganState.Change<Long> change = state.setLong(key, value, clamp, defaultValue);
        if (change.changed()) {
            NetworkUtil.sendOrganSlotUpdate(cc, stack);
        }
        return change;
    }

    public static OrganState.Change<Double> setDouble(OrganState state, ChestCavityInstance cc, ItemStack stack,
                                                      String key, double value, DoubleUnaryOperator clamp, double defaultValue) {
        if (state == null) {
            throw new IllegalArgumentException("state is null");
        }
        OrganState.Change<Double> change = state.setDouble(key, value, clamp, defaultValue);
        if (change.changed()) {
            NetworkUtil.sendOrganSlotUpdate(cc, stack);
        }
        return change;
    }

    public static OrganState.Change<Boolean> setBoolean(OrganState state, ChestCavityInstance cc, ItemStack stack,
                                                        String key, boolean value, boolean defaultValue) {
        if (state == null) {
            throw new IllegalArgumentException("state is null");
        }
        OrganState.Change<Boolean> change = state.setBoolean(key, value, defaultValue);
        if (change.changed()) {
            NetworkUtil.sendOrganSlotUpdate(cc, stack);
        }
        return change;
    }

}
