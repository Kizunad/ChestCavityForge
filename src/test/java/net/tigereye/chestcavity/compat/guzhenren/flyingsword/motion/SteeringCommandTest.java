package net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/**
 * SteeringCommand 纯逻辑单元测试：验证链式配置与默认值。
 */
class SteeringCommandTest {

  @Test
  void of_ShouldSetDirectionAndSpeedScale() {
    Vec3 dir = new Vec3(1, 2, 3);
    SteeringCommand cmd = SteeringCommand.of(dir, 0.75);
    assertEquals(dir, cmd.direction());
    assertEquals(0.75, cmd.speedScale(), 1e-9);
    assertNull(cmd.desiredMaxFactor());
    assertNull(cmd.accelOverride());
    assertNull(cmd.turnOverride());
    assertNull(cmd.turnPerTick());
    assertNull(cmd.headingKp());
    assertNull(cmd.minTurnFloor());
    assertFalse(cmd.suppressSeparation());
  }

  @Test
  void chain_ShouldBuildIndependentCopies() {
    SteeringCommand base = SteeringCommand.of(new Vec3(0, 0, 1), 1.0);
    SteeringCommand a = base.withDesiredMaxFactor(0.8);
    SteeringCommand b = a.withAccelFactor(1.2);
    SteeringCommand c = b.withTurnOverride(Math.PI/6).withTurnPerTick(Math.PI/10);
    SteeringCommand d = c.withHeadingKp(0.7).withMinTurnFloor(0.05).disableSeparation();

    // 基对象不变
    assertNull(base.desiredMaxFactor());
    assertNull(base.accelOverride());
    assertNull(base.turnOverride());
    assertNull(base.turnPerTick());
    assertNull(base.headingKp());
    assertNull(base.minTurnFloor());
    assertFalse(base.suppressSeparation());

    // 终态对象具有全部配置
    assertEquals(0.8, d.desiredMaxFactor(), 1e-9);
    assertEquals(1.2, d.accelOverride(), 1e-9);
    assertEquals(Math.PI/6, d.turnOverride(), 1e-9);
    assertEquals(Math.PI/10, d.turnPerTick(), 1e-9);
    assertEquals(0.7, d.headingKp(), 1e-9);
    assertEquals(0.05, d.minTurnFloor(), 1e-9);
    assertTrue(d.suppressSeparation());
  }
}

