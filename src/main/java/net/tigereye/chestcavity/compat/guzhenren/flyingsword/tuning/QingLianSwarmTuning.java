package net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning;

/**
 * 青莲剑群（Swarm）AI 调参与说明
 *
 * <p>该类集中管理 QingLianSwordSwarm 的全部常量数值，便于在一个位置查阅与调参。
 *
 * <p>分组（默认值见常量定义）：
 * - 目标选择：获取/保持半径（防边界抖动）
 * - IDLE/编队：最大速度、到达半径、停止半径、层高、槽位自转速度、中心平滑系数
 * - ATTACK/中心绕行：绕行距离（直线距离）、高度抬升、中心绕行角速度、槽位自转速度
 * - 调度/三段式：离队频率、单体最小离队间隔、各阶段到达/停止半径、命中范围、超时阈值
 * - 其他模式：轮流防御的轮换周期与护卫半径；合击的周期与散开半径
 */
public final class QingLianSwarmTuning {
  private QingLianSwarmTuning() {}

  // —— 目标选择 ——
  /**
   * 获取目标的最大距离（方块）。
   * 调大：更容易进入攻击；调小：更保守。建议与“保持半径”形成正向滞后。
   */
  public static final double SWARM_TARGET_ACQUIRE_RANGE = 32.0;
  /**
   * 继续保持当前目标的最大距离（方块）。需大于获取半径以避免边界来回切换造成抖动。
   */
  public static final double SWARM_TARGET_RETAIN_RANGE = 38.0;

  // —— IDLE / 编队 ——
  /**
   * IDLE 编队移动的绝对速度上限（方块/刻）。防止高属性下环绕过快。
   */
  public static final double SWARM_IDLE_MAX_SPEED = 0.10;
  /**
   * 编队到达半径（方块）。距离小于该值时进入减速区。
   */
  public static final double SWARM_FORMATION_ARRIVE_RADIUS = 2.5;
  /**
   * 编队停止半径（方块）。小于该值时直接视为到位并停下，用于消抖。
   */
  public static final double SWARM_FORMATION_STOP_RADIUS = 0.15;
  /**
   * 编队分层的层间高度（方块）。控制上下三层之间的垂直间隔。
   */
  public static final double SWARM_FORMATION_LAYER_HEIGHT = 0.75;
  /**
   * 编队中心跟随平滑系数 alpha（0~1）。数值越大越“跟手”，越小越“稳态”。
   */
  public static final double SWARM_CENTER_FOLLOW_SMOOTH = 0.35;
  /**
   * IDLE 槽位自转角速度（弧度/刻）。控制编队的视觉旋转速度。
   */
  public static final double SWARM_GLOBAL_ROTATION_IDLE_SPEED = 0.003;

  // 编队半径：按队伍规模从 MAX 线性压缩至 MIN（count∈[1,32]）
  /**
   * 队伍规模较小时（≈1~2 把剑）的编队半径上限（方块）。
   */
  public static final double SWARM_FORMATION_RADIUS_MAX = 3.0;
  /**
   * 队伍规模较大时（≈32 把剑）的编队半径下限（方块）。
   */
  public static final double SWARM_FORMATION_RADIUS_MIN = 1.6;

  // —— ATTACK / 中心绕行 ——
  /**
   * 攻击时“中心点”与目标的目标直线距离（方块）。体现远程消耗的站位。
   */
  public static final double SWARM_STANDOFF_DISTANCE = 10.0;
  /**
   * 攻击时“中心点”的垂直抬升高度（方块）。
   */
  // 需求：在目标上空10格编队，因此默认抬升高度改为10格
  public static final double SWARM_HEIGHT_OFFSET = 10.0;
  /**
   * 中心点绕目标的角速度（弧度/刻）。值越大中心转得越快。
   */
  public static final double SWARM_CENTER_ORBIT_SPEED = 0.02;
  /**
   * ATTACK 阶段编队槽位自转角速度（弧度/刻）。
   */
  public static final double SWARM_GLOBAL_ROTATION_ATTACK_SPEED = 0.004;

  // —— 调度 / 个体三段式 ——
  /**
   * 派发离队的时间间隔（tick）。2=0.1s（20TPS）。每次只派出一把剑。
   */
  public static final int SWARM_DISPATCH_INTERVAL_TICKS = 1; // 0.1s
  /**
   * 同一把剑两次离队之间的最小间隔（tick）。避免单体被高频调度。
   */
  public static final int SWARM_MIN_LAUNCH_INTERVAL_TICKS = 2;

  // DEPART
  /**
   * 离队阶段到达半径（方块）。进入减速区。
   */
  public static final double SWARM_DEPART_ARRIVE_RADIUS = 2.0;
  /**
   * 离队阶段停止半径（方块）。
   */
  public static final double SWARM_DEPART_STOP_RADIUS = 0.15;
  /**
   * 离队阶段与中心的最小径向距离（方块）。
   */
  public static final double SWARM_DEPART_MIN_DIST = 2.5;
  /**
   * 离队阶段额外的径向外扩距离（方块）。常与“编队半径”相加形成目标距离。
   */
  public static final double SWARM_DEPART_EXTRA_DIST = 1.5;
  /**
   * 视为到达离队点的距离阈值（方块）。
   */
  public static final double SWARM_DEPART_REACH_EPS = 0.25;
  /**
   * 离队阶段的超时时间（tick）。超过则强制进入攻击阶段。
   */
  public static final int SWARM_DEPART_TIMEOUT_TICKS = 20;

  // ATTACK
  /**
   * 攻击阶段到达半径（方块）。
   */
  public static final double SWARM_ATTACK_ARRIVE_RADIUS = 2.0;
  /**
   * 攻击阶段停止半径（方块）。更大能减少贴脸抖动，代价是命中精度略降。
   */
  public static final double SWARM_ATTACK_STOP_RADIUS = 0.25;
  /**
   * 视为“接近目标并可转返”的命中距离（方块）。
   */
  public static final double SWARM_ATTACK_HIT_RANGE = 1.6;
  /**
   * 攻击阶段的超时时间（tick）。达到后即转入归队。
   */
  public static final int SWARM_ATTACK_TIMEOUT_TICKS = 100;

  // RETURN
  /**
   * 归队阶段到达半径（方块）。
   */
  public static final double SWARM_RETURN_ARRIVE_RADIUS = 2.5;
  /**
   * 归队阶段停止半径（方块）。
   */
  public static final double SWARM_RETURN_STOP_RADIUS = 0.15;
  /**
   * 视为“已回到槽位”的距离阈值（方块）。
   */
  public static final double SWARM_RETURN_REACH_EPS = 0.2;
  /**
   * 归队阶段的超时时间（tick）。达到后强制视为已回队，避免卡滞。
   */
  public static final int SWARM_RETURN_TIMEOUT_TICKS = 200;

  // —— 轮流防御 ——
  /**
   * 轮流防御：攻防互换的周期（tick）。
   */
  public static final int ROTATING_DEFENSE_CYCLE_TICKS = 60;
  /**
   * 轮流防御：防守组环绕主人的编队半径（方块）。
   */
  public static final double ROTATING_DEFENSE_GUARD_RADIUS = 2.0;

  // —— 合击 ——
  /**
   * 合击：收拢+散开的总周期（tick）。默认 40t 收拢 + 20t 散开。
   */
  public static final int CONVERGE_CYCLE_TICKS = 60;
  /**
   * 合击：散开阶段的球面半径（方块）。
   */
  public static final double SCATTER_RADIUS = 4.0;
}
