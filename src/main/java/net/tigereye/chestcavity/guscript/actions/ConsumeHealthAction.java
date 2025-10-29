package net.tigereye.chestcavity.guscript.actions;

import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;

/**
 * Action to consume health.
 */
public class ConsumeHealthAction implements Action {
  public static final String ID = "consume.health";

  private final int amount;

  public ConsumeHealthAction(int amount) {
    this.amount = amount;
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public String description() {
    return "消耗生命 " + amount;
  }

  @Override
  public void execute(GuScriptContext context) {
    context.bridge().consumeHealth(amount);
  }
}
