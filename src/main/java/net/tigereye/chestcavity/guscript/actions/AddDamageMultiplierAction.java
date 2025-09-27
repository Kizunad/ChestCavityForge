package net.tigereye.chestcavity.guscript.actions;

import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;

public record AddDamageMultiplierAction(double multiplier) implements Action {
    public static final String ID = "modifier.damage_multiplier";

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
