package net.tigereye.chestcavity.soul.registry;

import java.util.UUID;

/**
 * Governs whether a particular write is allowed for a given owner/profile.
 * Implementations may enforce extra constraints (e.g., permissions, cooldowns).
 */
public interface SoulWritePolicy {

    /** Allow writing Owner profile from Owner self (not possessing). */
    default boolean allowOwnerSelfWrite(UUID ownerId) { return true; }

    /** Allow writing Owner profile from Owner shell (while possessing). */
    default boolean allowOwnerShellWrite(UUID ownerId) { return true; }

    /** Allow writing the given soul's profile from its SoulPlayer. */
    default boolean allowSoulWrite(UUID ownerId, UUID soulId) { return true; }
}

