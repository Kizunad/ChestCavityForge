package net.tigereye.chestcavity.soul.fakeplayer.brain.policy;

import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.soul.fakeplayer.brain.model.SurvivalSnapshot;

/**
 * Tracks a minimum safe window before leaving survival/retreat state.
 */
public final class SafetyWindowPolicy {

    private final int windowTicks;
    private final double exitScore;

    public SafetyWindowPolicy(int windowTicks, double exitScore) {
        this.windowTicks = Math.max(0, windowTicks);
        this.exitScore = Math.max(0.0, Math.min(1.0, exitScore));
    }

    public int windowTicks() {
        return windowTicks;
    }

    public double exitScore() {
        return exitScore;
    }

    /** Reset the safety window due to an unsafe event. */
    public void refreshUnsafe(MultiCooldown.Entry entry, long gameTime) {
        if (entry == null) {
            return;
        }
        entry.setReadyAt(gameTime + windowTicks);
    }

    /** Whether we can exit survival mode based on the current snapshot and cooldown. */
    public boolean isSafeToExit(MultiCooldown.Entry entry, long gameTime, SurvivalSnapshot snapshot) {
        if (entry == null || snapshot == null) {
            return false;
        }
        if (snapshot.shouldRetreat()) {
            return false;
        }
        if (!entry.isReady(gameTime)) {
            return false;
        }
        return snapshot.fleeScore() <= exitScore;
    }
}
