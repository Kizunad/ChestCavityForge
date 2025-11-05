package net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning;

import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.AIMode;

/**
 * 统一管理转向相关参数，便于调参与共享。
 */
public final class FlyingSwordSteeringTuning {

  private FlyingSwordSteeringTuning() {}

  public static double defaultTurnLimitRadians(AIMode mode) {
    return switch (mode) {
      case ORBIT -> 0.22;
      case GUARD -> 0.28;
      case HUNT -> 0.35;
      case HOVER -> 0.25;
      case RECALL -> 0.30;
      case SWARM -> 0.32;
    };
  }

  public static double minimumAccelerationFactor() {
    return 0.1;
  }
}
