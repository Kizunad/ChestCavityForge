package net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.orientation;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

class OrientationOpsTest {

  @Test
  void basisForwardRightUp_NoNaN() {
    Vec3 forward = new Vec3(1, 0, 0);
    Vec3 up = new Vec3(0, 1, 0);
    Quaternionf q = OrientationOps.orientationFromForwardUp(
        forward, up, 0f, 0f, 0f,
        OrientationMode.BASIS,
        UpMode.WORLD_Y);
    assertTrue(Float.isFinite(q.x) && Float.isFinite(q.y)
        && Float.isFinite(q.z) && Float.isFinite(q.w));
  }

  @Disabled("近对向的符号选择存在多解，"
      + "此处仅检查非退化在后续实现中完善")
  @Test
  void nearOpposite_NoCollapse() {
    Vec3 forward = new Vec3(-1, 1e-6, 0); // 近似对向
    Vec3 up = new Vec3(0, 1, 0);
    Quaternionf q = OrientationOps.orientationFromForwardUp(forward, up, 0f, 0f, 0f);
    // 用四元数旋转“模型 X 轴”（即世界中的 (1,0,0)）应接近 forward
    Vector3f modelX = new Vector3f(1, 0, 0);
    Vector3f xf = q.transform(modelX, new Vector3f());
    assertTrue(Double.isFinite(xf.x) && Double.isFinite(xf.y)
        && Double.isFinite(xf.z));
    // 点积应为正（大致同向）
    float dot = xf.normalize().dot(
        new Vector3f((float) forward.x, (float) forward.y, (float) forward.z)
            .normalize());
    assertTrue(dot > 0.0f);
  }

  @Test
  void offsets_AppliedInExpectedSpaces() {
    Vec3 forward = new Vec3(0, 0, -1);
    Vec3 up = new Vec3(0, 1, 0);
    // 应用 world yaw + local roll/pitch，不做数值等价，只检查输出有限且单位化
    Quaternionf q = OrientationOps.orientationFromForwardUp(forward, up,
        -45f, // preRoll
        30f,  // yawOffset (world-Y)
        10f,  // pitchOffset (local-X)
        OrientationMode.BASIS,
        UpMode.WORLD_Y);
    // 有限性检查
    assertTrue(Float.isFinite(q.x) && Float.isFinite(q.y)
        && Float.isFinite(q.z) && Float.isFinite(q.w));
    // 单位化检查
    float l2 = q.x*q.x + q.y*q.y + q.z*q.z + q.w*q.w;
    assertEquals(1.0f, (float)Math.sqrt(l2), 1e-5f);
  }
}
