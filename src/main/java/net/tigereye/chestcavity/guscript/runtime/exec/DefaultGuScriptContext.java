package net.tigereye.chestcavity.guscript.runtime.exec;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Mutable runtime context capturing performer, target, and runtime modifiers.
 */
public final class DefaultGuScriptContext implements GuScriptContext {

    private final Player performer;
    private final LivingEntity target;
    private final GuScriptExecutionBridge bridge;
    private double damageMultiplier;
    private double flatDamageBonus;

    public DefaultGuScriptContext(Player performer, LivingEntity target, GuScriptExecutionBridge bridge) {
        if (bridge == null) {
            throw new IllegalArgumentException("GuScriptExecutionBridge must not be null");
        }
        this.performer = performer;
        this.target = target;
        this.bridge = bridge;
    }

    @Override
    public Player performer() {
        return performer;
    }

    @Override
    public LivingEntity target() {
        return target;
    }

    @Override
    public GuScriptExecutionBridge bridge() {
        return bridge;
    }

    @Override
    public void addDamageMultiplier(double multiplier) {
        damageMultiplier += multiplier;
    }

    @Override
    public void addFlatDamage(double amount) {
        flatDamageBonus += amount;
    }

    @Override
    public double damageMultiplier() {
        return damageMultiplier;
    }

    @Override
    public double flatDamageBonus() {
        return flatDamageBonus;
    }
}
