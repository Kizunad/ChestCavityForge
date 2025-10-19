package net.tigereye.chestcavity.soul.fakeplayer.brain.model;

import net.minecraft.world.entity.LivingEntity;

/** Snapshot of survival heuristics for the current tick. */
public record SurvivalSnapshot(
        LivingEntity threat,
        double fleeScore,
        boolean shouldRetreat,
        boolean shouldHold,
        double healthRatio,
        double absorption,
        boolean hasRegen,
        boolean inDanger,
        double distanceToThreat
) {
    public boolean hasThreat() {
        return threat != null && threat.isAlive();
    }
}
