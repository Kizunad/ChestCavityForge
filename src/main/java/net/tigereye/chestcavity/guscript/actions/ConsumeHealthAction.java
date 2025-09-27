package net.tigereye.chestcavity.guscript.actions;

import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;

public record ConsumeHealthAction(int amount) implements Action {
    public static final String ID = "consume.health";

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
