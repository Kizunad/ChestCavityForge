package net.tigereye.chestcavity.soul.fakeplayer.brain.model;

import net.minecraft.world.phys.Vec3;

/** 描述探索候选点及其属性，供探索子大脑仲裁使用。 */
public record ExplorationTarget(
    Vec3 position, double novelty, double distanceHint, String rationale) {

  public ExplorationTarget {
    if (position == null) throw new IllegalArgumentException("position");
    novelty = clamp01(novelty);
    distanceHint = Math.max(0.0, distanceHint);
    rationale = rationale == null ? "unknown" : rationale;
  }

  public double distanceTo(Vec3 other) {
    if (other == null) {
      return Double.POSITIVE_INFINITY;
    }
    return other.distanceTo(position);
  }

  public ExplorationTarget withRationale(String newRationale) {
    return new ExplorationTarget(position, novelty, distanceHint, newRationale);
  }

  private static double clamp01(double value) {
    if (value < 0.0) return 0.0;
    if (value > 1.0) return 1.0;
    return value;
  }
}
