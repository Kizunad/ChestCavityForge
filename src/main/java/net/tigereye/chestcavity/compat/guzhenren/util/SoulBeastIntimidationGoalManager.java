package net.tigereye.chestcavity.compat.guzhenren.util;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import net.tigereye.chestcavity.mob_effect.SoulBeastIntimidatedEffect;

/**
 * Handles adding and removing flee goals for entities intimidated by soul beasts.
 */
public final class SoulBeastIntimidationGoalManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Mob, IntimidatedFleeGoal> GOALS = new WeakHashMap<>();

    private SoulBeastIntimidationGoalManager() {}

    public static void ensureFleeGoal(Mob mob) {
        if (mob == null || mob.level().isClientSide()) {
            return;
        }
        if (!(mob instanceof PathfinderMob pathfinder)) {
            LOGGER.info("[Intimidation] {} is not PathfinderMob, skipping flee goal", describe(mob));
            return;
        }
        IntimidatedFleeGoal goal = GOALS.get(mob);
        if (goal == null) {
            goal = new IntimidatedFleeGoal(pathfinder);
            GOALS.put(mob, goal);
            mob.goalSelector.addGoal(1, goal);
            LOGGER.info("[Intimidation] Added flee goal to {}", describe(mob));
        }
        mob.getNavigation().stop();
        mob.setTarget(null);
    }

    public static void clearFleeGoal(Mob mob) {
        if (mob == null) {
            return;
        }
        IntimidatedFleeGoal goal = GOALS.remove(mob);
        if (goal != null) {
            mob.goalSelector.removeGoal(goal);
            LOGGER.info("[Intimidation] Removed flee goal from {}", describe(mob));
        }
    }

    static boolean hasIntimidator(Mob mob) {
        return SoulBeastIntimidatedEffect.getIntimidator(mob).isPresent();
    }

    static boolean isCurrentIntimidator(Mob mob, LivingEntity candidate) {
        if (mob == null || candidate == null) {
            return false;
        }
        return SoulBeastIntimidatedEffect.getIntimidator(mob)
                .map(uuid -> uuid.equals(candidate.getUUID()))
                .orElse(false);
    }

    private static final class IntimidatedFleeGoal extends Goal {
        private final PathfinderMob mob;
        private final double walkSpeed;
        private final double sprintSpeed;
        private final double maxDistanceSq;
        private Path path;
        private LivingEntity intimidator;

        private IntimidatedFleeGoal(PathfinderMob mob) {
            this.mob = mob;
            this.walkSpeed = 1.25D;
            this.sprintSpeed = 1.5D;
            this.maxDistanceSq = 12.0D * 12.0D;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            this.intimidator = resolveIntimidator();
            if (this.intimidator == null) {
                return false;
            }
            Vec3 awayPos = DefaultRandomPos.getPosAway(this.mob, 16, 7, this.intimidator.position());
            if (awayPos == null) {
                LOGGER.info("[Intimidation] {} failed to find escape position", describe(mob));
                return false;
            }
            if (this.intimidator.distanceToSqr(awayPos.x, awayPos.y, awayPos.z) <= this.intimidator.distanceToSqr(this.mob)) {
                LOGGER.info("[Intimidation] {} escape target not farther than intimidator", describe(mob));
                return false;
            }
            this.path = this.mob.getNavigation().createPath(awayPos.x, awayPos.y, awayPos.z, 0);
            if (this.path == null) {
                LOGGER.info("[Intimidation] {} path creation failed", describe(mob));
                return false;
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.path != null && !this.mob.getNavigation().isDone() && this.intimidator != null
                    && this.intimidator.isAlive() && SoulBeastIntimidationGoalManager.hasIntimidator(this.mob);
        }

        @Override
        public void start() {
            LOGGER.info("[Intimidation] {} starting flee from {}", describe(mob), describe(this.intimidator));
            this.mob.getNavigation().moveTo(this.path, this.walkSpeed);
        }

        @Override
        public void stop() {
            LOGGER.info("[Intimidation] {} stopping flee", describe(mob));
            this.intimidator = null;
            this.mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (this.intimidator == null) {
                return;
            }
            double distanceSq = this.mob.distanceToSqr(this.intimidator);
            this.mob.getNavigation().setSpeedModifier(distanceSq < this.maxDistanceSq ? this.sprintSpeed : this.walkSpeed);
        }

        private LivingEntity resolveIntimidator() {
            return SoulBeastIntimidatedEffect.getIntimidator(this.mob)
                    .map(uuid -> this.mob.level().getPlayerByUUID(uuid))
                    .filter(player -> player != null && player.isAlive())
                    .orElse(null);
        }
    }

    private static String describe(LivingEntity entity) {
        return entity == null ? "<null>" : entity.getName().getString();
    }
}
