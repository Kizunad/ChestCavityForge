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
    private final ExecutionSession session;
    private double damageMultiplier;
    private double flatDamageBonus;
    private double addedMultiplier;
    private double addedFlat;
    private double directMultiplierExports;
    private double directFlatExports;
    private double directTimeScaleMultiplier = 1.0D;
    private boolean hasDirectTimeScaleMultiplier;
    private double directTimeScaleFlat;
    private boolean exportMultiplierDelta;
    private boolean exportFlatDelta;

    public DefaultGuScriptContext(Player performer, LivingEntity target, GuScriptExecutionBridge bridge) {
        this(performer, target, bridge, null);
    }

    public DefaultGuScriptContext(Player performer, LivingEntity target, GuScriptExecutionBridge bridge, ExecutionSession session) {
        if (bridge == null) {
            throw new IllegalArgumentException("GuScriptExecutionBridge must not be null");
        }
        this.performer = performer;
        this.target = target;
        this.bridge = bridge;
        this.session = session;
        if (session != null) {
            this.damageMultiplier = session.currentMultiplier();
            this.flatDamageBonus = session.currentFlat();
        }
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
        addedMultiplier += multiplier;
    }

    @Override
    public void addFlatDamage(double amount) {
        flatDamageBonus += amount;
        addedFlat += amount;
    }

    @Override
    public double damageMultiplier() {
        return damageMultiplier;
    }

    @Override
    public double flatDamageBonus() {
        return flatDamageBonus;
    }

    @Override
    public void exportDamageMultiplier(double amount) {
        if (amount == 0.0D) {
            return;
        }
        directMultiplierExports += amount;
        if (session != null) {
            session.exportMultiplier(amount);
        }
    }

    @Override
    public void exportFlatDamage(double amount) {
        if (amount == 0.0D) {
            return;
        }
        directFlatExports += amount;
        if (session != null) {
            session.exportFlat(amount);
        }
    }

    @Override
    public void exportTimeScaleMultiplier(double multiplier) {
        if (Double.isNaN(multiplier) || Double.isInfinite(multiplier) || multiplier <= 0.0D) {
            return;
        }
        directTimeScaleMultiplier *= multiplier;
        hasDirectTimeScaleMultiplier = true;
        if (session != null) {
            session.exportTimeScaleMultiplier(multiplier);
        }
    }

    @Override
    public void exportTimeScaleFlat(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount == 0.0D) {
            return;
        }
        directTimeScaleFlat += amount;
        if (session != null) {
            session.exportTimeScaleFlat(amount);
        }
    }

    @Override
    public void enableModifierExports(boolean exportMultiplier, boolean exportFlat) {
        this.exportMultiplierDelta = exportMultiplier;
        this.exportFlatDelta = exportFlat;
    }

    @Override
    public double exportedMultiplierDelta() {
        return exportMultiplierDelta ? addedMultiplier : 0.0D;
    }

    @Override
    public double exportedFlatDelta() {
        return exportFlatDelta ? addedFlat : 0.0D;
    }

    @Override
    public double directExportedMultiplier() {
        return directMultiplierExports;
    }

    @Override
    public double directExportedFlat() {
        return directFlatExports;
    }

    @Override
    public double directExportedTimeScaleMultiplier() {
        return hasDirectTimeScaleMultiplier ? directTimeScaleMultiplier : 1.0D;
    }

    @Override
    public double directExportedTimeScaleFlat() {
        return directTimeScaleFlat;
    }
}
