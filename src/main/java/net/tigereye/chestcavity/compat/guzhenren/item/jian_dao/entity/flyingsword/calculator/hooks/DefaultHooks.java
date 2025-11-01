package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator.hooks;

import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator.context.CalcOutputs;

/**
 * 内置默认钩子注册：将常见的全局规则集中注册，便于覆盖与禁用。
 */
public final class DefaultHooks {
  private static boolean registered = false;

  private DefaultHooks() {}

  public static void registerDefaults() {
    if (registered) return;
    registered = true;

    // 规则：速度、加速度、基础伤害按 (1 + 剑道道痕/1000) 调整（不设置上限）
    FlyingSwordCalcRegistry.register(
        (ctx, out) -> {
          double factor = 1.0 + ctx.ownerJianDaoScar / 1000.0;
          if (Double.isFinite(factor) && factor > 0.0) {
            out.speedBaseMult *= factor;
            out.speedMaxMult *= factor;
            out.accelMult *= factor;
            out.damageMult *= factor;
          }
        });

    // 规则：耐久损耗按 剑道流派经验 的倒数缩放 => 乘以 (1 / ownerSwordPathExp)
    // 经验越高，损耗越低；当经验<=0 或无定义时不生效
    FlyingSwordCalcRegistry.register(
        (ctx, out) -> {
          double exp = ctx.ownerSwordPathExp;
          if (Double.isFinite(exp) && exp > 0.0) {
            out.durabilityLossMult *= (1.0 / exp);
          }
        });
  }
}
