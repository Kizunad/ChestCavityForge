package net.tigereye.chestcavity.guzhenren.network.packets;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;

import java.util.Objects;

/**
 * Client-sent payload carrying the player's Mojang skin textures so integrated servers can retain
 * the data. Dedicated servers already receive this via the login handshake, but singleplayer hosts
 * lose the property without an explicit sync.
 */
public record PlayerSkinUploadPayload(String value, String signature) implements CustomPacketPayload {

    public static final Type<PlayerSkinUploadPayload> TYPE = new Type<>(ChestCavity.id("player_skin_upload"));

    public static final StreamCodec<FriendlyByteBuf, PlayerSkinUploadPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(Objects.requireNonNullElse(payload.value(), ""));
                buf.writeNullable(payload.signature(), FriendlyByteBuf::writeUtf);
            },
            buf -> new PlayerSkinUploadPayload(buf.readUtf(), buf.readNullable(FriendlyByteBuf::readUtf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlayerSkinUploadPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            String value = payload.value();
            if (value == null || value.isBlank()) {
                ChestCavity.LOGGER.debug("[compat/guzhenren][skin] Ignoring empty skin payload from {}", player.getGameProfile().getName());
                return;
            }
            GameProfile profile = player.getGameProfile();
            if (profile == null) {
                return;
            }
            PropertyMap properties = profile.getProperties();
            if (properties == null) {
                return;
            }
            String signature = payload.signature();
            Property property = (signature != null && !signature.isBlank())
                    ? new Property("textures", value, signature)
                    : new Property("textures", value);
            Property existing = properties.get("textures").stream().findFirst().orElse(null);
            if (existing != null && Objects.equals(existing.value(), property.value()) && Objects.equals(existing.signature(), property.signature())) {
                return;
            }
            properties.removeAll("textures");
            properties.put("textures", property);
            ChestCavity.LOGGER.debug("[compat/guzhenren][skin] Applied skin payload for {} (signed={})", profile.getName(), signature != null && !signature.isBlank());
        });
    }
}
