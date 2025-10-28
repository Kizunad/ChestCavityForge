package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_shi;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_shi.calculator.YuShiSummonComboLogic;

class YuShiSummonComboLogicTest {

  @Test
  void flowStatsCountsDistinctFlowsPerItem() {
    var flows =
        List.of(
            List.of("水道", "奴道"),
            List.of("水", "杂项"),
            List.of("奴道", "辅助"),
            List.of("无流派"));

    YuShiSummonComboLogic.FlowStats stats = YuShiSummonComboLogic.computeFlowStats(flows);
    assertEquals(2, stats.waterCount());
    assertEquals(2, stats.slaveCount());
  }

  @Test
  void modifiersScaleWithFlowsAndClamp() {
    YuShiSummonComboLogic.FlowStats stats = new YuShiSummonComboLogic.FlowStats(8, 12);
    YuShiSummonComboLogic.SummonModifiers modifiers =
        YuShiSummonComboLogic.computeModifiers(stats);

    assertEquals(1.30, modifiers.healthMultiplier(), 1e-6); // capped at +30%
    assertEquals(1.20, modifiers.speedMultiplier(), 1e-6); // capped at +20%
    assertTrue(modifiers.regenDurationTicks() > 0);
    assertEquals(1, modifiers.regenAmplifier());
    assertTrue(modifiers.resistanceDurationTicks() > 0);
    assertEquals(1, modifiers.resistanceAmplifier());
    assertEquals(12 * 20, modifiers.ttlBonusTicks());
  }

  @Test
  void zeroFlowsProduceNeutralModifiers() {
    YuShiSummonComboLogic.SummonModifiers modifiers =
        YuShiSummonComboLogic.computeModifiers(new YuShiSummonComboLogic.FlowStats(0, 0));

    assertEquals(1.0, modifiers.healthMultiplier(), 1e-6);
    assertEquals(1.0, modifiers.speedMultiplier(), 1e-6);
    assertEquals(0, modifiers.regenDurationTicks());
    assertEquals(0, modifiers.resistanceDurationTicks());
    assertEquals(0, modifiers.ttlBonusTicks());
  }
}
