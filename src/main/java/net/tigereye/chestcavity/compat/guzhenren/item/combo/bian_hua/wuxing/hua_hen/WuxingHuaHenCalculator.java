package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.hua_hen;

import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.hua_hen.state.WuxingHuaHenAttachment.Element;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.hua_hen.state.WuxingHuaHenAttachment.Mode;
import net.tigereye.chestcavity.compat.common.state.YinYangDualityAttachment;

/**
 * 五行化痕 数值计算工具。
 */
public final class WuxingHuaHenCalculator {

  private WuxingHuaHenCalculator() {}

  public static double resolveAmount(Mode mode, int fixedAmount, double available) {
    double requested =
        switch (mode) {
          case LAST -> fixedAmount;
          case ALL -> Math.min(available, WuxingHuaHenTuning.SINGLE_CAST_CAP);
          case RATIO_25 -> Math.min(available * 0.25, WuxingHuaHenTuning.SINGLE_CAST_CAP);
          case RATIO_50 -> Math.min(available * 0.50, WuxingHuaHenTuning.SINGLE_CAST_CAP);
          case RATIO_100 -> Math.min(available, WuxingHuaHenTuning.SINGLE_CAST_CAP);
          case FIXED_10 -> 10.0;
          case FIXED_25 -> 25.0;
          case FIXED_50 -> 50.0;
          case FIXED_100 -> 100.0;
        };
    return Math.max(0.0, Math.min(requested, available));
  }

  public static double calculateTax(Element element, YinYangDualityAttachment.Mode mode) {
    double tax = WuxingHuaHenTuning.BASE_TAX;

    if (mode == YinYangDualityAttachment.Mode.YIN) {
      if (element == Element.JIN || element == Element.YAN) {
        tax -= WuxingHuaHenTuning.MODE_TAX_REDUCTION;
      }
    } else {
      if (element == Element.MU || element == Element.SHUI || element == Element.TU) {
        tax -= WuxingHuaHenTuning.MODE_TAX_REDUCTION;
      }
    }

    return Math.max(0.0, Math.min(tax, WuxingHuaHenTuning.MAX_TAX));
  }

  public static double applyTax(double amountIn, double tax) {
    return Math.floor(amountIn * (1.0 - tax));
  }

  public static double computeUndoReturn(double amountOut) {
    return Math.floor(amountOut * WuxingHuaHenTuning.UNDO_RETURN_RATIO);
  }
}
