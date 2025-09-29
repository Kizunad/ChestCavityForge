package net.tigereye.chestcavity.listeners.damage;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

/**
 * Contract for organs or subsystems that can mitigate incoming damage.
 * Implementations are expected to mutate state (e.g. consume charge) and
 * return the remaining damage after mitigation.
 */
public interface IncomingDamageShield {

    ShieldResult absorb(ShieldContext context);

    /** Context passed to shields when evaluating mitigation. */
    record ShieldContext(
            DamageSource source,
            LivingEntity victim,
            ChestCavityInstance chestCavity,
            ItemStack organ,
            float incomingDamage,
            long gameTime
    ) {
    }

    /** Result of applying a shield to an incoming damage packet. */
    record ShieldResult(float remainingDamage, float blockedDamage, boolean fullyBlocked) {
        public static ShieldResult noShield(float originalDamage) {
            return new ShieldResult(originalDamage, 0.0f, false);
        }
    }
}
