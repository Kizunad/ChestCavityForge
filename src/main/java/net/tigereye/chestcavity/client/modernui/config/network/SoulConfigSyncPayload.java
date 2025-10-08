package net.tigereye.chestcavity.client.modernui.config.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.client.modernui.config.data.SoulConfigDataClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record SoulConfigSyncPayload(List<Entry> entries) implements CustomPacketPayload {

    public static final Type<SoulConfigSyncPayload> TYPE = new Type<>(ChestCavity.id("soul_config_sync"));

    public static final StreamCodec<FriendlyByteBuf, SoulConfigSyncPayload> STREAM_CODEC =
            StreamCodec.of(SoulConfigSyncPayload::write, SoulConfigSyncPayload::read);

    private static void write(FriendlyByteBuf buf, SoulConfigSyncPayload payload) {
        buf.writeVarInt(payload.entries.size());
        for (Entry entry : payload.entries) {
            buf.writeUUID(entry.soulId);
            buf.writeUtf(entry.displayName);
            buf.writeBoolean(entry.owner);
            buf.writeBoolean(entry.active);
            buf.writeFloat(entry.health);
            buf.writeFloat(entry.maxHealth);
            buf.writeFloat(entry.absorption);
            buf.writeVarInt(entry.food);
            buf.writeFloat(entry.saturation);
            buf.writeVarInt(entry.xpLevel);
            buf.writeFloat(entry.xpProgress);
        }
    }

    private static SoulConfigSyncPayload read(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            UUID soulId = buf.readUUID();
            String name = buf.readUtf();
            boolean owner = buf.readBoolean();
            boolean active = buf.readBoolean();
            float health = buf.readFloat();
            float maxHealth = buf.readFloat();
            float absorption = buf.readFloat();
            int food = buf.readVarInt();
            float saturation = buf.readFloat();
            int lvl = buf.readVarInt();
            float xpProgress = buf.readFloat();
            entries.add(new Entry(soulId, name, owner, active, health, maxHealth, absorption, food, saturation, lvl, xpProgress));
        }
        return new SoulConfigSyncPayload(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SoulConfigSyncPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            List<SoulConfigDataClient.SoulEntry> entries = payload.entries.stream()
                    .map(e -> new SoulConfigDataClient.SoulEntry(
                            e.soulId,
                            e.displayName,
                            e.owner,
                            e.active,
                            e.health,
                            e.maxHealth,
                            e.absorption,
                            e.food,
                            e.saturation,
                            e.xpLevel,
                            e.xpProgress))
                    .toList();
            SoulConfigDataClient.INSTANCE.updateSnapshot(new SoulConfigDataClient.Snapshot(entries));
        });
    }

    public record Entry(
            UUID soulId,
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
}
