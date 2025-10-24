package net.tigereye.chestcavity.guscript.actions;

import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;

public record ConsumeZhenyuanAction(int amount) implements Action {
  public static final String ID = "consume.zhenyuan";

  @Override
  public String id() {
    return ID;
  }

  @Override
  public String description() {
    return "消耗真元 " + amount;
  }

  @Override
  public void execute(GuScriptContext context) {
    context.bridge().consumeZhenyuan(amount);
  }
}
