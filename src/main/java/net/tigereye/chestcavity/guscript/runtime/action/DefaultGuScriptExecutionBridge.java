package net.tigereye.chestcavity.guscript.runtime.action;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptExecutionBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guscript.fx.FxEventParameters;
import net.tigereye.chestcavity.guscript.network.packets.FxEventPayload;
import net.tigereye.chestcavity.util.ProjectileParameterReceiver;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default bridge implementation delegating to GuzhenrenResourceBridge and vanilla helpers.
 */
public final class DefaultGuScriptExecutionBridge implements GuScriptExecutionBridge {

    private final Player performer;
    private final LivingEntity target;
    private final int rootIndex;
    private static final AtomicLong PROJECTILE_SEQUENCE = new AtomicLong();

    public DefaultGuScriptExecutionBridge(Player performer, LivingEntity target) {
        this(performer, target, 0);
    }

    public DefaultGuScriptExecutionBridge(Player performer, LivingEntity target, int rootIndex) {
        this.performer = performer;
        this.target = target;
        this.rootIndex = rootIndex;
    }

    public static DefaultGuScriptExecutionBridge forPlayer(Player performer) {
        return new DefaultGuScriptExecutionBridge(performer, performer, 0);
    }

    public static DefaultGuScriptExecutionBridge forPlayer(Player performer, int rootIndex) {
        return new DefaultGuScriptExecutionBridge(performer, performer, rootIndex);
    }

    @Override
    public void consumeZhenyuan(int amount) {
        if (amount <= 0) {
            return;
        }
        Optional<GuzhenrenResourceBridge.ResourceHandle> handle = GuzhenrenResourceBridge.open(performer);
        if (handle.isEmpty()) {
            ChestCavity.LOGGER.debug("[GuScript] No Guzhenren attachment for {}, skipping zhenyuan cost", performer.getGameProfile().getName());
            return;
        }
        boolean success = handle.map(h -> h.consumeScaledZhenyuan(amount).isPresent()).orElse(false);
        if (!success) {
            ChestCavity.LOGGER.debug("[GuScript] Failed to consume {} zhenyuan from {}", amount, performer.getGameProfile().getName());
        }
    }

    @Override
    public void consumeHealth(int amount) {
        if (amount <= 0) {
            return;
        }
        LivingEntity victim = this.target != null ? this.target : this.performer;
        DamageSource source = performer.damageSources().magic();
        victim.hurt(source, amount);
    }

    @Override
    public void emitProjectile(String projectileId, double damage, @Nullable CompoundTag parameters) {
        if (projectileId == null || projectileId.isBlank()) {
            return;
        }
        Level level = performer.level();
        if (level.isClientSide()) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(projectileId);
        if (id == null) {
            ChestCavity.LOGGER.warn("[GuScript] Invalid projectile id: {}", projectileId);
            return;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null) {
            ChestCavity.LOGGER.warn("[GuScript] Unknown projectile entity: {}", projectileId);
            return;
        }
        Entity entity;
        try {
            entity = type.create(level);
        } catch (Exception e) {
            ChestCavity.LOGGER.warn("[GuScript] Failed to instantiate projectile {}", projectileId, e);
            return;
        }
        if (entity == null) {
            ChestCavity.LOGGER.warn("[GuScript] Entity type {} returned null instance", projectileId);
            return;
        }

        Vec3 look = performer.getLookAngle();
        float yawOffset = (float) (0.05F * rootIndex);
        double cos = Math.cos(yawOffset);
        double sin = Math.sin(yawOffset);
        Vec3 adjustedLook = new Vec3(
                look.x * cos - look.z * sin,
                look.y,
                look.x * sin + look.z * cos
        ).normalize();

        Vec3 spawn = new Vec3(performer.getX(), performer.getEyeY() - 0.2, performer.getZ());
        double lateralOffset = 0.05D * rootIndex;
        if (Math.abs(lateralOffset) > 1.0E-4) {
            Vec3 lateral = new Vec3(-adjustedLook.z, 0.0, adjustedLook.x);
            if (lateral.lengthSqr() > 1.0E-6) {
                lateral = lateral.normalize().scale(lateralOffset);
                spawn = spawn.add(lateral);
            }
        }

        float yawDegrees = performer.getYRot() + (float) Math.toDegrees(yawOffset);
        entity.moveTo(spawn.x, spawn.y, spawn.z, yawDegrees, performer.getXRot());

        if (entity instanceof Projectile projectile) {
            projectile.setOwner(performer);
            projectile.shoot(adjustedLook.x, adjustedLook.y, adjustedLook.z, 1.5F, 0.0F);
            if (projectile instanceof AbstractArrow arrow) {
                arrow.setBaseDamage(damage);
            }
        } else {
            entity.setDeltaMovement(adjustedLook.scale(1.5));
        }

        CompoundTag appliedParameters = parameters != null ? parameters.copy() : null;
        if (entity instanceof ProjectileParameterReceiver receiver) {
            CompoundTag payload = appliedParameters == null ? new CompoundTag() : appliedParameters;
            receiver.applyProjectileParameters(performer, payload, damage);
            appliedParameters = payload;
        }

        long sequence = PROJECTILE_SEQUENCE.incrementAndGet();
        if (ChestCavity.LOGGER.isInfoEnabled()) {
            String parameterSummary = appliedParameters == null || appliedParameters.isEmpty()
                    ? ""
                    : ", params=" + appliedParameters;
            ChestCavity.LOGGER.info(
                    "[GuScript] Projectile #{} emitted for root {}: id={} damage={} spawn=({},{},{}){}",
                    sequence,
                    rootIndex,
                    id,
                    damage,
                    String.format("%.3f", spawn.x),
                    String.format("%.3f", spawn.y),
                    String.format("%.3f", spawn.z),
                    parameterSummary
            );
        }
        level.addFreshEntity(entity);
    }

    @Override
    public void playFx(ResourceLocation fxId, FxEventParameters parameters) {
        if (fxId == null) {
            return;
        }
        if (!(performer instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ServerLevel level = serverPlayer.serverLevel();
        FxEventParameters actual = parameters == null ? FxEventParameters.DEFAULT : parameters;
        Vec3 origin = center(performer).add(actual.originOffset());
        Vec3 look = performer.getLookAngle();
        Vec3 fallbackDirection = look;
        Vec3 targetPosition = null;
        int targetId = -1;
        if (target != null) {
            targetPosition = center(target).add(actual.targetOffset());
            targetId = target.getId();
        }
        FxEventPayload payload = new FxEventPayload(
                fxId,
                origin.x,
                origin.y,
                origin.z,
                (float) fallbackDirection.x,
                (float) fallbackDirection.y,
                (float) fallbackDirection.z,
                (float) look.x,
                (float) look.y,
                (float) look.z,
                actual.intensity(),
                targetPosition != null,
                targetPosition != null ? targetPosition.x : 0.0D,
                targetPosition != null ? targetPosition.y : 0.0D,
                targetPosition != null ? targetPosition.z : 0.0D,
                serverPlayer.getId(),
                targetId
        );
        broadcastFx(level, serverPlayer, origin, payload);
    }

    private static Vec3 center(LivingEntity entity) {
        return new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ());
    }

    private static void broadcastFx(ServerLevel level, ServerPlayer source, Vec3 origin, FxEventPayload payload) {
        double radius = 64.0D;
        double radiusSq = radius * radius;
        for (ServerPlayer viewer : level.players()) {
            if (viewer == source || viewer.distanceToSqr(origin) <= radiusSq) {
                viewer.connection.send(payload);
            }
        }
    }
}
