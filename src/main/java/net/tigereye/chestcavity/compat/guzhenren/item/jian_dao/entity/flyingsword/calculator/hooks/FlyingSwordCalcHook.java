package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator.hooks;

import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator.context.CalcContext;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator.context.CalcOutputs;

/**
 * 飞剑计算钩子接口：允许外部模块根据上下文修改输出参数。
 *
 * <p>实现方只需根据需要调整 CalcOutputs 中相关字段。
 */
@FunctionalInterface
public interface FlyingSwordCalcHook {
  void apply(CalcContext ctx, CalcOutputs out);
}

