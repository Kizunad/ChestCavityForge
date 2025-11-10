package net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion;

import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;

/**
 * 统一的转向模板接口。
 *
 * <p>轨迹/行为实现者只需关注如何产出 {@link SteeringCommand}，其余运动学细节由 {@link SteeringOps} 处理。
 */
public interface SteeringTemplate {

  /**
   * 计算期望的运动命令。
   *
   * @param ctx 当前 AI 上下文
   * @param intent 调度得到的意图结果
   * @param snapshot 当前运动学快照
   * @return 期望的运动命令
   */
  SteeringCommand compute(AIContext ctx, IntentResult intent, KinematicsSnapshot snapshot);

  /**
   * 指示速度倍率的单位。
   *
   * <p>轨迹可以明确表示速度参数是基于 base 还是 max，默认视为 BASE。
   */
  default SpeedUnit speedUnit() {
    return SpeedUnit.BASE;
  }

  /** 是否启用默认分离力。 */
  default boolean enableSeparation() {
    return true;
  }

  /**
   * 可选：覆盖角速度限制（弧度/ tick）。
   *
   * @return 若为空，则由运动层根据模式决定
   */
  default Double maxTurnRadiansOverride() {
    return null;
  }

  /** 速度倍率单位。 */
  enum SpeedUnit {
    BASE,
    MAX
  }
}
