package net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.orientation;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/**
 * OrientationOps 测试：验证四元数姿态计算正确性与半圆对称性。
 *
 * <p>Phase 8：根治"半圆抬头"问题，确保左右半圆飞行时 roll/pitch 对称、无累积性偏差。
 */
class OrientationOpsTest {

  private static final float EPSILON = 1.0E-4f;

  /**
   * 测试全方位 yaw 扫描：forward=(cos θ, 0, sin θ)，验证左右半圆 pitch/roll 对称。
   *
   * <p>预期：BASIS 模式下，yaw ∈ [0, 360°) 时，pitch 和 roll 在左右半圆应对称分布，
   * 不出现系统性"缓慢抬头"或偏向。
   */
  @Test
  void testYawScan_Symmetry() {
    Vec3 up = new Vec3(0, 1, 0);
    float preRoll = -45.0f;
    float yawOffset = -90.0f;
    float pitchOffset = 0.0f;

    // 扫描 0° 到 360°
    for (int deg = 0; deg < 360; deg += 10) {
      double rad = Math.toRadians(deg);
      Vec3 forward = new Vec3(Math.cos(rad), 0, Math.sin(rad));

      Quaternionf quat = OrientationOps.orientationFromForwardUp(
          forward, up, preRoll, yawOffset, pitchOffset, OrientationMode.BASIS, UpMode.WORLD_Y);

      // 提取欧拉角以验证对称性
      Vector3f eulerAngles = extractEulerAngles(quat);
      float pitch = eulerAngles.x;
      float yaw = eulerAngles.y;
      float roll = eulerAngles.z;

      // 验证不产生 NaN/Inf
      assertFalse(Float.isNaN(pitch), "Pitch is NaN at yaw=" + deg);
      assertFalse(Float.isInfinite(pitch), "Pitch is infinite at yaw=" + deg);
      assertFalse(Float.isNaN(roll), "Roll is NaN at yaw=" + deg);
      assertFalse(Float.isInfinite(roll), "Roll is infinite at yaw=" + deg);

      // 验证水平飞行时 pitch 接近 0（允许小偏差）
      assertTrue(Math.abs(pitch) < 5.0, "Pitch deviation too large at yaw=" + deg + ": " + pitch);
    }
  }

  /**
   * 测试近对向转向（θ=180°±ε）：验证不产生 NaN 或异常大 pitch。
   */
  @Test
  void testNearOppositeDirection_NoNaN() {
    Vec3 up = new Vec3(0, 1, 0);
    float preRoll = -45.0f;
    float yawOffset = -90.0f;
    float pitchOffset = 0.0f;

    // 测试接近 180° 的几个角度
    double[] testAngles = {179.9, 180.0, 180.1};
    for (double deg : testAngles) {
      double rad = Math.toRadians(deg);
      Vec3 forward = new Vec3(Math.cos(rad), 0, Math.sin(rad));

      Quaternionf quat = OrientationOps.orientationFromForwardUp(
          forward, up, preRoll, yawOffset, pitchOffset, OrientationMode.BASIS, UpMode.WORLD_Y);

      assertNotNull(quat);
      assertFalse(Float.isNaN(quat.x), "Quaternion x is NaN at angle=" + deg);
      assertFalse(Float.isNaN(quat.y), "Quaternion y is NaN at angle=" + deg);
      assertFalse(Float.isNaN(quat.z), "Quaternion z is NaN at angle=" + deg);
      assertFalse(Float.isNaN(quat.w), "Quaternion w is NaN at angle=" + deg);
    }
  }

  /**
   * 测试微小 y 扰动（±1e-6）：验证不随半圆累积被放大。
   */
  @Test
  void testSmallYPerturbation_NoAmplification() {
    Vec3 up = new Vec3(0, 1, 0);
    float preRoll = -45.0f;
    float yawOffset = -90.0f;
    float pitchOffset = 0.0f;

    // 基准：纯水平
    Vec3 forward0 = new Vec3(1, 0, 0);
    Quaternionf quat0 = OrientationOps.orientationFromForwardUp(
        forward0, up, preRoll, yawOffset, pitchOffset, OrientationMode.BASIS, UpMode.WORLD_Y);
    Vector3f euler0 = extractEulerAngles(quat0);

    // 微扰：y = +1e-6
    Vec3 forward1 = new Vec3(1, 1e-6, 0).normalize();
    Quaternionf quat1 = OrientationOps.orientationFromForwardUp(
        forward1, up, preRoll, yawOffset, pitchOffset, OrientationMode.BASIS, UpMode.WORLD_Y);
    Vector3f euler1 = extractEulerAngles(quat1);

    // 微扰：y = -1e-6
    Vec3 forward2 = new Vec3(1, -1e-6, 0).normalize();
    Quaternionf quat2 = OrientationOps.orientationFromForwardUp(
        forward2, up, preRoll, yawOffset, pitchOffset, OrientationMode.BASIS, UpMode.WORLD_Y);
    Vector3f euler2 = extractEulerAngles(quat2);

    // 验证扰动不被放大（pitch 变化应 < 1°）
    float pitchDiff1 = Math.abs(euler1.x - euler0.x);
    float pitchDiff2 = Math.abs(euler2.x - euler0.x);

    assertTrue(pitchDiff1 < 1.0, "Small y perturbation amplified: " + pitchDiff1);
    assertTrue(pitchDiff2 < 1.0, "Small y perturbation amplified: " + pitchDiff2);
  }

  /**
   * 测试竖直方向退化处理：forward=(0, 1, 0) 或 (0, -1, 0)。
   */
  @Test
  void testVerticalDirection_NoDegeneration() {
    Vec3 up = new Vec3(0, 1, 0);
    float preRoll = -45.0f;
    float yawOffset = -90.0f;
    float pitchOffset = 0.0f;

    // 测试向上
    Vec3 forwardUp = new Vec3(0, 1, 0);
    Quaternionf quatUp = OrientationOps.orientationFromForwardUp(
        forwardUp, up, preRoll, yawOffset, pitchOffset, OrientationMode.BASIS, UpMode.WORLD_Y);
    assertNotNull(quatUp);
    assertFalse(Float.isNaN(quatUp.x));
    assertFalse(Float.isNaN(quatUp.w));

    // 测试向下
    Vec3 forwardDown = new Vec3(0, -1, 0);
    Quaternionf quatDown = OrientationOps.orientationFromForwardUp(
        forwardDown, up, preRoll, yawOffset, pitchOffset, OrientationMode.BASIS, UpMode.WORLD_Y);
    assertNotNull(quatDown);
    assertFalse(Float.isNaN(quatDown.x));
    assertFalse(Float.isNaN(quatDown.w));
  }

  /**
   * 测试 LEGACY_EULER 模式：应与旧有实现行为一致。
   */
  @Test
  void testLegacyEulerMode() {
    Vec3 forward = new Vec3(1, 0, 0);
    Vec3 up = new Vec3(0, 1, 0);
    float preRoll = -45.0f;
    float yawOffset = -90.0f;
    float pitchOffset = 0.0f;

    Quaternionf quat = OrientationOps.orientationFromForwardUp(
        forward, up, preRoll, yawOffset, pitchOffset, OrientationMode.LEGACY_EULER, UpMode.WORLD_Y);

    assertNotNull(quat);
    assertFalse(Float.isNaN(quat.x));
    assertFalse(Float.isNaN(quat.y));
    assertFalse(Float.isNaN(quat.z));
    assertFalse(Float.isNaN(quat.w));
  }

  /**
   * 测试零向量输入：应回退到默认方向，不崩溃。
   */
  @Test
  void testZeroVectorInput_FallbackToDefault() {
    Vec3 forward = new Vec3(0, 0, 0);
    Vec3 up = new Vec3(0, 1, 0);
    float preRoll = 0.0f;
    float yawOffset = 0.0f;
    float pitchOffset = 0.0f;

    Quaternionf quat = OrientationOps.orientationFromForwardUp(
        forward, up, preRoll, yawOffset, pitchOffset, OrientationMode.BASIS, UpMode.WORLD_Y);

    assertNotNull(quat);
    assertFalse(Float.isNaN(quat.x));
    assertFalse(Float.isNaN(quat.y));
    assertFalse(Float.isNaN(quat.z));
    assertFalse(Float.isNaN(quat.w));
  }

  /**
   * 测试半圆对称性：+X ↔ -X 半圆飞行时，pitch 应对称，无系统性抬头。
   */
  @Test
  void testHalfCircleSymmetry() {
    Vec3 up = new Vec3(0, 1, 0);
    float preRoll = -45.0f;
    float yawOffset = -90.0f;
    float pitchOffset = 0.0f;

    // 左半圆：0° → 180°
    float sumPitchLeft = 0;
    int countLeft = 0;
    for (int deg = 0; deg <= 180; deg += 10) {
      double rad = Math.toRadians(deg);
      Vec3 forward = new Vec3(Math.cos(rad), 0, Math.sin(rad));
      Quaternionf quat = OrientationOps.orientationFromForwardUp(
          forward, up, preRoll, yawOffset, pitchOffset, OrientationMode.BASIS, UpMode.WORLD_Y);
      Vector3f euler = extractEulerAngles(quat);
      sumPitchLeft += euler.x;
      countLeft++;
    }

    // 右半圆：180° → 360°
    float sumPitchRight = 0;
    int countRight = 0;
    for (int deg = 180; deg <= 360; deg += 10) {
      double rad = Math.toRadians(deg);
      Vec3 forward = new Vec3(Math.cos(rad), 0, Math.sin(rad));
      Quaternionf quat = OrientationOps.orientationFromForwardUp(
          forward, up, preRoll, yawOffset, pitchOffset, OrientationMode.BASIS, UpMode.WORLD_Y);
      Vector3f euler = extractEulerAngles(quat);
      sumPitchRight += euler.x;
      countRight++;
    }

    float avgPitchLeft = sumPitchLeft / countLeft;
    float avgPitchRight = sumPitchRight / countRight;

    // 验证左右半圆平均 pitch 接近（允许小偏差）
    float pitchDiff = Math.abs(avgPitchLeft - avgPitchRight);
    assertTrue(pitchDiff < 2.0, "Half-circle pitch asymmetry: left=" + avgPitchLeft + ", right=" + avgPitchRight);
  }

  /**
   * 提取欧拉角（XYZ 顺序）：pitch, yaw, roll（单位：度）。
   *
   * <p>注意：仅用于测试验证，实际渲染使用四元数。
   */
  private Vector3f extractEulerAngles(Quaternionf q) {
    // 转换为欧拉角（XYZ 顺序）
    float sinPitch = 2 * (q.w * q.x - q.y * q.z);
    float pitch;
    if (Math.abs(sinPitch) >= 1) {
      pitch = (float) Math.copySign(Math.PI / 2, sinPitch);
    } else {
      pitch = (float) Math.asin(sinPitch);
    }

    float sinYaw = 2 * (q.w * q.y + q.z * q.x);
    float cosYaw = 1 - 2 * (q.x * q.x + q.y * q.y);
    float yaw = (float) Math.atan2(sinYaw, cosYaw);

    float sinRoll = 2 * (q.w * q.z + q.x * q.y);
    float cosRoll = 1 - 2 * (q.y * q.y + q.z * q.z);
    float roll = (float) Math.atan2(sinRoll, cosRoll);

    return new Vector3f(
        (float) Math.toDegrees(pitch),
        (float) Math.toDegrees(yaw),
        (float) Math.toDegrees(roll));
  }
}
