package net.tigereye.chestcavity.guscript.actions;

import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;

/**
 * Action to add flat damage.
 */
public class AddFlatDamageAction implements Action {
  public static final String ID = "modifier.damage_flat";

  private final double amount;

  public AddFlatDamageAction(double amount) {
    this.amount = amount;
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public String description() {
    return "固定伤害 +" + amount;
  }

  @Override
  public void execute(GuScriptContext context) {
    context.addFlatDamage(amount);
  }
}
