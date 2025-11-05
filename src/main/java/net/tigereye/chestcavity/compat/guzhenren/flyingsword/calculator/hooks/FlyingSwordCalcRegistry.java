package net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator.hooks;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator.context.CalcContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator.context.CalcOutputs;

/**
 * 飞剑计算钩子注册表：集中管理、按顺序应用。
 */
public final class FlyingSwordCalcRegistry {
  private static final List<FlyingSwordCalcHook> HOOKS = new CopyOnWriteArrayList<>();

  private FlyingSwordCalcRegistry() {}

  public static void register(FlyingSwordCalcHook hook) {
    if (hook != null) HOOKS.add(hook);
  }

  public static void unregister(FlyingSwordCalcHook hook) {
    HOOKS.remove(hook);
  }

  public static CalcOutputs applyAll(CalcContext ctx) {
    CalcOutputs out = CalcOutputs.create();
    for (var hook : HOOKS) {
      try {
        hook.apply(ctx, out);
      } catch (Throwable t) {
        // 静默失败，避免影响主流程
      }
    }
    return out;
  }
}

