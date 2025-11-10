package net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.ward;

import java.util.UUID;

/**
 * 护幕数值供给接口
 *
 * <p>实现应通过以下来源动态计算护幕参数：
 *
 * <ul>
 *   <li>道痕等级 (Sword Trail Level)
 *   <li>流派经验 (Sect Experience)
 *   <li>玩家当前 debuff 与 buff
 *   <li>全局配置参数
 * </ul>
 *
 * <h3>设计原则</h3>
 *
 * <ul>
 *   <li>所有方法应返回合理的默认值，避免抛出异常
 *   <li>参数计算应快速（避免复杂查询）
 *   <li>支持配置热加载（实现可缓存计算结果）
 * </ul>
 */
public interface WardTuning {

  // ====== 护幕数量与配置 ======

  /**
   * 最大护幕飞剑数
   *
   * <p>公式：
   *
   * <pre>
   * N = clamp(1 + floor(sqrt(道痕/100)) + floor(经验/1000), 1, max)
   * </pre>
   *
   * 示例值：
   *
   * <ul>
   *   <li>道痕=0, 经验=0 → N=1
   *   <li>道痕=400, 经验=2000 → N=1+2+2=5 → clamp到最大值
   * </ul>
   *
   * @param owner 玩家 UUID
   * @return 最大护幕数量（建议范围：1-4）
   */
  int maxSwords(UUID owner);

  /**
   * 护幕环绕半径
   *
   * <p>公式：
   *
   * <pre>
   * r = 2.6 + 0.4 * N
   * </pre>
   *
   * 其中 N 为当前护幕数量
   *
   * @param owner 玩家 UUID
   * @param currentSwordCount 当前护幕飞剑数量
   * @return 环绕半径（米）
   */
  double orbitRadius(UUID owner, int currentSwordCount);

  // ====== 运动性能 ======

  /**
   * 最大速度 (m/s)
   *
   * <p>公式：
   *
   * <pre>
   * vMax = 6.0 + 0.02 * 道痕 + 0.001 * 经验
   * </pre>
   *
   * 示例值：
   *
   * <ul>
   *   <li>道痕=0, 经验=0 → vMax=6.0 m/s
   *   <li>道痕=100, 经验=1000 → vMax=6.0+2.0+1.0=9.0 m/s
   * </ul>
   *
   * @param owner 玩家 UUID
   * @return 最大速度（米/秒）
   */
  double vMax(UUID owner);

  /**
   * 最大加速度 (m/s²)
   *
   * <p>建议值：aMax = 40.0 (常数或可调)
   *
   * <p>影响飞剑的机动性和转向速度
   *
   * @param owner 玩家 UUID
   * @return 最大加速度（米/秒²）
   */
  double aMax(UUID owner);

  /**
   * 反应延迟 (秒)
   *
   * <p>公式：
   *
   * <pre>
   * reaction = clamp(0.06 - 0.00005 * 经验, 0.02, 0.06)
   * </pre>
   *
   * 示例值：
   *
   * <ul>
   *   <li>经验=0 → 0.06s (3 tick)
   *   <li>经验=800 → 0.02s (1 tick, 下限)
   * </ul>
   *
   * @param owner 玩家 UUID
   * @return 反应延迟（秒）
   */
  double reactionDelay(UUID owner);

  // ====== 反击条件 ======

  /**
   * 触发反击的最大距离 (米)
   *
   * <p>默认值：5.0 m
   *
   * <p>只有当攻击者距离主人 ≤ 此距离时，才会触发反击
   *
   * @return 反击距离（米）
   */
  double counterRange();

  // ====== 时间窗口 ======

  /**
   * 最小可达时间窗 (秒)
   *
   * <p>默认值：0.1 s
   *
   * <p>如果飞剑到达拦截点的时间 < 此值，视为"太近，反应不及"
   *
   * @return 最小时间窗（秒）
   */
  double windowMin();

  /**
   * 最大可达时间窗 (秒)
   *
   * <p>默认值：1.0 s
   *
   * <p>如果飞剑到达拦截点的时间 > 此值，视为"太远，不值得拦截"
   *
   * @return 最大时间窗（秒）
   */
  double windowMax();

  // ====== 耐久消耗系数 ======

  /**
   * 成功拦截的耐久消耗
   *
   * <p>公式：
   *
   * <pre>
   * R = clamp(经验 / (经验 + 2000), 0, 0.6)
   * costBlock = round(8 * (1 - R))
   * </pre>
   *
   * 示例值：
   *
   * <ul>
   *   <li>经验=0 → R=0 → cost=8
   *   <li>经验=2000 → R=0.5 → cost=4
   *   <li>经验=10000 → R=0.6 → cost=3
   * </ul>
   *
   * @param owner 玩家 UUID
   * @return 拦截耐久消耗
   */
  int costBlock(UUID owner);

  /**
   * 成功反击的耐久消耗
   *
   * <p>公式：
   *
   * <pre>
   * R = clamp(经验 / (经验 + 2000), 0, 0.6)
   * costCounter = round(10 * (1 - R))
   * </pre>
   *
   * 示例值：
   *
   * <ul>
   *   <li>经验=0 → R=0 → cost=10
   *   <li>经验=2000 → R=0.5 → cost=5
   *   <li>经验=10000 → R=0.6 → cost=4
   * </ul>
   *
   * @param owner 玩家 UUID
   * @return 反击耐久消耗
   */
  int costCounter(UUID owner);

  /**
   * 失败尝试的耐久消耗
   *
   * <p>公式：
   *
   * <pre>
   * R = clamp(经验 / (经验 + 2000), 0, 0.6)
   * costFail = round(2 * (1 - 0.5*R))
   * </pre>
   *
   * 示例值：
   *
   * <ul>
   *   <li>经验=0 → R=0 → cost=2
   *   <li>经验=2000 → R=0.5 → cost=2 * 0.75 = 1
   *   <li>经验=10000 → R=0.6 → cost=2 * 0.7 = 1
   * </ul>
   *
   * @param owner 玩家 UUID
   * @return 失败耐久消耗
   */
  int costFail(UUID owner);

  // ====== 反击伤害 ======

  /**
   * 反击伤害基线
   *
   * <p>公式：
   *
   * <pre>
   * D_counter = base(5) + 0.05 * 道痕 + 0.01 * 经验
   * </pre>
   *
   * 示例值：
   *
   * <ul>
   *   <li>道痕=0, 经验=0 → D=5.0
   *   <li>道痕=100, 经验=1000 → D=5.0+5.0+10.0=20.0
   * </ul>
   *
   * @param owner 玩家 UUID
   * @return 反击伤害（生命值）
   */
  double counterDamage(UUID owner);

  // ====== 初始耐久 ======

  /**
   * 护幕飞剑的初始耐久
   *
   * <p>公式：
   *
   * <pre>
   * Dur0 = 60 + 0.3 * 道痕 + 0.1 * 经验
   * </pre>
   *
   * 示例值：
   *
   * <ul>
   *   <li>道痕=0, 经验=0 → Dur0=60
   *   <li>道痕=100, 经验=1000 → Dur0=60+30+100=190
   * </ul>
   *
   * @param owner 玩家 UUID
   * @return 初始耐久值
   */
  double initialWardDurability(UUID owner);
}
