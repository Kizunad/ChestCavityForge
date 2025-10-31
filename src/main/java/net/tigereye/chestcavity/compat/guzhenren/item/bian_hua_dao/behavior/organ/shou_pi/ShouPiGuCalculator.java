package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.organ.shou_pi;

import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.tuning.ShouPiGuTuning;
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
    if (player == null || resistanceDurationTicks <= 0) {
      return;
    }
    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
        net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,
        resistanceDurationTicks,
        Math.max(0, resistanceAmplifier),
        true,
        false));
  }

  public static void applyRollSlow(ServerPlayer player, int slowDurationTicks, int slowAmplifier,
      double slowRadius) {
    if (player == null || slowDurationTicks <= 0 || slowRadius <= 0) {
      return;
    }
    net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(
        player.getX() - slowRadius,
        player.getY() - slowRadius,
        player.getZ() - slowRadius,
        player.getX() + slowRadius,
        player.getY() + slowRadius,
        player.getZ() + slowRadius);
    java.util.List<net.minecraft.world.entity.LivingEntity> targets =
        player.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, area,
            (entity) -> entity != player && entity.isAlive());
    for (net.minecraft.world.entity.LivingEntity target : targets) {
      target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
          net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
          slowDurationTicks,
          Math.max(0, slowAmplifier)));
    }
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

  /**
   * 计算实际承受伤害，应用翻滚减伤/冲撞免疫与柔反池抵消，并做下限钳制。
   */
  public static float computeIncomingDamage(OrganState state, float baseDamage, long now) {
    float damage = baseDamage;
    if (state.getLong(ShouPiGuTuning.KEY_ROLL_EXPIRE, 0L) > now) {
      damage *= (1.0 - ShouPiGuTuning.ROLL_DAMAGE_REDUCTION);
    }
    if (state.getLong(ShouPiGuTuning.KEY_CRASH_IMMUNE, 0L) > now) {
      return 0F;
    }
    double reflected = resolveSoftPool(state, now);
    if (reflected > 0) {
      damage -= reflected;
    }
    if (damage < 0) {
      damage = 0;
    }
    return damage;
  }

  /**
   * 受击时更新坚忍层数并在阈值时给予护盾与锁定。
   */
  public static void updateStoicStacksAndMaybeShield(LivingEntity player, OrganState state,
      long now) {
    if (state.getLong(ShouPiGuTuning.KEY_STOIC_LOCK_UNTIL, 0L) < now) {
      int stacks = state.getInt(ShouPiGuTuning.KEY_STOIC_STACKS, 0) + 1;
      if (stacks >= ShouPiGuTuning.STOIC_MAX_STACKS) {
        var tierParams = tierParameters(state);
        applyShield(player, tierParams.stoicShield());
        state.setLong(ShouPiGuTuning.KEY_STOIC_LOCK_UNTIL, now + tierParams.lockTicks());
        state.setInt(ShouPiGuTuning.KEY_STOIC_STACKS, 0);
      } else {
        state.setInt(ShouPiGuTuning.KEY_STOIC_STACKS, stacks);
      }
    }
  }

  /**
   * 近战命中时按比例累加柔反池，并刷新过期时间。
   */
  public static void accumulateSoftPoolOnHit(OrganState state, float dealtDamage, long now) {
    double currentPool = state.getDouble(ShouPiGuTuning.KEY_SOFT_POOL_VALUE, 0);
    double add = dealtDamage * ShouPiGuTuning.SOFT_POOL_ON_HIT_FRACTION;
    state.setDouble(ShouPiGuTuning.KEY_SOFT_POOL_VALUE, currentPool + add);
    state.setLong(ShouPiGuTuning.KEY_SOFT_POOL_EXPIRE, now + ShouPiGuTuning.SOFT_POOL_WINDOW_TICKS);
  }
}
