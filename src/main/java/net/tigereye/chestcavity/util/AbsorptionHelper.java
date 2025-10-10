package net.tigereye.chestcavity.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Utility for safely granting absorption without manually juggling {@link Attributes#MAX_ABSORPTION}.
 */
public final class AbsorptionHelper {

    private static final double EPSILON = 1.0E-4D;

    private AbsorptionHelper() {
    }

    /**
     * Ensures the target entity holds enough absorption capacity, applies the requested absorption value,
     * and clamps the result so it never exceeds the computed cap.
     *
     * @param entity       target entity
     * @param absorption   desired absorption amount (negative values are treated as zero)
     * @param modifierId   identifier used to install the helper's attribute modifier
     * @param onlyIncrease when {@code true}, the existing absorption will only increase (never decrease)
     * @return the absorption amount after the operation
     */
    public static float applyAbsorption(
            LivingEntity entity,
            double absorption,
            ResourceLocation modifierId,
            boolean onlyIncrease
    ) {
        if (entity == null) {
            return 0.0F;
        }
        double sanitized = Math.max(0.0D, absorption);
        if (sanitized <= EPSILON) {
            clearAbsorptionCapacity(entity, modifierId);
            if (!onlyIncrease) {
                entity.setAbsorptionAmount(0.0F);
            }
            return entity.getAbsorptionAmount();
        }
        AttributeInstance attribute = entity.getAttribute(Attributes.MAX_ABSORPTION);
        if (attribute == null) {
            return entity.getAbsorptionAmount();
        }
        ensureCapacity(attribute, sanitized, modifierId);
        float before = entity.getAbsorptionAmount();
        float desired = (float) sanitized;
        float updated = onlyIncrease ? Math.max(before, desired) : desired;
        entity.setAbsorptionAmount(updated);
        clampToCapacity(entity, attribute);
        return updated;
    }

    /**
     * Removes the helper's attribute modifier (if present) and clamps the entity's absorption
     * so it does not exceed the remaining capacity.
     */
    public static void clearAbsorptionCapacity(LivingEntity entity, ResourceLocation modifierId) {
        if (entity == null) {
            return;
        }
        AttributeInstance attribute = entity.getAttribute(Attributes.MAX_ABSORPTION);
        if (attribute == null) {
            return;
        }
        if (attribute.hasModifier(modifierId)) {
            attribute.removeModifier(modifierId);
        }
        clampToCapacity(entity, attribute);
    }

    private static void ensureCapacity(
            AttributeInstance attribute,
            double required,
            ResourceLocation modifierId
    ) {
        if (attribute.hasModifier(modifierId)) {
            attribute.removeModifier(modifierId);
        }
        double baseline = attribute.getValue();
        double shortfall = required - baseline;
        if (shortfall <= EPSILON) {
            return;
        }
        AttributeModifier modifier = new AttributeModifier(
                modifierId,
                shortfall,
                AttributeModifier.Operation.ADD_VALUE
        );
        attribute.addTransientModifier(modifier);
    }

    private static void clampToCapacity(LivingEntity entity, AttributeInstance attribute) {
        if (entity == null || attribute == null) {
            return;
        }
        double capacity = attribute.getValue();
        float current = entity.getAbsorptionAmount();
        if (current > capacity + EPSILON) {
            entity.setAbsorptionAmount((float) Math.max(0.0D, capacity));
        }
    }
}
