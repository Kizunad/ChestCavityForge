package net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * KinematicsOps 边界与回归测试。
 *
 * 聚焦 180° 对向转向与八象限稳定性，帮助定位“途中剑头朝上/朝向翻转”类问题。
 */
class KinematicsOpsEdgeCaseTest {

  @Test
  void limitTurn_OppositeDirections_SmallTurn_NoProgress() {
    Vec3 current = new Vec3(1, 0, 0);
    Vec3 desired = new Vec3(-1, 0, 0);
    Vec3 out = KinematicsOps.limitTurn(current, desired, Math.PI / 18); // 10°

    // 当前实现：在 180° 对向且 t<0.5 时，线性插值归一化仍返回 current 方向（无进展）。
    assertEquals(KinematicsOps.normaliseSafe(current), out);
  }

  @Test
  void limitTurn_OppositeDirections_HalfTurn_ProducesZeroVector() {
    Vec3 current = new Vec3(1, 0, 0);
    Vec3 desired = new Vec3(-1, 0, 0);
    Vec3 out = KinematicsOps.limitTurn(current, desired, Math.PI / 2); // t=0.5 → 退化

    // 线性插值在正反方向中点退化为零向量，normaliseSafe 返回 Vec3.ZERO。
    assertEquals(Vec3.ZERO, out);
  }

  @Test
  void limitTurn_NearOpposite_AvoidNaN_StayHorizontal() {
    Vec3 current = new Vec3(1, 0, 0);
    // 近似对向，但加入极小 Y/Z 扰动，避免精确退化
    Vec3 desired = new Vec3(-1, 0.0, 1e-6).normalize();
    Vec3 out = KinematicsOps.limitTurn(current, desired, Math.PI / 6);

    // 不应出现 NaN/Inf
    assertTrue(Double.isFinite(out.x) && Double.isFinite(out.y) && Double.isFinite(out.z));
    // 期望基本保持水平（Y 轴不应被明显“抬起”）
    assertTrue(Math.abs(out.y) < 1e-3);
  }

  @Disabled("理想行为：应支持跨越 180° 的渐进转向；当前实现在对向时停滞/零向量退化")
  @Test
  void simulateTurningAcross180_ShouldProgressGradually() {
    Vec3 current = new Vec3(1, 0, 0);
    Vec3 desired = new Vec3(-1, 0, 0);
    double maxTurn = Math.PI / 18; // 10°/tick

    Vec3 dir = current;
    for (int i = 0; i < 36; i++) { // 期望 36 步可以完成 180° 转向
      dir = KinematicsOps.limitTurn(dir, desired, maxTurn);
      assertTrue(dir.length() > 0.0, "direction collapsed to zero unexpectedly");
      assertTrue(Math.abs(dir.y) < 1e-6, "unexpected vertical component raised");
    }
    // 最终应接近 desired（>0.99 余弦相似度）
    assertTrue(dir.normalize().dot(desired) > 0.99);
  }
}

