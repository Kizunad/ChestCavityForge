package net.tigereye.chestcavity.guscript.actions;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;

/**
 * Deals mental (indirect magic) damage to the current GuScript target if it is within range.
 */
public record MindShockAction(double damage, double range) implements Action {

    public static final String ID = "mind.shock";

    public MindShockAction {
        double sanitizedDamage = Double.isNaN(damage) ? 0.0D : damage;
        if (sanitizedDamage < 0.0D) {
            sanitizedDamage = 0.0D;
        }
        double sanitizedRange = Double.isNaN(range) ? 16.0D : range;
        if (sanitizedRange <= 0.0D) {
            sanitizedRange = 16.0D;
        }
        damage = sanitizedDamage;
        range = sanitizedRange;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "心灵冲击 " + damage + " (range=" + range + ")";
    }

    @Override
    public void execute(GuScriptContext context) {
        Player performer = context.performer();
        if (performer == null || damage <= 0.0D) {
            return;
        }
        LivingEntity target = context.target();
        if (target == null || target == performer || !target.isAlive()) {
            return;
        }
        double maxDistanceSq = range * range;
        if (!Double.isInfinite(maxDistanceSq) && performer.distanceToSqr(target) > maxDistanceSq) {
            return;
        }
        double resolvedDamage = context.applyDamageModifiers(damage);
        if (resolvedDamage <= 0.0D) {
            return;
        }
        DamageSource source = performer.damageSources().indirectMagic(performer, performer);
        target.invulnerableTime = 0;
        target.hurt(source, (float) resolvedDamage);
        target.invulnerableTime = 0;
    }
}
