package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.TrajectoryType;

/**
 * Intent 的决策输出。仅描述“做什么”，不直接驱动移动。
 *
 * target：可以是实体或坐标（取其一）；
 * priority：本帧优先级，越大越优先；
 * trajectoryType：建议采用的轨迹类型；
 * params：轨迹/执行所需的额外参数（可选）。
 */
public final class IntentResult {
  @Nullable private final LivingEntity targetEntity;
  @Nullable private final Vec3 targetPos;
  private final double priority;
  private final TrajectoryType trajectoryType;
  private final Map<String, Double> params;

  private IntentResult(
      @Nullable LivingEntity targetEntity,
      @Nullable Vec3 targetPos,
      double priority,
      TrajectoryType trajectoryType,
      Map<String, Double> params) {
    this.targetEntity = targetEntity;
    this.targetPos = targetPos;
    this.priority = priority;
    this.trajectoryType = trajectoryType;
    this.params = params == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(params));
  }

  public static Builder builder() { return new Builder(); }

  public Optional<LivingEntity> getTargetEntity() { return Optional.ofNullable(targetEntity); }

  public Optional<Vec3> getTargetPos() { return Optional.ofNullable(targetPos); }

  public double getPriority() { return priority; }

  public TrajectoryType getTrajectoryType() { return trajectoryType; }

  public Map<String, Double> getParams() { return params; }

  public static final class Builder {
    private LivingEntity targetEntity;
    private Vec3 targetPos;
    private double priority;
    private TrajectoryType trajectoryType = TrajectoryType.Orbit; // 默认环绕
    private final Map<String, Double> params = new HashMap<>();

    public Builder target(LivingEntity entity) { this.targetEntity = entity; return this; }
    public Builder target(Vec3 pos) { this.targetPos = pos; return this; }
    public Builder priority(double p) { this.priority = p; return this; }
    public Builder trajectory(TrajectoryType t) { this.trajectoryType = t; return this; }
    public Builder param(String k, double v) { this.params.put(k, v); return this; }

    public IntentResult build() {
      return new IntentResult(targetEntity, targetPos, priority, trajectoryType, params);
    }
  }
}

