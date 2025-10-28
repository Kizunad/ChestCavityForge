package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun.calculator.YuQunComboLogic;
import org.junit.jupiter.api.Test;

class YuQunComboLogicTest {

  @Test
  void computeParametersBaseline() {
    var params = YuQunComboLogic.computeParameters(0);
    assertEquals(10.0, params.range(), 1e-6);
    assertEquals(1.75, params.width(), 1e-6);
    assertEquals(0.45, params.pushStrength(), 1e-6);
    assertEquals(60, params.slowDurationTicks());
    assertEquals(0, params.slowAmplifier());
    assertFalse(params.spawnSplashParticles());
  }

  @Test
  void computeParametersCapsAtTen() {
    var params = YuQunComboLogic.computeParameters(15);
    assertEquals(16.0, params.range(), 1e-6); // 10 + 10*0.6
    assertEquals(2.75, params.width(), 1e-6); // 1.75 + 10*0.1
    assertEquals(0.65, params.pushStrength(), 1e-6);
    assertEquals(100, params.slowDurationTicks());
    assertEquals(1, params.slowAmplifier());
    assertTrue(params.spawnSplashParticles());
  }

  @Test
  void isWithinConeRespectsRangeAndWidth() {
    Vec3 origin = Vec3.ZERO;
    Vec3 dir = new Vec3(1, 0, 0);
    Vec3 inside = new Vec3(5, 0.2, 0.2);
    Vec3 outsideRange = new Vec3(12, 0, 0);
    Vec3 outsideWidth = new Vec3(5, 2, 0);

    assertTrue(YuQunComboLogic.isWithinCone(origin, dir, inside, 10.0, 1.0));
    assertFalse(YuQunComboLogic.isWithinCone(origin, dir, outsideRange, 10.0, 1.0));
    assertFalse(YuQunComboLogic.isWithinCone(origin, dir, outsideWidth, 10.0, 1.0));
  }
}
