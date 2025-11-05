package net.tigereye.chestcavity.compat.guzhenren.flyingsword.systems;

import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.combat.FlyingSwordCombat;

/**
 * Phase 2: 战斗系统 (Combat System)
 *
 * <p>职责:
 * <ul>
 *   <li>集中管理碰撞检测</li>
 *   <li>计算伤害 (速度² 公式)</li>
 *   <li>触发战斗事件 (OnHitEntity, PostHit)</li>
 *   <li>管理攻击冷却</li>
 * </ul>
 *
 * <p>设计原则:
 * <ul>
 *   <li>无状态: 所有方法为静态方法，不持有实例变量</li>
 *   <li>事件驱动: 关键操作触发事件钩子</li>
 *   <li>可测试: 输入输出明确，便于单元测试</li>
 * </ul>
 *
 * <p>实现说明:
 * <ul>
 *   <li>Phase 2 保持现有战斗逻辑不变，仅重构调用方式</li>
 *   <li>委托给 FlyingSwordCombat 处理具体战斗逻辑</li>
 *   <li>未来可在此层增加战斗事件扩展 (PostHit, BlockBreakAttempt 等)</li>
 * </ul>
 */
public final class CombatSystem {

  private CombatSystem() {
    // 禁止实例化
  }

  /**
   * Phase 2: 战斗系统 Tick 入口
   *
   * <p>执行碰撞检测、伤害计算、经验获取、耐久消耗
   *
   * @param sword 飞剑实体
   * @param attackCooldown 当前攻击冷却 (ticks)
   * @return 新的攻击冷却值 (ticks)
   */
  public static int tick(FlyingSwordEntity sword, int attackCooldown) {
    if (sword == null || sword.level() == null) {
      return attackCooldown;
    }

    // Phase 2: 委托给现有战斗模块处理
    // FlyingSwordCombat 已实现完整的战斗逻辑，包括：
    // - 冷却递减
    // - 碰撞检测
    // - 伤害计算 (速度² 公式)
    // - 事件触发 (OnHitEntity)
    // - 经验获取
    // - 耐久消耗
    // - 粒子/音效
    return FlyingSwordCombat.tickCollisionAttack(sword, attackCooldown);
  }

  /**
   * Phase 2: 计算当前伤害 (不执行攻击)
   *
   * <p>用于 UI 显示和调试
   *
   * @param sword 飞剑实体
   * @return 计算的伤害值
   */
  public static double calculateCurrentDamage(FlyingSwordEntity sword) {
    if (sword == null) {
      return 0.0;
    }
    return FlyingSwordCombat.calculateCurrentDamage(sword);
  }

  // ========== Phase 3 扩展预留接口 ==========

  /**
   * Phase 3 预留: PostHit 事件钩子
   *
   * <p>在命中目标后触发，允许额外效果 (如击退、减速、追伤等)
   *
   * <p>当前 Phase 2 暂不实现，保持与原逻辑一致
   */
  // public static void firePostHitEvent(FlyingSwordEntity sword, LivingEntity target, double damage) {
  //   // Phase 3: 触发 PostHit 事件
  // }

  /**
   * Phase 3 预留: BlockBreakAttempt 事件钩子
   *
   * <p>在尝试破坏方块前触发，允许扩展模块干预破块逻辑
   *
   * <p>当前 Phase 2 暂不实现，保持与原逻辑一致
   */
  // public static boolean fireBlockBreakAttemptEvent(FlyingSwordEntity sword, BlockPos pos, BlockState state) {
  //   // Phase 3: 触发 BlockBreakAttempt 事件
  //   return true; // 默认允许破块
  // }
}
