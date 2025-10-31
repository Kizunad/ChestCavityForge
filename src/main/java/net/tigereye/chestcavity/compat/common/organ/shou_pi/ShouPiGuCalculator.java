package net.tigereye.chestcavity.compat.common.organ.shou_pi;

import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.common.tuning.ShouPiGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.skill.effects.SkillEffectBus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public final class ShouPiGuCalculator {

  private ShouPiGuCalculator() {}

  public static MultiCooldown cooldown(ChestCavityInstance cc, ItemStack organ, OrganState state) {
    return MultiCooldown.builder(state).withSync(cc, organ).build();
  }

  public static void ensureStage(OrganState state, ChestCavityInstance cc, ItemStack organ) {
    if (state.getInt(ShouPiGuTuning.KEY_TIER, 0) == 0) {
      // TODO: Replace with quality-based tiering logic
      state.setInt(ShouPiGuTuning.KEY_TIER, 1);
    }
  }

  public static ShouPiGuTuning.TierParameters tierParameters(OrganState state) {
    int tier = state.getInt(ShouPiGuTuning.KEY_TIER, 1);
    return switch (tier) {
      case 2 -> ShouPiGuTuning.TIER2;
      case 3 -> ShouPiGuTuning.TIER3;
      case 4 -> ShouPiGuTuning.TIER4;
      case 5 -> ShouPiGuTuning.TIER5;
      default -> ShouPiGuTuning.TIER1;
    };
  }

  public static ItemStack findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (isOrgan(stack, ShouPiGuTuning.ORGAN_ID)) {
        return stack;
      }
    }
    return ItemStack.EMPTY;
  }

  public static boolean hasOrgan(ChestCavityInstance cc, ResourceLocation organId) {
    if (cc == null || organId == null || cc.inventory == null) {
      return false;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (isOrgan(stack, organId)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isOrgan(ItemStack stack, ResourceLocation organId) {
    if (stack == null || stack.isEmpty() || organId == null) {
      return false;
    }
    ResourceLocation stackId = BuiltInRegistries.ITEM.getKey(stack.getItem());
    return Objects.equals(stackId, organId);
  }

  public static OrganState resolveState(ItemStack organ) {
    return OrganState.of(organ, ShouPiGuTuning.STATE_ROOT_KEY);
  }

  public static ShouPiGuTuning.ScalingParameters calculateScaling(
      ServerPlayer player, ResourceLocation skillId) {
    double daohen =
        SkillEffectBus.consumeMetadata(player, skillId, "shou_pi:daohen_bianhuadao", 0.0);
    double liupai =
        SkillEffectBus.consumeMetadata(player, skillId, "shou_pi:liupai_bianhuadao", 0.0);

    double costMultiplier = 1.0 - (daohen * ShouPiGuTuning.DAO_HEN_COST_SCALE)
        + (liupai * ShouPiGuTuning.LIUPAI_COST_SCALE);
    costMultiplier = Math.max(ShouPiGuTuning.MIN_COST_MULTIPLIER, costMultiplier);

    double cooldownMultiplier = 1.0 + (daohen * ShouPiGuTuning.DAO_HEN_COOLDOWN_SCALE)
        + (liupai * ShouPiGuTuning.LIUPAI_COOLDOWN_SCALE);
    cooldownMultiplier = Math.max(ShouPiGuTuning.MIN_COOLDOWN_MULTIPLIER, cooldownMultiplier);

    // Placeholder for duration and magnitude scaling
    double durationMultiplier = 1.0;
    double magnitudeMultiplier = 1.0;

    return new ShouPiGuTuning.ScalingParameters(costMultiplier, cooldownMultiplier,
        durationMultiplier, magnitudeMultiplier);
  }


  public static void applyRollCounter(LivingEntity player, int resistanceDurationTicks,
      int resistanceAmplifier) {
    // empty
  }

  public static void applyRollSlow(ServerPlayer player, int slowDurationTicks, int slowAmplifier,
      double slowRadius) {
    // empty
  }

  public static void dealCrashDamage(ServerPlayer player, Vec3 center,
      double damage, double radius) {
    AABB area = new AABB(center.x - radius, center.y - radius, center.z - radius,
        center.x + radius, center.y + radius, center.z + radius);
    List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area,
        (entity) -> entity != player && entity.isAlive());
    for (LivingEntity target : targets) {
      target.hurt(player.damageSources().playerAttack(player), (float) damage);
    }
  }

  public static void applyStoicSlow(LivingEntity player) {
    AABB area = new AABB(player.getX() - ShouPiGuTuning.STOIC_SLOW_RADIUS,
        player.getY() - ShouPiGuTuning.STOIC_SLOW_RADIUS,
        player.getZ() - ShouPiGuTuning.STOIC_SLOW_RADIUS,
        player.getX() + ShouPiGuTuning.STOIC_SLOW_RADIUS,
        player.getY() + ShouPiGuTuning.STOIC_SLOW_RADIUS,
        player.getZ() + ShouPiGuTuning.STOIC_SLOW_RADIUS);
    List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area,
        (entity) -> entity != player && entity.isAlive());
    for (LivingEntity target : targets) {
      target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
          net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
          ShouPiGuTuning.STOIC_SLOW_TICKS, ShouPiGuTuning.STOIC_SLOW_AMPLIFIER));
    }
  }

  public static void applyShield(LivingEntity player, double shieldAmount) {
    player.setAbsorptionAmount((float) (player.getAbsorptionAmount() + shieldAmount));
  }

  public static double resolveSoftPool(OrganState state, long now) {
    double poolValue = state.getDouble(ShouPiGuTuning.KEY_SOFT_POOL_VALUE, 0);
    if (poolValue > 0) {
      state.setDouble(ShouPiGuTuning.KEY_SOFT_POOL_VALUE, poolValue / 2);
      return poolValue / 2;
    }
    return 0;
  }
}
