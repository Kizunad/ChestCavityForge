package net.tigereye.chestcavity.guscript.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
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
            LivingEntity target = resolveTarget(player, payload.targetEntityId);
            GuScriptExecutor.triggerKeybind(player, target, attachment);
        });
    }

    static LivingEntity resolveTarget(ServerPlayer player, int targetEntityId) {
        if (targetEntityId < 0) {
            return null;
        }
        var entity = player.level().getEntity(targetEntityId);
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            return null;
        }
        if (living == player) {
            return living;
        }

        double distanceSqr = player.distanceToSqr(living);
        if (distanceSqr > MAX_TARGET_RANGE_SQR) {
            ChestCavity.LOGGER.debug("[GuScript] Rejected spoofed target {} beyond range {}", living.getUUID(), Math.sqrt(MAX_TARGET_RANGE_SQR));
            return null;
        }
        if (!player.hasLineOfSight(living)) {
            ChestCavity.LOGGER.debug("[GuScript] Rejected spoofed target {} without line of sight", living.getUUID());
            return null;
        }

        Vec3 eyePos = player.getEyePosition();
        Vec3 toTarget = living.getEyePosition().subtract(eyePos);
        double squaredLength = toTarget.lengthSqr();
        if (squaredLength < 1.0E-6) {
            return living;
        }
        Vec3 look = player.getLookAngle();
        double dot = look.dot(toTarget.normalize());
        if (dot < MIN_VIEW_DOT) {
            ChestCavity.LOGGER.debug("[GuScript] Rejected spoofed target {} outside view cone (dot={})", living.getUUID(), dot);
            return null;
        }
        return living;
    }

    private static final double MAX_TARGET_RANGE_SQR = 400.0; // 20 blocks squared
    private static final double MIN_VIEW_DOT = Math.cos(Math.toRadians(70.0));
}
