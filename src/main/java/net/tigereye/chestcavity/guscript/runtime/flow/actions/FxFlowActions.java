package net.tigereye.chestcavity.guscript.runtime.flow.actions;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import com.mojang.math.Axis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.guscript.ability.AbilityFxDispatcher;
import net.tigereye.chestcavity.guscript.fx.FxEventParameters;
import net.tigereye.chestcavity.guscript.fx.gecko.GeckoFxDispatcher;
import net.tigereye.chestcavity.guscript.network.packets.GeckoFxEventPayload;
import net.tigereye.chestcavity.guscript.runtime.action.DefaultGuScriptExecutionBridge;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowEdgeAction;
import net.tigereye.chestcavity.guscript.runtime.flow.fx.GeckoFxAnchor;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 粒子、Gecko FX 等特效相关 Action。
 */
final class FxFlowActions {

    private FxFlowActions() {
    }

    static FlowEdgeAction emitFxOnAllies(ResourceLocation fxId, double allyRadius, float intensity) {
        double r = Math.max(0.0, allyRadius);
        float clamped = intensity <= 0.0F ? 1.0F : intensity;
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (!(performer instanceof ServerPlayer serverPlayer) || fxId == null) {
                    return;
                }
                ServerLevel server = serverPlayer.serverLevel();
                AABB box = new AABB(performer.blockPosition()).inflate(r);
                List<LivingEntity> allies = server.getEntitiesOfClass(LivingEntity.class, box, e -> FlowActionUtils.isAlly(performer, e));
                for (LivingEntity ally : allies) {
                    AbilityFxDispatcher.play(server,
                            fxId,
                            new Vec3(ally.getX(), ally.getY() + ally.getBbHeight() * 0.5D, ally.getZ()),
                            ally.getLookAngle(), ally.getLookAngle(),
                            serverPlayer,
                            ally,
                            clamped);
                }
            }

            @Override
            public String describe() { return "emit_fx_on_allies(fx=" + fxId + ", r=" + r + ")"; }
        };
    }

    static FlowEdgeAction emitGeckoOnAllies(ResourceLocation fxId, double allyRadius, Vec3 offset, float scale, int tint, float alpha, boolean loop, int duration) {
        if (fxId == null) {
            return FlowActionUtils.describe(() -> "emit_gecko_on_allies(nop)");
        }
        double r = Math.max(0.0, allyRadius);
        Vec3 safeOffset = offset == null ? Vec3.ZERO : offset;
        float sanitizedAlpha = Mth.clamp(alpha, 0.0F, 1.0F);
        float sanitizedScale = scale <= 0.0F ? 1.0F : scale;
        int sanitizedDuration = Math.max(1, duration);
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (!(performer instanceof ServerPlayer serverPlayer)) {
                    return;
                }
                ServerLevel server = serverPlayer.serverLevel();
                AABB box = new AABB(performer.blockPosition()).inflate(r);
                List<LivingEntity> allies = server.getEntitiesOfClass(LivingEntity.class, box, e -> FlowActionUtils.isAlly(performer, e));
                for (LivingEntity ally : allies) {
                    Vec3 base = ally.position();
                    Vec3 origin = base.add(safeOffset);
                    UUID eventId = computeFxEventId(fxId, ally, loop);
                    GeckoFxEventPayload payload = new GeckoFxEventPayload(
                            fxId,
                            GeckoFxAnchor.ENTITY,
                            ally.getId(),
                            ally.getUUID(),
                            base.x,
                            base.y,
                            base.z,
                            safeOffset.x,
                            safeOffset.y,
                            safeOffset.z,
                            0.0D,
                            0.0D,
                            0.0D,
                            ally.getYRot(),
                            ally.getXRot(),
                            0.0F,
                            sanitizedScale,
                            tint,
                            sanitizedAlpha,
                            loop,
                            sanitizedDuration,
                            null,
                            null,
                            null,
                            eventId
                    );
                    GeckoFxDispatcher.emit(server, origin, payload);
                }
            }

            @Override
            public String describe() { return "emit_gecko_on_allies(fx=" + fxId + ")"; }
        };
    }

    static FlowEdgeAction emitFx(String fxId, float baseIntensity, String variableName, double defaultScale) {
        ResourceLocation fx = fxId == null ? null : ResourceLocation.tryParse(fxId);
        if (fx == null) {
            return FlowActionUtils.describe(() -> "emit_fx(nop)");
        }
        float baseline = Math.max(0.0F, baseIntensity);
        double fallback = defaultScale;
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null) {
                    return;
                }
                DefaultGuScriptExecutionBridge bridge = DefaultGuScriptExecutionBridge.forPlayer(performer);
                double scale = fallback;
                if (controller != null && variableName != null) {
                    scale = controller.getDouble(variableName, fallback);
                }
                float intensity = (float) Mth.clamp(baseline * scale, 0.0D, 16.0D);
                bridge.playFx(fx, new FxEventParameters(Vec3.ZERO, Vec3.ZERO, intensity));
            }

            @Override
            public String describe() {
                return "emit_fx(" + fx + ")";
            }
        };
    }

    static FlowEdgeAction emitFxConditional(ResourceLocation fxId, String variableName, double skipValue, float baseIntensity) {
        if (fxId == null) {
            return FlowActionUtils.describe(() -> "emit_fx_conditional(nop)");
        }
        float sanitizedIntensity = baseIntensity <= 0.0F ? 1.0F : baseIntensity;
        double skip = Double.isNaN(skipValue) ? Double.NaN : skipValue;
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null) {
                    return;
                }
                if (controller != null && variableName != null) {
                    double value = controller.getDouble(variableName, Double.NaN);
                    if (Double.isFinite(skip) && Double.isFinite(value) && Math.abs(value - skip) < 1.0E-4D) {
                        return;
                    }
                }
                DefaultGuScriptExecutionBridge bridge = DefaultGuScriptExecutionBridge.forPlayer(performer);
                bridge.playFx(fxId, new FxEventParameters(Vec3.ZERO, Vec3.ZERO, sanitizedIntensity));
            }

            @Override
            public String describe() {
                return "emit_fx_conditional(" + fxId + ")";
            }
        };
    }

    static FlowEdgeAction emitGecko(FlowActions.GeckoFxParameters parameters) {
        if (parameters == null || parameters.fxId() == null) {
            return FlowActionUtils.describe(() -> "emit_gecko(nop)");
        }
        Vec3 offset = parameters.offset() == null ? Vec3.ZERO : parameters.offset();
        Vec3 relativeOffset = parameters.relativeOffset() == null ? Vec3.ZERO : parameters.relativeOffset();
        Vec3 worldPosition = parameters.worldPosition();
        float alpha = Mth.clamp(parameters.alpha(), 0.0F, 1.0F);
        float scale = parameters.scale() <= 0.0F ? 1.0F : parameters.scale();
        boolean loop = parameters.loop();
        int duration = Math.max(1, parameters.durationTicks());
        GeckoFxAnchor anchor = parameters.anchor() == null ? GeckoFxAnchor.PERFORMER : parameters.anchor();

        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (!(performer instanceof ServerPlayer serverPlayer)) {
                    return;
                }
                ServerLevel level = serverPlayer.serverLevel();
                if (level.isClientSide()) {
                    return;
                }

                Entity attachedEntity = null;
                Vec3 basePosition;
                switch (anchor) {
                    case TARGET -> {
                        if (target == null) {
                            attachedEntity = serverPlayer;
                        } else {
                            attachedEntity = target;
                        }
                    }
                    case ENTITY -> {
                        if (controller == null || parameters.entityIdVariable() == null) {
                            return;
                        }
                        long stored = controller.getLong(parameters.entityIdVariable(), -1L);
                        if (stored < 0L) {
                            return;
                        }
                        Entity resolved = level.getEntity((int) stored);
                        if (resolved == null) {
                            return;
                        }
                        attachedEntity = resolved;
                    }
                    case WORLD -> {
                        basePosition = worldPosition != null ? worldPosition : serverPlayer.position();
                        float yawInput = parameters.yaw() != null ? parameters.yaw() : serverPlayer.getYRot();
                        float pitchInput = parameters.pitch() != null ? parameters.pitch() : serverPlayer.getXRot();
                        float rollInput = parameters.roll() != null ? parameters.roll() : 0.0F;
                        float payloadYaw = -yawInput;
                        float payloadPitch = -pitchInput;
                        float payloadRoll = -rollInput;
                        Vec3 rotatedRelative = rotateRelativeOffset(relativeOffset, payloadYaw, payloadPitch, payloadRoll);
                        Vec3 originOffset = offset.add(rotatedRelative);
                        Vec3 origin = basePosition.add(originOffset);
                        UUID eventId = loop
                                ? UUID.nameUUIDFromBytes((parameters.fxId() + "|" + basePosition.x + "," + basePosition.y + "," + basePosition.z)
                                .getBytes(StandardCharsets.UTF_8))
                                : UUID.randomUUID();
                        GeckoFxEventPayload payload = new GeckoFxEventPayload(
                                parameters.fxId(),
                                anchor,
                                -1,
                                null,
                                basePosition.x,
                                basePosition.y,
                                basePosition.z,
                                offset.x,
                                offset.y,
                                offset.z,
                                relativeOffset.x,
                                relativeOffset.y,
                                relativeOffset.z,
                                payloadYaw,
                                payloadPitch,
                                payloadRoll,

                                scale,
                                parameters.tint(),
                                alpha,
                                loop,
                                duration,
                                null,
                                null,
                                null,
                                eventId
                        );
                        GeckoFxDispatcher.emit(level, origin, payload);
                        return;
                    }
                    case PERFORMER -> attachedEntity = serverPlayer;
                }

                if (attachedEntity == null) {
                    return;
                }
                basePosition = attachedEntity.position();

                float yawInput = parameters.yaw() != null ? parameters.yaw() : attachedEntity.getYRot();
                float pitchInput = parameters.pitch() != null ? parameters.pitch() : attachedEntity.getXRot();
                float rollInput = parameters.roll() != null ? parameters.roll() : 0.0F;

                float payloadYaw = -yawInput;
                float payloadPitch = -pitchInput;
                float payloadRoll = -rollInput;

                Vec3 rotatedRelative = rotateRelativeOffset(relativeOffset, payloadYaw, payloadPitch, payloadRoll);
                Vec3 originOffset = offset.add(rotatedRelative);
                Vec3 origin = basePosition.add(originOffset);

                UUID eventId = computeFxEventId(parameters.fxId(), attachedEntity, loop);

                GeckoFxEventPayload payload = new GeckoFxEventPayload(
                        parameters.fxId(),
                        anchor,
                        attachedEntity.getId(),
                        attachedEntity.getUUID(),
                        basePosition.x,
                        basePosition.y,
                        basePosition.z,
                        offset.x,
                        offset.y,
                        offset.z,
                        relativeOffset.x,
                        relativeOffset.y,
                        relativeOffset.z,
                        payloadYaw,
                        payloadPitch,
                        payloadRoll,
                        scale,
                        parameters.tint(),
                        alpha,
                        loop,
                        duration,
                        null,
                        null,
                        null,
                        eventId
                );

                GeckoFxDispatcher.emit(level, origin, payload);
            }

            @Override
            public String describe() {
                return "emit_gecko(" + parameters.fxId() + ", anchor=" + anchor.serializedName() + ")";
            }
        };
    }

    static UUID computeFxEventId(ResourceLocation fxId, Entity anchor, boolean loop) {
        if (!loop || fxId == null || anchor == null) {
            return UUID.randomUUID();
        }
        String seed = fxId + "|" + anchor.getUUID();
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    static Vec3 rotateRelativeOffset(Vec3 relativeOffset, float yawDegrees, float pitchDegrees, float rollDegrees) {
        if (relativeOffset == null || relativeOffset.equals(Vec3.ZERO)) {
            return Vec3.ZERO;
        }
        boolean hasYaw = yawDegrees != 0.0F;
        boolean hasPitch = pitchDegrees != 0.0F;
        boolean hasRoll = rollDegrees != 0.0F;
        if (!hasYaw && !hasPitch && !hasRoll) {
            return relativeOffset;
        }

        Quaternionf rotation = new Quaternionf();
        if (hasYaw) {
            rotation.mul(Axis.YP.rotationDegrees(yawDegrees));
        }
        if (hasPitch) {
            rotation.mul(Axis.XP.rotationDegrees(pitchDegrees));
        }
        if (hasRoll) {
            rotation.mul(Axis.ZP.rotationDegrees(rollDegrees));
        }

        Vector3f working = new Vector3f((float) relativeOffset.x, (float) relativeOffset.y, (float) relativeOffset.z);
        rotation.transform(working);
        return new Vec3(working.x(), working.y(), working.z());
    }
}
