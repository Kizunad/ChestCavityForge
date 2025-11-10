package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory;

import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.SteeringTemplate.SpeedUnit;

/** 轨迹的元数据，用于新旧系统桥接。 */
public record TrajectoryMeta(
    SpeedUnit speedUnit, boolean enableSeparation, Double maxTurnOverride, Double accelOverride) {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private SpeedUnit speedUnit = SpeedUnit.BASE;
    private boolean enableSeparation = true;
    private Double maxTurnOverride;
    private Double accelOverride;

    public Builder speedUnit(SpeedUnit unit) {
      this.speedUnit = unit;
      return this;
    }

    public Builder separation(boolean enable) {
      this.enableSeparation = enable;
      return this;
    }

    public Builder maxTurnOverride(Double radians) {
      this.maxTurnOverride = radians;
      return this;
    }

    public Builder accelOverride(Double factor) {
      this.accelOverride = factor;
      return this;
    }

    public TrajectoryMeta build() {
      return new TrajectoryMeta(speedUnit, enableSeparation, maxTurnOverride, accelOverride);
    }
  }
}
