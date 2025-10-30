package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun.tuning;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

public class YuQunTuningTest {

    @Test
    public void testComputeCooldownTicks() {
        // Test case 1: No experience, should return base cooldown
        assertEquals(YuQunTuning.BASE_COOLDOWN_TICKS, YuQunTuning.computeCooldownTicks(0));

        // Test case 2: Experience is exactly the threshold
        assertEquals(80, YuQunTuning.computeCooldownTicks(10001.0D));

        // Test case 3: Experience is half of the threshold
        assertEquals(160, YuQunTuning.computeCooldownTicks(5000.5D));

        // Test case 4: Experience is more than the threshold
        assertEquals(80, YuQunTuning.computeCooldownTicks(20000));

        // Test case 5: Cooldown calculation with very high experience should return the minimum cooldown tier
        assertEquals(80, YuQunTuning.computeCooldownTicks(Double.MAX_VALUE));
    }

    @Test
    public void testComputeCooldownTicksMinimum() {
        // Create a custom list of cooldown tiers for testing the minimum cooldown
        List<YuQunTuning.CooldownTier> testTiers = List.of(new YuQunTuning.CooldownTier(100.0D, 4));

        // Test case 6: Cooldown calculation results in less than 5 ticks
        assertEquals(5, YuQunTuning.computeCooldownTicks(100.0D, testTiers));
    }
}
