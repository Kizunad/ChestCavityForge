package net.tigereye.chestcavity.guscript.actions;

import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;

public record ExportMultiplierModifierAction(double amount) implements Action {
  public static final String ID = "export.modifier.multiplier";

  @Override
  public String id() {
    return ID;
  }

  @Override
  public String description() {
    return "导出伤害倍率 +" + amount;
  }

  @Override
  public void execute(GuScriptContext context) {
    context.exportDamageMultiplier(amount);
  }
}
