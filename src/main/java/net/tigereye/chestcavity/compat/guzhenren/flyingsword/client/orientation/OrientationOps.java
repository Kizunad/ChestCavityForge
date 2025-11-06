package net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.orientation;

import com.mojang.math.Axis;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 飞剑姿态计算工具（Flying Sword Orientation Operations）。
 *
 * <p>Phase 8：根治"半圆抬头"问题，通过正交基/四元数统一姿态计算。
 *
 * <p>核心设计：
 * <ul>
 *   <li>基于右手坐标系构建正交基：right = up × forward, trueUp = forward × right</li>
 *   <li>退化处理：当 forward 与 up 近平行时，选择水平 fallback</li>
 *   <li>偏移应用：preRoll（绕 forward）、yaw（绕 world-Y）、pitch（绕局部 X）</li>
 *   <li>返回四元数，可直接用于 poseStack.mulPose(quat)</li>
 * </ul>
 */
public final class OrientationOps {
  private OrientationOps() {}

  private static final float EPSILON = 1.0E-6f;

  /**
   * 从朝向向量和上向计算姿态四元数。
   *
   * @param forward 前向向量（通常为速度或瞄准方向）
   * @param up 上向参考（默认世界 Y 轴）
   * @param preRollDeg 绕 forward 轴的预旋转（刀面纠正），单位：度
   * @param yawOffsetDeg 绕世界 Y 轴的偏移，单位：度
   * @param pitchOffsetDeg 绕局部 X 轴的偏移，单位：度
   * @param mode 计算模式（BASIS 或 LEGACY_EULER）
   * @param upMode 上向模式（WORLD_Y 或 OWNER_UP）
   * @return 姿态四元数
   */
  public static Quaternionf orientationFromForwardUp(
      Vec3 forward,
      Vec3 up,
      float preRollDeg,
      float yawOffsetDeg,
      float pitchOffsetDeg,
      OrientationMode mode,
      UpMode upMode) {

    if (mode == OrientationMode.LEGACY_EULER) {
      // 沿用旧有欧拉角顺序（兼容路径）
      return legacyEulerOrientation(forward, preRollDeg, yawOffsetDeg, pitchOffsetDeg);
    }

    // BASIS 模式：正交基/四元数计算
    return basisOrientation(forward, up, preRollDeg, yawOffsetDeg, pitchOffsetDeg, upMode);
  }

  /**
   * BASIS 模式：基于正交基构建四元数。
   */
  private static Quaternionf basisOrientation(
      Vec3 forward,
      Vec3 up,
      float preRollDeg,
      float yawOffsetDeg,
      float pitchOffsetDeg,
      UpMode upMode) {

    // 归一化 forward
    Vec3 forwardN = forward.normalize();
    if (forwardN.lengthSqr() < EPSILON) {
      forwardN = new Vec3(0, 0, -1); // 默认朝向 -Z
    }

    // 归一化 up
    Vec3 upN = up.normalize();
    if (upN.lengthSqr() < EPSILON) {
      upN = new Vec3(0, 1, 0); // 默认世界 Y
    }

    // 计算 right = up × forward（右手系）
    Vec3 right = upN.cross(forwardN);
    double rightLenSq = right.lengthSqr();

    // 退化处理：forward 与 up 近平行
    if (rightLenSq < EPSILON) {
      // 选择水平 fallback：若 forward.y 接近 ±1，则选择水平向量
      if (Math.abs(forwardN.x) < EPSILON && Math.abs(forwardN.z) < EPSILON) {
        // forward 竖直，选水平 X 或 Z
        right = new Vec3(1, 0, 0);
      } else {
        // forward 水平或倾斜，选垂直于 forward 的水平向量
        right = new Vec3(-forwardN.z, 0, forwardN.x).normalize();
      }
    } else {
      right = right.normalize();
    }

    // 重新计算 trueUp = forward × right（确保正交）
    Vec3 trueUp = forwardN.cross(right).normalize();

    // 构建旋转矩阵（列向量）：
    // right = X 轴（局部右），trueUp = Y 轴（局部上），forwardN = Z 轴（局部前）
    // Minecraft 模型坐标系通常：X=前，Y=上，Z=右（或类似变体）
    // 为了使"模型 X 轴"对齐到 forwardN，我们需要调整基向量顺序：
    //
    // 渲染坐标映射：
    //   模型 X → forwardN（前）
    //   模型 Y → trueUp（上）
    //   模型 Z → right（右）
    Vector3f xAxis = new Vector3f((float) forwardN.x, (float) forwardN.y, (float) forwardN.z);
    Vector3f yAxis = new Vector3f((float) trueUp.x, (float) trueUp.y, (float) trueUp.z);
    Vector3f zAxis = new Vector3f((float) right.x, (float) right.y, (float) right.z);

    // 从基向量构建四元数
    Quaternionf quat = quaternionFromBasis(xAxis, yAxis, zAxis);

    // 应用偏移（按顺序）
    // 1. preRoll：绕 forward（模型 X）轴旋转
    if (Math.abs(preRollDeg) > EPSILON) {
      quat.mul(Axis.XP.rotationDegrees(preRollDeg));
    }

    // 2. yawOffset：绕世界 Y 轴旋转
    if (Math.abs(yawOffsetDeg) > EPSILON) {
      Quaternionf yawQuat = Axis.YP.rotationDegrees(yawOffsetDeg);
      quat.premul(yawQuat); // 世界空间旋转，左乘
    }

    // 3. pitchOffset：绕局部 X 轴旋转
    if (Math.abs(pitchOffsetDeg) > EPSILON) {
      quat.mul(Axis.XP.rotationDegrees(pitchOffsetDeg));
    }

    return quat;
  }

  /**
   * LEGACY_EULER 模式：沿用旧有 Y→Z 欧拉顺序（兼容路径）。
   */
  private static Quaternionf legacyEulerOrientation(
      Vec3 forward,
      float preRollDeg,
      float yawOffsetDeg,
      float pitchOffsetDeg) {

    // 计算 yaw / pitch
    float yaw = (float) Math.toDegrees(Math.atan2(forward.x, forward.z));
    double horizontalLength = Math.sqrt(forward.x * forward.x + forward.z * forward.z);
    float pitch = (float) Math.toDegrees(Math.atan2(-forward.y, horizontalLength));

    Quaternionf quat = new Quaternionf();

    // 按旧有顺序应用：preRoll → yaw → pitch
    quat.mul(Axis.XP.rotationDegrees(preRollDeg));
    quat.mul(Axis.YP.rotationDegrees(yaw + yawOffsetDeg));
    quat.mul(Axis.ZP.rotationDegrees(pitch + pitchOffsetDeg));

    return quat;
  }

  /**
   * 从正交基向量构建四元数。
   *
   * <p>基于旋转矩阵转四元数的标准算法。
   *
   * @param xAxis X 轴向量（归一化）
   * @param yAxis Y 轴向量（归一化）
   * @param zAxis Z 轴向量（归一化）
   * @return 对应的四元数
   */
  private static Quaternionf quaternionFromBasis(Vector3f xAxis, Vector3f yAxis, Vector3f zAxis) {
    // 旋转矩阵：
    // [ x.x  y.x  z.x ]
    // [ x.y  y.y  z.y ]
    // [ x.z  y.z  z.z ]
    float m00 = xAxis.x, m10 = xAxis.y, m20 = xAxis.z;
    float m01 = yAxis.x, m11 = yAxis.y, m21 = yAxis.z;
    float m02 = zAxis.x, m12 = zAxis.y, m22 = zAxis.z;

    float trace = m00 + m11 + m22;
    Quaternionf q = new Quaternionf();

    if (trace > 0) {
      float s = (float) Math.sqrt(trace + 1.0) * 2; // s = 4 * qw
      q.w = 0.25f * s;
      q.x = (m21 - m12) / s;
      q.y = (m02 - m20) / s;
      q.z = (m10 - m01) / s;
    } else if (m00 > m11 && m00 > m22) {
      float s = (float) Math.sqrt(1.0 + m00 - m11 - m22) * 2; // s = 4 * qx
      q.w = (m21 - m12) / s;
      q.x = 0.25f * s;
      q.y = (m01 + m10) / s;
      q.z = (m02 + m20) / s;
    } else if (m11 > m22) {
      float s = (float) Math.sqrt(1.0 + m11 - m00 - m22) * 2; // s = 4 * qy
      q.w = (m02 - m20) / s;
      q.x = (m01 + m10) / s;
      q.y = 0.25f * s;
      q.z = (m12 + m21) / s;
    } else {
      float s = (float) Math.sqrt(1.0 + m22 - m00 - m11) * 2; // s = 4 * qz
      q.w = (m10 - m01) / s;
      q.x = (m02 + m20) / s;
      q.y = (m12 + m21) / s;
      q.z = 0.25f * s;
    }

    return q.normalize();
  }
}
