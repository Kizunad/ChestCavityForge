package net.tigereye.chestcavity.soul.fakeplayer.brain.scoring;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;

/**
 * Immutable inputs for utility scoring. A single instance should be prepared
 * per tick and reused by all scorers/brains to avoid duplicate computation.
 */
public record ScoreInputs(
        SoulPlayer soul,
        LivingEntity primaryTarget,
        double distanceToTarget,
        double healthRatio,
        double absorption,
        boolean hasRegen,
        boolean inDanger,
        Vec3 ownerPosition
) {
    public static Builder builder(SoulPlayer soul) { return new Builder(soul); }

    public static final class Builder {
        private final SoulPlayer soul;
        private LivingEntity primaryTarget;
        private double distanceToTarget;
        private double healthRatio;
        private double absorption;
        private boolean hasRegen;
        private boolean inDanger;
        private Vec3 ownerPosition;

        public Builder(SoulPlayer soul) { this.soul = soul; }
        public Builder target(LivingEntity target, double distance) { this.primaryTarget = target; this.distanceToTarget = distance; return this; }
        public Builder health(double healthRatio, double absorption) { this.healthRatio = healthRatio; this.absorption = absorption; return this; }
        public Builder effects(boolean hasRegen) { this.hasRegen = hasRegen; return this; }
        public Builder danger(boolean inDanger) { this.inDanger = inDanger; return this; }
        public Builder owner(Vec3 ownerPos) { this.ownerPosition = ownerPos; return this; }
        public ScoreInputs build() {
            return new ScoreInputs(soul, primaryTarget, distanceToTarget, clamp01(healthRatio), Math.max(0, absorption), hasRegen, inDanger, ownerPosition);
        }
        private static double clamp01(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
    }
}

