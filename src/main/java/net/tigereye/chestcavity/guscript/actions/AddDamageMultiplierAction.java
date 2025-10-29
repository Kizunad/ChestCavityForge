package net.tigereye.chestcavity.guscript.actions;

import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;

/**
 * Action to add damage multiplier.
 */
public class AddDamageMultiplierAction implements Action {
  public static final String ID = "modifier.damage_multiplier";

  private final double multiplier;

  public AddDamageMultiplierAction(double multiplier) {
    this.multiplier = multiplier;
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public String description() {
    return "伤害倍率 +" + multiplier;
  }

  @Override
  public void execute(GuScriptContext context) {
    context.addDamageMultiplier(multiplier);
  }
}
