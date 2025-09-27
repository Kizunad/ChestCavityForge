package net.tigereye.chestcavity.guscript.actions;

import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;

public record EmitProjectileAction(String projectileId, double damage) implements Action {
    public static final String ID = "emit.projectile";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "发射 " + projectileId + " (伤害 " + damage + ")";
    }

    @Override
    public void execute(GuScriptContext context) {
        context.bridge().emitProjectile(projectileId, damage);
    }
}
