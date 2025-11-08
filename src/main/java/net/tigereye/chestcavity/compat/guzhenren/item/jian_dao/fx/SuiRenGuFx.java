package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.engine.fx.FxEngine;
import net.tigereye.chestcavity.engine.fx.FxTrackSpec;
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
    String trackId =
        "suirengu-shard-burst-" + player.getUUID() + "-" + System.currentTimeMillis();

    FxTrackSpec spec =
        FxTrackSpec.builder(trackId)
            .ttl(60) // 持续 3 秒（60 ticks）
            .tickInterval(2) // 每 2 tick 生成一次粒子
            .owner(player.getUUID())
            .level(level)
            .onTick(
                (lvl, elapsed) -> {
                  // 进度：0.0 -> 1.0
                  double progress = elapsed / 60.0;

                  // 碎片飞舞：随着时间推移，碎片向外扩散并逐渐上升
                  double expandRadius = 1.0 + progress * 2.0; // 半径从 1.0 扩展到 3.0
                  double riseHeight = progress * 1.5; // 上升高度逐渐增加

                  // 粒子数量随时间递减（模拟消散）
                  int particleCount = (int) (16 * (1.0 - progress * 0.7));

                  for (int i = 0; i < particleCount; i++) {
                    double angle = (Math.PI * 2.0 * i) / particleCount + elapsed * 0.1;
                    double spiralRadius = expandRadius * (0.8 + Math.random() * 0.4);

                    double x = swordPos.x + Math.cos(angle) * spiralRadius;
                    double z = swordPos.z + Math.sin(angle) * spiralRadius;
                    double y = swordPos.y + 0.5 + riseHeight + Math.sin(elapsed * 0.5) * 0.2;

                    // 碎片粒子（附魔粒子 + 光芒）
                    lvl.sendParticles(
                        ParticleTypes.ENCHANT, x, y, z, 1, 0.0, 0.05, 0.0, 0.01);

                    // 随机添加剑气余韵（每隔几个粒子）
                    if (i % 4 == 0) {
                      lvl.sendParticles(
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

                  // 中心残留能量（灵魂粒子，逐渐减弱）
                  if (elapsed % 5 == 0 && progress < 0.6) {
                    for (int i = 0; i < 4; i++) {
                      double angle = (Math.PI * 2.0 * i) / 4.0 + elapsed * 0.15;
                      double r = 0.3 * (1.0 - progress);
                      lvl.sendParticles(
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
                })
            .build();

    // 调度到 FxEngine
    FxEngine.scheduler().schedule(spec);
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
    String trackId =
        "suirengu-activation-" + player.getUUID() + "-" + System.currentTimeMillis();

    FxTrackSpec spec =
        FxTrackSpec.builder(trackId)
            .ttl(80) // 持续 4 秒（80 ticks）
            .tickInterval(1) // 每 tick 生成粒子（更流畅）
            .owner(player.getUUID())
            .level(level)
            .onTick(
                (lvl, elapsed) -> {
                  // 进度：0.0 -> 1.0
                  double progress = elapsed / 80.0;

                  // 阶段 1（0-40 ticks）：能量从地面向上螺旋上升
                  if (elapsed < 40) {
                    double spiralProgress = elapsed / 40.0;
                    double currentHeight = spiralProgress * 2.5; // 上升到玩家头顶
                    double spiralRadius = 1.5 * (1.0 - spiralProgress * 0.5); // 半径逐渐收缩

                    // 多条螺旋上升（根据剑数量调整螺旋数）
                    int spiralCount = Math.min(swordCount, 8);
                    for (int i = 0; i < spiralCount; i++) {
                      double angle =
                          (Math.PI * 2.0 * i) / spiralCount + elapsed * 0.3; // 旋转上升
                      double x = playerPos.x + Math.cos(angle) * spiralRadius;
                      double z = playerPos.z + Math.sin(angle) * spiralRadius;
                      double spiralY = playerPos.y + currentHeight;

                      // 螺旋能量粒子（附魔 + 灵魂火焰）
                      lvl.sendParticles(
                          ParticleTypes.ENCHANT, x, spiralY, z, 1, 0.0, 0.05, 0.0, 0.01);

                      if (i % 2 == 0) {
                        lvl.sendParticles(
                            ParticleTypes.SOUL_FIRE_FLAME,
                            x,
                            spiralY,
                            z,
                            1,
                            0.0,
                            0.08,
                            0.0,
                            0.02);
                      }
                    }

                    // 地面持续发光（脉冲效果）
                    if (elapsed % 10 == 0) {
                      for (int i = 0; i < 12; i++) {
                        double angle = (Math.PI * 2.0 * i) / 12.0;
                        double r = 1.2;
                        lvl.sendParticles(
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
                  }

                  // 阶段 2（40-80 ticks）：能量汇聚融入玩家体内
                  if (elapsed >= 40) {
                    double absorbProgress = (elapsed - 40) / 40.0;

                    // 能量向玩家中心收束
                    int absorbParticleCount = (int) (12 * (1.0 - absorbProgress * 0.7));
                    for (int i = 0; i < absorbParticleCount; i++) {
                      double angle = (Math.PI * 2.0 * i) / absorbParticleCount + elapsed * 0.2;
                      double currentRadius = 2.0 * (1.0 - absorbProgress); // 半径逐渐缩小
                      double height =
                          1.0 + Math.sin(elapsed * 0.2 + i * 0.5) * 0.3; // 上下波动

                      double x = playerPos.x + Math.cos(angle) * currentRadius;
                      double z = playerPos.z + Math.sin(angle) * currentRadius;
                      double absorbY = playerPos.y + height;

                      // 向中心移动的粒子
                      double dx = (playerPos.x - x) * 0.15;
                      double dz = (playerPos.z - z) * 0.15;
                      double dy = (playerPos.y + 1.0 - absorbY) * 0.1;

                      lvl.sendParticles(
                          ParticleTypes.ENCHANTED_HIT, x, absorbY, z, 1, dx, dy, dz, 0.1);

                      // 额外光效（图腾粒子）
                      if (i % 3 == 0) {
                        lvl.sendParticles(
                            ParticleTypes.TOTEM_OF_UNDYING, x, absorbY, z, 1, dx, dy, dz, 0.08);
                      }
                    }

                    // 玩家周围的光环效果
                    if (elapsed % 5 == 0) {
                      for (int i = 0; i < 8; i++) {
                        double angle = (Math.PI * 2.0 * i) / 8.0 + elapsed * 0.15;
                        double r = 0.5 + Math.sin(elapsed * 0.3) * 0.2;
                        lvl.sendParticles(
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
                })
            .onStop(
                (lvl, reason) -> {
                  // 完成时的最终爆发（成功融入）
                  if (reason == net.tigereye.chestcavity.engine.fx.StopReason.TTL_EXPIRED) {
                    // 最终爆发粒子
                    for (int i = 0; i < 20; i++) {
                      double angle = (Math.PI * 2.0 * i) / 20.0;
                      double radius = 0.5;
                      double dx = Math.cos(angle) * 0.3;
                      double dz = Math.sin(angle) * 0.3;

                      lvl.sendParticles(
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

                    // 成功音效
                    lvl.playSound(
                        null,
                        playerPos.x,
                        playerPos.y,
                        playerPos.z,
                        net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS,
                        0.6F,
                        1.5F);
                  }
                })
            .build();

    // 调度到 FxEngine
    FxEngine.scheduler().schedule(spec);
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
}
