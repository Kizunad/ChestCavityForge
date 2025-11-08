package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx;

import java.util.function.IntConsumer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps;
import net.tigereye.chestcavity.engine.fx.FxEngine;
import net.tigereye.chestcavity.engine.fx.FxTrackSpec;
import net.tigereye.chestcavity.engine.fx.StopReason;
import net.tigereye.chestcavity.registration.CCSoundEvents;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

/**
 * 碎刃蛊特效与提示接口。
 *
 * <p>提供视觉/音效入口，便于后续扩展与调参。
 */
public final class SuiRenGuFx {

  private SuiRenGuFx() {}

  /**
   * 播放冷却提示（客户端 toast）。
   *
   * @param player 玩家
   * @param abilityId 技能 ID
   * @param readyAtTick 冷却结束时间
   * @param nowTick 当前时间
   */
  public static void scheduleCooldownToast(
      ServerPlayer player, ResourceLocation abilityId, long readyAtTick, long nowTick) {
    ActiveSkillRegistry.scheduleReadyToast(player, abilityId, readyAtTick, nowTick);
  }

  /**
   * 播放飞剑碎裂特效（粒子与音效）。
   *
   * <p>建议：在飞剑位置生成"刃落如霰"粒子效果。
   *
   * @param player 玩家
   * @param sword 被牺牲的飞剑
   */
  public static void playShardBurst(ServerPlayer player, FlyingSwordEntity sword) {
    if (!(player.level() instanceof ServerLevel level)) return;

    Vec3 swordPos = sword.position();

    // ===== 立即播放：碎裂音效 + 初始爆发粒子 =====
    // 音效：组合使用多个原版音效创造碎裂感
    level.playSound(
        null,
        swordPos.x,
        swordPos.y,
        swordPos.z,
        CCSoundEvents.CUSTOM_FLYINGSWORD_SHATTER.get(),
        SoundSource.PLAYERS,
        1.0F,
        1.0F);

    // 初始爆发：剑刃碎片向四周飞溅
    for (int i = 0; i < 32; i++) {
      double angle = (Math.PI * 2.0 * i) / 32.0;
      double radius = 0.3 + Math.random() * 0.2;
      double dx = Math.cos(angle) * radius;
      double dz = Math.sin(angle) * radius;
      double dy = (Math.random() - 0.5) * 0.3;

      // 剑气碎片（扫击粒子）
      level.sendParticles(
          ParticleTypes.SWEEP_ATTACK,
          swordPos.x + dx * 0.5,
          swordPos.y + 0.5 + dy,
          swordPos.z + dz * 0.5,
          1,
          dx * 0.8,
          dy * 0.5 + 0.1,
          dz * 0.8,
          0.3);

      // 剑光碎片（暴击粒子）
      if (i % 2 == 0) {
        level.sendParticles(
            ParticleTypes.CRIT,
            swordPos.x,
            swordPos.y + 0.5,
            swordPos.z,
            2,
            dx * 0.5,
            dy * 0.3,
            dz * 0.5,
            0.2);
      }
    }

    // 中心冲击波（云粒子）
    level.sendParticles(
        ParticleTypes.CLOUD, swordPos.x, swordPos.y + 0.5, swordPos.z, 12, 0.3, 0.3, 0.3, 0.15);

    // ===== 延迟特效：使用 FxEngine 实现碎片飞舞与消散 =====
    final int shardFxTtl = 60; // 3 秒
    final int shardFxInterval = 2;
    String trackId =
        "suirengu-shard-burst-" + player.getUUID() + "-" + System.currentTimeMillis();

    FxTrackSpec spec =
        FxTrackSpec.builder(trackId)
            .ttl(shardFxTtl)
            .tickInterval(shardFxInterval)
            .owner(player.getUUID())
            .level(level)
            .onTick(
                (lvl, elapsed) -> emitShardBurstTrail(lvl, swordPos, elapsed, shardFxTtl))
            .build();

    // 调度到 FxEngine（若不可用则手动 fallback）
    scheduleFxWithFallback(
        level,
        spec,
        elapsed -> emitShardBurstTrail(level, swordPos, elapsed, shardFxTtl),
        null);
  }

  /**
   * 播放技能激活完成特效。
   *
   * @param player 玩家
   * @param swordCount 牺牲的飞剑数量
   * @param totalDelta 获得的总道痕增幅
   */
  public static void playActivationComplete(ServerPlayer player, int swordCount, int totalDelta) {
    if (!(player.level() instanceof ServerLevel level)) return;

    Vec3 playerPos = player.position();

    // ===== 立即播放：道痕法阵 + 激活音效 =====
    // 音效：成功激活（使用原版的铁砧落地音效，表示增幅生效）
    level.playSound(
        null,
        playerPos.x,
        playerPos.y,
        playerPos.z,
        net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
        SoundSource.PLAYERS,
        0.8F,
        1.2F);

    // 脚下法阵：三层圆环（类似飞剑召唤阵，但更加华丽）
    double y = playerPos.y + 0.05;
    double[] radii = new double[] {0.8, 1.3, 1.8};
    int samples = 48;

    for (double r : radii) {
      for (int i = 0; i < samples; i++) {
        double a = (i / (double) samples) * Math.PI * 2.0;
        double x = playerPos.x + Math.cos(a) * r;
        double z = playerPos.z + Math.sin(a) * r;

        // 主环（附魔粒子）
        level.sendParticles(ParticleTypes.ENCHANT, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);

        // 强调节点（每隔一定间隔）
        if (i % 8 == 0) {
          level.sendParticles(
              ParticleTypes.END_ROD, x, y + 0.05, z, 2, 0.02, 0.05, 0.02, 0.01);
        }
      }
    }

    // 放射状能量纹路（从中心向外）
    int spokes = 8;
    for (int s = 0; s < spokes; s++) {
      double a = (s / (double) spokes) * Math.PI * 2.0;
      double cos = Math.cos(a);
      double sin = Math.sin(a);
      for (int t = 0; t <= 10; t++) {
        double r = radii[0] + (radii[2] - radii[0]) * (t / 10.0);
        double x = playerPos.x + cos * r;
        double z = playerPos.z + sin * r;
        level.sendParticles(
            ParticleTypes.SOUL_FIRE_FLAME, x, y + 0.02, z, 1, 0.0, 0.0, 0.0, 0.0);
      }
    }

    // 中心能量汇聚（根据剑数量和增幅调整强度）
    int burstIntensity = Math.min(swordCount * 2, 20);
    for (int i = 0; i < burstIntensity; i++) {
      double angle = (Math.PI * 2.0 * i) / burstIntensity;
      double radius = 0.3;
      level.sendParticles(
          ParticleTypes.TOTEM_OF_UNDYING,
          playerPos.x + Math.cos(angle) * radius,
          playerPos.y + 1.0,
          playerPos.z + Math.sin(angle) * radius,
          1,
          0.0,
          0.2,
          0.0,
          0.05);
    }

    // ===== 延迟特效：使用 FxEngine 实现能量上涌与融入 =====
    final int activationFxTtl = 80; // 4 秒
    final int activationFxInterval = 1;
    String trackId =
        "suirengu-activation-" + player.getUUID() + "-" + System.currentTimeMillis();

    FxTrackSpec spec =
        FxTrackSpec.builder(trackId)
            .ttl(activationFxTtl)
            .tickInterval(activationFxInterval)
            .owner(player.getUUID())
            .level(level)
            .onTick(
                (lvl, elapsed) ->
                    emitActivationTimelineTick(
                        lvl, playerPos, swordCount, elapsed, activationFxTtl))
            .onStop(
                (lvl, reason) -> {
                  if (reason == StopReason.TTL_EXPIRED) {
                    emitActivationCompletionBurst(lvl, playerPos);
                  }
                })
            .build();

    // 调度到 FxEngine（若不可用则手动 fallback）
    scheduleFxWithFallback(
        level,
        spec,
        elapsed ->
            emitActivationTimelineTick(level, playerPos, swordCount, elapsed, activationFxTtl),
        () -> emitActivationCompletionBurst(level, playerPos));
  }

  /**
   * 播放 buff 结束特效。
   *
   * @param player 玩家
   */
  public static void playBuffExpired(ServerPlayer player) {
    // TODO: 实现 buff 结束提示
    // 建议：淡出粒子效果或音效提示
  }

  private static void emitShardBurstTrail(
      ServerLevel level, Vec3 swordPos, int elapsedTicks, int ttlTicks) {
    if (level == null || swordPos == null || ttlTicks <= 0) {
      return;
    }

    double progress = Math.min(1.0, elapsedTicks / (double) ttlTicks);

    double expandRadius = 1.0 + progress * 2.0;
    double riseHeight = progress * 1.5;
    int particleCount = (int) (16 * (1.0 - progress * 0.7));

    for (int i = 0; i < particleCount; i++) {
      double angle = (Math.PI * 2.0 * i) / particleCount + elapsedTicks * 0.1;
      double spiralRadius = expandRadius * (0.8 + Math.random() * 0.4);

      double x = swordPos.x + Math.cos(angle) * spiralRadius;
      double z = swordPos.z + Math.sin(angle) * spiralRadius;
      double y = swordPos.y + 0.5 + riseHeight + Math.sin(elapsedTicks * 0.5) * 0.2;

      level.sendParticles(ParticleTypes.ENCHANT, x, y, z, 1, 0.0, 0.05, 0.0, 0.01);

      if (i % 4 == 0) {
        level.sendParticles(
            ParticleTypes.END_ROD,
            x,
            y + 0.1,
            z,
            1,
            (Math.random() - 0.5) * 0.1,
            0.08,
            (Math.random() - 0.5) * 0.1,
            0.02);
      }
    }

    if (elapsedTicks % 5 == 0 && progress < 0.6) {
      for (int i = 0; i < 4; i++) {
        double angle = (Math.PI * 2.0 * i) / 4.0 + elapsedTicks * 0.15;
        double r = 0.3 * (1.0 - progress);
        level.sendParticles(
            ParticleTypes.SOUL_FIRE_FLAME,
            swordPos.x + Math.cos(angle) * r,
            swordPos.y + 0.5,
            swordPos.z + Math.sin(angle) * r,
            1,
            0.0,
            0.1,
            0.0,
            0.02);
      }
    }
  }

  private static void emitActivationTimelineTick(
      ServerLevel level, Vec3 playerPos, int swordCount, int elapsedTicks, int totalTicks) {
    if (level == null || playerPos == null || totalTicks <= 0) {
      return;
    }

    // 阶段 1：能量从地面向上螺旋上升
    if (elapsedTicks < totalTicks / 2) {
      double spiralProgress = elapsedTicks / (totalTicks / 2.0);
      double currentHeight = spiralProgress * 2.5;
      double spiralRadius = 1.5 * (1.0 - spiralProgress * 0.5);

      int spiralCount = Math.min(Math.max(1, swordCount), 8);
      for (int i = 0; i < spiralCount; i++) {
        double angle = (Math.PI * 2.0 * i) / spiralCount + elapsedTicks * 0.3;
        double x = playerPos.x + Math.cos(angle) * spiralRadius;
        double z = playerPos.z + Math.sin(angle) * spiralRadius;
        double spiralY = playerPos.y + currentHeight;

        level.sendParticles(ParticleTypes.ENCHANT, x, spiralY, z, 1, 0.0, 0.05, 0.0, 0.01);

        if (i % 2 == 0) {
          level.sendParticles(
              ParticleTypes.SOUL_FIRE_FLAME, x, spiralY, z, 1, 0.0, 0.08, 0.0, 0.02);
        }
      }

      if (elapsedTicks % 10 == 0) {
        for (int i = 0; i < 12; i++) {
          double angle = (Math.PI * 2.0 * i) / 12.0;
          double r = 1.2;
          level.sendParticles(
              ParticleTypes.END_ROD,
              playerPos.x + Math.cos(angle) * r,
              playerPos.y + 0.1,
              playerPos.z + Math.sin(angle) * r,
              1,
              0.0,
              0.1,
              0.0,
              0.02);
        }
      }
      return;
    }

    // 阶段 2：能量汇聚融入玩家体内
    double absorbProgress = (elapsedTicks - totalTicks / 2.0) / (totalTicks / 2.0);
    int absorbParticleCount = (int) (12 * (1.0 - absorbProgress * 0.7));
    for (int i = 0; i < absorbParticleCount; i++) {
      double angle = (Math.PI * 2.0 * i) / Math.max(1, absorbParticleCount) + elapsedTicks * 0.2;
      double currentRadius = 2.0 * (1.0 - absorbProgress);
      double height = 1.0 + Math.sin(elapsedTicks * 0.2 + i * 0.5) * 0.3;

      double x = playerPos.x + Math.cos(angle) * currentRadius;
      double z = playerPos.z + Math.sin(angle) * currentRadius;
      double absorbY = playerPos.y + height;

      double dx = (playerPos.x - x) * 0.15;
      double dz = (playerPos.z - z) * 0.15;
      double dy = (playerPos.y + 1.0 - absorbY) * 0.1;

      level.sendParticles(ParticleTypes.ENCHANTED_HIT, x, absorbY, z, 1, dx, dy, dz, 0.1);

      if (i % 3 == 0) {
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, absorbY, z, 1, dx, dy, dz, 0.08);
      }
    }

    if (elapsedTicks % 5 == 0) {
      for (int i = 0; i < 8; i++) {
        double angle = (Math.PI * 2.0 * i) / 8.0 + elapsedTicks * 0.15;
        double r = 0.5 + Math.sin(elapsedTicks * 0.3) * 0.2;
        level.sendParticles(
            ParticleTypes.END_ROD,
            playerPos.x + Math.cos(angle) * r,
            playerPos.y + 1.0,
            playerPos.z + Math.sin(angle) * r,
            1,
            0.0,
            0.05,
            0.0,
            0.02);
      }
    }
  }

  private static void emitActivationCompletionBurst(ServerLevel level, Vec3 playerPos) {
    if (level == null || playerPos == null) {
      return;
    }

    for (int i = 0; i < 20; i++) {
      double angle = (Math.PI * 2.0 * i) / 20.0;
      double dx = Math.cos(angle) * 0.3;
      double dz = Math.sin(angle) * 0.3;
      level.sendParticles(
          ParticleTypes.FIREWORK,
          playerPos.x,
          playerPos.y + 1.0,
          playerPos.z,
          3,
          dx,
          0.3,
          dz,
          0.15);
    }

    level.playSound(
        null,
        playerPos.x,
        playerPos.y,
        playerPos.z,
        net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
        SoundSource.PLAYERS,
        0.6F,
        1.5F);
  }

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
    runManualTimeline(level, spec.getTtlTicks(), spec.getTickInterval(), fallbackTick, fallbackStop);
  }

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
