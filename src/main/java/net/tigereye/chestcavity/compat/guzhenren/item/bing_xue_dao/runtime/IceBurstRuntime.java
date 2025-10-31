package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.calculator.AoEFalloff;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.calculator.BurstParamOps.BurstParams;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.fx.BingJiFx;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.messages.BingXueMessages;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.engine.reaction.ResidueManager;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowControllerManager;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowProgram;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowProgramRegistry;

/** 执行冰爆：收集目标、结算伤害与缓慢、触发残留域与流。 */
public final class IceBurstRuntime {
  private IceBurstRuntime() {}

  public static void execute(
      ServerPlayer player,
      ChestCavityInstance cc,
      CCConfig.GuzhenrenBingXueDaoConfig.BingJiGuConfig cfg,
      ResourceLocation iceColdEffectId,
      ResourceLocation flowId,
      BurstParams params) {
    if (player == null || player.level().isClientSide()) return;
    ServerLevel server = player.serverLevel();

    // SFX
    BingJiFx.playBurstSound(server, player);

    Vec3 origin = player.position();
    List<LivingEntity> victims = gatherTargets(player, server, params.radius());
    for (LivingEntity target : victims) {
      double distance = Math.sqrt(target.distanceToSqr(origin));
      double falloff = AoEFalloff.linear(distance, params.radius());
      float damage = (float) (params.baseDamage() * falloff);
      if (damage > 0.0f) {
        DamageSource source = player.damageSources().playerAttack(player);
        target.hurt(source, damage);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, params.slowDurationTicks(), params.slowAmplifier(), false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, params.slowDurationTicks(), params.slowAmplifier(), false, true, true));
        ColdEffectOps.applyColdEffect(target, cfg, iceColdEffectId);
      }
    }

    triggerBurstFlow(player, flowId, params.radius(), victims.size(), cfg);

    // 残留域 + 粒子 + 提示
    float residueRadius = (float) Math.max(0.5F, params.radius() * 0.6F);
    int residueDuration = Math.max(40, (int) (cfg.iceEffectDurationTicks * 0.8));
    int slowAmp = Math.max(0, params.slowAmplifier());
    ResidueManager.spawnOrRefreshFrost(server, origin.x, origin.y, origin.z, residueRadius, residueDuration, slowAmp);
    BingJiFx.snowflakeBurst(server, origin.x, origin.y, origin.z);
    player.sendSystemMessage(Component.translatable(BingXueMessages.ICEBURST_RESIDUE));
  }

  private static List<LivingEntity> gatherTargets(LivingEntity user, ServerLevel level, double radius) {
    AABB area = user.getBoundingBox().inflate(radius);
    return level.getEntitiesOfClass(
        LivingEntity.class,
        area,
        target -> target != user && target.isAlive() && !target.isAlliedTo(user));
  }

  private static void triggerBurstFlow(
      ServerPlayer player,
      ResourceLocation flowId,
      double radius,
      int victims,
      CCConfig.GuzhenrenBingXueDaoConfig.BingJiGuConfig cfg) {
    Optional<FlowProgram> programOpt = FlowProgramRegistry.get(flowId);
    if (programOpt.isEmpty()) return;
    ServerLevel level = player.serverLevel();
    FlowController controller = FlowControllerManager.get(player);
    Map<String, String> params = new HashMap<>();
    params.put("burst.radius", String.format(Locale.ROOT, "%.4f", Math.max(0.0D, radius)));
    double victimContribution = Math.max(0, victims) * 0.25D;
    double radiusContribution = Math.max(0.0D, radius - cfg.iceBurstRadius) * 0.1D;
    double scale = Math.max(1.0D, Math.min(6.0D, 1.0D + victimContribution + radiusContribution));
    params.put("burst.scale", String.format(Locale.ROOT, "%.4f", scale));
    controller.start(programOpt.get(), player, 1.0D, params, level.getGameTime(), "bing_ji_gu.iceburst");
  }
}

