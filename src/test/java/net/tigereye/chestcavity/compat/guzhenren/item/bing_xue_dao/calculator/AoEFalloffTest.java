package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.calculator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AoEFalloffTest {

  @Test
  void linear_basicBoundaries() {
    assertEquals(1.0, AoEFalloff.linear(0.0, 4.0), 1e-9);
    assertEquals(0.0, AoEFalloff.linear(4.0, 4.0), 1e-9);
    assertEquals(0.0, AoEFalloff.linear(5.0, 4.0), 1e-9);
  }

  @Test
  void linear_monotonic() {
    double r = 10.0;
    double a = AoEFalloff.linear(2.0, r);
    double b = AoEFalloff.linear(5.0, r);
    double c = AoEFalloff.linear(8.0, r);
    assertTrue(a > b && b > c);
  }

  @Test
  void linear_zeroRadius() {
    assertEquals(0.0, AoEFalloff.linear(0.0, 0.0), 1e-9);
  }
}

