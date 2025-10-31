package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.qian_jia_crash.behavior;

import java.util.OptionalDouble;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.common.organ.shou_pi.ShouPiGuOps;
import net.tigereye.chestcavity.compat.common.tuning.ShouPiGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.common.ShouPiComboUtil;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.qian_jia_crash.calculator.ShouPiQianJiaCrashCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.qian_jia_crash.calculator.ShouPiQianJiaCrashCalculator.CrashParameters;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.qian_jia_crash.tuning.ShouPiQianJiaCrashTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TeleportOps;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ComboSkillRegistry;
import net.tigereye.chestcavity.util.NetworkUtil;

/** 嵌甲冲撞——兽皮蛊联动的爆发突进。 */
public final class ShouPiQianJiaCrashBehavior {

  public static final ShouPiQianJiaCrashBehavior INSTANCE = new ShouPiQianJiaCrashBehavior();
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "shou_pi_qian_jia_crash");

  private static final int SLOW_TICKS = 20;
  private static final int SLOW_AMPLIFIER = 0;

  static {
    OrganActivationListeners.register(
        ABILITY_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            activate(player, cc);
          }
        });
  }

  private ShouPiQianJiaCrashBehavior() {}

  private static void activate(ServerPlayer player, ChestCavityInstance cc) {
    if (player == null || cc == null) {
      return;
    }
    ItemStack organ = ShouPiComboUtil.findOrgan(cc).orElse(ItemStack.EMPTY);
    if (organ.isEmpty()) {
      return;
    }
    int synergy = ShouPiComboUtil.countArmorSynergy(cc);
    if (synergy <= 0) {
      return;
    }

    var state = ShouPiComboUtil.resolveState(organ);
    ShouPiGuOps.ensureStage(state, cc, organ);

    MultiCooldown cooldown = ShouPiGuOps.cooldown(cc, organ, state);
    long now = player.level().getGameTime();
    MultiCooldown.Entry entry =
        cooldown.entry(ShouPiGuTuning.KEY_CRASH_READY).withDefault(0L);
    if (!entry.isReady(now)) {
      return;
    }

    OptionalDouble consumed =
        ResourceOps.tryConsumeScaledZhenyuan(player, ShouPiQianJiaCrashTuning.ZHENYUAN_COST);
    if (consumed.isEmpty()) {
      return;
    }

    double softPool = ShouPiGuOps.resolveSoftPool(state, now);
    double attackDamage =
        player.getAttribute(Attributes.ATTACK_DAMAGE) == null
            ? 0.0D
            : player.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
    CrashParameters params =
        ShouPiQianJiaCrashCalculator.compute(softPool, attackDamage, synergy);

    Vec3 look = player.getLookAngle();
    Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
    if (horizontal.lengthSqr() < 1.0E-4D) {
      horizontal = new Vec3(1.0D, 0.0D, 0.0D);
    }
    Vec3 offset = horizontal.normalize().scale(ShouPiGuTuning.CRASH_DISTANCE);
    Vec3 center = TeleportOps.blinkOffset(player, offset).orElse(player.position());

    resetSoftPool(state, cc, organ);
    if (params.damage() > 0.0D) {
      ShouPiGuOps.dealCrashDamage(player, center, params.damage(), params.radius());
      applyCrashSlow(player, center, params.radius());
    }

    OrganStateOps.setLong(
        state,
        cc,
        organ,
        ShouPiGuTuning.KEY_CRASH_IMMUNE,
        now + ShouPiGuTuning.CRASH_IMMUNE_TICKS,
        value -> Math.max(0L, value),
        0L);
    entry.setReadyAt(now + params.cooldown());

    player
        .level()
        .playSound(
            null,
            center.x,
            center.y,
            center.z,
            SoundEvents.ANVIL_FALL,
            SoundSource.PLAYERS,
            0.7F,
            1.1F);

    ComboSkillRegistry.scheduleReadyToast(player, ABILITY_ID, entry.getReadyTick(), now);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
  }

  private static void resetSoftPool(OrganState state, ChestCavityInstance cc, ItemStack organ) {
    OrganStateOps.setDouble(
        state, cc, organ, ShouPiGuTuning.KEY_SOFT_POOL_VALUE, 0.0D, value -> 0.0D, 0.0D);
    OrganStateOps.setLong(
        state, cc, organ, ShouPiGuTuning.KEY_SOFT_POOL_EXPIRE, 0L, value -> 0L, 0L);
  }

  private static void applyCrashSlow(ServerPlayer player, Vec3 center, double radius) {
    if (!(player.level() instanceof ServerLevel serverLevel)) {
      return;
    }
    AABB box = new AABB(center, center).inflate(Math.max(0.5D, radius));
    for (LivingEntity target :
        serverLevel.getEntitiesOfClass(
            LivingEntity.class,
            box,
            entity -> entity != player && entity.isAlive() && !entity.isSpectator())) {
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, SLOW_AMPLIFIER));
    }
  }
}
