package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.runtime.SwordShadowRuntime;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianYingTuning;
import org.junit.jupiter.api.Test;

public class SnapshotScalingTest {

  @Test
  void lifetimeMultiplier_scalesAndCaps() {
    // zero snapshot => 1.0
    assertEquals(1.0, SwordShadowRuntime.computeCloneLifetimeMultiplier(0.0, 0.0), 1e-6);

    // moderate values
    double d = 100.0; // daoHen
    double e = 100.0; // liuPai
    double expected =
        1.0 + Math.min(JianYingTuning.CLONE_DURATION_BONUS_CAP,
            d * JianYingTuning.CLONE_DURATION_DAOHEN_COEF
                + e * JianYingTuning.CLONE_DURATION_LIUPAI_COEF);
    assertEquals(expected, SwordShadowRuntime.computeCloneLifetimeMultiplier(d, e), 1e-6);

    // huge values -> capped
    double cap = 1.0 + JianYingTuning.CLONE_DURATION_BONUS_CAP;
    assertEquals(cap, SwordShadowRuntime.computeCloneLifetimeMultiplier(1e6, 1e6), 1e-6);
  }

  @Test
  void damageMultiplier_scalesAndCaps() {
    assertEquals(1.0, SwordShadowRuntime.computeCloneDamageMultiplier(0.0), 1e-6);

    double d = 200.0;
    double expected =
        1.0
            + Math.min(
                JianYingTuning.CLONE_DAMAGE_BONUS_CAP,
                d * JianYingTuning.CLONE_DAMAGE_DAOHEN_COEF);
    assertEquals(expected, SwordShadowRuntime.computeCloneDamageMultiplier(d), 1e-6);

    double cap = 1.0 + JianYingTuning.CLONE_DAMAGE_BONUS_CAP;
    assertEquals(cap, SwordShadowRuntime.computeCloneDamageMultiplier(1e6), 1e-6);
  }
}

