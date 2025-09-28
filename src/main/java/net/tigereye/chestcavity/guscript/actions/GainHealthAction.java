package net.tigereye.chestcavity.guscript.actions;

import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;

/**
 * Heals the performer by a fixed amount of health.
 */
public record GainHealthAction(double amount) implements Action {

    public static final String ID = "gain.health";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "治疗 " + amount;
    }

    @Override
    public void execute(GuScriptContext context) {
        LivingEntity entity = context.performer();
        if (entity == null || amount <= 0.0) {
            return;
        }
        entity.heal((float) amount);
    }
}

