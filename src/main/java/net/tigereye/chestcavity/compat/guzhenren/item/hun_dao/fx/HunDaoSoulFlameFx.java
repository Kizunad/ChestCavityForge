package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.fx;

import java.util.UUID;
import java.util.function.IntConsumer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps;
import net.tigereye.chestcavity.engine.fx.FxEngine;
import net.tigereye.chestcavity.engine.fx.FxTrackSpec;
import net.tigereye.chestcavity.registration.CCSoundEvents;

/**
 * 魂焰 DoT 特效工具类。
 *
 * <p>提供魂焰粒子效果和音效播放，用于魂兽攻击触发的持续伤害反馈。
 */
public final class HunDaoSoulFlameFx {

  private HunDaoSoulFlameFx() {}

  /**
   * 播放魂焰 DoT 特效（粒子 + 音效）。
   *
   * @param target 受到魂焰影响的目标
   * @param durationSeconds 持续时间（秒）
   */
  public static void playSoulFlame(LivingEntity target, int durationSeconds) {
    if (target == null || !(target.level() instanceof ServerLevel level)) {
      return;
    }

    Vec3 targetPos = target.position();

    // ===== 立即播放：魂焰音效 =====
    level.playSound(
        null,
        targetPos.x,
        targetPos.y,
        targetPos.z,
        CCSoundEvents.CUSTOM_SOULBEAST_DOT.get(),
        SoundSource.HOSTILE,
        0.6F,
        1.0F);

    // ===== 立即播放：初始魂焰粒子爆发 =====
    emitSoulFlameParticles(level, target, 0);

    // ===== 延迟特效：使用 FxEngine 实现持续的魂焰效果 =====
    final int soulFlameTtl = durationSeconds * 20; // 转换为 ticks
    final int soulFlameFxInterval = 5; // 每 5 tick 刷新一次粒子
    String trackId = "hun_dao-soul_flame-" + target.getUUID() + "-" + System.currentTimeMillis();

    FxTrackSpec spec =
        FxTrackSpec.builder(trackId)
            .ttl(soulFlameTtl)
            .tickInterval(soulFlameFxInterval)
            .owner(target.getUUID())
            .level(level)
            .onTick((lvl, elapsed) -> emitSoulFlameParticles(lvl, target, elapsed))
            .build();

    // 调度到 FxEngine（若不可用则手动 fallback）
    scheduleFxWithFallback(
        level, spec, elapsed -> emitSoulFlameParticles(level, target, elapsed), null);
  }

  /**
   * 发射魂焰粒子效果（紫色灵魂火焰环绕目标）。
   *
   * @param level 服务端世界
   * @param target 目标实体
   * @param elapsedTicks 已经过的 ticks
   */
  private static void emitSoulFlameParticles(
      ServerLevel level, LivingEntity target, int elapsedTicks) {
    if (level == null || target == null || !target.isAlive()) {
      return;
    }

    Vec3 targetPos = target.position();
    double targetHeight = target.getBbHeight();

    // 魂焰粒子：围绕目标的螺旋上升效果
    int particleCount = 8;
    for (int i = 0; i < particleCount; i++) {
      double angle = (Math.PI * 2.0 * i) / particleCount + elapsedTicks * 0.1;
      double radius = 0.5 + Math.sin(elapsedTicks * 0.05 + i) * 0.1;
      double height = 0.3 + (Math.sin(elapsedTicks * 0.1 + i * 0.5) + 1.0) * targetHeight * 0.3;

      double px = targetPos.x + Math.cos(angle) * radius;
      double py = targetPos.y + height;
      double pz = targetPos.z + Math.sin(angle) * radius;

      // 向中心和向上的速度
      double vx = -Math.cos(angle) * 0.02;
      double vy = 0.05;
      double vz = -Math.sin(angle) * 0.02;

      // 主要粒子：紫色灵魂火焰
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 1, vx, vy, vz, 0.02);

      // 辅助粒子：灵魂粒子（每隔一个）
      if (i % 2 == 0) {
        level.sendParticles(ParticleTypes.SOUL, px, py, pz, 1, vx, vy, vz, 0.01);
      }
    }

    // 脚下环形魂焰（每10 tick一次）
    if (elapsedTicks % 10 == 0) {
      int ringCount = 12;
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

  /**
   * 调度 FX，若 FxEngine 不可用则使用手动 fallback。
   *
   * @param level 服务端世界
   * @param spec FX 轨迹规格
   * @param fallbackTick Fallback tick 消费者
   * @param fallbackStop Fallback 停止动作
   */
  private static void scheduleFxWithFallback(
      ServerLevel level,
      FxTrackSpec spec,
      IntConsumer fallbackTick,
      Runnable fallbackStop) {
    if (spec == null) {
      return;
    }
    String trackId = FxEngine.scheduler().schedule(spec);
    if (trackId != null) {
      return;
    }
    // FxEngine 不可用，使用手动调度
    runManualTimeline(level, spec.getTtlTicks(), spec.getTickInterval(), fallbackTick, fallbackStop);
  }

  /**
   * 手动运行 FX 时间线（Fallback 方案）。
   *
   * @param level 服务端世界
   * @param ttlTicks 总持续 ticks
   * @param tickInterval Tick 间隔
   * @param tickConsumer Tick 消费者
   * @param stopAction 停止动作
   */
  private static void runManualTimeline(
      ServerLevel level,
      int ttlTicks,
      int tickInterval,
      IntConsumer tickConsumer,
      Runnable stopAction) {
    if (level == null || ttlTicks <= 0) {
      if (stopAction != null) {
        stopAction.run();
      }
      return;
    }

    final int interval = Math.max(1, tickInterval);

    class ManualFxRunner implements Runnable {
      private int elapsed = 0;

      @Override
      public void run() {
        if (elapsed >= ttlTicks) {
          if (stopAction != null) {
            stopAction.run();
          }
          return;
        }
        if (tickConsumer != null && elapsed % interval == 0) {
          tickConsumer.accept(elapsed);
        }
        elapsed++;
        TickOps.schedule(level, this, 1);
      }
    }

    TickOps.schedule(level, new ManualFxRunner(), 1);
  }
}
