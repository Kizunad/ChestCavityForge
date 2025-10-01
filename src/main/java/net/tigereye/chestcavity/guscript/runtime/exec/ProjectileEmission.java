package net.tigereye.chestcavity.guscript.runtime.exec;

import javax.annotation.Nullable;

/**
 * Immutable description of a projectile spawn request produced by GuScript actions.
 */
public record ProjectileEmission(
        String projectileId,
        double damage,
        @Nullable Double length,
        @Nullable Double thickness,
        @Nullable Integer lifespanTicks,
        @Nullable Integer maxPierce,
        @Nullable Double breakPower
) {

    public ProjectileEmission {
        if (projectileId == null || projectileId.isBlank()) {
            throw new IllegalArgumentException("projectileId must be provided");
        }
    }

    public static ProjectileEmission of(String projectileId, double damage) {
        return new ProjectileEmission(projectileId, damage, null, null, null, null, null);
    }
}
