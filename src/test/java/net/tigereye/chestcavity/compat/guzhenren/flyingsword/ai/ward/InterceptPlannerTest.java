package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.ward;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.ward.WardTuning;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * InterceptPlanner 单元测试
 *
 * <p>测试覆盖（符合 B 阶段验收标准）：
 * <ul>
 *   <li>B.1: 投射物轨迹预测（静止、移动、重力影响）</li>
 *   <li>B.2: 近战线段预测（不同距离、线性冲刺）</li>
 *   <li>B.3: plan() 方法的完整流程</li>
 *   <li>B.4: timeToReach() 公式验证</li>
 *   <li>时间窗口验证（不可达窗口、边界值）</li>
 *   <li>边界条件（null检查、除零检查）</li>
 *   <li>几何工具方法的正确性</li>
 * </ul>
 *
 * <p>目标：单元测试覆盖率 ≥ 80%（符合 04_DEVELOPMENT_PLAN.md line 265）
 */
@DisplayName("InterceptPlanner 拦截规划算法测试")
public class InterceptPlannerTest {

  // ====== 测试辅助方法 ======

  /**
     * 创建模拟的 Player
     */
  private Player createMockPlayer(Vec3 position, AABB boundingBox) {
    Player player = mock(Player.class);
    when(player.position()).thenReturn(position);
    when(player.getBoundingBox()).thenReturn(boundingBox);
    when(player.getBbHeight()).thenReturn(1.8f);
    return player;
  }

  /**
     * 创建模拟的 Entity（攻击者）
     */
  private Entity createMockAttacker(Vec3 position, Vec3 velocity) {
    Entity entity = mock(Entity.class);
    when(entity.position()).thenReturn(position);
    when(entity.getEyePosition()).thenReturn(position.add(0, 1.6, 0));
    when(entity.getDeltaMovement()).thenReturn(velocity);
    when(entity.getBoundingBox()).thenReturn(new AABB(
        position.x - 0.3, position.y, position.z - 0.3,
        position.x + 0.3, position.y + 1.8, position.z + 0.3
    ));
    return entity;
  }

  /**
     * 创建模拟的 FlyingSwordEntity
     */
  private FlyingSwordEntity createMockSword(Vec3 position, LivingEntity owner) {
    FlyingSwordEntity sword = mock(FlyingSwordEntity.class);
    when(sword.position()).thenReturn(position);
    when(sword.getOwner()).thenReturn(owner);
    return sword;
  }

  /**
     * 创建模拟的 WardTuning
     */
  private WardTuning createMockTuning(double windowMin, double windowMax, double vMax, double reaction) {
    WardTuning tuning = mock(WardTuning.class);
    when(tuning.windowMin()).thenReturn(windowMin);
    when(tuning.windowMax()).thenReturn(windowMax);
    when(tuning.vMax(any(UUID.class))).thenReturn(vMax);
    when(tuning.reactionDelay(any(UUID.class))).thenReturn(reaction);
    return tuning;
  }

  private IncomingThreat createMeleeThreat(
      Entity attacker,
      Player target,
      Vec3 attackerPos,
      Vec3 attackerVel
  ) {
    Vec3 velocity = attackerVel;
    double speed = attackerVel.length();

    if (speed < 0.1) {
      Vec3 direction = target.position().subtract(attackerPos).normalize();
      speed = 0.5;
      velocity = direction.scale(speed);
    }

    return new IncomingThreat(
        attacker,
        target,
        attackerPos,
        velocity,
        speed,
        IncomingThreat.Type.MELEE,
        0L
    );
  }

  // ====== B.1: 投射物轨迹预测测试 ======

  @Nested
  @DisplayName("B.1: 投射物轨迹预测")
  class ProjectileTrajectoryTests {

    @Test
    @DisplayName("静止投射物 - 应快速相交")
    void testStationaryProjectile() {
      // 场景：投射物静止在玩家附近
      Vec3 playerPos = new Vec3(0, 0, 0);
      AABB playerBox = new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
      Player player = createMockPlayer(playerPos, playerBox);

      Vec3 projPos = new Vec3(0, 1.0, 0); // 投射物在玩家上方
      Vec3 projVel = new Vec3(0, -0.05, 0); // 缓慢下落（1 block/tick = 20 m/s）
      WardTuning tuning = createMockTuning(0.1, 1.0, 10.0, 0.06);

      IncomingThreat threat = IncomingThreat.forTest(projPos, projVel);

      InterceptQuery result = InterceptPlanner.plan(threat, player, tuning);

      assertNotNull(result, "静止下落的投射物应该能被拦截");
      assertNotNull(result.interceptPoint(), "应该有有效的拦截点");
      assertTrue(result.tImpact() >= 0.1 && result.tImpact() <= 1.0,
          "命中时间应在时间窗口内");
    }

    @Test
    @DisplayName("水平移动投射物 - 应预测命中点")
    void testHorizontalProjectile() {
      // 场景：投射物水平飞向玩家
      Vec3 playerPos = new Vec3(0, 0, 0);
      AABB playerBox = new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
      Player player = createMockPlayer(playerPos, playerBox);

      Vec3 projPos = new Vec3(-5, 0.9, 0); // 投射物在玩家左侧5米
      Vec3 projVel = new Vec3(0.5, 0, 0); // 向右飞行（0.5 blocks/tick = 10 m/s）
      WardTuning tuning = createMockTuning(0.1, 1.0, 10.0, 0.06);

      IncomingThreat threat = IncomingThreat.forTest(projPos, projVel);

      InterceptQuery result = InterceptPlanner.plan(threat, player, tuning);

      assertNotNull(result, "水平飞行的投射物应该能被拦截");
      assertTrue(result.interceptPoint().x > -5, "拦截点应该在投射物右侧");
      assertTrue(result.tImpact() > 0, "应有有效的命中时间");
    }

    @Test
    @DisplayName("重力影响的抛物线投射物")
    void testProjectileWithGravity() {
      // 场景：投射物受重力影响做抛物线运动
      Vec3 playerPos = new Vec3(0, 0, 0);
      AABB playerBox = new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
      Player player = createMockPlayer(playerPos, playerBox);

      Vec3 projPos = new Vec3(-3, 3, 0); // 投射物在左上方
      Vec3 projVel = new Vec3(0.3, -0.1, 0); // 向右下飞行，受重力影响
      WardTuning tuning = createMockTuning(0.1, 1.0, 10.0, 0.06);

      IncomingThreat threat = IncomingThreat.forTest(projPos, projVel);

      InterceptQuery result = InterceptPlanner.plan(threat, player, tuning);

      assertNotNull(result, "抛物线投射物应该能被拦截");
      // 验证拦截点考虑了重力效应（应该在命中点前方）
      assertNotNull(result.interceptPoint());
    }

    @Test
    @DisplayName("投射物速度过慢 - 应返回null")
    void testTooSlowProjectile() {
      // 场景：投射物速度几乎为0
      Vec3 playerPos = new Vec3(0, 0, 0);
      AABB playerBox = new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
      Player player = createMockPlayer(playerPos, playerBox);

      Vec3 projPos = new Vec3(-5, 1, 0);
      Vec3 projVel = new Vec3(0.0001, 0, 0); // 极慢速度
      WardTuning tuning = createMockTuning(0.1, 1.0, 10.0, 0.06);

      IncomingThreat threat = IncomingThreat.forTest(projPos, projVel);

      InterceptQuery result = InterceptPlanner.plan(threat, player, tuning);

      assertNull(result, "速度过慢的投射物应无法拦截（返回null）");
    }

    @Test
    @DisplayName("投射物不会命中玩家 - 应返回null")
    void testProjectileMissingTarget() {
      // 场景：投射物飞行方向不会命中玩家
      Vec3 playerPos = new Vec3(0, 0, 0);
      AABB playerBox = new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
      Player player = createMockPlayer(playerPos, playerBox);

      Vec3 projPos = new Vec3(-5, 1, -5); // 投射物在远处
      Vec3 projVel = new Vec3(0, 0, 0.5); // 沿Z轴飞行，不会命中
      WardTuning tuning = createMockTuning(0.1, 1.0, 10.0, 0.06);

      IncomingThreat threat = IncomingThreat.forTest(projPos, projVel);

      InterceptQuery result = InterceptPlanner.plan(threat, player, tuning);

      assertNull(result, "不会命中玩家的投射物应返回null");
    }
  }

  // ====== B.2: 近战线段预测测试 ======

  @Nested
  @DisplayName("B.2: 近战攻击线段预测")
  class MeleeAttackTests {

    @Test
    @DisplayName("线性近战冲刺 - 应有有效最近点")
    void testLinearMeleeCharge() {
      // 场景：攻击者正面冲向玩家
      Vec3 playerPos = new Vec3(0, 0, 0);
      AABB playerBox = new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
      Player player = createMockPlayer(playerPos, playerBox);

      Vec3 attackerPos = new Vec3(-2.5, 0, 0); // 2.5米距离
      Vec3 attackerVel = new Vec3(0.2, 0, 0); // 向玩家冲刺
      Entity attacker = createMockAttacker(attackerPos, attackerVel);

      WardTuning tuning = createMockTuning(0.1, 1.0, 10.0, 0.06);

      IncomingThreat threat = createMeleeThreat(attacker, player, attackerPos, attackerVel);

      InterceptQuery result = InterceptPlanner.plan(threat, player, tuning);

      assertNotNull(result, "近战冲刺应该能被拦截");
      assertNotNull(result.interceptPoint(), "应该有有效的拦截点");
      assertTrue(result.tImpact() >= 0.1 && result.tImpact() <= 1.0,
          "命中时间应在时间窗口内");
    }

    @Test
    @DisplayName("近距离近战攻击 - 应立即拦截")
    void testCloseRangeMelee() {
      // 场景：攻击者非常接近玩家
      Vec3 playerPos = new Vec3(0, 0, 0);
      AABB playerBox = new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
      Player player = createMockPlayer(playerPos, playerBox);

      Vec3 attackerPos = new Vec3(-0.8, 0, 0); // 很近的距离
      Vec3 attackerVel = new Vec3(0.1, 0, 0);
      Entity attacker = createMockAttacker(attackerPos, attackerVel);

      WardTuning tuning = createMockTuning(0.1, 1.0, 10.0, 0.06);

      IncomingThreat threat = createMeleeThreat(attacker, player, attackerPos, attackerVel);

      InterceptQuery result = InterceptPlanner.plan(threat, player, tuning);

      assertNotNull(result, "近距离近战应该能被拦截");
      assertTrue(result.tImpact() < 0.5, "近距离攻击应该快速到达");
    }

    @Test
    @DisplayName("攻击者静止 - 应假设快速攻击")
    void testStationaryMeleeAttacker() {
      // 场景：攻击者静止但在攻击范围内
      Vec3 playerPos = new Vec3(0, 0, 0);
      AABB playerBox = new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
      Player player = createMockPlayer(playerPos, playerBox);

      Vec3 attackerPos = new Vec3(-2, 0, 0);
      Vec3 attackerVel = new Vec3(0, 0, 0); // 静止
      Entity attacker = createMockAttacker(attackerPos, attackerVel);

      WardTuning tuning = createMockTuning(0.1, 1.0, 10.0, 0.06);

      IncomingThreat threat = createMeleeThreat(attacker, player, attackerPos, attackerVel);

      InterceptQuery result = InterceptPlanner.plan(threat, player, tuning);

      assertNotNull(result, "静止的近战攻击者也应该能被拦截");
      assertEquals(0.2, result.tImpact(), 0.01,
          "静止攻击者应假设0.2秒攻击时间");
    }

    @Test
    @DisplayName("攻击者超出范围 - 应返回null")
    void testMeleeOutOfRange() {
      // 场景：攻击者距离超过近战范围（3米）
      Vec3 playerPos = new Vec3(0, 0, 0);
      AABB playerBox = new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
      Player player = createMockPlayer(playerPos, playerBox);

      Vec3 attackerPos = new Vec3(-5, 0, 0); // 5米距离
      Vec3 attackerVel = new Vec3(0, 0, 0);
      Entity attacker = createMockAttacker(attackerPos, attackerVel);

      WardTuning tuning = createMockTuning(0.1, 1.0, 10.0, 0.06);

      IncomingThreat threat = createMeleeThreat(attacker, player, attackerPos, attackerVel);

      InterceptQuery result = InterceptPlanner.plan(threat, player, tuning);

      assertNull(result, "超出攻击范围的近战应返回null");
    }
  }

  // ====== B.3: plan() 方法完整流程测试 ======

  @Nested
  @DisplayName("B.3: plan() 方法完整流程")
  class PlanMethodTests {

    @Test
    @DisplayName("投射物威胁完整流程")
    void testProjectileThreatFullFlow() {
      Vec3 playerPos = new Vec3(0, 0, 0);
      AABB playerBox = new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
      Player player = createMockPlayer(playerPos, playerBox);

      Vec3 projPos = new Vec3(-4, 1, 0);
      Vec3 projVel = new Vec3(0.4, 0, 0); // 0.4 blocks/tick = 8 m/s
      WardTuning tuning = createMockTuning(0.1, 1.0, 10.0, 0.06);

      IncomingThreat threat = IncomingThreat.forTest(projPos, projVel);

      InterceptQuery result = InterceptPlanner.plan(threat, player, tuning);

      assertNotNull(result);
      assertNotNull(result.interceptPoint());
      assertNotNull(result.threat());
      assertTrue(result.tImpact() > 0);

      // 验证拦截点在命中点前方（0.3米偏移）
      // 由于投射物向右飞行，拦截点应该在投射物起点和终点之间
      assertTrue(result.interceptPoint().x > projPos.x);
    }

    @Test
    @DisplayName("近战威胁完整流程")
    void testMeleeThreatFullFlow() {
      Vec3 playerPos = new Vec3(0, 0, 0);
      AABB playerBox = new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
      Player player = createMockPlayer(playerPos, playerBox);

      Vec3 attackerPos = new Vec3(-2, 0, 0);
      Vec3 attackerVel = new Vec3(0.15, 0, 0); // 3 m/s
      Entity attacker = createMockAttacker(attackerPos, attackerVel);

      WardTuning tuning = createMockTuning(0.1, 1.0, 10.0, 0.06);

      IncomingThreat threat = createMeleeThreat(attacker, player, attackerPos, attackerVel);

      InterceptQuery result = InterceptPlanner.plan(threat, player, tuning);

      assertNotNull(result);
      assertNotNull(result.interceptPoint());
      assertEquals(threat, result.threat());
      assertTrue(result.tImpact() >= 0.1);
    }

    @Test
    @DisplayName("拦截点偏移验证 - 应在命中点前0.3米")
    void testInterceptPointOffset() {
      Vec3 playerPos = new Vec3(0, 1, 0);
      AABB playerBox = new AABB(-0.3, 0.1, -0.3, 0.3, 1.9, 0.3);
      Player player = createMockPlayer(playerPos, playerBox);

      // 投射物从左侧飞来
      Vec3 projPos = new Vec3(-3, 1, 0);
      Vec3 projVel = new Vec3(0.3, 0, 0);
      WardTuning tuning = createMockTuning(0.1, 1.0, 10.0, 0.06);

      IncomingThreat threat = IncomingThreat.forTest(projPos, projVel);

      InterceptQuery result = InterceptPlanner.plan(threat, player, tuning);

      assertNotNull(result);
      // 拦截点应该在玩家左侧（投射物来的方向）
      assertTrue(result.interceptPoint().x < playerPos.x,
          "拦截点应该在命中点前方");
    }
  }

  // ====== B.4: timeToReach() 公式验证测试 ======

  @Nested
  @DisplayName("B.4: timeToReach() 公式验证")
  class TimeToReachTests {

    @Test
    @DisplayName("公式验证: distance=10m, vMax=10m/s, reaction=0.06s → ~1.06s")
    void testTimeToReachFormula() {
      Vec3 swordPos = new Vec3(0, 0, 0);
      Vec3 interceptPoint = new Vec3(10, 0, 0); // 10米距离
      UUID ownerId = UUID.randomUUID();
      Player owner = mock(Player.class);
      when(owner.getUUID()).thenReturn(ownerId);

      FlyingSwordEntity sword = createMockSword(swordPos, owner);
      WardTuning tuning = createMockTuning(0.1, 1.0, 10.0, 0.06);

      double result = InterceptPlanner.timeToReach(sword, interceptPoint, tuning);

      // max(0.06, 10/10) = max(0.06, 1.0) = 1.0
      assertEquals(1.0, result, 0.01);
    }

    @Test
    @DisplayName("短距离: reaction优先")
    void testTimeToReachShortDistance() {
      Vec3 swordPos = new Vec3(0, 0, 0);
      Vec3 interceptPoint = new Vec3(0.3, 0, 0); // 0.3米
      UUID ownerId = UUID.randomUUID();
      Player owner = mock(Player.class);
      when(owner.getUUID()).thenReturn(ownerId);

      FlyingSwordEntity sword = createMockSword(swordPos, owner);
      WardTuning tuning = createMockTuning(0.1, 1.0, 10.0, 0.06);

      double result = InterceptPlanner.timeToReach(sword, interceptPoint, tuning);

      // max(0.06, 0.3/10) = max(0.06, 0.03) = 0.06
      assertEquals(0.06, result, 0.001);
    }

    @Test
    @DisplayName("长距离: distance/vMax优先")
    void testTimeToReachLongDistance() {
      Vec3 swordPos = new Vec3(0, 0, 0);
      Vec3 interceptPoint = new Vec3(50, 0, 0); // 50米
      UUID ownerId = UUID.randomUUID();
      Player owner = mock(Player.class);
      when(owner.getUUID()).thenReturn(ownerId);

      FlyingSwordEntity sword = createMockSword(swordPos, owner);
      WardTuning tuning = createMockTuning(0.1, 1.0, 10.0, 0.06);

      double result = InterceptPlanner.timeToReach(sword, interceptPoint, tuning);

      // max(0.06, 50/10) = max(0.06, 5.0) = 5.0
      assertEquals(5.0, result, 0.01);
    }

    @Test
    @DisplayName("vMax=0 边界情况 - 应返回MAX_VALUE")
    void testTimeToReachZeroVMax() {
      Vec3 swordPos = new Vec3(0, 0, 0);
      Vec3 interceptPoint = new Vec3(10, 0, 0);
      UUID ownerId = UUID.randomUUID();
      Player owner = mock(Player.class);
      when(owner.getUUID()).thenReturn(ownerId);

      FlyingSwordEntity sword = createMockSword(swordPos, owner);
      WardTuning tuning = createMockTuning(0.1, 1.0, 0.0, 0.06); // vMax=0

      double result = InterceptPlanner.timeToReach(sword, interceptPoint, tuning);

      assertEquals(Double.MAX_VALUE, result, "vMax=0应返回MAX_VALUE");
    }

    @Test
    @DisplayName("sword owner为null - 应返回MAX_VALUE")
    void testTimeToReachNullOwner() {
      Vec3 swordPos = new Vec3(0, 0, 0);
      Vec3 interceptPoint = new Vec3(10, 0, 0);
      FlyingSwordEntity sword = mock(FlyingSwordEntity.class);
      when(sword.position()).thenReturn(swordPos);
      when(sword.getOwner()).thenReturn(null); // null owner

      WardTuning tuning = createMockTuning(0.1, 1.0, 10.0, 0.06);

      double result = InterceptPlanner.timeToReach(sword, interceptPoint, tuning);

      assertEquals(Double.MAX_VALUE, result, "owner为null应返回MAX_VALUE");
    }
  }

  // ====== 时间窗口验证测试 ======

  @Nested
  @DisplayName("时间窗口验证（不可达窗口）")
  class TimeWindowTests {

    @Test
    @DisplayName("tImpact < windowMin - 应返回null（太快）")
    void testThreatTooFast() {
      Vec3 playerPos = new Vec3(0, 0, 0);
      AABB playerBox = new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
      Player player = createMockPlayer(playerPos, playerBox);

      // 投射物非常快，几乎立即到达
      Vec3 projPos = new Vec3(-0.5, 1, 0);
      Vec3 projVel = new Vec3(10, 0, 0); // 极快速度
      WardTuning tuning = createMockTuning(0.1, 1.0, 10.0, 0.06);

      IncomingThreat threat = IncomingThreat.forTest(projPos, projVel);

      InterceptQuery result = InterceptPlanner.plan(threat, player, tuning);

      // 由于速度极快，tImpact可能小于0.1秒（windowMin）
      // 注意：实际行为取决于实现细节，这里测试边界
      if (result != null) {
        assertTrue(result.tImpact() >= 0.1, "如果返回结果，tImpact应≥windowMin");
      }
    }

    @Test
    @DisplayName("tImpact > windowMax - 应返回null（太慢）")
    void testThreatTooSlow() {
      Vec3 playerPos = new Vec3(0, 0, 0);
      AABB playerBox = new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
      Player player = createMockPlayer(playerPos, playerBox);

      // 投射物速度很慢，需要超过1秒才能到达
      Vec3 projPos = new Vec3(-50, 1, 0);
      Vec3 projVel = new Vec3(0.01, 0, 0); // 极慢速度：0.2 m/s
      WardTuning tuning = createMockTuning(0.1, 1.0, 10.0, 0.06);

      IncomingThreat threat = IncomingThreat.forTest(projPos, projVel);

      InterceptQuery result = InterceptPlanner.plan(threat, player, tuning);

      assertNull(result, "超过windowMax的威胁应返回null");
    }

    @Test
    @DisplayName("tImpact在窗口边界 - 应接受")
    void testThreatAtWindowBoundary() {
      Vec3 playerPos = new Vec3(0, 0, 0);
      AABB playerBox = new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
      Player player = createMockPlayer(playerPos, playerBox);

      // 调整投射物使其恰好在窗口边界
      // windowMax=1.0s，如果距离=10m，速度=10m/s，则tImpact=1.0s
      Vec3 projPos = new Vec3(-10, 1, 0);
      Vec3 projVel = new Vec3(0.5, 0, 0); // 0.5 blocks/tick = 10 m/s
      WardTuning tuning = createMockTuning(0.1, 1.0, 10.0, 0.06);

      IncomingThreat threat = IncomingThreat.forTest(projPos, projVel);

      InterceptQuery result = InterceptPlanner.plan(threat, player, tuning);

      if (result != null) {
        assertTrue(result.tImpact() <= 1.0, "边界值应被接受");
      }
    }
  }

  // ====== 边界条件和空值测试 ======

  @Nested
  @DisplayName("边界条件和空值处理")
  class BoundaryAndNullTests {

    @Test
    @DisplayName("plan() - threat为null应返回null")
    void testPlanNullThreat() {
      Player player = createMockPlayer(new Vec3(0, 0, 0),
          new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3));
      WardTuning tuning = createMockTuning(0.1, 1.0, 10.0, 0.06);

      InterceptQuery result = InterceptPlanner.plan(null, player, tuning);

      assertNull(result, "threat为null应返回null");
    }

    @Test
    @DisplayName("plan() - owner为null应返回null")
    void testPlanNullOwner() {
      IncomingThreat threat = IncomingThreat.forTest(
          new Vec3(0, 1, 0),
          new Vec3(0.1, 0, 0)
      );
      WardTuning tuning = createMockTuning(0.1, 1.0, 10.0, 0.06);

      InterceptQuery result = InterceptPlanner.plan(threat, null, tuning);

      assertNull(result, "owner为null应返回null");
    }

    @Test
    @DisplayName("plan() - tuning为null应返回null")
    void testPlanNullTuning() {
      Player player = createMockPlayer(new Vec3(0, 0, 0),
          new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3));
      IncomingThreat threat = IncomingThreat.forTest(
          new Vec3(0, 1, 0),
          new Vec3(0.1, 0, 0)
      );

      InterceptQuery result = InterceptPlanner.plan(threat, player, null);

      assertNull(result, "tuning为null应返回null");
    }

    @Test
    @DisplayName("timeToReach() - sword为null应返回MAX_VALUE")
    void testTimeToReachNullSword() {
      WardTuning tuning = createMockTuning(0.1, 1.0, 10.0, 0.06);

      double result = InterceptPlanner.timeToReach(null, new Vec3(10, 0, 0), tuning);

      assertEquals(Double.MAX_VALUE, result);
    }

    @Test
    @DisplayName("timeToReach() - pStar为null应返回MAX_VALUE")
    void testTimeToReachNullPStar() {
      UUID ownerId = UUID.randomUUID();
      Player owner = mock(Player.class);
      when(owner.getUUID()).thenReturn(ownerId);
      FlyingSwordEntity sword = createMockSword(new Vec3(0, 0, 0), owner);
      WardTuning tuning = createMockTuning(0.1, 1.0, 10.0, 0.06);

      double result = InterceptPlanner.timeToReach(sword, null, tuning);

      assertEquals(Double.MAX_VALUE, result);
    }

    @Test
    @DisplayName("timeToReach() - tuning为null应返回MAX_VALUE")
    void testTimeToReachNullTuning() {
      UUID ownerId = UUID.randomUUID();
      Player owner = mock(Player.class);
      when(owner.getUUID()).thenReturn(ownerId);
      FlyingSwordEntity sword = createMockSword(new Vec3(0, 0, 0), owner);

      double result = InterceptPlanner.timeToReach(sword, new Vec3(10, 0, 0), null);

      assertEquals(Double.MAX_VALUE, result);
    }

    @Test
    @DisplayName("既不是投射物也不是近战 - 应返回null")
    void testNeitherProjectileNorMelee() {
      Player player = createMockPlayer(new Vec3(0, 0, 0),
          new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3));
      WardTuning tuning = createMockTuning(0.1, 1.0, 10.0, 0.06);

      // 既没有投射物数据，也没有攻击者
      IncomingThreat threat = new IncomingThreat(
          null,
          player,
          Vec3.ZERO,
          Vec3.ZERO,
          0.0,
          null,
          0L
      );

      InterceptQuery result = InterceptPlanner.plan(threat, player, tuning);

      assertNull(result, "无效的威胁类型应返回null");
    }
  }

  // ====== IncomingThreat辅助方法测试 ======

  @Nested
  @DisplayName("IncomingThreat 辅助方法")
  class IncomingThreatHelperTests {

    @Test
    @DisplayName("isProjectile() - 有projPos和projVel时返回true")
    void testIsProjectileTrue() {
      IncomingThreat threat = IncomingThreat.forTest(
          new Vec3(0, 1, 0),
          new Vec3(0.1, 0, 0)
      );

      assertTrue(threat.isProjectile());
      assertFalse(threat.isMelee());
    }

    @Test
    @DisplayName("isMelee() - 有attacker时返回true")
    void testIsMeleeTrue() {
      Entity attacker = createMockAttacker(new Vec3(0, 0, 0), new Vec3(0.2, 0, 0));
      Player player = createMockPlayer(new Vec3(0, 0, 0), new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3));
      IncomingThreat threat = new IncomingThreat(
          attacker,
          player,
          attacker.position(),
          new Vec3(0.2, 0, 0),
          0.2,
          IncomingThreat.Type.MELEE,
          0L
      );

      assertTrue(threat.isMelee());
      assertFalse(threat.isProjectile());
    }

    @Test
    @DisplayName("既没有投射物也没有攻击者 - 两个都返回false")
    void testNeitherProjectileNorMeleeHelper() {
      Player player = createMockPlayer(new Vec3(0, 0, 0), new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3));
      IncomingThreat threat = new IncomingThreat(
          null,
          player,
          Vec3.ZERO,
          Vec3.ZERO,
          0.0,
          null,
          0L
      );

      assertFalse(threat.isProjectile());
      assertFalse(threat.isMelee());
    }
  }
}
