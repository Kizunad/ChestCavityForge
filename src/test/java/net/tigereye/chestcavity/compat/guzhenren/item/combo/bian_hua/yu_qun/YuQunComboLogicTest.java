package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun;

import static org.junit.jupiter.api.Assertions.*;

import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun.calculator.YuQunComboLogic;
import org.junit.jupiter.api.Test;

class YuQunComboLogicTest {

    @Test
    void computeParameters_baseline_noSynergyNoDaoHen() {
        var params = YuQunComboLogic.computeParameters(0, 0, 0, 0);
        assertEquals(8.0, params.range(), 1e-6);
        assertEquals(0.8, params.width(), 1e-6);
        assertEquals(1.2, params.pushStrength(), 1e-6);
        assertEquals(60, params.slowDurationTicks());
        assertEquals(0, params.slowAmplifier());
        assertFalse(params.spawnSplashParticles());
    }

    @Test
    void computeParameters_withSynergyOnly() {
        var params = YuQunComboLogic.computeParameters(0, 0, 0, 9); // GS = 3
        assertTrue(params.range() > 8.0);
        assertTrue(params.pushStrength() > 1.2);
        assertTrue(params.width() < 0.8); // Synergy widens the cone (decreases cos)
        assertEquals(96, params.slowDurationTicks());
        assertEquals(1, params.slowAmplifier());
        assertTrue(params.spawnSplashParticles());
    }

    @Test
    void computeParameters_withWaterDaoHenOnly() {
        var params = YuQunComboLogic.computeParameters(600, 0, 0, 0); // FW ≈ 0.693
        assertTrue(params.range() > 8.0);
        assertTrue(params.pushStrength() > 1.2);
        assertTrue(params.width() < 0.8);
        assertTrue(params.slowDurationTicks() > 60);
        assertEquals(0, params.slowAmplifier());
        assertFalse(params.spawnSplashParticles());
    }

    @Test
    void computeParameters_withFireDaoHenOnly_shouldDebuff() {
        var params = YuQunComboLogic.computeParameters(0, 0, 600, 0); // FF ≈ 0.693
        assertTrue(params.range() < 8.0);
        assertTrue(params.pushStrength() < 1.2);
        assertTrue(params.width() > 0.8); // Fire narrows the cone (increases cos)
    }

    @Test
    void computeParameters_mixedScenario_allStats() {
        var params = YuQunComboLogic.computeParameters(1200, 800, 400, 16);
        assertNotNull(params);
        // Assert that the final values are reasonable and not NaN or Infinity
        assertTrue(Double.isFinite(params.range()));
        assertTrue(Double.isFinite(params.width()));
        assertTrue(Double.isFinite(params.pushStrength()));
    }

    @Test
    void width_shouldBeClamped() {
        // High water/change/synergy to push width down to its minimum
        var params1 = YuQunComboLogic.computeParameters(1_000_000, 1_000_000, 0, 400);
        assertEquals(0.05, params1.width(), 1e-6, "Width should be clamped at COS_MIN");

        // High fire to push width up to its maximum
        var params2 = YuQunComboLogic.computeParameters(0, 0, 1_000_000, 0);
        assertEquals(0.95, params2.width(), 1e-6, "Width should be clamped at COS_MAX");
    }

    @Test
    void isWithinConeRespectsRangeAndWidth() {
        net.minecraft.world.phys.Vec3 origin = net.minecraft.world.phys.Vec3.ZERO;
        net.minecraft.world.phys.Vec3 dir = new net.minecraft.world.phys.Vec3(1, 0, 0);
        net.minecraft.world.phys.Vec3 inside = new net.minecraft.world.phys.Vec3(5, 0.2, 0.2);
        net.minecraft.world.phys.Vec3 outsideRange = new net.minecraft.world.phys.Vec3(12, 0, 0);
        net.minecraft.world.phys.Vec3 outsideWidth = new net.minecraft.world.phys.Vec3(5, 2, 0);

        assertTrue(YuQunComboLogic.isWithinCone(origin, dir, inside, 10.0, 0.98));
        assertFalse(YuQunComboLogic.isWithinCone(origin, dir, outsideRange, 10.0, 0.98));
        assertFalse(YuQunComboLogic.isWithinCone(origin, dir, outsideWidth, 10.0, 0.98));
    }
}
