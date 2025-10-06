package net.tigereye.chestcavity.soul.registry;

import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;

import java.util.UUID;

/**
 * Determines whether a given soul should be included in background snapshots.
 */
public interface BackgroundSnapshotFilter {
    boolean shouldSnapshot(UUID ownerId, UUID soulId, SoulPlayer soulPlayer);
}

