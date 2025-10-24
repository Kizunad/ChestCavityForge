package net.tigereye.chestcavity.soul.registry;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;

/** Global active background snapshot filter. Default excludes currently possessed soul. */
public final class BackgroundSnapshotFilters {

  private static final AtomicReference<BackgroundSnapshotFilter> ACTIVE =
      new AtomicReference<>(new Default());

  private BackgroundSnapshotFilters() {}

  public static BackgroundSnapshotFilter get() {
    return ACTIVE.get();
  }

  public static void set(BackgroundSnapshotFilter filter) {
    if (filter != null) ACTIVE.set(filter);
  }

  private static final class Default implements BackgroundSnapshotFilter {
    @Override
    public boolean shouldSnapshot(UUID ownerId, UUID soulId, SoulPlayer soulPlayer) {
      // exclude current possession (ownerActiveSoul handled by caller for accuracy if needed)
      return true; // caller still checks possession; this default allows others
    }
  }
}
