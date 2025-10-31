package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.calculator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ConeFilterTest {

  @Test
  void matches_forwardAndSide() {
    // look along +Z, target also along +Z -> hit when threshold <= 1
    assertTrue(ConeFilter.matches(0, 0, 1, 0, 0, 2, 0.5));
    // 90 degrees to the side: dot=0 -> hit if threshold <= 0
    assertTrue(ConeFilter.matches(0, 0, 1, 1, 0, 0, 0.0));
    assertFalse(ConeFilter.matches(0, 0, 1, 1, 0, 0, 0.1));
  }

  @Test
  void matches_backward() {
    // opposite direction: dot=-1 -> only hit if threshold <= -1
    assertTrue(ConeFilter.matches(0, 0, 1, 0, 0, -5, -1.0));
    assertFalse(ConeFilter.matches(0, 0, 1, 0, 0, -5, -0.9));
  }
}

