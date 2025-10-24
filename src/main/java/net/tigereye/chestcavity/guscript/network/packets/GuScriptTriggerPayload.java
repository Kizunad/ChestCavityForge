package net.tigereye.chestcavity.guscript.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.data.GuScriptAttachment;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptExecutor;
import net.tigereye.chestcavity.registration.CCAttachments;

// VULNERABILITY FIX: Removed targetEntityId from payload. Server will determine target via raycast.
public record GuScriptTriggerPayload(int pageIndex) implements CustomPacketPayload {

  public static final Type<GuScriptTriggerPayload> TYPE =
      new Type<>(ChestCavity.id("guscript_trigger"));

  public static final StreamCodec<FriendlyByteBuf, GuScriptTriggerPayload> STREAM_CODEC =
      StreamCodec.of(
          (buf, payload) -> {
            buf.writeVarInt(payload.pageIndex);
          },
          buf -> new GuScriptTriggerPayload(buf.readVarInt()));

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(GuScriptTriggerPayload payload, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          if (!(context.player() instanceof ServerPlayer player)) {
            return;
          }
          GuScriptAttachment attachment = CCAttachments.getGuScript(player);
          if (payload.pageIndex >= 0) {
            attachment.setCurrentPage(payload.pageIndex);
          }
          // VULNERABILITY FIX: Target is now resolved on the server
          LivingEntity target = resolveTarget(player);
          // If no target is found, the script might still run with the player as the target.
          // This is existing behavior, so we keep it.
          GuScriptExecutor.triggerKeybind(player, target, attachment);
        });
  }

  // VULNERABILITY FIX: This method now performs a server-side raycast to find the target,
  // ignoring any client input.
  static LivingEntity resolveTarget(ServerPlayer player) {
    double range = MAX_TARGET_RANGE;
    Vec3 eyePos = player.getEyePosition();
    Vec3 lookVec = player.getLookAngle();
    Vec3 endPos = eyePos.add(lookVec.scale(range));
    AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0D);

    EntityHitResult hitResult =
        ProjectileUtil.getEntityHitResult(
            player.level(),
            player, // The entity performing the pick, to be ignored
            eyePos,
            endPos,
            searchBox,
            (entity) ->
                !entity.isSpectator() && entity.isPickable() && entity instanceof LivingEntity);

    if (hitResult != null && hitResult.getEntity() != null) {
      LivingEntity target = (LivingEntity) hitResult.getEntity();
      double distanceSqr = player.distanceToSqr(target);
      // Final check on distance and ensuring it's not the player unless they are the only target.
      if (distanceSqr <= MAX_TARGET_RANGE_SQR) {
        return target;
      }
    }

    // As a fallback, or if no entity is hit, the script will target the player themselves.
    // This is handled in GuScriptExecutor.triggerKeybind if the target is null.
    return null;
  }

  static final double MAX_TARGET_RANGE = 20.0;
  static final double MAX_TARGET_RANGE_SQR = MAX_TARGET_RANGE * MAX_TARGET_RANGE;
}
