package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.calculator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ChanceOpsTest {

  @Test
  void deterministicWithSeed() {
    // 固定种子下，低概率基本为false，高概率基本为true
    assertFalse(ChanceOps.roll(123L, 0.01));
    assertTrue(ChanceOps.roll(123L, 0.99));
  }

  @Test
  void bounds() {
    assertFalse(ChanceOps.roll(42L, 0.0));
    assertTrue(ChanceOps.roll(42L, 1.0));
  }
}

