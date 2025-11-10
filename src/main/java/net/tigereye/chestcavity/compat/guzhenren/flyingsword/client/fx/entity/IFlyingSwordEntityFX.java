package net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.fx.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;

/**
 * 飞剑实体粒子特效接口
 *
 * <p>定义飞剑各种状态和事件的粒子特效方法。 不同类型的飞剑通过实现此接口来提供特定的视觉效果。
 */
public interface IFlyingSwordEntityFX {

  /**
   * 飞行轨迹粒子
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   */
  void spawnFlightTrail(ServerLevel level, FlyingSwordEntity sword);

  /**
   * 攻击碰撞粒子
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   * @param pos 碰撞位置
   * @param damage 伤害值
   */
  void spawnAttackImpact(ServerLevel level, FlyingSwordEntity sword, Vec3 pos, double damage);

  /**
   * 召回特效
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   */
  void spawnRecallEffect(ServerLevel level, FlyingSwordEntity sword);

  /**
   * 升级特效
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   * @param newLevel 新等级
   */
  void spawnLevelUpEffect(ServerLevel level, FlyingSwordEntity sword, int newLevel);

  /**
   * 速度冲刺粒子（高速飞行时）
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   */
  void spawnSpeedBoostEffect(ServerLevel level, FlyingSwordEntity sword);

  /**
   * 环绕模式的轨道粒子
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   */
  void spawnOrbitTrail(ServerLevel level, FlyingSwordEntity sword);

  /**
   * 出击模式的狩猎粒子
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   */
  void spawnHuntTrail(ServerLevel level, FlyingSwordEntity sword);

  /**
   * 防守模式的守护粒子
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   */
  void spawnGuardTrail(ServerLevel level, FlyingSwordEntity sword);

  /**
   * 召回模式的轨迹粒子
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   */
  void spawnRecallTrail(ServerLevel level, FlyingSwordEntity sword);
}
