package net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.orientation;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * P8.1: 渲染姿态统一入口（正交基/四元数）。
 *
 * <p>职责：将“朝向向量 + 上向约束 + 偏移（preRoll/yaw/pitch）”转换为可直接 应用于 PoseStack 的四元数。该类为纯函数工具，不做任何 I/O 或游戏状态访问。
 */
public final class OrientationOps {

  private static final double EPS = 1.0e-9;

  private OrientationOps() {}

  // 使用已存在的顶层枚举：OrientationMode / UpMode

  /**
   * 计算姿态四元数（正交基）。
   *
   * @param forward 朝向向量（未归一化可）
   * @param upRef 上向约束（当 upMode=CUSTOM 时使用；WORLD_Y 时可传 (0,1,0)）
   * @param preRollDeg 刀面滚转（绕 forward 轴，度）
   * @param yawOffsetDeg 额外 yaw 偏移（绕世界 Y 轴，度）
   * @param pitchOffsetDeg 额外 pitch 偏移（绕局部 X/Right 轴，度）
   * @param mode 姿态模式（BASIS | LEGACY_EULER）
   * @param upMode 上向模式（WORLD_Y | CUSTOM）
   * @return 可直接用于 PoseStack.mulPose(quat) 的四元数
   */
  public static Quaternionf orientationFromForwardUp(
      Vec3 forward,
      Vec3 upRef,
      float preRollDeg,
      float yawOffsetDeg,
      float pitchOffsetDeg,
      OrientationMode mode,
      UpMode upMode) {
    if (mode == OrientationMode.LEGACY_EULER) {
      // 兼容模式不在此类计算，返回单位四元数，交由旧路径处理。
      return new Quaternionf();
    }

    // 1) 基础向量与退化处理
    Vector3f fwd = toUnit(forward, new Vector3f(0, 0, -1));
    Vector3f up =
        (upMode == UpMode.WORLD_Y)
            ? new Vector3f(0, 1, 0)
            : toUnit(upRef, new Vector3f(0, 1, 0)); // OWNER_UP 预留：目前仍用世界上向或传入的 upRef

    // 若 up 与 forward 近乎平行，选择水平 fallback 上向
    if (isParallel(up, fwd)) {
      if (Math.abs(fwd.y()) < 0.999f) {
        up.set(0, 1, 0);
      } else {
        up.set(1, 0, 0);
      }
    }

    // 2) 正交基：right = up × forward；trueUp = forward × right
    Vector3f right = up.cross(new Vector3f(fwd), new Vector3f());
    if (right.lengthSquared() < 1e-12f) {
      // 极端退化，选用固定 right
      right.set(1, 0, 0);
    } else {
      right.normalize();
    }
    Vector3f trueUp = new Vector3f(fwd).cross(right);

    // 3) 由基构建四元数（列向量采用“模型 X=forward”的约定：F, U, R）
    Quaternionf q = basisToQuat_ModelXForward(fwd, trueUp, right);

    // 4) 应用偏移：
    // world yaw: 左乘（世界坐标系）
    if (Math.abs(yawOffsetDeg) > 1e-6f) {
      float yawRad = (float) Math.toRadians(yawOffsetDeg);
      Quaternionf qWorldYaw = new Quaternionf().rotateAxis(yawRad, 0, 1, 0);
      q = qWorldYaw.mul(q);
    }

    // local preRoll: 绕 forward 轴右乘
    if (Math.abs(preRollDeg) > 1e-6f) {
      float rollRad = (float) Math.toRadians(preRollDeg);
      q.rotateAxis(rollRad, fwd.x(), fwd.y(), fwd.z());
    }

    // local pitch: 绕 right 轴右乘
    if (Math.abs(pitchOffsetDeg) > 1e-6f) {
      float pitchRad = (float) Math.toRadians(pitchOffsetDeg);
      q.rotateAxis(pitchRad, right.x(), right.y(), right.z());
    }

    return q;
  }

  /** 便捷重载：默认 BASIS + WORLD_Y。 */
  public static Quaternionf orientationFromForwardUp(
      Vec3 forward, Vec3 upRef, float preRollDeg, float yawOffsetDeg, float pitchOffsetDeg) {
    return orientationFromForwardUp(
        forward,
        upRef,
        preRollDeg,
        yawOffsetDeg,
        pitchOffsetDeg,
        OrientationMode.BASIS,
        UpMode.WORLD_Y);
  }

  // ========== 内部工具 ==========

  private static Vector3f toUnit(Vec3 v, Vector3f fallback) {
    if (v == null) return new Vector3f(fallback);
    Vector3f out = new Vector3f((float) v.x, (float) v.y, (float) v.z);
    float len2 = out.lengthSquared();
    if (len2 < 1e-12f || !Float.isFinite(len2)) return new Vector3f(fallback);
    out.mul((float) (1.0 / Math.sqrt(len2)));
    return out;
  }

  private static boolean isParallel(Vector3f a, Vector3f b) {
    float dot = a.dot(b);
    return Math.abs(dot) > 1.0f - 1e-6f;
  }

  /** 将正交基转换为四元数。输入必须近似正交归一。 列向量：R (right), U (up), F (forward)。 */
  private static Quaternionf basisToQuat_ModelXForward(Vector3f F, Vector3f U, Vector3f R) {
    // 矩阵→四元数（右手，列主序）
    // 列 0 = F (模型 X 轴指向前进方向)
    // 列 1 = U
    // 列 2 = R
    float m00 = F.x(), m01 = U.x(), m02 = R.x();
    float m10 = F.y(), m11 = U.y(), m12 = R.y();
    float m20 = F.z(), m21 = U.z(), m22 = R.z();

    float trace = m00 + m11 + m22;
    Quaternionf q = new Quaternionf();
    if (trace > 0) {
      float s = (float) Math.sqrt(trace + 1.0f) * 2f;
      q.w = 0.25f * s;
      q.x = (m21 - m12) / s;
      q.y = (m02 - m20) / s;
      q.z = (m10 - m01) / s;
    } else if (m00 > m11 && m00 > m22) {
      float s = (float) Math.sqrt(1.0f + m00 - m11 - m22) * 2f;
      q.w = (m21 - m12) / s;
      q.x = 0.25f * s;
      q.y = (m01 + m10) / s;
      q.z = (m02 + m20) / s;
    } else if (m11 > m22) {
      float s = (float) Math.sqrt(1.0f + m11 - m00 - m22) * 2f;
      q.w = (m02 - m20) / s;
      q.x = (m01 + m10) / s;
      q.y = 0.25f * s;
      q.z = (m12 + m21) / s;
    } else {
      float s = (float) Math.sqrt(1.0f + m22 - m00 - m11) * 2f;
      q.w = (m10 - m01) / s;
      q.x = (m02 + m20) / s;
      q.y = (m12 + m21) / s;
      q.z = 0.25f * s;
    }
    q.normalize();
    return q;
  }
}
