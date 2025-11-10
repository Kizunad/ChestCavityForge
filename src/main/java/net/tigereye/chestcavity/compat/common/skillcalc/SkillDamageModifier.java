package net.tigereye.chestcavity.compat.common.skillcalc;

/** 可插拔伤害修正规则。 */
public interface SkillDamageModifier {
  /**
   * 应用修正。
   *
   * @param ctx 计算上下文
   * @param current 当前伤害值
   * @param sink 明细收集器
   * @return 修正后的伤害
   */
  double apply(DamageComputeContext ctx, double current, SkillDamageSink sink);

  /** 明细输出接口。 */
  interface SkillDamageSink {
    void mul(String label, double factor, double after);

    void add(String label, double delta, double after);

    void clamp(String label, double after);
  }
}
