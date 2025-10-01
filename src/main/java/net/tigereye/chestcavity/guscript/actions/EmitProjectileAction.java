package net.tigereye.chestcavity.guscript.actions;

import javax.annotation.Nullable;

import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;
import net.tigereye.chestcavity.guscript.runtime.exec.FlowVariableProvider;
import net.tigereye.chestcavity.guscript.runtime.exec.ProjectileEmission;

public record EmitProjectileAction(
        String projectileId,
        double damage,
        @Nullable Double length,
        @Nullable Double thickness,
        @Nullable Integer lifespanTicks,
        @Nullable Integer maxPierce,
        @Nullable Double breakPower,
        @Nullable String lengthVariable,
        @Nullable String thicknessVariable,
        @Nullable String lifespanVariable,
        @Nullable String maxPierceVariable,
        @Nullable String breakPowerVariable
) implements Action {
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
        double finalDamage = context.applyDamageModifiers(damage);
        Double resolvedLength = resolveDouble(length, lengthVariable, context);
        Double resolvedThickness = resolveDouble(thickness, thicknessVariable, context);
        Integer resolvedLifespan = resolveInteger(lifespanTicks, lifespanVariable, context);
        Integer resolvedMaxPierce = resolveInteger(maxPierce, maxPierceVariable, context);
        Double resolvedBreakPower = resolveDouble(breakPower, breakPowerVariable, context);
        ProjectileEmission emission = new ProjectileEmission(
                projectileId,
                finalDamage,
                resolvedLength,
                resolvedThickness,
                resolvedLifespan,
                resolvedMaxPierce,
                resolvedBreakPower
        );
        context.bridge().emitProjectile(emission);
    }

    private static Double resolveDouble(@Nullable Double base, @Nullable String variable, GuScriptContext context) {
        Double result = base;
        if (variable != null && context instanceof FlowVariableProvider provider) {
            double fallback = result != null ? result : 0.0D;
            result = provider.resolveFlowVariable(variable, fallback);
        }
        return result;
    }

    private static Integer resolveInteger(@Nullable Integer base, @Nullable String variable, GuScriptContext context) {
        Integer result = base;
        if (variable != null && context instanceof FlowVariableProvider provider) {
            long fallback = result != null ? result : 0L;
            result = (int) provider.resolveFlowVariableAsLong(variable, fallback);
        }
        return result;
    }
}
