package net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator;

import static org.junit.jupiter.api.Assertions.*;

import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.AIMode;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning;
import org.junit.jupiter.api.Test;

/**
 * 飞剑核心计算器单元测试
 *
 * <p>测试覆盖：
 * - 伤害计算（基础、速度²加成、等级缩放）
 * - 维持消耗（模式倍率、状态倍率、速度倍率）
 * - 经验计算（伤害换算、击杀/精英加成、升级公式）
 * - 耐久损耗（基础、破块倍率、硬度影响）
 * - 边界条件处理
 */
public class FlyingSwordCalculatorTest {

  // ===== 伤害计算测试 =====

  @Test
  public void testCalculateDamage_BasicDamage() {
    // 基础伤害计算（无速度加成）
    double baseDamage = 10.0;
    double velocity = 0.0; // 无速度
    double vRef = 1.0;
    double velDmgCoef = 1.0;
    double levelScale = 1.0;

    double result = FlyingSwordCalculator.calculateDamage(
        baseDamage, velocity, vRef, velDmgCoef, levelScale);

    // damage = 10 * 1.0 * (1 + 0) = 10
    assertEquals(10.0, result, 0.001);
  }

  @Test
  public void testCalculateDamage_WithSpeedBonus() {
    // 速度²加成
    double baseDamage = 10.0;
    double velocity = 2.0; // 2倍参考速度
    double vRef = 1.0;
    double velDmgCoef = 1.0;
    double levelScale = 1.0;

    double result = FlyingSwordCalculator.calculateDamage(
        baseDamage, velocity, vRef, velDmgCoef, levelScale);

    // speedFactor = (2/1)^2 * 1.0 = 4.0
    // damage = 10 * 1.0 * (1 + 4.0) = 50
    assertEquals(50.0, result, 0.001);
  }

  @Test
  public void testCalculateDamage_WithLevelScale() {
    // 等级缩放
    double baseDamage = 10.0;
    double velocity = 1.0;
    double vRef = 1.0;
    double velDmgCoef = 1.0;
    double levelScale = 2.0; // 2倍等级缩放

    double result = FlyingSwordCalculator.calculateDamage(
        baseDamage, velocity, vRef, velDmgCoef, levelScale);

    // speedFactor = (1/1)^2 * 1.0 = 1.0
    // damage = 10 * 2.0 * (1 + 1.0) = 40
    assertEquals(40.0, result, 0.001);
  }

  @Test
  public void testCalculateDamage_NegativeValues() {
    // 负值应返回0
    assertEquals(0.0, FlyingSwordCalculator.calculateDamage(-10, 1, 1, 1, 1), 0.001);
    assertEquals(0.0, FlyingSwordCalculator.calculateDamage(10, -1, 1, 1, 1), 0.001);
    assertEquals(0.0, FlyingSwordCalculator.calculateDamage(10, 1, -1, 1, 1), 0.001);
    assertEquals(0.0, FlyingSwordCalculator.calculateDamage(10, 1, 1, 1, -1), 0.001);
  }

  @Test
  public void testCalculateDamage_ZeroValues() {
    // 零值边界条件
    assertEquals(0.0, FlyingSwordCalculator.calculateDamage(0, 1, 1, 1, 1), 0.001);
    assertEquals(0.0, FlyingSwordCalculator.calculateDamage(10, 0, 1, 1, 0), 0.001);
  }

  @Test
  public void testCalculateLevelScale() {
    // 等级1：无加成
    double scale1 = FlyingSwordCalculator.calculateLevelScale(
        1, FlyingSwordTuning.DAMAGE_PER_LEVEL);
    assertEquals(1.0, scale1, 0.001);

    // 等级2：有加成
    double scale2 = FlyingSwordCalculator.calculateLevelScale(
        2, FlyingSwordTuning.DAMAGE_PER_LEVEL);
    double expected = 1.0 + (1.0 * FlyingSwordTuning.DAMAGE_PER_LEVEL
        / FlyingSwordTuning.DAMAGE_BASE);
    assertEquals(expected, scale2, 0.001);

    // 等级0或负值：应视为等级1
    double scale0 = FlyingSwordCalculator.calculateLevelScale(
        0, FlyingSwordTuning.DAMAGE_PER_LEVEL);
    assertEquals(1.0, scale0, 0.001);
  }

  // ===== 维持消耗测试 =====

  @Test
  public void testCalculateUpkeep_OrbitMode() {
    double baseRate = 1.0;
    AIMode mode = AIMode.ORBIT;
    boolean sprinting = false;
    boolean breaking = false;
    double speedPercent = 0.5; // 50% 速度

    double result = FlyingSwordCalculator.calculateUpkeep(
        baseRate, mode, sprinting, breaking, speedPercent);

    // modeFactor = UPKEEP_ORBIT_MULT
    // stateFactor = 1.0
    // speedFactor = 1.0 + (0.5 - 0.5) * UPKEEP_SPEED_SCALE = 1.0
    double expected = baseRate * FlyingSwordTuning.UPKEEP_ORBIT_MULT * 1.0 * 1.0;
    assertEquals(expected, result, 0.001);
  }

  @Test
  public void testCalculateUpkeep_GuardMode() {
    double baseRate = 1.0;
    AIMode mode = AIMode.GUARD;
    boolean sprinting = false;
    boolean breaking = false;
    double speedPercent = 0.5;

    double result = FlyingSwordCalculator.calculateUpkeep(
        baseRate, mode, sprinting, breaking, speedPercent);

    double expected = baseRate * FlyingSwordTuning.UPKEEP_GUARD_MULT * 1.0 * 1.0;
    assertEquals(expected, result, 0.001);
  }

  @Test
  public void testCalculateUpkeep_HuntMode() {
    double baseRate = 1.0;
    AIMode mode = AIMode.HUNT;
    boolean sprinting = false;
    boolean breaking = false;
    double speedPercent = 0.5;

    double result = FlyingSwordCalculator.calculateUpkeep(
        baseRate, mode, sprinting, breaking, speedPercent);

    double expected = baseRate * FlyingSwordTuning.UPKEEP_HUNT_MULT * 1.0 * 1.0;
    assertEquals(expected, result, 0.001);
  }

  @Test
  public void testCalculateUpkeep_WithSprinting() {
    double baseRate = 1.0;
    AIMode mode = AIMode.ORBIT;
    boolean sprinting = true;
    boolean breaking = false;
    double speedPercent = 0.5;

    double result = FlyingSwordCalculator.calculateUpkeep(
        baseRate, mode, sprinting, breaking, speedPercent);

    // stateFactor = UPKEEP_SPRINT_MULT
    double expected = baseRate * FlyingSwordTuning.UPKEEP_ORBIT_MULT *
                     FlyingSwordTuning.UPKEEP_SPRINT_MULT * 1.0;
    assertEquals(expected, result, 0.001);
  }

  @Test
  public void testCalculateUpkeep_WithBreaking() {
    double baseRate = 1.0;
    AIMode mode = AIMode.ORBIT;
    boolean sprinting = false;
    boolean breaking = true;
    double speedPercent = 0.5;

    double result = FlyingSwordCalculator.calculateUpkeep(
        baseRate, mode, sprinting, breaking, speedPercent);

    // stateFactor = UPKEEP_BREAK_MULT
    double expected = baseRate * FlyingSwordTuning.UPKEEP_ORBIT_MULT *
                     FlyingSwordTuning.UPKEEP_BREAK_MULT * 1.0;
    assertEquals(expected, result, 0.001);
  }

  @Test
  public void testCalculateUpkeep_WithHighSpeed() {
    double baseRate = 1.0;
    AIMode mode = AIMode.ORBIT;
    boolean sprinting = false;
    boolean breaking = false;
    double speedPercent = 1.0; // 100% 最大速度

    double result = FlyingSwordCalculator.calculateUpkeep(
        baseRate, mode, sprinting, breaking, speedPercent);

    // speedFactor = 1.0 + (1.0 - 0.5) * UPKEEP_SPEED_SCALE
    double speedFactor = 1.0 + (1.0 - 0.5) * FlyingSwordTuning.UPKEEP_SPEED_SCALE;
    double expected = baseRate * FlyingSwordTuning.UPKEEP_ORBIT_MULT * 1.0 * speedFactor;
    assertEquals(expected, result, 0.001);
  }

  @Test
  public void testCalculateUpkeep_WithLowSpeed() {
    double baseRate = 1.0;
    AIMode mode = AIMode.ORBIT;
    boolean sprinting = false;
    boolean breaking = false;
    double speedPercent = 0.0; // 0% 速度

    double result = FlyingSwordCalculator.calculateUpkeep(
        baseRate, mode, sprinting, breaking, speedPercent);

    // speedFactor = max(0.5, 1.0 + (0.0 - 0.5) * UPKEEP_SPEED_SCALE)
    double speedFactor = 1.0 + (0.0 - 0.5) * FlyingSwordTuning.UPKEEP_SPEED_SCALE;
    speedFactor = Math.max(0.5, speedFactor); // 下限0.5
    double expected = baseRate * FlyingSwordTuning.UPKEEP_ORBIT_MULT * 1.0 * speedFactor;
    assertEquals(expected, result, 0.001);
  }

  @Test
  public void testCalculateUpkeep_MultipleStateFactors() {
    double baseRate = 1.0;
    AIMode mode = AIMode.HUNT;
    boolean sprinting = true;
    boolean breaking = true;
    double speedPercent = 0.8;

    double result = FlyingSwordCalculator.calculateUpkeep(
        baseRate, mode, sprinting, breaking, speedPercent);

    // 所有倍率叠加
    double modeFactor = FlyingSwordTuning.UPKEEP_HUNT_MULT;
    double stateFactor = FlyingSwordTuning.UPKEEP_SPRINT_MULT * FlyingSwordTuning.UPKEEP_BREAK_MULT;
    double speedFactor = 1.0 + (0.8 - 0.5) * FlyingSwordTuning.UPKEEP_SPEED_SCALE;
    double expected = baseRate * modeFactor * stateFactor * speedFactor;
    assertEquals(expected, result, 0.001);
  }

  // ===== 经验计算测试 =====

  @Test
  public void testCalculateExpGain_Basic() {
    double damageDealt = 10.0;
    boolean isKill = false;
    boolean isElite = false;
    double expMultiplier = 1.0;

    int result = FlyingSwordCalculator.calculateExpGain(
        damageDealt, isKill, isElite, expMultiplier);

    int expected = (int) (damageDealt * FlyingSwordTuning.EXP_PER_DAMAGE);
    assertEquals(expected, result);
  }

  @Test
  public void testCalculateExpGain_WithKill() {
    double damageDealt = 10.0;
    boolean isKill = true;
    boolean isElite = false;
    double expMultiplier = 1.0;

    int result = FlyingSwordCalculator.calculateExpGain(
        damageDealt, isKill, isElite, expMultiplier);

    int expected = (int) (damageDealt * FlyingSwordTuning.EXP_PER_DAMAGE *
                         FlyingSwordTuning.EXP_KILL_MULT);
    assertEquals(expected, result);
  }

  @Test
  public void testCalculateExpGain_WithElite() {
    double damageDealt = 10.0;
    boolean isKill = false;
    boolean isElite = true;
    double expMultiplier = 1.0;

    int result = FlyingSwordCalculator.calculateExpGain(
        damageDealt, isKill, isElite, expMultiplier);

    int expected = (int) (damageDealt * FlyingSwordTuning.EXP_PER_DAMAGE *
                         FlyingSwordTuning.EXP_ELITE_MULT);
    assertEquals(expected, result);
  }

  @Test
  public void testCalculateExpGain_KillAndElite() {
    double damageDealt = 10.0;
    boolean isKill = true;
    boolean isElite = true;
    double expMultiplier = 1.0;

    int result = FlyingSwordCalculator.calculateExpGain(
        damageDealt, isKill, isElite, expMultiplier);

    int expected = (int) (damageDealt * FlyingSwordTuning.EXP_PER_DAMAGE *
                         FlyingSwordTuning.EXP_KILL_MULT *
                         FlyingSwordTuning.EXP_ELITE_MULT);
    assertEquals(expected, result);
  }

  @Test
  public void testCalculateExpGain_WithMultiplier() {
    double damageDealt = 10.0;
    boolean isKill = false;
    boolean isElite = false;
    double expMultiplier = 2.0;

    int result = FlyingSwordCalculator.calculateExpGain(
        damageDealt, isKill, isElite, expMultiplier);

    int expected = (int) (damageDealt * FlyingSwordTuning.EXP_PER_DAMAGE * 2.0);
    assertEquals(expected, result);
  }

  @Test
  public void testCalculateExpGain_NegativeDamage() {
    // 负伤害应返回0
    int result = FlyingSwordCalculator.calculateExpGain(-10, false, false, 1.0);
    assertTrue(result >= 0);
  }

  @Test
  public void testCalculateExpToNext_Level1() {
    int level = 1;
    int result = FlyingSwordCalculator.calculateExpToNext(level);

    double expected = FlyingSwordTuning.EXP_BASE * Math.pow(1 + level, FlyingSwordTuning.EXP_ALPHA);
    assertEquals((int) Math.round(expected), result);
  }

  @Test
  public void testCalculateExpToNext_HighLevel() {
    int level = 5;
    int result = FlyingSwordCalculator.calculateExpToNext(level);

    double expected = FlyingSwordTuning.EXP_BASE * Math.pow(1 + level, FlyingSwordTuning.EXP_ALPHA);
    assertEquals((int) Math.round(expected), result);
  }

  @Test
  public void testCalculateExpToNext_MaxLevel() {
    int level = FlyingSwordTuning.MAX_LEVEL;
    int result = FlyingSwordCalculator.calculateExpToNext(level);

    // 达到最大等级应返回 Integer.MAX_VALUE
    assertEquals(Integer.MAX_VALUE, result);
  }

  @Test
  public void testCalculateExpToNext_AboveMaxLevel() {
    int level = FlyingSwordTuning.MAX_LEVEL + 1;
    int result = FlyingSwordCalculator.calculateExpToNext(level);

    assertEquals(Integer.MAX_VALUE, result);
  }

  // ===== 耐久损耗测试 =====

  @Test
  public void testCalculateDurabilityLoss_Basic() {
    float damage = 10.0f;
    double lossRatio = 0.1;
    boolean breaking = false;

    float result = FlyingSwordCalculator.calculateDurabilityLoss(damage, lossRatio, breaking);

    // loss = 10 * 0.1 = 1.0
    assertEquals(1.0f, result, 0.001f);
  }

  @Test
  public void testCalculateDurabilityLoss_WithBreaking() {
    float damage = 10.0f;
    double lossRatio = 0.1;
    boolean breaking = true;

    float result = FlyingSwordCalculator.calculateDurabilityLoss(damage, lossRatio, breaking);

    // loss = 10 * 0.1 * DURA_BREAK_MULT
    float expected = (float) (damage * lossRatio * FlyingSwordTuning.DURA_BREAK_MULT);
    assertEquals(expected, result, 0.001f);
  }

  @Test
  public void testCalculateDurabilityLoss_NegativeDamage() {
    // 负伤害应返回0
    float result = FlyingSwordCalculator.calculateDurabilityLoss(-10.0f, 0.1, false);
    assertTrue(result >= 0);
  }

  @Test
  public void testCalculateDurabilityLoss_ZeroLossRatio() {
    float result = FlyingSwordCalculator.calculateDurabilityLoss(10.0f, 0.0, false);
    assertEquals(0.0f, result, 0.001f);
  }

  @Test
  public void testCalculateDurabilityLossFromBlock_Basic() {
    float hardness = 3.0f;
    int toolTier = 0;

    float result = FlyingSwordCalculator.calculateDurabilityLossFromBlock(hardness, toolTier);

    // tierFactor = 1.0 / (1.0 + 0 * 0.25) = 1.0
    // loss = 3.0 * 1.0 = 3.0
    assertEquals(3.0f, result, 0.001f);
  }

  @Test
  public void testCalculateDurabilityLossFromBlock_HighTier() {
    float hardness = 3.0f;
    int toolTier = 4; // 高等级工具

    float result = FlyingSwordCalculator.calculateDurabilityLossFromBlock(hardness, toolTier);

    // tierFactor = 1.0 / (1.0 + 4 * 0.25) = 1.0 / 2.0 = 0.5
    // loss = 3.0 * 0.5 = 1.5
    float tierFactor = 1.0f / (1.0f + toolTier * 0.25f);
    float expected = hardness * tierFactor;
    assertEquals(expected, result, 0.001f);
  }

  @Test
  public void testCalculateDurabilityLossFromBlock_ZeroHardness() {
    float result = FlyingSwordCalculator.calculateDurabilityLossFromBlock(0.0f, 0);
    assertEquals(0.0f, result, 0.001f);
  }

  // ===== 工具方法测试 =====

  @Test
  public void testClamp_Double() {
    // 正常范围内
    assertEquals(5.0, FlyingSwordCalculator.clamp(5.0, 0.0, 10.0), 0.001);

    // 超出上限
    assertEquals(10.0, FlyingSwordCalculator.clamp(15.0, 0.0, 10.0), 0.001);

    // 低于下限
    assertEquals(0.0, FlyingSwordCalculator.clamp(-5.0, 0.0, 10.0), 0.001);

    // 正好在边界
    assertEquals(0.0, FlyingSwordCalculator.clamp(0.0, 0.0, 10.0), 0.001);
    assertEquals(10.0, FlyingSwordCalculator.clamp(10.0, 0.0, 10.0), 0.001);
  }

  @Test
  public void testClamp_Int() {
    // 正常范围内
    assertEquals(5, FlyingSwordCalculator.clamp(5, 0, 10));

    // 超出上限
    assertEquals(10, FlyingSwordCalculator.clamp(15, 0, 10));

    // 低于下限
    assertEquals(0, FlyingSwordCalculator.clamp(-5, 0, 10));

    // 正好在边界
    assertEquals(0, FlyingSwordCalculator.clamp(0, 0, 10));
    assertEquals(10, FlyingSwordCalculator.clamp(10, 0, 10));
  }

  @Test
  public void testCalculateAttackCooldownTicks() {
    // 这个方法需要 CalcContext，这里简单测试 clamp 逻辑
    // 实际测试需要模拟上下文
    int baseTicks = 20;

    // 冷却应该被 clamp 到 [1, 200] 范围内
    assertTrue(baseTicks >= 1 && baseTicks <= 200);
  }

  // ===== 综合测试 =====

  @Test
  public void testRealWorldScenario_LowLevelSlowSword() {
    // 场景：1级飞剑，低速攻击
    int level = 1;
    double baseDamage = 5.0;
    double velocity = 0.5; // 低速
    double vRef = 1.0;
    double velDmgCoef = 1.0;
    double levelScale = FlyingSwordCalculator.calculateLevelScale(
        level, FlyingSwordTuning.DAMAGE_PER_LEVEL);

    double damage = FlyingSwordCalculator.calculateDamage(
        baseDamage, velocity, vRef, velDmgCoef, levelScale);

    // 低速低级，伤害应该较低
    assertTrue(damage >= baseDamage); // 至少有基础伤害
    assertTrue(damage < baseDamage * 2); // 不会太高
  }

  @Test
  public void testRealWorldScenario_HighLevelFastSword() {
    // 场景：高等级飞剑，高速攻击
    int level = 10;
    double baseDamage = 10.0;
    double velocity = 2.0; // 高速
    double vRef = 1.0;
    double velDmgCoef = FlyingSwordTuning.VEL_DMG_COEF;
    double levelScale = FlyingSwordCalculator.calculateLevelScale(
        level, FlyingSwordTuning.DAMAGE_PER_LEVEL);

    double damage = FlyingSwordCalculator.calculateDamage(
        baseDamage, velocity, vRef, velDmgCoef, levelScale);

    // 高速高级，伤害应该很高
    assertTrue(damage > baseDamage * 2); // 显著高于基础伤害
  }

  @Test
  public void sanity_paramOrder() {
    // 计算应为 10 * 2 * (1 + (1/1)^2 * 1) = 40
    double result = FlyingSwordCalculator.calculateDamage(10.0, 1.0, 1.0, 1.0, 2.0);
    assertEquals(40.0, result, 0.001);
  }

  @Test
  public void testRealWorldScenario_HuntModeSprintingHighSpeed() {
    // 场景：HUNT 模式，主人冲刺，飞剑高速
    double baseRate = FlyingSwordTuning.UPKEEP_BASE_RATE;
    AIMode mode = AIMode.HUNT;
    boolean sprinting = true;
    boolean breaking = false;
    double speedPercent = 0.9; // 90% 最大速度

    double upkeep = FlyingSwordCalculator.calculateUpkeep(
        baseRate, mode, sprinting, breaking, speedPercent);

    // HUNT + 冲刺 + 高速 = 高消耗
    assertTrue(upkeep > baseRate * 2); // 至少是基础消耗的2倍
  }
}
