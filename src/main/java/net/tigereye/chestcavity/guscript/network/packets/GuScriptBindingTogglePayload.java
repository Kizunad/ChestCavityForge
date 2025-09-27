package net.tigereye.chestcavity.guscript.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.data.BindingTarget;
import net.tigereye.chestcavity.guscript.data.GuScriptAttachment;
import net.tigereye.chestcavity.guscript.data.ListenerType;
import net.tigereye.chestcavity.guscript.ui.GuScriptMenu;
import net.tigereye.chestcavity.registration.CCAttachments;

public record GuScriptBindingTogglePayload(Operation operation) implements CustomPacketPayload {

    public static final Type<GuScriptBindingTogglePayload> TYPE =
            new Type<>(ChestCavity.id("guscript_binding_toggle"));

    public static final StreamCodec<FriendlyByteBuf, GuScriptBindingTogglePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeEnum(payload.operation),
            buf -> new GuScriptBindingTogglePayload(buf.readEnum(Operation.class))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GuScriptBindingTogglePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                ChestCavity.LOGGER.warn("[GuScript] Binding toggle received without server player context");
                return;
            }
            GuScriptAttachment attachment = CCAttachments.getGuScript(player);
            switch (payload.operation) {
                case TOGGLE_TARGET -> attachment.cycleBindingTarget();
                case CYCLE_LISTENER -> attachment.cycleListenerType();
            }

            if (attachment.getBindingTarget() == BindingTarget.KEYBIND) {
                // Leave listener selection unchanged; nothing extra required
            } else if (attachment.getListenerType() == null) {
                attachment.setListenerType(ListenerType.ON_HIT);
            }

            if (player.containerMenu instanceof GuScriptMenu menu) {
                menu.syncFromAttachment(attachment);
            }
        });
    }

    public enum Operation {
        TOGGLE_TARGET,
        CYCLE_LISTENER;
    }
}
