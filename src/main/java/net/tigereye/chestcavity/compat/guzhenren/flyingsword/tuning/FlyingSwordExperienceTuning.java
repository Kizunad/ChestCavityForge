package net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning;

/**
 * 飞剑经验/等级成长参数
 *
 * <p>核心公式：
 *
 * <ul>
 *   <li>经验获取 = 造成伤害 × EXP_PER_DAMAGE × (击杀?EXP_KILL_MULT:1) × (精英?EXP_ELITE_MULT:1) × 额外倍率
 *   <li>升级所需经验 = EXP_BASE × (1 + 当前等级) ^ EXP_ALPHA
 *   <li>等级伤害加成 = 1 + (当前等级 - 1) × DAMAGE_PER_LEVEL / 基础伤害
 * </ul>
 *
 * <p>升级经验指数 EXP_ALPHA 说明： 该参数控制升级曲线的陡峭程度，作为指数应用于 (1 + 当前等级) 的幂运算。 当 EXP_ALPHA = 1.5
 * 时，表示升级所需经验呈超线性增长，等级越高升级越困难。 例如：从1级升2级需要 40 × (1+1)^1.5 ≈ 113 经验，而从99级升100级需要 40 × (1+99)^1.5 ≈
 * 40,000 经验。
 */
public final class FlyingSwordExperienceTuning {
  private FlyingSwordExperienceTuning() {}

  /** 每点伤害获得的基础经验值（0.001 = 每1000点伤害获得1经验） */
  public static final double EXP_PER_DAMAGE = 0.001;

  /** 击杀倍率：击杀目标时经验乘以该值（5倍） */
  public static final int EXP_KILL_MULT = 5;

  /** 精英倍率：攻击精英目标时经验乘以该值（2倍） */
  public static final int EXP_ELITE_MULT = 2;

  /** 升级经验基数：控制升级曲线的基准值 */
  public static final double EXP_BASE = 400.0;

  /** 升级经验指数：控制升级曲线的陡峭程度（1.5 = 超线性增长） */
  public static final double EXP_ALPHA = 8;

  /** 最高等级限制 */
  public static final int MAX_LEVEL = 999;

  /** 每级增加的伤害值（9点伤害/级） */
  public static final double DAMAGE_PER_LEVEL = 10;
}
