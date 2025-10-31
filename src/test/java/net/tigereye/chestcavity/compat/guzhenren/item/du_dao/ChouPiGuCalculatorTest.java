package net.tigereye.chestcavity.compat.guzhenren.item.du_dao;

import static org.junit.jupiter.api.Assertions.*;

import net.tigereye.chestcavity.compat.guzhenren.item.du_dao.calculator.ChouPiGuCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.du_dao.tuning.ChouPiTuning;
import org.junit.jupiter.api.Test;

public class ChouPiGuCalculatorTest {

  @Test
  void triggerChanceWithIncrease_clampsAndScales() {
    assertEquals(0.20, ChouPiGuCalculator.triggerChanceWithIncrease(0.20, 0.0), 1e-9);
    assertEquals(0.40, ChouPiGuCalculator.triggerChanceWithIncrease(0.20, 1.0), 1e-9);
    assertEquals(0.20, ChouPiGuCalculator.triggerChanceWithIncrease(0.20, -5.0), 1e-9);
    assertEquals(1.0, ChouPiGuCalculator.triggerChanceWithIncrease(0.75, 1.0), 1e-9);
  }

  @Test
  void foodTriggerChance_respectsRottenMultiplierAndClamp() {
    double base = 0.30;
    assertEquals(base, ChouPiGuCalculator.foodTriggerChance(false, base), 1e-9);
    double rotten = ChouPiGuCalculator.foodTriggerChance(true, base);
    assertTrue(rotten >= base);
    assertTrue(rotten <= 1.0);
  }

  @Test
  void shouldTrigger_behavesWithRandom() {
    assertTrue(ChouPiGuCalculator.shouldTrigger(0.5, 0.2));
    assertFalse(ChouPiGuCalculator.shouldTrigger(0.2, 0.5));
    assertFalse(ChouPiGuCalculator.shouldTrigger(0.0, 0.0));
    assertTrue(ChouPiGuCalculator.shouldTrigger(1.0, 0.99999));
  }

  @Test
  void effectDurationTicks_respectsMinAndStacks() {
    int d1 = ChouPiGuCalculator.effectDurationTicks(1);
    assertTrue(d1 >= ChouPiTuning.MIN_EFFECT_DURATION_TICKS);
    int d3 = ChouPiGuCalculator.effectDurationTicks(3);
    assertEquals(Math.max(ChouPiTuning.MIN_EFFECT_DURATION_TICKS, 3 * ChouPiTuning.DURATION_PER_STACK_TICKS), d3);
  }

  @Test
  void poisonAmplifier_isFloorOfIncrease() {
    assertEquals(0, ChouPiGuCalculator.poisonAmplifier(0.0));
    assertEquals(0, ChouPiGuCalculator.poisonAmplifier(0.5));
    assertEquals(1, ChouPiGuCalculator.poisonAmplifier(1.0));
    assertEquals(2, ChouPiGuCalculator.poisonAmplifier(2.9));
    assertEquals(0, ChouPiGuCalculator.poisonAmplifier(-10));
  }

  @Test
  void residueCalculations_matchDesign() {
    float r = ChouPiGuCalculator.residueRadius(ChouPiTuning.EFFECT_RADIUS);
    assertTrue(r >= ChouPiTuning.RESIDUE_RADIUS_MIN);
    int rd = ChouPiGuCalculator.residueDurationTicks(120);
    assertTrue(rd >= ChouPiTuning.RESIDUE_MIN_DURATION_TICKS);
    assertEquals(60, ChouPiGuCalculator.residueDurationTicks(120));
  }

  @Test
  void randomIntervalTicks_inRangeAndUniformMapping() {
    int min = 100, max = 400;
    assertEquals(min, ChouPiGuCalculator.randomIntervalTicks(min, max, 0.0));
    assertTrue(ChouPiGuCalculator.randomIntervalTicks(min, max, 0.999999) <= max);
    assertEquals(min + (max - min) / 2, ChouPiGuCalculator.randomIntervalTicks(min, max, 0.5));
    assertEquals(min, ChouPiGuCalculator.randomIntervalTicks(min, min, 0.3));
  }
}

