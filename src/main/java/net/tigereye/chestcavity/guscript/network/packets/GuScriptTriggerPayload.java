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
        boolean hasLineOfSight = player.hasLineOfSight(living);
        Vec3 eyePos = player.getEyePosition();
        Vec3 toTarget = living.getEyePosition().subtract(eyePos);
        double squaredLength = toTarget.lengthSqr();
        double dot = squaredLength < 1.0E-6
                ? 1.0
                : player.getLookAngle().dot(toTarget.normalize());

        if (!isTargetAllowed(distanceSqr, hasLineOfSight, dot)) {
            ChestCavity.LOGGER.debug(
                    "[GuScript] Rejected spoofed target {} (distanceSqr={}, hasLineOfSight={}, dot={})",
                    living.getUUID(),
                    distanceSqr,
                    hasLineOfSight,
                    dot
            );
            return null;
        }
        return living;
    }

    static boolean isTargetAllowed(double distanceSqr, boolean hasLineOfSight, double viewDot) {
        if (!hasLineOfSight) {
            return false;
        }
        if (distanceSqr > MAX_TARGET_RANGE_SQR) {
            return false;
        }
        return viewDot >= MIN_VIEW_DOT;
    }

    static final double MAX_TARGET_RANGE_SQR = 400.0; // 20 blocks squared
    static final double MIN_VIEW_DOT = Math.cos(Math.toRadians(70.0));
}
