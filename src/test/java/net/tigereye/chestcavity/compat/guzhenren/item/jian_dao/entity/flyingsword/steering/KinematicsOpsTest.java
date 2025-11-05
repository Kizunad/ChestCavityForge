package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.steering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class KinematicsOpsTest {

  @Test
  void limitTurnShouldRespectMaxRadians() {
    Vec3 current = new Vec3(1, 0, 0);
    Vec3 desired = new Vec3(0, 0, 1);

    Vec3 limited = KinematicsOps.limitTurn(current, desired, Math.PI / 4);
    double angle =
        Math.acos(
            KinematicsOps.normaliseSafe(current)
                .dot(KinematicsOps.normaliseSafe(limited)));

    assertTrue(angle <= Math.PI / 4 + 1e-6);
  }

  @Test
  void limitDeltaShouldCapLength() {
    Vec3 delta = new Vec3(3, 4, 0);
    Vec3 limited = KinematicsOps.limitDelta(delta, 2.0);
    assertEquals(2.0, limited.length(), 1e-6);
  }

  @Test
  void clampVectorLengthKeepsShortVectors() {
    Vec3 vec = new Vec3(0.5, 0, 0);
    Vec3 limited = KinematicsOps.clampVectorLength(vec, 2.0);
    assertEquals(vec, limited);
  }
}
