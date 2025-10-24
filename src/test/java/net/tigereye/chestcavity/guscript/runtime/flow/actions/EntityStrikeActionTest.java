package net.tigereye.chestcavity.guscript.runtime.flow.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class EntityStrikeActionTest {

  @Test
  void rotateOffsetByYawAlignsRelativeToPerformer() {
    Vec3 offset = new Vec3(1.0D, 0.5D, -2.0D);
    Vec3 rotated = FlowActions.rotateOffsetByYaw(offset, 90.0F);

    assertEquals(-2.0D, rotated.x, 1.0E-6D);
    assertEquals(0.5D, rotated.y, 1.0E-6D);
    assertEquals(-1.0D, rotated.z, 1.0E-6D);
  }

  @Test
  void computeEntityStrikePositionAppliesDashAndYawOffset() {
    Vec3 performer = new Vec3(10.0D, 65.0D, 10.0D);
    Vec3 offset = new Vec3(1.0D, 0.5D, -2.0D);

    Vec3 finalPosition =
        FlowActions.computeEntityStrikePosition(performer, 90.0F, offset, -15.0F, 3.0D);

    assertEquals(5.1022D, finalPosition.x, 1.0E-3D);
    assertEquals(65.5D, finalPosition.y, 1.0E-3D);
    assertEquals(9.7764D, finalPosition.z, 1.0E-3D);
  }
}
