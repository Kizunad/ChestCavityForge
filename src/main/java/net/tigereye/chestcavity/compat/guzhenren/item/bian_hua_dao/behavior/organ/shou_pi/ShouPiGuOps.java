package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.organ.shou_pi;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.tuning.ShouPiGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;

/** 兽皮蛊聚合入口。 */
public final class ShouPiGuOps {
  private ShouPiGuOps() {}

  public static final ResourceLocation ORGAN_ID = ShouPiGuTuning.ORGAN_ID;

  public static MultiCooldown cooldown(ChestCavityInstance cc, ItemStack organ, OrganState state) {
    return ShouPiGuCalculator.cooldown(cc, organ, state);
  }

  public static void ensureStage(OrganState state, ChestCavityInstance cc, ItemStack organ) {
    ShouPiGuCalculator.ensureStage(state, cc, organ);
  }

  public static ShouPiGuTuning.TierParameters tierParameters(OrganState state) {
    return ShouPiGuCalculator.tierParameters(state);
  }

  public static ItemStack findOrgan(ChestCavityInstance cc) {
    return ShouPiGuCalculator.findOrgan(cc);
  }

  public static boolean hasOrgan(
      ChestCavityInstance cc, net.minecraft.resources.ResourceLocation organId) {
    return ShouPiGuCalculator.hasOrgan(cc, organId);
  }

  public static boolean isOrgan(ItemStack stack, net.minecraft.resources.ResourceLocation organId) {
    return ShouPiGuCalculator.isOrgan(stack, organId);
  }

  public static OrganState resolveState(ItemStack organ) {
    return ShouPiGuCalculator.resolveState(organ);
  }

  public static void applyRollCounter(
      LivingEntity player, int resistanceDurationTicks, int resistanceAmplifier) {
    ShouPiGuCalculator.applyRollCounter(player, resistanceDurationTicks, resistanceAmplifier);
  }

  public static void applyRollSlow(
      ServerPlayer player, int slowDurationTicks, int slowAmplifier, double slowRadius) {
    ShouPiGuCalculator.applyRollSlow(player, slowDurationTicks, slowAmplifier, slowRadius);
  }

  public static void dealCrashDamage(
      ServerPlayer player, Vec3 center, double damage, double radius) {
    ShouPiGuCalculator.dealCrashDamage(player, center, damage, radius);
  }

  public static void applyStoicSlow(LivingEntity player) {
    ShouPiGuCalculator.applyStoicSlow(player);
  }

  public static void applyShield(LivingEntity player, double shieldAmount) {
    ShouPiGuCalculator.applyShield(player, shieldAmount);
  }

  public static double resolveSoftPool(OrganState state, long now) {
    return ShouPiGuCalculator.resolveSoftPool(state, now);
  }
}
