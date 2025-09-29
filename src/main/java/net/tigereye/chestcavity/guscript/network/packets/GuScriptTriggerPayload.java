package net.tigereye.chestcavity.guscript.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.data.GuScriptAttachment;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptExecutor;
import net.tigereye.chestcavity.registration.CCAttachments;

/**
 * Client-to-server payload requesting GuScript execution via keybind.
 */
public record GuScriptTriggerPayload(int pageIndex, int targetEntityId) implements CustomPacketPayload {

    public static final Type<GuScriptTriggerPayload> TYPE = new Type<>(ChestCavity.id("guscript_trigger"));

    public static final StreamCodec<FriendlyByteBuf, GuScriptTriggerPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.pageIndex);
                buf.writeVarInt(payload.targetEntityId);
            },
            buf -> new GuScriptTriggerPayload(buf.readVarInt(), buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GuScriptTriggerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            GuScriptAttachment attachment = CCAttachments.getGuScript(player);
            if (payload.pageIndex >= 0) {
                attachment.setCurrentPage(payload.pageIndex);
            }
            LivingEntity target = null;
            if (payload.targetEntityId >= 0) {
                var entity = player.level().getEntity(payload.targetEntityId);
                if (entity instanceof LivingEntity living && entity.isAlive()) {
                    target = living;
                }
            }
            GuScriptExecutor.triggerKeybind(player, target, attachment);
        });
    }
}
