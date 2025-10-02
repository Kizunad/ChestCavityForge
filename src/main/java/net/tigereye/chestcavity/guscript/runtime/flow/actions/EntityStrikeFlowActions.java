package net.tigereye.chestcavity.guscript.runtime.flow.actions;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowEdgeAction;

/**
 * Entity Strike 专用的 Action 与工具方法。
 */
final class EntityStrikeFlowActions {

    private EntityStrikeFlowActions() {
    }

    static FlowEdgeAction entityStrike(
            double allyRadius,
            Vec3 relativeOffset,
            float yawOffset,
            double dashDistance,
            String targetSelector,
            ResourceLocation soundId,
            String entityIdVariable
    ) {
        double radius = Math.max(0.0D, allyRadius);
        Vec3 offset = relativeOffset == null ? Vec3.ZERO : relativeOffset;
        float additionalYaw = yawOffset;
        double dash = Math.max(0.0D, dashDistance);
        StrikeTargetSelector selector = StrikeTargetSelector.from(targetSelector);
        ResourceLocation resolvedSound = soundId == null
                ? ResourceLocation.fromNamespaceAndPath("minecraft", "entity.polar_bear.attack")
                : soundId;
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null) {
                    return;
                }
                Level level = performer.level();
                if (!(level instanceof ServerLevel server)) {
                    return;
                }

                LivingEntity striker = resolveStrikeSource(server, performer, controller, entityIdVariable, radius);
                if (striker == null || !striker.isAlive()) {
                    return;
                }

                LivingEntity strikeTarget = selector.select(performer, target);
                if (strikeTarget != null && (!strikeTarget.isAlive() || strikeTarget == striker)) {
                    strikeTarget = null;
                }

                float finalYaw = performer.getYRot() + additionalYaw;
                Vec3 finalPosition = computeEntityStrikePosition(
                        performer.position(),
                        performer.getYRot(),
                        offset,
                        additionalYaw,
                        dash
                );

                float pitch = striker.getXRot();
                striker.setDeltaMovement(Vec3.ZERO);
                striker.moveTo(finalPosition.x, finalPosition.y, finalPosition.z, finalYaw, pitch);
                striker.setYRot(finalYaw);
                striker.setYBodyRot(finalYaw);
                striker.setYHeadRot(finalYaw);

                if (striker instanceof Mob mob) {
                    mob.setTarget(strikeTarget);
                }

                if (strikeTarget != null) {
                    striker.swing(InteractionHand.MAIN_HAND, true);
                    striker.doHurtTarget(strikeTarget);
                }

                playStrikeSound(server, striker, resolvedSound);
            }

            @Override
            public String describe() {
                return "entity_strike(entityIdVariable=" + entityIdVariable + ")";
            }
        };
    }

    static Vec3 rotateOffsetByYaw(Vec3 offset, float yawDegrees) {
        if (offset == null || offset.equals(Vec3.ZERO)) {
            return Vec3.ZERO;
        }
        double radians = Math.toRadians(-yawDegrees);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        double x = offset.x * cos - offset.z * sin;
        double z = offset.x * sin + offset.z * cos;
        return new Vec3(x, offset.y, z);
    }

    static Vec3 computeEntityStrikePosition(
            Vec3 performerPosition,
            float performerYaw,
            Vec3 offset,
            float yawOffset,
            double dashDistance
    ) {
        Vec3 base = performerPosition.add(rotateOffsetByYaw(offset, performerYaw));
        if (dashDistance <= 0.0D) {
            return base;
        }
        Vec3 dashVector = Vec3.directionFromRotation(0.0F, performerYaw + yawOffset).scale(dashDistance);
        return base.add(dashVector);
    }

    private static LivingEntity resolveStrikeSource(
            ServerLevel server,
            Player performer,
            FlowController controller,
            String entityIdVariable,
            double allyRadius
    ) {
        LivingEntity resolved = null;
        if (controller != null && entityIdVariable != null && !entityIdVariable.isBlank()) {
            long stored = controller.getLong(entityIdVariable, -1L);
            if (stored >= 0L) {
                Entity entity = server.getEntity((int) stored);
                if (entity instanceof LivingEntity living) {
                    resolved = living;
                }
            }
        }
        if (resolved != null) {
            return resolved;
        }
        if (allyRadius <= 0.0D) {
            return null;
        }
        AABB box = new AABB(performer.blockPosition()).inflate(allyRadius);
        List<LivingEntity> allies = server.getEntitiesOfClass(LivingEntity.class, box, entity -> FlowActionUtils.isAlly(performer, entity));
        if (allies.isEmpty()) {
            return null;
        }
        return allies.stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(performer)))
                .orElse(null);
    }

    private static void playStrikeSound(ServerLevel level, LivingEntity striker, ResourceLocation soundId) {
        if (level == null || striker == null || soundId == null) {
            return;
        }
        Optional<Holder.Reference<SoundEvent>> holder = BuiltInRegistries.SOUND_EVENT.getHolder(soundId);
        if (holder.isEmpty()) {
            return;
        }
        SoundEvent soundEvent = holder.get().value();
        level.playSound(
                null,
                striker.getX(),
                striker.getY(),
                striker.getZ(),
                soundEvent,
                striker.getSoundSource(),
                1.0F,
                1.0F
        );
    }

    private enum StrikeTargetSelector {
        FLOW_TARGET,
        PERFORMER;

        static StrikeTargetSelector from(String raw) {
            if (raw == null || raw.isBlank()) {
                return FLOW_TARGET;
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "performer", "self" -> PERFORMER;
                default -> FLOW_TARGET;
            };
        }

        LivingEntity select(Player performer, LivingEntity flowTarget) {
            return switch (this) {
                case FLOW_TARGET -> flowTarget;
                case PERFORMER -> performer;
            };
        }
    }
}
