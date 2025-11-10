package net.tigereye.chestcavity.compat.guzhenren.flyingsword.util;

import net.minecraft.world.item.ItemStack;

/**
 * 耐久映射工具：提供百分比 ↔ Damage 值的换算，与对 ItemStack 的应用。
 *
 * <p>百分比 percent 语义：1.0 = 满耐久，0.0 = 无耐久。
 */
public final class ItemDurabilityUtil {
  private ItemDurabilityUtil() {}

  /**
   * 将耐久百分比转换为 Damage 值（四舍五入）。
   *
   * @param percent [0,1]，1 表示满耐久
   * @param maxDamage 物品最大耐久（Damage 上限）
   */
  public static int percentToDamageValue(double percent, int maxDamage) {
    if (maxDamage <= 0) return 0;
    double p = Double.isFinite(percent) ? percent : 1.0;
    p = Math.max(0.0, Math.min(1.0, p));
    double missing = 1.0 - p; // 缺失比例
    long dmg = Math.round(missing * (double) maxDamage);
    if (dmg < 0L) return 0;
    if (dmg > (long) maxDamage) return maxDamage;
    return (int) dmg;
  }

  /** 将 Damage 值转换为百分比（满耐久=1）。 */
  public static double damageValueToPercent(int damage, int maxDamage) {
    if (maxDamage <= 0) return 1.0;
    int d = Math.max(0, Math.min(maxDamage, damage));
    double missing = (double) d / (double) maxDamage;
    double p = 1.0 - missing;
    if (!Double.isFinite(p)) return 1.0;
    if (p < 0.0) return 0.0;
    if (p > 1.0) return 1.0;
    return p;
  }

  /** 将百分比应用到物品耐久（若不可损耗则忽略）。 */
  public static void applyPercentToStack(ItemStack stack, double percent) {
    if (stack == null || stack.isEmpty() || !stack.isDamageableItem()) return;
    int max = stack.getMaxDamage();
    int dmg = percentToDamageValue(percent, max);
    stack.setDamageValue(dmg);
  }
}
