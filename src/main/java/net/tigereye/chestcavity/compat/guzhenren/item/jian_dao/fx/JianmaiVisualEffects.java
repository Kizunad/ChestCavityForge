package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx;

import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianmaiTuning;
import org.joml.Vector3f;

/**
 * 剑脉蛊视觉特效封装。
 *
 * <p>包含：
 * <ul>
 *   <li>被动效果：飞剑能量脉络和粒子</li>
 *   <li>主动效果：雷电链和电击粒子</li>
 * </ul>
 */
public final class JianmaiVisualEffects {

  private JianmaiVisualEffects() {}

  // ==================== 被动效果：飞剑脉络 ====================

  /**
   * 渲染被动能量线效果（每Tick调用）。
   *
   * <p>为玩家周围每把飞剑绘制能量线并沿线放置粒子。
   *
   * @param player 玩家
   * @param swords 周围的飞剑列表
   * @param now 当前游戏时间
   */
  public static void renderPassiveLines(ServerPlayer player, List<FlyingSwordEntity> swords, long now) {
    if (player == null || swords == null || swords.isEmpty()) {
      return;
    }
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }

    Vec3 playerPos = player.position().add(0, 1.0, 0); // 玩家胸口位置
    int totalParticles = 0;

    // 为每把飞剑绘制能量线
    for (FlyingSwordEntity sword : swords) {
      if (!sword.isAlive()) {
        continue;
      }

      Vec3 swordPos = sword.position();
      double distance = playerPos.distanceTo(swordPos);

      // 如果距离太远，跳过
      if (distance > JianmaiTuning.JME_RADIUS) {
        continue;
      }

      // 计算距离衰减（距离越远线条越淡）
      double ratio = distance / JianmaiTuning.JME_RADIUS;
      double weight = Math.max(0.0, 1.0 - Math.pow(ratio, JianmaiTuning.JME_DIST_ALPHA));

      // 沿线放置粒子
      int particlesOnLine = spawnLineParticles(level, playerPos, swordPos, distance, weight);
      totalParticles += particlesOnLine;

      // 限制总粒子数防止过载
      if (totalParticles >= JianmaiTuning.VEIN_MAX_PARTICLES) {
        break;
      }
    }
  }

  /**
   * 沿能量线放置粒子。
   *
   * @param level 世界
   * @param from 起点（玩家）
   * @param to 终点（飞剑）
   * @param distance 距离
   * @param weight 距离权重（影响粒子透明度）
   * @return 放置的粒子数量
   */
  private static int spawnLineParticles(ServerLevel level, Vec3 from, Vec3 to, double distance, double weight) {
    // 计算需要放置的粒子数量（每1米一个）
    int numParticles = (int) Math.ceil(distance / JianmaiTuning.VEIN_PARTICLE_SPACING);

    // 粒子颜色：淡蓝色，透明度受距离影响
    float alpha = (float) (JianmaiTuning.VEIN_ALPHA * weight);
    int[] color = JianmaiTuning.VEIN_COLOR;
    Vector3f rgb = new Vector3f(color[0] / 255f, color[1] / 255f, color[2] / 255f);

    // 沿线均匀放置粒子
    for (int i = 0; i < numParticles; i++) {
      double t = (i + 1.0) / (numParticles + 1.0); // 均匀插值
      Vec3 particlePos = from.lerp(to, t);

      // 使用 DUST 粒子（可自定义颜色和大小）
      DustParticleOptions particle = new DustParticleOptions(rgb, 0.3f * (float) weight);

      level.sendParticles(
          particle,
          particlePos.x,
          particlePos.y,
          particlePos.z,
          1, // 数量
          0.0, 0.0, 0.0, // 偏移
          0.0); // 速度
    }

    return numParticles;
  }

  // ==================== 主动效果：雷电链 ====================

  /**
   * 渲染主动雷电链效果。
   *
   * <p>从玩家到所有飞剑绘制强烈的雷电效果。
   *
   * @param player 玩家
   * @param swords 目标飞剑列表
   * @param deltaAmount 增幅量（影响效果强度）
   * @param now 当前游戏时间
   */
  public static void renderActiveLightning(
      ServerPlayer player, List<FlyingSwordEntity> swords, double deltaAmount, long now) {
    if (player == null || swords == null || swords.isEmpty()) {
      return;
    }
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }

    Vec3 playerPos = player.position().add(0, 1.0, 0); // 玩家胸口位置

    // 在玩家位置生成激活点粒子
    spawnActivationBurst(level, playerPos);

    // 为每把飞剑绘制雷电链
    for (FlyingSwordEntity sword : swords) {
      if (!sword.isAlive()) {
        continue;
      }

      Vec3 swordPos = sword.position();
      spawnLightningChain(level, playerPos, swordPos);
    }
  }

  /**
   * 在激活点生成爆发粒子。
   *
   * @param level 世界
   * @param pos 位置
   */
  private static void spawnActivationBurst(ServerLevel level, Vec3 pos) {
    // 使用电光粒子（ELECTRIC_SPARK）
    for (int i = 0; i < 20; i++) {
      double offsetX = (Math.random() - 0.5) * 0.5;
      double offsetY = (Math.random() - 0.5) * 0.5;
      double offsetZ = (Math.random() - 0.5) * 0.5;
      double velocityX = (Math.random() - 0.5) * 0.1;
      double velocityY = (Math.random() - 0.5) * 0.1;
      double velocityZ = (Math.random() - 0.5) * 0.1;

      level.sendParticles(
          ParticleTypes.ELECTRIC_SPARK,
          pos.x + offsetX,
          pos.y + offsetY,
          pos.z + offsetZ,
          1,
          velocityX,
          velocityY,
          velocityZ,
          0.05);
    }

    // 添加蓝色尘埃粒子强化效果
    int[] color = JianmaiTuning.LIGHTNING_COLOR;
    Vector3f rgb = new Vector3f(color[0] / 255f, color[1] / 255f, color[2] / 255f);
    DustParticleOptions blueParticle = new DustParticleOptions(rgb, 1.0f);

    for (int i = 0; i < 15; i++) {
      double offsetX = (Math.random() - 0.5) * 0.4;
      double offsetY = (Math.random() - 0.5) * 0.4;
      double offsetZ = (Math.random() - 0.5) * 0.4;

      level.sendParticles(blueParticle, pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ, 1, 0, 0, 0, 0);
    }
  }

  /**
   * 绘制雷电链。
   *
   * @param level 世界
   * @param from 起点（玩家）
   * @param to 终点（飞剑）
   */
  private static void spawnLightningChain(ServerLevel level, Vec3 from, Vec3 to) {
    double distance = from.distanceTo(to);
    int numParticles = (int) Math.ceil(distance / JianmaiTuning.LIGHTNING_PARTICLE_SPACING);

    // 雷电链颜色：纯蓝色
    int[] color = JianmaiTuning.LIGHTNING_COLOR;
    Vector3f rgb = new Vector3f(color[0] / 255f, color[1] / 255f, color[2] / 255f);

    // 沿链放置粒子，带随机偏移模拟雷电效果
    for (int i = 0; i < numParticles; i++) {
      double t = (i + 1.0) / (numParticles + 1.0);
      Vec3 basePos = from.lerp(to, t);

      // 添加随机偏移模拟雷电弯曲
      double offsetX = (Math.random() - 0.5) * 0.3;
      double offsetY = (Math.random() - 0.5) * 0.3;
      double offsetZ = (Math.random() - 0.5) * 0.3;

      Vec3 particlePos = basePos.add(offsetX, offsetY, offsetZ);

      // 使用电光粒子
      level.sendParticles(
          ParticleTypes.ELECTRIC_SPARK,
          particlePos.x,
          particlePos.y,
          particlePos.z,
          2,
          0.05,
          0.05,
          0.05,
          0.02);

      // 每隔几个粒子添加蓝色尘埃强化效果
      if (i % 2 == 0) {
        DustParticleOptions blueParticle = new DustParticleOptions(rgb, 0.8f);
        level.sendParticles(blueParticle, particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
      }
    }

    // 在目标飞剑位置生成击中效果
    spawnHitEffect(level, to);
  }

  /**
   * 在目标位置生成击中效果。
   *
   * @param level 世界
   * @param pos 位置
   */
  private static void spawnHitEffect(ServerLevel level, Vec3 pos) {
    // 电光爆炸
    for (int i = 0; i < 8; i++) {
      double velocityX = (Math.random() - 0.5) * 0.15;
      double velocityY = (Math.random() - 0.5) * 0.15;
      double velocityZ = (Math.random() - 0.5) * 0.15;

      level.sendParticles(
          ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 1, velocityX, velocityY, velocityZ, 0.1);
    }

    // 蓝色闪光
    int[] color = JianmaiTuning.LIGHTNING_COLOR;
    Vector3f rgb = new Vector3f(color[0] / 255f, color[1] / 255f, color[2] / 255f);
    DustParticleOptions blueParticle = new DustParticleOptions(rgb, 1.2f);

    for (int i = 0; i < 5; i++) {
      double offsetX = (Math.random() - 0.5) * 0.2;
      double offsetY = (Math.random() - 0.5) * 0.2;
      double offsetZ = (Math.random() - 0.5) * 0.2;

      level.sendParticles(blueParticle, pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ, 1, 0, 0, 0, 0);
    }
  }
}
