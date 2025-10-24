package net.tigereye.chestcavity.soul.profile.capability;

import java.util.concurrent.atomic.AtomicBoolean;
import net.tigereye.chestcavity.soul.profile.capability.guzhenren.GuzhenrenSnapshot;

public final class CapabilitySnapshots {

  private static final AtomicBoolean BOOTSTRAPPED = new AtomicBoolean(false);

  private CapabilitySnapshots() {}

  public static void bootstrap() {
    if (BOOTSTRAPPED.compareAndSet(false, true)) {
      CapabilitySnapshotRegistry.register(ChestCavitySnapshot.ID, ChestCavitySnapshot::new);
      CapabilitySnapshotRegistry.register(GuzhenrenSnapshot.ID, GuzhenrenSnapshot::new);
    }
  }
}
