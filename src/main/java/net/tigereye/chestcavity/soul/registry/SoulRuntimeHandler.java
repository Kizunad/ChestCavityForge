package net.tigereye.chestcavity.soul.registry;

import net.minecraft.world.damagesource.DamageSource;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;

/**
 * Hook interface for SoulPlayer runtime logic (tick, hurt, etc.).
 */
public interface SoulRuntimeHandler {

    default void onTickStart(SoulPlayer player) {}

    default void onTickEnd(SoulPlayer player) {}

    /**
     * Allow handlers to cancel or modify incoming damage.
     */
    default SoulHurtResult onHurt(SoulPlayer player, DamageSource source, float amount) {
        return SoulHurtResult.pass();
    }
}

