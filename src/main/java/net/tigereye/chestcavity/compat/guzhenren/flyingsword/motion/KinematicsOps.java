package net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion;

import net.minecraft.world.phys.Vec3;

/**
 * 纯函数集合：负责角速度、线速度相关的辅助计算。
 */
public final class KinematicsOps {

  private static final double EPS = 1.0e-6;

  private KinematicsOps() {}

  /**
   * 将向量限制在给定长度（若超出则归一化后放大到 limit）。
   */
  public static Vec3 clampVectorLength(Vec3 vec, double limit) {
    double len = vec.length();
    if (len < EPS || len <= limit) {
      return vec;
    }
    return vec.scale(limit / len);
  }

  /**
   * 限制“速度向量变化量”，用于线速度加速度限制。
   */
  public static Vec3 limitDelta(Vec3 delta, double maxDelta) {
    return clampVectorLength(delta, maxDelta);
  }

  /**
   * 限制方向变化角度（弧度）。输入向量不要求归一化。
   */
  public static Vec3 limitTurn(Vec3 currentDir, Vec3 desiredDir, double maxRadians) {
    Vec3 from = normaliseSafe(currentDir);
    Vec3 to = normaliseSafe(desiredDir);

    double dot = from.dot(to);
    dot = Math.max(-1.0, Math.min(1.0, dot));
    double angle = Math.acos(dot);

    if (angle <= maxRadians) {
      return to;
    }

    double t = maxRadians / Math.max(EPS, angle);
    // 线性插值 + 归一化作为近似，避免 trig 计算。
    Vec3 lerped = from.scale(1.0 - t).add(to.scale(t));
    return normaliseSafe(lerped);
  }

  /**
   * 将向量归一化，若长度过小则返回零向量。
   */
  public static Vec3 normaliseSafe(Vec3 vec) {
    double len = vec.length();
    if (len < EPS) {
      return Vec3.ZERO;
    }
    return vec.scale(1.0 / len);
  }
}
