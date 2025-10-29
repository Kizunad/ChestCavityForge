package net.tigereye.chestcavity.guscript.actions;

import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;

/**
 * Action to export flat modifier.
 */
public class ExportFlatModifierAction implements Action {
  public static final String ID = "export.modifier.flat";

  private final double amount;

  public ExportFlatModifierAction(double amount) {
    this.amount = amount;
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public String description() {
    return "导出额外伤害 +" + amount;
  }

  @Override
  public void execute(GuScriptContext context) {
    context.exportFlatDamage(amount);
  }
}
