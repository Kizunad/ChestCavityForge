package net.tigereye.chestcavity.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior.HuoYiGuOrganBehavior;
import net.tigereye.chestcavity.registration.CCAttachments;

public record ChestCavityHotkeyPayload(ResourceLocation abilityId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ChestCavityHotkeyPayload> TYPE = new CustomPacketPayload.Type<>(ChestCavity.id("hotkey"));

    public static final StreamCodec<FriendlyByteBuf, ChestCavityHotkeyPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeResourceLocation(payload.abilityId),
            buf -> new ChestCavityHotkeyPayload(buf.readResourceLocation())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChestCavityHotkeyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() != null) {
                var cc = CCAttachments.getChestCavity(context.player());
                OrganActivationListeners.activate(payload.abilityId, cc);
            }
        });
    }
}
