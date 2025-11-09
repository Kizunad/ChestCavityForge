package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning;

import net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts.Tier;

/**
 * 蕴剑青莲蛊调参配置
 *
 * <p>五转剑道核心器官，莲瓣飞剑+青莲剑域。
 */
public final class YunJianQingLianGuTuning {

  private YunJianQingLianGuTuning() {}

  // ========== 飞剑配置 ==========

  /** 基础飞剑数量（莲瓣形态） */
  public static final int BASE_SWORD_COUNT = 8;

  /** 最少飞剑数量（半径最大时） */
  public static final int MIN_SWORD_COUNT = 2;

  /** 最多飞剑数量（半径最小时，剑域蛊增幅） */
  public static final int MAX_SWORD_COUNT = 32;

  /** 飞剑攻击间隔（秒） */
  public static final double SWORD_ATTACK_INTERVAL = 2.0;

  /** 飞剑环绕基础半径（方块） */
  public static final double ORBIT_RADIUS_BASE = 3.0;

  /** 飞剑速度基础值 */
  public static final double SWORD_SPEED_BASE = 1.5;

  /** 青莲飞剑独立最大耐久度（完全覆盖默认值，不走继承增量） */
  public static final double QINGLIAN_SWORD_MAX_DURABILITY = 150.0; // 基准值，可调

  // ========== 资源消耗 ==========

  /** 设计转数（五转） */
  public static final int DESIGN_ZHUANSHU = 5;

  /** 设计阶段（一阶段） */
  public static final int DESIGN_JIEDUAN = 1;

  /** 真元消耗Tier等级（BURST适合大招） */
  public static final Tier ZHENYUAN_TIER = Tier.BURST;

  /** 精力消耗（每秒） */
  public static final double JINGLI_PER_SEC = 10.0;

  /** 念头消耗（每秒） */
  public static final double NIANTOU_PER_SEC = 2.0;

  // ========== 青莲护体（致命一击格挡） ==========

  /** 青莲护体冷却时间（秒） */
  public static final int SHIELD_COOLDOWN_SEC = 60;

  /** 青莲护体真元消耗系数（伤害值×此系数） */
  public static final double SHIELD_COST_MULT = 0.05;

  /** 青莲护体判定阈值（低于此生命值视为致命） */
  public static final float SHIELD_LETHAL_THRESHOLD = 0.5f;

  // ========== 被动恢复 ==========

  /** 魂魄恢复速率（每秒） */
  public static final double HUNPO_REGEN_PER_SEC = 0.5;

  /** 生命恢复速率（每秒） */
  public static final float HEALTH_REGEN_PER_SEC = 0.1f;

  // ========== 剑域蛊增幅参数 ==========

  /** 飞剑数量计算的域控系数权重 */
  public static final double SWORD_COUNT_P_OUT_WEIGHT = 0.2;

  /** 领域半径计算的域控系数权重 */
  public static final double DOMAIN_RADIUS_P_OUT_WEIGHT = 0.5;

  /** 默认域控系数（未装备剑域蛊时） */
  public static final double DEFAULT_P_OUT = 5.0;
}
