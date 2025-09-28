package net.tigereye.chestcavity.guscript.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowState;
import net.tigereye.chestcavity.guscript.runtime.flow.client.GuScriptClientFlows;

/**
 * Mirrors flow state changes to the client.
 */
public record FlowSyncPayload(int entityId, ResourceLocation programId, FlowState state, long stateGameTime, int ticksInState)
        implements CustomPacketPayload {

    public static final Type<FlowSyncPayload> TYPE = new Type<>(ChestCavity.id("guscript_flow_sync"));

    public static final StreamCodec<FriendlyByteBuf, FlowSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.entityId);
                buf.writeResourceLocation(payload.programId);
                buf.writeEnum(payload.state);
                buf.writeVarLong(payload.stateGameTime);
                buf.writeVarInt(payload.ticksInState);
            },
            buf -> new FlowSyncPayload(
                    buf.readVarInt(),
                    buf.readResourceLocation(),
                    buf.readEnum(FlowState.class),
                    buf.readVarLong(),
                    buf.readVarInt()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FlowSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> GuScriptClientFlows.handleSync(payload));
    }
}
