package net.tigereye.chestcavity.network.packets;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.client.ui.ReminderToast;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Server-to-client: show a small toast when a cooldown finishes.
 */
public record CooldownReadyToastPayload(
        boolean useItemIcon,
        ResourceLocation iconId,
        String title,
        String subtitle
) implements CustomPacketPayload {

    public static final Type<CooldownReadyToastPayload> TYPE = new Type<>(ChestCavity.id("cooldown_ready_toast"));

    public static final StreamCodec<FriendlyByteBuf, CooldownReadyToastPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBoolean(payload.useItemIcon);
                buf.writeResourceLocation(payload.iconId);
                buf.writeUtf(payload.title);
                buf.writeUtf(payload.subtitle);
            },
            buf -> new CooldownReadyToastPayload(
                    buf.readBoolean(),
                    buf.readResourceLocation(),
                    buf.readUtf(),
                    buf.readUtf()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CooldownReadyToastPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) return;
            try {
                if (payload.useItemIcon) {
                    var opt = BuiltInRegistries.ITEM.getOptional(payload.iconId);
                    if (opt.isPresent()) {
                        Item item = opt.get();
                        ReminderToast.showItem(payload.title, payload.subtitle, new ItemStack(item));
                        return;
                    }
                }
                // fallback to texture icon
                ReminderToast.show(payload.title, payload.subtitle, payload.iconId);
            } catch (Throwable t) {
                ChestCavity.LOGGER.warn("[toast] Failed to render cooldown toast: {}", t.toString());
            }
        });
    }
}

