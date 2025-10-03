package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.storage;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Abstraction over how Hun Dao organs capture and release beast souls.
 * Implementations are responsible for persisting the serialized entity payload on the organ stack.
 */
public interface BeastSoulStorage {

    /** @return {@code true} if a beast soul payload is already present on the provided organ. */
    boolean hasStoredSoul(ItemStack organ);

    /**
     * Quick validation hook that callers can use before attempting to store a soul.
     * Implementations may reject certain entity types (players, bosses, etc.).
     */
    default boolean canStore(ItemStack organ, LivingEntity entity) {
        return organ != null && !organ.isEmpty() && entity != null && !(entity instanceof net.minecraft.world.entity.player.Player) && !hasStoredSoul(organ);
    }

    /**
     * Serialises the provided entity and saves the payload into the organ stack.
     *
     * @param organ          organ stack that will host the payload
     * @param entity         entity whose soul should be captured
     * @param storedGameTime timestamp (in game ticks) when the capture occurred
     * @return the stored record if successful
     */
    Optional<BeastSoulRecord> store(ItemStack organ, LivingEntity entity, long storedGameTime);

    /** Returns the stored soul payload without mutating the underlying stack. */
    Optional<BeastSoulRecord> peek(ItemStack organ);

    /**
     * Removes and returns the stored payload. Callers can then respawn the entity elsewhere.
     */
    Optional<BeastSoulRecord> consume(ItemStack organ);

    /** Deletes any stored payload without returning it. */
    void clear(ItemStack organ);
}
