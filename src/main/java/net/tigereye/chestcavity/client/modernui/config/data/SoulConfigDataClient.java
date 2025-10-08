package net.tigereye.chestcavity.client.modernui.config.data;

import icyllis.modernui.annotation.MainThread;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.tigereye.chestcavity.client.modernui.config.network.SoulConfigRequestPayload;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SoulConfigDataClient {

    public static final SoulConfigDataClient INSTANCE = new SoulConfigDataClient();

    private volatile Snapshot snapshot = Snapshot.empty();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private SoulConfigDataClient() {}

    public Snapshot snapshot() {
        return snapshot;
    }

    public void updateSnapshot(Snapshot newSnapshot) {
        snapshot = newSnapshot;
        for (Listener listener : listeners) {
            listener.onSoulConfigUpdated(newSnapshot);
        }
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void requestSync() {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) {
            return;
        }
        mc.execute(() -> connection.send(new SoulConfigRequestPayload()));
    }

    public record Snapshot(List<SoulEntry> entries) {
        public static Snapshot empty() {
            return new Snapshot(List.of());
        }

        public boolean isEmpty() {
            return entries.isEmpty();
        }
    }

    public record SoulEntry(
            java.util.UUID soulId,
            String displayName,
            boolean owner,
            boolean active,
            float health,
            float maxHealth,
            float absorption,
            int food,
            float saturation,
            int xpLevel,
            float xpProgress
    ) {}

    @FunctionalInterface
    public interface Listener {
        @MainThread
        void onSoulConfigUpdated(Snapshot snapshot);
    }
}
