package net.tigereye.chestcavity.compat.guzhenren.item.combo.wuxing.gui_bian;

import net.tigereye.chestcavity.compat.guzhenren.item.combo.wuxing.hua_hen.state.WuxingHuaHenAttachment;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment;

/**
 * 五行归变·逆转 数值计算。
 *
 * <p>提供纯逻辑函数，便于单元测试覆盖不同输入组合。
 */
public final class WuxingGuiBianCalculator {

  private WuxingGuiBianCalculator() {}

  public static double resolveAmount(
      WuxingHuaHenAttachment.Mode mode, int fixedAmount, double available) {
    double requested =
        switch (mode) {
          case LAST -> fixedAmount;
          case ALL -> Math.min(available, WuxingGuiBianTuning.SINGLE_CAST_CAP);
          case RATIO_25 -> Math.min(available * 0.25, WuxingGuiBianTuning.SINGLE_CAST_CAP);
          case RATIO_50 -> Math.min(available * 0.50, WuxingGuiBianTuning.SINGLE_CAST_CAP);
          case RATIO_100 -> Math.min(available, WuxingGuiBianTuning.SINGLE_CAST_CAP);
          case FIXED_10 -> 10.0;
          case FIXED_25 -> 25.0;
          case FIXED_50 -> 50.0;
          case FIXED_100 -> 100.0;
        };
    return Math.max(0.0, Math.min(requested, available));
  }

  public static double calculateTax(
      WuxingHuaHenAttachment.Element element,
      YinYangDualityAttachment.Mode yinYangMode,
      int anchorCount,
      boolean isTemporaryMode) {
    if (isTemporaryMode) {
      return 0.0;
    }

    double tax = WuxingGuiBianTuning.BASE_TAX;

    if (anchorCount > 1) {
      tax -= (anchorCount - 1) * WuxingGuiBianTuning.TAX_REDUCTION_PER_ANCHOR;
    }

    if (yinYangMode == YinYangDualityAttachment.Mode.YIN) {
      if (element == WuxingHuaHenAttachment.Element.JIN
          || element == WuxingHuaHenAttachment.Element.YAN) {
        tax -= WuxingGuiBianTuning.YINYANG_TAX_REDUCTION;
      }
    } else {
      if (element == WuxingHuaHenAttachment.Element.MU
          || element == WuxingHuaHenAttachment.Element.SHUI
          || element == WuxingHuaHenAttachment.Element.TU) {
        tax -= WuxingGuiBianTuning.YINYANG_TAX_REDUCTION;
      }
    }

    return Math.max(0.0, Math.min(tax, 1.0));
  }

  public static double applyTax(double amount, double tax) {
    return Math.floor(amount * (1.0 - tax));
  }
}
