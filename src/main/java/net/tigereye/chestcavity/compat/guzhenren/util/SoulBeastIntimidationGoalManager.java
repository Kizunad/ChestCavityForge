package net.tigereye.chestcavity.compat.guzhenren.util;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;

import net.tigereye.chestcavity.mob_effect.SoulBeastIntimidatedEffect;

/**
 * Handles adding and removing flee goals for entities intimidated by soul beasts.
 */
public final class SoulBeastIntimidationGoalManager {

    private static final Map<Mob, IntimidatedFleeGoal> GOALS = new WeakHashMap<>();

    private SoulBeastIntimidationGoalManager() {}

    public static void ensureFleeGoal(Mob mob) {
        if (mob == null || mob.level().isClientSide()) {
            return;
        }
        if (!(mob instanceof PathfinderMob pathfinder)) {
            return;
        }
        IntimidatedFleeGoal goal = GOALS.get(mob);
        if (goal == null) {
            goal = new IntimidatedFleeGoal(pathfinder);
            GOALS.put(mob, goal);
            mob.goalSelector.addGoal(1, goal);
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

    private static final class IntimidatedFleeGoal extends AvoidEntityGoal<LivingEntity> {

        private final PathfinderMob mob;

        private IntimidatedFleeGoal(PathfinderMob mob) {
            super(mob, LivingEntity.class, 12.0F, 1.25D, 1.5D,
                    candidate -> SoulBeastIntimidationGoalManager.isCurrentIntimidator(mob, candidate));
            this.mob = mob;
        }

        @Override
        public boolean canUse() {
            return SoulBeastIntimidationGoalManager.hasIntimidator(this.mob) && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return SoulBeastIntimidationGoalManager.hasIntimidator(this.mob) && super.canContinueToUse();
        }
    }
}
