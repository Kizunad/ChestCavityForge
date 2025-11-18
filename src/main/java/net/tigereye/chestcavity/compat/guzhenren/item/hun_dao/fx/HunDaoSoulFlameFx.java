package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.fx;

import com.mojang.logging.LogUtils;

import java.util.function.IntConsumer;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps;
import net.tigereye.chestcavity.engine.fx.FxContext;
import net.tigereye.chestcavity.engine.fx.FxEngine;
import net.tigereye.chestcavity.engine.fx.FxRegistry;
import net.tigereye.chestcavity.registration.CCSoundEvents;

import org.slf4j.Logger;

/**
 * 魂焰 DoT 特效工具类。
 *
 * <p>优先使用 FxEngine/FxRegistry 播放数据驱动特效，若资源缺失或 FxEngine 关闭则退化为内置粒子。
 */
public final class HunDaoSoulFlameFx {

  private static final Logger LOGGER = LogUtils.getLogger();

  private HunDaoSoulFlameFx() {}

  /**
   * 播放魂焰 DoT 特效（粒子 + 音效）。
   *
   * @param target 受到魂焰影响的目标
   * @param fxId 数据驱动 FX ID
   * @param durationSeconds 持续时间（秒）
   */
  public static void playSoulFlame(
      LivingEntity target, ResourceLocation fxId, int durationSeconds) {
    if (target == null
        || fxId == null
        || durationSeconds <= 0
        || !(target.level() instanceof ServerLevel level)) {
      return;
    }

    Vec3 targetPos = target.position();

    // 立即播放魂焰音效，确保玩家能获得听觉反馈
    level.playSound(
        null,
        targetPos.x,
        targetPos.y,
        targetPos.z,
        CCSoundEvents.CUSTOM_SOULBEAST_DOT.get(),
        SoundSource.HOSTILE,
        0.6F,
        1.0F);

    boolean fxDispatched = dispatchFx(level, target, fxId, durationSeconds);
    if (fxDispatched) {
      return;
    }

    // 数据驱动 FX 不可用，使用内置粒子方案兜底
    LOGGER.debug(
        "[hun_dao][soul_flame_fx] FX {} unavailable (engineDisabled={} registered={}), using fallback particles",
        fxId,
        !FxEngine.getConfig().enabled,
        FxEngine.registry().isRegistered(fxId.toString()));
    playFallbackParticles(level, target, durationSeconds);
  }

  private static boolean dispatchFx(
      ServerLevel level, LivingEntity target, ResourceLocation fxId, int durationSeconds) {
    if (!FxEngine.getConfig().enabled) {
      return false;
    }
    FxRegistry registry = FxEngine.registry();
    String fxKey = fxId.toString();
    if (!registry.isRegistered(fxKey)) {
      return false;
    }
    FxContext context =
        FxContext.builder(level)
            .owner(target.getUUID())
            .position(target.position())
            .customParam("targetId", target.getId())
            .customParam("durationTicks", durationSeconds * 20)
            .customParam("targetHeight", target.getBbHeight())
            .customParam("targetWidth", target.getBbWidth())
            .build();
    return registry.play(fxKey, context) != null;
  }

  private static void playFallbackParticles(
      ServerLevel level, LivingEntity target, int durationSeconds) {
    emitSoulFlameParticles(level, target, 0);
    final int ttlTicks = Math.max(1, durationSeconds * 20);
    final int interval = 5;
    scheduleFallbackTimeline(
        level,
        ttlTicks,
        interval,
        elapsed -> emitSoulFlameParticles(level, target, elapsed),
        () -> {});
  }

  private static void emitSoulFlameParticles(
      ServerLevel level, LivingEntity target, int elapsedTicks) {
    if (level == null || target == null || !target.isAlive()) {
      return;
    }

    Vec3 targetPos = target.position();
    double targetHeight = target.getBbHeight();

    int particleCount = 4;
    for (int i = 0; i < particleCount; i++) {
      double angle = (Math.PI * 2.0 * i) / particleCount + elapsedTicks * 0.1;
      double radius = 0.5 + Math.sin(elapsedTicks * 0.05 + i) * 0.1;
      double height = 0.3 + (Math.sin(elapsedTicks * 0.1 + i * 0.5) + 1.0) * targetHeight * 0.3;

      double px = targetPos.x + Math.cos(angle) * radius;
      double py = targetPos.y + height;
      double pz = targetPos.z + Math.sin(angle) * radius;

      double vx = -Math.cos(angle) * 0.02;
      double vy = 0.05;
      double vz = -Math.sin(angle) * 0.02;

      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 1, vx, vy, vz, 0.02);
      if (i % 2 == 0) {
        level.sendParticles(ParticleTypes.SOUL, px, py, pz, 1, vx, vy, vz, 0.01);
      }
    }

    if (elapsedTicks % 10 == 0) {
      int ringCount = 6;
      for (int i = 0; i < ringCount; i++) {
        double angle = (Math.PI * 2.0 * i) / ringCount;
        double r = 0.7;
        double px = targetPos.x + Math.cos(angle) * r;
        double pz = targetPos.z + Math.sin(angle) * r;

        level.sendParticles(
            ParticleTypes.SOUL_FIRE_FLAME, px, targetPos.y + 0.1, pz, 1, 0.0, 0.05, 0.0, 0.01);
      }
    }
  }

  private static void scheduleFallbackTimeline(
      ServerLevel level, int ttlTicks, int tickInterval, IntConsumer tickConsumer, Runnable stop) {
    if (level == null || ttlTicks <= 0) {
      if (stop != null) {
        stop.run();
      }
      return;
    }
    final int interval = Math.max(1, tickInterval);
    class ManualRunner implements Runnable {
      private int elapsed = 0;

      @Override
      public void run() {
        if (elapsed >= ttlTicks || tickConsumer == null) {
          if (elapsed >= ttlTicks && stop != null) {
            stop.run();
          }
          return;
        }
        tickConsumer.accept(elapsed);
        elapsed += interval;
        TickOps.schedule(level, this, interval);
      }
    }
    TickOps.schedule(level, new ManualRunner(), interval);
  }
}
