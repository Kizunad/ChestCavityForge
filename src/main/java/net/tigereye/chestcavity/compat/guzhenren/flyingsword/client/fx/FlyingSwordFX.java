package net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.fx;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordType;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.fx.entity.DefaultFlyingSwordEntityFX;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.fx.entity.IFlyingSwordEntityFX;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.fx.entity.RenShouZangShengFlyingSwordEntityFX;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.fx.entity.ZhengDaoFlyingSwordEntityFX;

/**
 * 飞剑粒子特效调度器
 *
 * <p>根据飞剑类型路由到对应的粒子特效实现。 提供统一的API入口，内部委托给特定类型的粒子特效处理器。
 *
 * <p>支持的飞剑类型：
 *
 * <ul>
 *   <li>正道（ZHENG_DAO）- 青色、亮色、光芒类粒子
 *   <li>魔道（REN_SHOU_ZANG_SHENG）- 深红色、暗色、血滴类粒子
 *   <li>默认（DEFAULT）- 标准白色/混合粒子
 * </ul>
 */
public final class FlyingSwordFX {

  private FlyingSwordFX() {}

  /**
   * 根据飞剑类型获取对应的粒子特效处理器
   *
   * @param type 飞剑类型
   * @return 对应的粒子特效处理器
   */
  private static IFlyingSwordEntityFX getEntityFX(FlyingSwordType type) {
    return switch (type) {
      case ZHENG_DAO -> ZhengDaoFlyingSwordEntityFX.getInstance();
      case REN_SHOU_ZANG_SHENG -> RenShouZangShengFlyingSwordEntityFX.getInstance();
      default -> DefaultFlyingSwordEntityFX.getInstance();
    };
  }

  /**
   * 飞行轨迹粒子
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   */
  public static void spawnFlightTrail(ServerLevel level, FlyingSwordEntity sword) {
    getEntityFX(sword.getSwordType()).spawnFlightTrail(level, sword);
  }

  /**
   * 攻击碰撞粒子
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   * @param pos 碰撞位置
   * @param damage 伤害值（影响粒子数量）
   */
  public static void spawnAttackImpact(
      ServerLevel level, FlyingSwordEntity sword, Vec3 pos, double damage) {
    getEntityFX(sword.getSwordType()).spawnAttackImpact(level, sword, pos, damage);
  }

  /**
   * 召回特效
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   */
  public static void spawnRecallEffect(ServerLevel level, FlyingSwordEntity sword) {
    getEntityFX(sword.getSwordType()).spawnRecallEffect(level, sword);
  }

  /**
   * 升级特效
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   * @param newLevel 新等级
   */
  public static void spawnLevelUpEffect(ServerLevel level, FlyingSwordEntity sword, int newLevel) {
    getEntityFX(sword.getSwordType()).spawnLevelUpEffect(level, sword, newLevel);
  }

  /**
   * 速度冲刺粒子（高速飞行时）
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   */
  public static void spawnSpeedBoostEffect(ServerLevel level, FlyingSwordEntity sword) {
    getEntityFX(sword.getSwordType()).spawnSpeedBoostEffect(level, sword);
  }

  /**
   * 环绕模式的轨道粒子
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   */
  public static void spawnOrbitTrail(ServerLevel level, FlyingSwordEntity sword) {
    getEntityFX(sword.getSwordType()).spawnOrbitTrail(level, sword);
  }

  /**
   * 出击模式的狩猎粒子
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   */
  public static void spawnHuntTrail(ServerLevel level, FlyingSwordEntity sword) {
    getEntityFX(sword.getSwordType()).spawnHuntTrail(level, sword);
  }

  /**
   * 防守模式的守护粒子
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   */
  public static void spawnGuardTrail(ServerLevel level, FlyingSwordEntity sword) {
    getEntityFX(sword.getSwordType()).spawnGuardTrail(level, sword);
  }

  /**
   * 召回模式的轨迹粒子
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   */
  public static void spawnRecallTrail(ServerLevel level, FlyingSwordEntity sword) {
    getEntityFX(sword.getSwordType()).spawnRecallTrail(level, sword);
  }

  /**
   * 生成飞剑召唤时的"剑阵"粒子：玩家脚下三环圆阵并带有花纹。
   *
   * <p>为降低开销，采用稀疏采样：每环48点，8条放射纹路，每条8点。 不同类型飞剑在粒子选择上略有差异。
   */
  public static void spawnSummonArrayAt(ServerLevel level, Vec3 center, FlyingSwordType type) {
    if (level == null || center == null) return;

    // 取脚下略低的位置，避免与模型Z冲突
    double y = center.y - 0.05;

    // 三环半径
    double[] radii = new double[] {0.6, 1.0, 1.4};
    int samples = 48;

    // 选择粒子：正道偏光亮，默认使用 ENCHANT 与 END_ROD，魔道使用 SOUL/SOUL_FIRE_FLAME
    var ringParticle =
        switch (type) {
          case ZHENG_DAO -> ParticleTypes.ENCHANT;
          case REN_SHOU_ZANG_SHENG -> ParticleTypes.SOUL;
          default -> ParticleTypes.ENCHANT;
        };
    var accentParticle =
        switch (type) {
          case ZHENG_DAO -> ParticleTypes.END_ROD;
          case REN_SHOU_ZANG_SHENG -> ParticleTypes.SOUL_FIRE_FLAME;
          default -> ParticleTypes.END_ROD;
        };

    // 画三重环
    for (double r : radii) {
      for (int i = 0; i < samples; i++) {
        double a = (i / (double) samples) * Math.PI * 2.0;
        double x = center.x + Math.cos(a) * r;
        double z = center.z + Math.sin(a) * r;
        level.sendParticles(ringParticle, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);

        // 每隔12个点放一个强调粒子，形成“花纹”节点
        if (i % 12 == 0) {
          level.sendParticles(accentParticle, x, y + 0.02, z, 2, 0.02, 0.0, 0.02, 0.0);
        }
      }
    }

    // 放射状花纹（spokes）：8 方向，从内环到外环
    int spokes = 8;
    for (int s = 0; s < spokes; s++) {
      double a = (s / (double) spokes) * Math.PI * 2.0;
      double cos = Math.cos(a);
      double sin = Math.sin(a);
      for (int t = 0; t <= 8; t++) { // 8段
        double r = radii[0] + (radii[2] - radii[0]) * (t / 8.0);
        double x = center.x + cos * r;
        double z = center.z + sin * r;
        level.sendParticles(accentParticle, x, y + 0.01, z, 1, 0.0, 0.0, 0.0, 0.0);
      }
    }

    // 内圈符纹（短弧），在内环上再加 6 段短弧
    int glyphs = 6;
    int glyphSamples = 8;
    double r0 = radii[0] + 0.05;
    for (int g = 0; g < glyphs; g++) {
      double start = (g / (double) glyphs) * Math.PI * 2.0 + Math.PI / glyphs * 0.3;
      for (int k = 0; k < glyphSamples; k++) {
        double a = start + (k / (double) glyphSamples) * (Math.PI / glyphs);
        double x = center.x + Math.cos(a) * r0;
        double z = center.z + Math.sin(a) * r0;
        level.sendParticles(ringParticle, x, y + 0.015, z, 1, 0.0, 0.0, 0.0, 0.0);
      }
    }
  }
}
