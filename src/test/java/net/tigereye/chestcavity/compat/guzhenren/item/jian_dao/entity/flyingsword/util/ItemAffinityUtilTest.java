package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.util.ItemAffinityUtil;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordAttributes;

class ItemAffinityUtilTest {

  @Test
  void toModifiers_FromMetrics_ApplyConfig() {
    ItemAffinityUtil.ItemMetrics m = new ItemAffinityUtil.ItemMetrics();
    m.attackDamage = 10.0;
    m.attackSpeed = -3.0; // negative typical
    m.armor = 5.0;
    m.armorToughness = 2.0;
    m.toolTier = 3;
    m.toolSpeed = 8.0;
    m.maxDamage = 1500;
    m.sharpnessLevel = 2;
    m.unbreakingLevel = 1;
    m.sweepingLevel = 1;
    m.efficiencyLevel = 3;

    ItemAffinityUtil.Config cfg = new ItemAffinityUtil.Config();
    cfg.attackDamageCoef = 0.5;
    cfg.attackSpeedAbsCoef = 0.05;
    cfg.sharpnessDmgPerLvl = 0.5;
    cfg.sharpnessVelPerLvl = 0.03;
    cfg.unbreakingLossMultPerLvl = 0.9;
    cfg.sweepingBase = 0.30;
    cfg.sweepingPerLvl = 0.15;
    cfg.efficiencyBlockEffPerLvl = 0.5;
    cfg.miningSpeedToBlockEffCoef = 0.05;
    cfg.maxDamageToMaxDurabilityCoef = 0.10;
    cfg.armorToMaxDurabilityCoef = 8.0;
    cfg.armorDuraLossMultPerPoint = 0.97;

    FlyingSwordAttributes.AttributeModifiers mod = ItemAffinityUtil.toModifiers(m, cfg);

    // Damage: 10*0.5 + sharpness(2*0.5) = 5 + 1 = 6
    assertEquals(6.0, mod.damageBase, 1e-6);
    // SpeedMax: | -3 |*0.05 = 0.15
    assertEquals(0.15, mod.speedMax, 1e-6);
    // velDmgCoef: sharpness 2*0.03 = 0.06
    assertEquals(0.06, mod.velDmgCoef, 1e-6);
    // maxDurability: 1500*0.10 + (armorScore 5+2*0.5=6)*8 = 150 + 48 = 198
    assertEquals(198.0, mod.maxDurability, 1e-6);
    // duraLossRatioMult: unbreaking 0.9^1, armor 0.97^6
    double expectedLossMult = Math.pow(0.9, 1) * Math.pow(0.97, 6.0);
    assertEquals(expectedLossMult, mod.duraLossRatioMult, 1e-6);
    // blockBreakEff: efficiency 3*0.5 + toolSpeed 8*0.05 = 1.5 + 0.4 = 1.9
    assertEquals(1.9, mod.blockBreakEff, 1e-6);
    // toolTier propagated (>=)
    assertEquals(3, mod.toolTier);
    // sweeping percent
    assertEquals(0.45, mod.sweepPercent, 1e-6);
  }
}
