package net.tigereye.chestcavity.guscript.runtime.exec;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Runtime context passed to action execution, providing entity references and bridges.
 */
public interface GuScriptContext {

    Player performer();

    LivingEntity target();

    GuScriptExecutionBridge bridge();

    void addDamageMultiplier(double multiplier);

    void addFlatDamage(double amount);

    double damageMultiplier();

    double flatDamageBonus();

    default double applyDamageModifiers(double baseDamage) {
        double scaled = baseDamage * (1.0 + damageMultiplier());
        return Math.max(0.0, scaled + flatDamageBonus());
    }
}
