package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.runtime.shou_pi;

import java.util.OptionalDouble;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.common.organ.shou_pi.ShouPiGuCalculator;
import net.tigereye.chestcavity.compat.common.organ.shou_pi.ShouPiGuOps;
import net.tigereye.chestcavity.compat.common.tuning.ShouPiGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.fx.ShouPiFx;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.messages.FailNotifier;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.messages.ShouPiMessages;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.NetworkUtil;

/** 兽皮蛊运行时公共逻辑。 */
public final class ShouPiRuntime {

  private static final ResourceLocation DRUM_KNOCKBACK_RESISTANCE_ID =
      ResourceLocation.fromNamespaceAndPath("chestcavity", "shou_pi_drum_knockback_resistance");

  private ShouPiRuntime() {}

  public static void activateDrum(ServerPlayer player, ChestCavityInstance cc, long now) {
    if (player == null || cc == null) {
      return;
    }
    ItemStack organ = ShouPiGuOps.findOrgan(cc);
    if (organ.isEmpty()) {
      FailNotifier.notifyThrottled(player,
          Component.translatable(ShouPiMessages.FAIL_MISSING_ORGAN));
      return;
    }
    OrganState state = ShouPiGuOps.resolveState(organ);
    MultiCooldown cooldown = ShouPiGuOps.cooldown(cc, organ, state);
    MultiCooldown.Entry entry =
        cooldown.entry(ShouPiGuTuning.KEY_ACTIVE_DRUM_READY).withDefault(0L);
    if (!entry.isReady(now)) {
      FailNotifier.notifyThrottled(player,
          Component.translatable(ShouPiMessages.FAIL_ON_COOLDOWN));
      return;
    }

    // Snapshot scaling
    var scaling = ShouPiGuCalculator.calculateScaling(player, ShouPiGuTuning.ACTIVE_DRUM_ID);
    double finalCost = ShouPiGuTuning.ACTIVE_DRUM_BASE_COST * scaling.costMultiplier();

    OptionalDouble consumed = ResourceOps.tryConsumeScaledZhenyuan(player, finalCost);
    if (consumed.isEmpty()) {
      FailNotifier.notifyThrottled(player,
          Component.translatable(ShouPiMessages.FAIL_NO_ZHENYUAN));
      return;
    }

    long finalCooldown =
        (long) (ShouPiGuTuning.ACTIVE_DRUM_COOLDOWN_TICKS * scaling.cooldownMultiplier());

    entry.setReadyAt(now + finalCooldown);
    state.setLong(
        ShouPiGuTuning.KEY_ACTIVE_DRUM_EXPIRE,
        now + ShouPiGuTuning.ACTIVE_DRUM_DURATION_TICKS,
        value -> Math.max(0L, value),
        0L);
    applyDrumBuff(player);
    ShouPiFx.playDrumSound(player.serverLevel(), player);
    ShouPiFx.drumBurst(player.serverLevel(), player.getX(), player.getY(), player.getZ());
    ActiveSkillRegistry.scheduleReadyToast(
        player, ShouPiGuTuning.ACTIVE_DRUM_ID, entry.getReadyTick(), now);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
  }

  private static void applyDrumBuff(ServerPlayer player) {
    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
        ShouPiGuTuning.ACTIVE_DRUM_DURATION_TICKS, 0));
    AttributeInstance knockbackResistance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
    if (knockbackResistance != null) {
      knockbackResistance.removeModifier(DRUM_KNOCKBACK_RESISTANCE_ID);
      knockbackResistance.addTransientModifier(
          new AttributeModifier(DRUM_KNOCKBACK_RESISTANCE_ID,
              ShouPiGuTuning.ACTIVE_DRUM_KNOCKBACK_RESIST,
              AttributeModifier.Operation.ADD_VALUE));
    }
  }

  public static void activateRoll(ServerPlayer player, ChestCavityInstance cc, long now) {
    if (player == null || cc == null) {
      return;
    }
    ItemStack organ = ShouPiGuOps.findOrgan(cc);
    if (organ.isEmpty()) {
      FailNotifier.notifyThrottled(player,
          Component.translatable(ShouPiMessages.FAIL_MISSING_ORGAN));
      return;
    }
    OrganState state = ShouPiGuOps.resolveState(organ);
    MultiCooldown cooldown = ShouPiGuOps.cooldown(cc, organ, state);
    MultiCooldown.Entry entry =
        cooldown.entry(ShouPiGuTuning.KEY_ROLL_READY).withDefault(0L);
    if (!entry.isReady(now)) {
      FailNotifier.notifyThrottled(player,
          Component.translatable(ShouPiMessages.FAIL_ON_COOLDOWN));
      return;
    }
    var scaling = ShouPiGuCalculator.calculateScaling(player, ShouPiGuTuning.ACTIVE_ROLL_ID);
    double finalCost = ShouPiGuTuning.ACTIVE_ROLL_BASE_COST * scaling.costMultiplier();

    OptionalDouble consumed = ResourceOps.tryConsumeScaledZhenyuan(player, finalCost);
    if (consumed.isEmpty()) {
      FailNotifier.notifyThrottled(player,
          Component.translatable(ShouPiMessages.FAIL_NO_ZHENYUAN));
      return;
    }

    long finalCooldown =
        (long) (ShouPiGuTuning.ACTIVE_ROLL_COOLDOWN_TICKS * scaling.cooldownMultiplier());
    entry.setReadyAt(now + finalCooldown);
    state.setLong(
        ShouPiGuTuning.KEY_ROLL_EXPIRE,
        now + ShouPiGuTuning.ROLL_DAMAGE_WINDOW_TICKS,
        value -> Math.max(0L, value),
        0L);
    player.move(net.minecraft.world.entity.MoverType.SELF, player.getLookAngle().scale(ShouPiGuTuning.ROLL_DISTANCE));
    ShouPiFx.playRollSound(player.serverLevel(), player);
    ActiveSkillRegistry.scheduleReadyToast(
        player, ShouPiGuTuning.ACTIVE_ROLL_ID, entry.getReadyTick(), now);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
  }

  public static void activateCrash(ServerPlayer player, ChestCavityInstance cc, long now) {
    if (player == null || cc == null) {
      return;
    }
    ItemStack organ = ShouPiGuOps.findOrgan(cc);
    if (organ.isEmpty()) {
      FailNotifier.notifyThrottled(player,
          Component.translatable(ShouPiMessages.FAIL_MISSING_ORGAN));
      return;
    }
    OrganState state = ShouPiGuOps.resolveState(organ);
    MultiCooldown cooldown = ShouPiGuOps.cooldown(cc, organ, state);
    MultiCooldown.Entry entry =
        cooldown.entry(ShouPiGuTuning.KEY_CRASH_READY).withDefault(0L);
    if (!entry.isReady(now)) {
      FailNotifier.notifyThrottled(player,
          Component.translatable(ShouPiMessages.FAIL_ON_COOLDOWN));
      return;
    }
    var scaling = ShouPiGuCalculator.calculateScaling(player, ShouPiGuTuning.ACTIVE_CRASH_ID);
    double finalCost = ShouPiGuTuning.SYNERGY_CRASH_BASE_COST * scaling.costMultiplier();

    OptionalDouble consumed = ResourceOps.tryConsumeScaledZhenyuan(player, finalCost);
    if (consumed.isEmpty()) {
      FailNotifier.notifyThrottled(player,
          Component.translatable(ShouPiMessages.FAIL_NO_ZHENYUAN));
      return;
    }

    long finalCooldown =
        (long) (ShouPiGuTuning.SYNERGY_CRASH_COOLDOWN_TICKS * scaling.cooldownMultiplier());
    entry.setReadyAt(now + finalCooldown);
    state.setLong(
        ShouPiGuTuning.KEY_CRASH_IMMUNE,
        now + ShouPiGuTuning.CRASH_IMMUNE_TICKS,
        value -> Math.max(0L, value),
        0L);
    player.move(net.minecraft.world.entity.MoverType.SELF, player.getLookAngle().scale(ShouPiGuTuning.CRASH_DISTANCE));
    ShouPiFx.playCrashSound(player.serverLevel(), player);
    ShouPiFx.crashBurst(player.serverLevel(), player.getX(), player.getY(), player.getZ());
    ShouPiGuCalculator.dealCrashDamage(player, player.position(), 10, ShouPiGuTuning.CRASH_SPLASH_RADIUS);
    ActiveSkillRegistry.scheduleReadyToast(
        player, ShouPiGuTuning.ACTIVE_CRASH_ID, entry.getReadyTick(), now);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
  }

  public static void onSlowTick(ServerPlayer player, ChestCavityInstance cc, ItemStack organ,
      long now) {
    OrganState state = ShouPiGuOps.resolveState(organ);
    if (state.getLong(ShouPiGuTuning.KEY_SOFT_POOL_EXPIRE, 0L) < now) {
      state.setDouble(ShouPiGuTuning.KEY_SOFT_POOL_VALUE, 0);
    }
    if (state.getLong(ShouPiGuTuning.KEY_STOIC_LOCK_UNTIL, 0L) > 0
        && state.getLong(ShouPiGuTuning.KEY_STOIC_LOCK_UNTIL, 0L) < now) {
      state.setLong(ShouPiGuTuning.KEY_STOIC_LOCK_UNTIL, 0L);
    }
    if (state.getLong(ShouPiGuTuning.KEY_STOIC_ACTIVE_UNTIL, 0L) > now) {
      ShouPiGuCalculator.applyStoicSlow(player);
    }
  }

  public static float onHurt(ServerPlayer player, ChestCavityInstance cc, ItemStack organ,
      net.minecraft.world.damagesource.DamageSource source, float damage, long now) {
    OrganState state = ShouPiGuOps.resolveState(organ);
    // Roll and Crash immunity windows
    if (state.getLong(ShouPiGuTuning.KEY_ROLL_EXPIRE, 0L) > now) {
      damage *= (1.0 - ShouPiGuTuning.ROLL_DAMAGE_REDUCTION);
    }
    if (state.getLong(ShouPiGuTuning.KEY_CRASH_IMMUNE, 0L) > now) {
      damage = 0;
    }

    // Soft reflection pool reduction
    double reflected = ShouPiGuCalculator.resolveSoftPool(state, now);
    if (reflected > 0) {
      damage -= reflected;
      if (damage < 0) {
        damage = 0;
      }
    }
    // Stoic trigger and shield
    if (state.getLong(ShouPiGuTuning.KEY_STOIC_LOCK_UNTIL, 0L) < now) {
      int stacks = state.getInt(ShouPiGuTuning.KEY_STOIC_STACKS, 0) + 1;
      if (stacks >= ShouPiGuTuning.STOIC_MAX_STACKS) {
        var tierParams = ShouPiGuCalculator.tierParameters(state);
        ShouPiGuCalculator.applyShield(player, tierParams.stoicShield());
        state.setLong(ShouPiGuTuning.KEY_STOIC_LOCK_UNTIL, now + tierParams.lockTicks());
        state.setInt(ShouPiGuTuning.KEY_STOIC_STACKS, 0);
      } else {
        state.setInt(ShouPiGuTuning.KEY_STOIC_STACKS, stacks);
      }
    }

    return damage;
  }

  public static void onHit(ServerPlayer player, ChestCavityInstance cc, ItemStack organ,
      net.minecraft.world.entity.LivingEntity target, float damage, long now) {
    OrganState state = ShouPiGuOps.resolveState(organ);
    MultiCooldown cooldown = ShouPiGuOps.cooldown(cc, organ, state);
    MultiCooldown.Entry entry =
        cooldown.entry(ShouPiGuTuning.KEY_SOFT_THORNS_WINDOW).withDefault(0L);
    if (entry.isReady(now)) {
      double currentPool = state.getDouble(ShouPiGuTuning.KEY_SOFT_POOL_VALUE, 0);
      state.setDouble(ShouPiGuTuning.KEY_SOFT_POOL_VALUE, currentPool + damage / 2);
      state.setLong(ShouPiGuTuning.KEY_SOFT_POOL_EXPIRE, now + ShouPiGuTuning.SOFT_POOL_WINDOW_TICKS);
      entry.setReadyAt(now + ShouPiGuTuning.SOFT_PROJECTILE_COOLDOWN_TICKS);
    }
  }
}
