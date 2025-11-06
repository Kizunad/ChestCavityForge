package net.tigereye.chestcavity.compat.guzhenren.flyingsword.systems;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordAttributes;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.AIMode;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator.context.CalcContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

/**
 * CombatSystem 集成测试。
 *
 * <p>由于 CombatSystem 是薄包装层，委托给 FlyingSwordCombat 处理具体逻辑，
 * 且需要完整的 Minecraft 环境（Entity、Level、碰撞检测等），本测试主要覆盖：
 * <ul>
 *   <li>null 安全检查</li>
 *   <li>伤害计算接口的基本行为</li>
 *   <li>tick 方法的基本调用路径</li>
 * </ul>
 *
 * <p>注意：完整的战斗逻辑测试（碰撞检测、经验获取、耐久消耗等）需要集成测试环境，
 * 优先级较低，建议通过手动测试验证（参考 docs/MANUAL_TEST_CHECKLIST.md）。
 */
@Disabled("需要 MC 实体/关卡环境，单元测试阶段跳过；请在集成环境或游戏内验证")
class CombatSystemTest {

  // ========== null 安全检查 ==========

  @Test
  void tick_NullSword_ShouldNotThrow() {
    assertDoesNotThrow(() -> CombatSystem.tick(null),
        "tick() should handle null sword gracefully");
  }

  @Test
  void tick_SwordWithNullLevel_ShouldNotThrow() {
    FlyingSwordEntity mockSword = mock(FlyingSwordEntity.class);
    when(mockSword.level()).thenReturn(null);

    assertDoesNotThrow(() -> CombatSystem.tick(mockSword),
        "tick() should handle null level gracefully");
  }

  @Test
  void calculateCurrentDamage_NullSword_ReturnsZero() {
    double damage = CombatSystem.calculateCurrentDamage(null);
    assertEquals(0.0, damage, 1e-9, "null sword should return 0 damage");
  }

  // ========== 伤害计算接口测试 ==========

  @Test
  void calculateCurrentDamage_ValidSword_ReturnsNonNegativeDamage() {
    FlyingSwordEntity mockSword = createMockSwordWithBasicAttributes();

    double damage = CombatSystem.calculateCurrentDamage(mockSword);

    assertTrue(damage >= 0.0, "Damage should be non-negative: " + damage);
  }

  @Test
  void calculateCurrentDamage_ZeroVelocity_ReturnsBaseDamage() {
    FlyingSwordEntity mockSword = createMockSwordWithBasicAttributes();
    when(mockSword.getDeltaMovement()).thenReturn(Vec3.ZERO);

    double damage = CombatSystem.calculateCurrentDamage(mockSword);

    // 零速度时应返回基础伤害（取决于 FlyingSwordCalculator 的实现）
    assertTrue(damage >= 0.0, "Zero velocity should return non-negative base damage");
  }

  @Test
  void calculateCurrentDamage_HighVelocity_ReturnsHigherDamage() {
    FlyingSwordEntity mockSword = createMockSwordWithBasicAttributes();

    // 低速
    when(mockSword.getDeltaMovement()).thenReturn(new Vec3(0.5, 0, 0));
    double lowSpeedDamage = CombatSystem.calculateCurrentDamage(mockSword);

    // 高速（速度²应该导致伤害增加）
    when(mockSword.getDeltaMovement()).thenReturn(new Vec3(2.0, 0, 0));
    double highSpeedDamage = CombatSystem.calculateCurrentDamage(mockSword);

    assertTrue(highSpeedDamage > lowSpeedDamage,
        "Higher velocity should result in higher damage: " +
        highSpeedDamage + " vs " + lowSpeedDamage);
  }

  @Test
  void calculateCurrentDamage_DifferentLevels_AffectsDamage() {
    FlyingSwordEntity mockSword = createMockSwordWithBasicAttributes();
    when(mockSword.getDeltaMovement()).thenReturn(new Vec3(1.0, 0, 0));

    // 等级1
    when(mockSword.getSwordLevel()).thenReturn(1);
    double level1Damage = CombatSystem.calculateCurrentDamage(mockSword);

    // 等级10（等级缩放应该增加伤害）
    when(mockSword.getSwordLevel()).thenReturn(10);
    double level10Damage = CombatSystem.calculateCurrentDamage(mockSword);

    assertTrue(level10Damage >= level1Damage,
        "Higher level should result in equal or higher damage: " +
        level10Damage + " vs " + level1Damage);
  }

  // ========== tick 方法基本行为测试 ==========

  @Test
  void tick_ClientSide_ShouldNotCrash() {
    FlyingSwordEntity mockSword = createMockSwordWithBasicAttributes();
    // 客户端 world（非 ServerLevel）
    when(mockSword.level()).thenReturn(mock(net.minecraft.world.level.Level.class));

    assertDoesNotThrow(() -> CombatSystem.tick(mockSword),
        "tick() should handle client-side world gracefully");
  }

  @Test
  void tick_ServerSide_ShouldNotCrash() {
    FlyingSwordEntity mockSword = createMockSwordWithBasicAttributes();
    ServerLevel mockLevel = mock(ServerLevel.class);
    when(mockSword.level()).thenReturn(mockLevel);

    // 基本的 mock 设置以避免 NPE
    when(mockSword.position()).thenReturn(Vec3.ZERO);
    when(mockSword.getBoundingBox()).thenReturn(
        new net.minecraft.world.phys.AABB(0, 0, 0, 1, 1, 1));

    assertDoesNotThrow(() -> CombatSystem.tick(mockSword),
        "tick() should not crash on server-side");
  }

  // ========== 边界条件测试 ==========

  @Test
  void calculateCurrentDamage_NegativeLevel_ShouldNotCrash() {
    FlyingSwordEntity mockSword = createMockSwordWithBasicAttributes();
    when(mockSword.getSwordLevel()).thenReturn(-1);

    assertDoesNotThrow(() -> {
      double damage = CombatSystem.calculateCurrentDamage(mockSword);
      assertTrue(damage >= 0.0, "Negative level should not produce negative damage");
    }, "Should handle negative level gracefully");
  }

  @Test
  void calculateCurrentDamage_VeryHighVelocity_ShouldNotOverflow() {
    FlyingSwordEntity mockSword = createMockSwordWithBasicAttributes();
    // 极高速度
    when(mockSword.getDeltaMovement()).thenReturn(new Vec3(100.0, 0, 0));

    double damage = CombatSystem.calculateCurrentDamage(mockSword);

    assertTrue(Double.isFinite(damage), "Very high velocity should not cause overflow");
    assertTrue(damage >= 0.0, "Damage should remain non-negative");
  }

  @Test
  void calculateCurrentDamage_ZeroAttributes_ShouldNotCrash() {
    FlyingSwordEntity mockSword = mock(FlyingSwordEntity.class);
    ServerLevel mockLevel = mock(ServerLevel.class);

    // 极端情况：所有属性为0
    FlyingSwordAttributes zeroAttrs = new FlyingSwordAttributes();
    zeroAttrs.speedBase = 0.0;
    zeroAttrs.speedMax = 0.0;
    zeroAttrs.accel = 0.0;
    zeroAttrs.turnRate = 0.0;
    zeroAttrs.damageBase = 0.0;
    zeroAttrs.velDmgCoef = 0.0;
    zeroAttrs.maxDurability = 0.0;
    zeroAttrs.duraLossRatio = 0.0;

    when(mockSword.level()).thenReturn(mockLevel);
    when(mockSword.getSwordLevel()).thenReturn(1);
    when(mockSword.getDeltaMovement()).thenReturn(Vec3.ZERO);
    when(mockSword.getSwordAttributes()).thenReturn(zeroAttrs);
    when(mockSword.getAIMode()).thenReturn(AIMode.ORBIT);

    assertDoesNotThrow(() -> {
      double damage = CombatSystem.calculateCurrentDamage(mockSword);
      assertTrue(damage >= 0.0, "Zero attributes should not produce negative damage");
    }, "Should handle zero attributes gracefully");
  }

  // ========== 集成行为测试 ==========

  @Test
  void calculateCurrentDamage_ConsistentResults_ForSameInput() {
    FlyingSwordEntity mockSword = createMockSwordWithBasicAttributes();
    when(mockSword.getDeltaMovement()).thenReturn(new Vec3(1.5, 0, 0));
    when(mockSword.getSwordLevel()).thenReturn(5);

    double damage1 = CombatSystem.calculateCurrentDamage(mockSword);
    double damage2 = CombatSystem.calculateCurrentDamage(mockSword);

    assertEquals(damage1, damage2, 1e-9,
        "Same input should produce consistent damage calculations");
  }

  @Test
  void calculateCurrentDamage_DifferentVelocityDirections_SameMagnitude_SameDamage() {
    FlyingSwordEntity mockSword = createMockSwordWithBasicAttributes();

    // 不同方向但相同速率
    when(mockSword.getDeltaMovement()).thenReturn(new Vec3(1.0, 0, 0));
    double damageX = CombatSystem.calculateCurrentDamage(mockSword);

    when(mockSword.getDeltaMovement()).thenReturn(new Vec3(0, 1.0, 0));
    double damageY = CombatSystem.calculateCurrentDamage(mockSword);

    when(mockSword.getDeltaMovement()).thenReturn(new Vec3(0, 0, 1.0));
    double damageZ = CombatSystem.calculateCurrentDamage(mockSword);

    // 伤害应该只依赖速度大小，而非方向
    assertEquals(damageX, damageY, 1e-6,
        "Damage should be independent of velocity direction");
    assertEquals(damageY, damageZ, 1e-6,
        "Damage should be independent of velocity direction");
  }

  // ========== 辅助方法 ==========

  /**
   * 创建一个带有基础属性的 mock FlyingSwordEntity
   */
  private FlyingSwordEntity createMockSwordWithBasicAttributes() {
    FlyingSwordEntity mockSword = mock(FlyingSwordEntity.class);
    ServerLevel mockLevel = mock(ServerLevel.class);

    // 基本属性
    FlyingSwordAttributes attrs = FlyingSwordAttributes.createDefault();
    attrs.speedBase = 0.5;
    attrs.speedMax = 2.0;
    attrs.accel = 0.1;
    attrs.turnRate = 0.2;
    attrs.damageBase = 1.0;
    attrs.velDmgCoef = 1.0;

    when(mockSword.level()).thenReturn(mockLevel);
    when(mockSword.getSwordLevel()).thenReturn(1);
    when(mockSword.getDeltaMovement()).thenReturn(new Vec3(1.0, 0, 0));
    when(mockSword.getSwordAttributes()).thenReturn(attrs);
    when(mockSword.getAIMode()).thenReturn(AIMode.ORBIT);
    when(mockSword.position()).thenReturn(Vec3.ZERO);

    return mockSword;
  }
}
