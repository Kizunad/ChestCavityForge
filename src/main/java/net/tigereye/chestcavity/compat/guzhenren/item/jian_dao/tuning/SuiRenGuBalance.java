package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning;

/**
 * 碎刃蛊（五转·剑道爆发器官）平衡调参。
 *
 * <p>本蛊通过牺牲在场飞剑，临时获得剑道道痕增幅，增幅基于飞剑经验与属性计算。
 * <p>所有增幅在持续时间结束后自动回滚，不永久影响道痕。
 *
 * <p>核心机制：
 * <ul>
 *   <li>只牺牲"当前在场、可回收、耐久>0"的自有飞剑</li>
 *   <li>每把剑的收益上限 {@link #PER_SWORD_CAP}，总上限 {@link #CAST_TOTAL_CAP}</li>
 *   <li>持续时间基础 {@link #BASE_DURATION_TICKS}，每把剑+{@link #DURATION_PER_SWORD}，上限 {@link #DURATION_CAP}</li>
 *   <li>消耗真元/精力/念头：{@link #BASE_COST_ZHENYUAN}/{@link #BASE_COST_JINGLI}/{@link #BASE_COST_NIANTOU}</li>
 *   <li>冷却 {@link #COOLDOWN_TICKS}</li>
 * </ul>
 */
public final class SuiRenGuBalance {

  private SuiRenGuBalance() {}

  // ========== 经验与属性归一化 ==========

  /** 经验上限（超过此值的经验不再额外增益）。*/
  public static final int E_CAP = 200;

  /** 耐久归一化参考值（剑耐久 / D_REF，取 min 与 1.0）。*/
  public static final float D_REF = 500f;

  /** 攻击归一化参考值（剑攻击 / A_REF，取 min 与 1.0）。*/
  public static final float A_REF = 10f;

  /** 速度归一化参考值（剑速度 / S_REF，取 min 与 1.0）。*/
  public static final float S_REF = 1.5f;

  // ========== 收益计算权重 ==========

  /** 经验项权重：例如 E=50 时经验部分贡献 ALPHA_EXP * 50 = 100 道痕。*/
  public static final float ALPHA_EXP = 2.0f;

  /** 属性项权重：与归一化属性组合 W_D/W_A/W_S 联动。*/
  public static final float BETA_ATTR = 20f;

  /** 耐久属性占比（在属性综合评分中）。*/
  public static final float W_D = 0.50f;

  /** 攻击属性占比。*/
  public static final float W_A = 0.35f;

  /** 速度属性占比。*/
  public static final float W_S = 0.15f;

  // ========== 收益上限 ==========

  /** 单把飞剑的道痕增幅上限。*/
  public static final int PER_SWORD_CAP = 400;

  /** 单次施放的总道痕增幅上限（即使牺牲多把剑也不能超过）。*/
  public static final int CAST_TOTAL_CAP = 2000;

  // ========== 持续时间 ==========

  /** 基础持续时间（ticks）= 10秒。*/
  public static final int BASE_DURATION_TICKS = 200;

  /** 每把飞剑增加的持续时间（ticks）= 2秒。*/
  public static final int DURATION_PER_SWORD = 40;

  /** 持续时间上限（ticks）= 60秒。*/
  public static final int DURATION_CAP = 1200;

  // ========== 资源消耗 ==========

  /** 真元基础消耗。*/
  public static final double BASE_COST_ZHENYUAN = 120.0;

  /** 精力基础消耗。*/
  public static final double BASE_COST_JINGLI = 60.0;

  /** 念头基础消耗。*/
  public static final double BASE_COST_NIANTOU = 40.0;

  // ========== 冷却 ==========

  /** 冷却时间（ticks）= 45秒。*/
  public static final int COOLDOWN_TICKS = 900;
}
