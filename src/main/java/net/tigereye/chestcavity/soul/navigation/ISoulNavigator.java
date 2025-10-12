package net.tigereye.chestcavity.soul.navigation;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;

/**
 * Pluggable navigation engine interface for Soul navigation.
 * Implementations must be side-effect free w.r.t world entity spawning and
 * apply movement only to the provided {@link SoulPlayer} in {@link #tick}.
 */
public interface ISoulNavigator {
    void setGoal(SoulPlayer soul, Vec3 target, double speedModifier, double stopDistance);
    void clearGoal();
    void tick(SoulPlayer soul);
}

