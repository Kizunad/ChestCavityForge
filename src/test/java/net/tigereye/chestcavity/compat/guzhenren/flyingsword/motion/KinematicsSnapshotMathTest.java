package net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/**
 * KinematicsSnapshot 纯函数行为测试：缩放与基准换算。
 */
class KinematicsSnapshotMathTest {

  @Test
  void scaledValues_ShouldRespectDomainScale() {
    // currentVelocity 任意即可，测试缩放数学
    Vec3 v = new Vec3(0.3, 0, 0.4);
    KinematicsSnapshot snap = new KinematicsSnapshot(
        v, /*effectiveBase*/ 2.0, /*effectiveMax*/ 6.0, /*effectiveAccel*/ 1.2,
        /*turnRate*/ 0.4, /*domainScale*/ 0.5);

    assertEquals(3.0, snap.scaledMaxSpeed(), 1e-9);
    assertEquals(1.0, snap.scaledBaseSpeed(), 1e-9);
    assertEquals(0.6, snap.scaledAccel(), 1e-9);
    assertEquals(0.2, snap.scaledTurnRate(), 1e-9);
  }

  @Test
  void baseScale_WithSmallRawBase_ShouldFallbackTo1() {
    Vec3 v = Vec3.ZERO;
    KinematicsSnapshot snap = new KinematicsSnapshot(v, 2.0, 4.0, 1.0, 0.2, 1.0);
    assertEquals(1.0, snap.baseScale(0.0));
    assertEquals(1.0, snap.baseScale(1.0e-12));
  }

  @Test
  void baseScale_Normal() {
    KinematicsSnapshot snap = new KinematicsSnapshot(Vec3.ZERO, 2.5, 5.0, 1.0, 0.2, 1.0);
    assertEquals(0.5, snap.baseScale(5.0), 1e-9);
  }
}

