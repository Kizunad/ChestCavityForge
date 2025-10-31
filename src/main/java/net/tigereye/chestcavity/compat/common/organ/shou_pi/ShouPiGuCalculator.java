package net.tigereye.chestcavity.compat.common.organ.shou_pi;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.compat.common.tuning.ShouPiGuTuning;

public final class ShouPiGuCalculator {

  private ShouPiGuCalculator() {}

    public static MultiCooldown cooldown(ChestCavityInstance cc, ItemStack organ, OrganState state) {
        return MultiCooldown.builder(state).withSync(cc, organ).build();
    }

    public static void applyDrumBuff(ServerPlayer player) {
        //empty
    }

    public static void ensureStage(OrganState state, ChestCavityInstance cc, ItemStack organ) {
        //empty
    }

    public static ShouPiGuTuning.TierParameters tierParameters(OrganState state) {
        return null;
    }

    public static ItemStack findOrgan(ChestCavityInstance cc) {
        return ItemStack.EMPTY;
    }

    public static boolean hasOrgan(ChestCavityInstance cc, net.minecraft.resources.ResourceLocation organId) {
        return false;
    }

    public static boolean isOrgan(ItemStack stack, net.minecraft.resources.ResourceLocation organId) {
        return false;
    }

    public static OrganState resolveState(ItemStack organ) {
        return null;
    }

    public static void applyRollCounter(LivingEntity player, int resistanceDurationTicks, int resistanceAmplifier) {
        //empty
    }

    public static void applyRollSlow(ServerPlayer player, int slowDurationTicks, int slowAmplifier, double slowRadius) {
        //empty
    }

    public static void dealCrashDamage(ServerPlayer player, net.minecraft.world.phys.Vec3 center, double damage, double radius) {
        //empty
    }

    public static void applyStoicSlow(LivingEntity player) {
        //empty
    }

    public static void applyShield(LivingEntity player, double shieldAmount) {
        //empty
    }

    public static double resolveSoftPool(OrganState state, long now) {
        return 0;
    }
}
