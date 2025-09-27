package net.tigereye.chestcavity.guscript.runtime.action;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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

import java.util.Optional;

/**
 * Default bridge implementation delegating to GuzhenrenResourceBridge and vanilla helpers.
 */
public final class DefaultGuScriptExecutionBridge implements GuScriptExecutionBridge {

    private final Player performer;
    private final LivingEntity target;

    public DefaultGuScriptExecutionBridge(Player performer, LivingEntity target) {
        this.performer = performer;
        this.target = target;
    }

    public static DefaultGuScriptExecutionBridge forPlayer(Player performer) {
        return new DefaultGuScriptExecutionBridge(performer, performer);
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
    public void emitProjectile(String projectileId, double damage) {
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
        entity.moveTo(performer.getX(), performer.getEyeY() - 0.2, performer.getZ(), performer.getYRot(), performer.getXRot());

        if (entity instanceof Projectile projectile) {
            projectile.setOwner(performer);
            projectile.shoot(look.x, look.y, look.z, 1.5F, 0.0F);
            if (projectile instanceof AbstractArrow arrow) {
                arrow.setBaseDamage(damage);
            }
        } else {
            entity.setDeltaMovement(look.scale(1.5));
        }

        level.addFreshEntity(entity);
    }
}
