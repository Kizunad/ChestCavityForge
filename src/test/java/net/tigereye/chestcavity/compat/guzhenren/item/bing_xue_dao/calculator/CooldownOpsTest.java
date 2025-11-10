package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.calculator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CooldownOpsTest {

    @Test
    public void testWithBingXueExp() {
        long baseCooldown = 200; // 10 seconds

        // Test at exp = 0 (no reduction)
        assertEquals(baseCooldown, CooldownOps.withBingXueExp(baseCooldown, 0));

        // Test at exp = 5000 (mid-point, approx 50% reduction)
        // Expected: 200 - round( (200 - 20) * (5000 / 10001.0) )
        //         = 200 - round(180 * 0.49995) = 200 - 90 = 110
        assertEquals(110, CooldownOps.withBingXueExp(baseCooldown, 5000));

        // Test at exp = 10001 (max reduction)
        assertEquals(20, CooldownOps.withBingXueExp(baseCooldown, 10001));

        // Test with a base cooldown already below the floor
        assertEquals(15, CooldownOps.withBingXueExp(15, 5000));

        // Test with negative experience (should be clamped to 0)
        assertEquals(baseCooldown, CooldownOps.withBingXueExp(baseCooldown, -100));

        // Test with experience over the max (should be clamped to max)
        assertEquals(20, CooldownOps.withBingXueExp(baseCooldown, 12000));
    }
}
