package net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.damage;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;

/**
 * Immutable snapshot describing a Soul Beast damage interception.
 */
public record SoulBeastDamageContext(LivingEntity victim, DamageSource source, float incomingDamage) {

    public SoulBeastDamageContext {
        Objects.requireNonNull(victim, "victim");
        Objects.requireNonNull(source, "source");
    }
}
