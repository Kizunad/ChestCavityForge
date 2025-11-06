package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.flyingsword.util;

import static org.junit.jupiter.api.Assertions.*;

import net.tigereye.chestcavity.compat.guzhenren.flyingsword.util.ItemDurabilityUtil;
import org.junit.jupiter.api.Test;

public class ItemDurabilityUtilTest {

  @Test
  void percentToDamage_basic() {
    assertEquals(0, ItemDurabilityUtil.percentToDamageValue(1.0, 100));
    assertEquals(100, ItemDurabilityUtil.percentToDamageValue(0.0, 100));
    assertEquals(50, ItemDurabilityUtil.percentToDamageValue(0.5, 100));
  }

  @Test
  void percentToDamage_roundingClamp() {
    // 99.8% → 0.2 → round(20/100)=0 in damage for 100 max
    assertEquals(0, ItemDurabilityUtil.percentToDamageValue(0.998, 100));
    // 超界值钳制
    assertEquals(0, ItemDurabilityUtil.percentToDamageValue(10.0, 100));
    assertEquals(100, ItemDurabilityUtil.percentToDamageValue(-5.0, 100));
    // 非法值
    assertEquals(0, ItemDurabilityUtil.percentToDamageValue(0.0/0.0, 100)); // NaN -> treat as 1.0 → 0 damage
  }

  @Test
  void damageToPercent_basic() {
    assertEquals(1.0, ItemDurabilityUtil.damageValueToPercent(0, 100), 1e-9);
    assertEquals(0.0, ItemDurabilityUtil.damageValueToPercent(100, 100), 1e-9);
    assertEquals(0.5, ItemDurabilityUtil.damageValueToPercent(50, 100), 1e-9);
  }

  @Test
  void invertApprox() {
    int max = 157; // 任意不整百，检验四舍五入边界
    for (double p : new double[] {1.0, 0.95, 0.5, 0.02, 0.0}) {
      int dmg = ItemDurabilityUtil.percentToDamageValue(p, max);
      double back = ItemDurabilityUtil.damageValueToPercent(dmg, max);
      // 允许四舍五入导致的 1/max 量级误差
      assertTrue(Math.abs(back - p) <= (1.0 / max + 1e-9),
          () -> "round-trip mismatch: p=" + p + " dmg=" + dmg + " back=" + back);
    }
  }
}
