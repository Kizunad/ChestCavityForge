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
        ShouPiGuTuning.ACTIVE_DRUM_DURATION_TICKS,
        ShouPiGuTuning.ACTIVE_DRUM_RESISTANCE_AMPLIFIER));
    AttributeInstance knockbackResistance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
    if (knockbackResistance != null) {
      knockbackResistance.removeModifier(DRUM_KNOCKBACK_RESISTANCE_ID);
      knockbackResistance.addTransientModifier(
          new AttributeModifier(DRUM_KNOCKBACK_RESISTANCE_ID,
              ShouPiGuTuning.ACTIVE_DRUM_KNOCKBACK_RESIST,
              AttributeModifier.Operation.ADD_VALUE));
    }
  }

  // 注意：翻滚与冲撞主动已迁至 combo 行为实现（见 ShouPiRollEvasionBehavior / ShouPiQianJiaCrashBehavior）。
  // 这里不再提供对应的 Runtime 激活动作，避免 item 与 combo 混杂。

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
    damage = ShouPiGuCalculator.computeIncomingDamage(state, damage, now);
    ShouPiGuCalculator.updateStoicStacksAndMaybeShield(player, state, now);
    return damage;
  }

  public static void onHit(ServerPlayer player, ChestCavityInstance cc, ItemStack organ,
      net.minecraft.world.entity.LivingEntity target, float damage, long now) {
    OrganState state = ShouPiGuOps.resolveState(organ);
    MultiCooldown cooldown = ShouPiGuOps.cooldown(cc, organ, state);
    MultiCooldown.Entry entry =
        cooldown.entry(ShouPiGuTuning.KEY_SOFT_THORNS_WINDOW).withDefault(0L);
    if (entry.isReady(now)) {
      ShouPiGuCalculator.accumulateSoftPoolOnHit(state, damage, now);
      entry.setReadyAt(now + ShouPiGuTuning.SOFT_PROJECTILE_COOLDOWN_TICKS);
    }
  }
}
