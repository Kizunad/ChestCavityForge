package net.tigereye.chestcavity.soul.navigation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/**
 * Utility helpers to pre-plan navigation goals towards surrounding entities.
 */
public final class SoulGoalPlanner {

    private SoulGoalPlanner() {}

    /**
     * Immutable navigation descriptor for driving {@link SoulNavigationMirror}.
     *
     * @param target       entity we intend to approach
     * @param goal         world-space target vector (usually the entity position or an offset ring)
     * @param speed        navigation speed modifier
     * @param stopDistance acceptable distance from {@code goal} to stop trying to move closer
     */
    public record NavigationGoal(LivingEntity target, Vec3 goal, double speed, double stopDistance) {}

    /**
     * Builds an ordered list of {@link NavigationGoal}s for all living entities within {@code range} of {@code origin}.
     * <p>
     * The returned list is ordered via a nearest-neighbour sweep: we start from {@code origin}, always selecting the
     * closest remaining candidate to the previous goal position. This keeps adjacent entries spatially close, which is
     * helpful for path planning heuristics that iteratively try nearby goals.
     *
     * @param origin        entity whose surroundings should be scanned
     * @param range         search radius in blocks (must be positive)
     * @param filter        optional predicate to reject candidates (null accepts everything)
     * @param goalResolver  maps each accepted entity to the desired navigation target position (defaults to entity position)
     * @param stopResolver  computes the stop distance for each goal (defaults to touching distance based on hitboxes)
     * @param speedModifier navigation speed multiplier to embed into each goal
     * @return an immutable ordered list of navigation goals; empty if origin is invalid, not on the server, or nothing matches
     */
    public static List<NavigationGoal> collectEntityGoals(
            LivingEntity origin,
            double range,
            Predicate<? super LivingEntity> filter,
            Function<? super LivingEntity, Vec3> goalResolver,
            ToDoubleFunction<? super LivingEntity> stopResolver,
            double speedModifier
    ) {
        if (!(origin != null && range > 0.0)) {
            return List.of();
        }
        if (!(origin.level() instanceof ServerLevel level)) {
            return List.of();
        }
        Predicate<? super LivingEntity> effectiveFilter = filter != null ? filter : target -> true;
        Function<? super LivingEntity, Vec3> effectiveGoalResolver = goalResolver != null ? goalResolver : LivingEntity::position;
        ToDoubleFunction<? super LivingEntity> effectiveStopResolver = stopResolver != null
                ? stopResolver
                : target -> {
                    double radius = (origin.getBbWidth() + target.getBbWidth()) * 0.5;
                    return Math.max(0.5, radius);
                };

        AABB search = origin.getBoundingBox().inflate(range);
        List<LivingEntity> raw = level.getEntitiesOfClass(LivingEntity.class, search, candidate ->
                candidate != null && candidate != origin && candidate.isAlive() && effectiveFilter.test(candidate));
        if (raw.isEmpty()) {
            return List.of();
        }

        List<LivingEntity> ordered = orderByNearestNeighbour(raw, origin.position());
        List<NavigationGoal> goals = new ArrayList<>(ordered.size());
        for (LivingEntity target : ordered) {
            Vec3 goal = Objects.requireNonNullElseGet(effectiveGoalResolver.apply(target), target::position);
            double stop = effectiveStopResolver.applyAsDouble(target);
            goals.add(new NavigationGoal(target, goal, speedModifier, Math.max(0.0, stop)));
        }
        return Collections.unmodifiableList(goals);
    }

    /**
     * Convenience overload that uses the origin-to-target touching distance and the target's current position as the goal.
     */
    public static List<NavigationGoal> collectEntityGoals(
            LivingEntity origin,
            double range,
            Predicate<? super LivingEntity> filter,
            double speedModifier
    ) {
        return collectEntityGoals(origin, range, filter, null, null, speedModifier);
    }

    private static List<LivingEntity> orderByNearestNeighbour(List<LivingEntity> candidates, Vec3 start) {
        List<LivingEntity> remaining = new ArrayList<>(candidates);
        List<LivingEntity> ordered = new ArrayList<>(remaining.size());
        Vec3 cursor = start;
        while (!remaining.isEmpty()) {
            int bestIndex = 0;
            double bestDistance = Double.MAX_VALUE;
            for (int i = 0; i < remaining.size(); i++) {
                LivingEntity candidate = remaining.get(i);
                double dist = candidate.position().distanceToSqr(cursor);
                if (dist < bestDistance || (dist == bestDistance && candidate.getId() < remaining.get(bestIndex).getId())) {
                    bestDistance = dist;
                    bestIndex = i;
                }
            }
            LivingEntity next = remaining.remove(bestIndex);
            ordered.add(next);
            cursor = next.position();
        }
        return ordered;
    }
}
