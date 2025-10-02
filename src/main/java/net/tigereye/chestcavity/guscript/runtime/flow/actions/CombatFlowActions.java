package net.tigereye.chestcavity.guscript.runtime.flow.actions;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowEdgeAction;

/**
 * 战斗相关的群体效果与投射物控制行为。
 */
final class CombatFlowActions {

    private CombatFlowActions() {
    }

    static FlowEdgeAction explode(float power) {
        float sanitized = power <= 0.0F ? 1.0F : power;
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null) {
                    return;
                }
                Level level = performer.level();
                if (level.isClientSide()) {
                    return;
                }
                level.explode(performer, performer.getX(), performer.getY(), performer.getZ(), sanitized, Level.ExplosionInteraction.TNT);
            }

            @Override
            public String describe() { return "explode(" + sanitized + ")"; }
        };
    }

    static FlowEdgeAction areaEffect(ResourceLocation effectId, int duration, int amplifier, double radius, String radiusVariable, boolean hostilesOnly, boolean includeSelf, boolean showParticles, boolean showIcon) {
        int actualDuration = Math.max(1, duration);
        double defaultRadius = Math.max(0.5D, radius);
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null || effectId == null) {
                    return;
                }
                var holderOpt = BuiltInRegistries.MOB_EFFECT.getHolder(effectId);
                if (holderOpt.isEmpty()) {
                    ChestCavity.LOGGER.warn("[Flow] Unknown effect {} in area_effect", effectId);
                    return;
                }
                if (!(performer.level() instanceof ServerLevel level)) {
                    return;
                }
                double resolvedRadius = defaultRadius;
                if (controller != null && radiusVariable != null) {
                    resolvedRadius = Math.max(0.5D, controller.getDouble(radiusVariable, defaultRadius));
                }
                Vec3 origin = performer.position();
                AABB box = new AABB(origin, origin).inflate(resolvedRadius);
                var effect = holderOpt.get();
                List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box, entity -> entity.isAlive() && (includeSelf || entity != performer));
                for (LivingEntity entity : entities) {
                    if (hostilesOnly && !(entity instanceof Enemy)) {
                        continue;
                    }
                    entity.addEffect(new MobEffectInstance(effect, actualDuration, Math.max(0, amplifier), false, showParticles, showIcon));
                }
            }

            @Override
            public String describe() {
                return "area_effect(" + effectId + ")";
            }
        };
    }

    static FlowEdgeAction dampenProjectiles(double radius, String radiusVariable, double factor, int capPerTick) {
        double defaultRadius = Math.max(0.5D, radius);
        double actualFactor = Math.max(0.0D, factor);
        int cap = Math.max(1, capPerTick);
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null || !(performer.level() instanceof ServerLevel level)) {
                    return;
                }
                double resolvedRadius = defaultRadius;
                if (controller != null && radiusVariable != null) {
                    resolvedRadius = Math.max(0.5D, controller.getDouble(radiusVariable, defaultRadius));
                }
                Vec3 origin = performer.position();
                AABB box = new AABB(origin, origin).inflate(resolvedRadius);
                int remaining = cap;
                for (AbstractArrow arrow : level.getEntitiesOfClass(AbstractArrow.class, box, entity -> entity.isAlive())) {
                    dampenVelocity(arrow);
                    if (--remaining <= 0) {
                        return;
                    }
                }
                if (remaining <= 0) {
                    return;
                }
                for (ThrowableProjectile projectile : level.getEntitiesOfClass(ThrowableProjectile.class, box, entity -> entity.isAlive())) {
                    dampenVelocity(projectile);
                    if (--remaining <= 0) {
                        return;
                    }
                }
            }

            private void dampenVelocity(AbstractArrow arrow) {
                Vec3 motion = arrow.getDeltaMovement();
                Vec3 scaled = motion.scale(actualFactor);
                arrow.setDeltaMovement(scaled);
                arrow.hasImpulse = true;
            }

            private void dampenVelocity(ThrowableProjectile projectile) {
                Vec3 motion = projectile.getDeltaMovement();
                projectile.setDeltaMovement(motion.scale(actualFactor));
            }

            @Override
            public String describe() {
                return "dampen_projectiles(radius=" + defaultRadius + ", factor=" + actualFactor + ")";
            }
        };
    }

    static FlowEdgeAction highlightHostiles(double radius, String radiusVariable, int durationTicks) {
        double defaultRadius = Math.max(0.5D, radius);
        int duration = Math.max(1, durationTicks);
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null || !(performer.level() instanceof ServerLevel level)) {
                    return;
                }
                double resolvedRadius = defaultRadius;
                if (controller != null && radiusVariable != null) {
                    resolvedRadius = Math.max(0.5D, controller.getDouble(radiusVariable, defaultRadius));
                }
                Vec3 origin = performer.position();
                AABB box = new AABB(origin, origin).inflate(resolvedRadius);
                List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box, entity -> entity instanceof Enemy && entity.isAlive());
                for (LivingEntity hostile : entities) {
                    hostile.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false, false));
                }
            }

            @Override
            public String describe() {
                return "highlight_hostiles(radius=" + defaultRadius + ")";
            }
        };
    }
}
