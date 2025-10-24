package net.tigereye.chestcavity.soul.registry;

import java.util.UUID;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;

/** Determines whether a given soul should be included in background snapshots. */
public interface BackgroundSnapshotFilter {
  boolean shouldSnapshot(UUID ownerId, UUID soulId, SoulPlayer soulPlayer);
}
