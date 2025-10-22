package net.tigereye.chestcavity.soul.ai;

import java.util.Comparator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.util.ConstantMobs;
import net.tigereye.chestcavity.soul.combat.SoulAttackRegistry;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.navigation.SoulNavigationMirror;
import net.tigereye.chestcavity.soul.util.SoulLook;

/** Shared combat driving for actions (force_fight/guard) that do not rely on orders. */
public final class SoulCombatOps {
  private SoulCombatOps() {}

  private static final double SPEED = 1.2;
  private static final double GUARD_RADIUS = 16.0;
  private static final double KITE_MIN_DIST = 1.5;
  private static final double KITE_TARGET_DIST = 3.0;

  public static void applyForceFightTick(SoulPlayer soul, ServerPlayer owner) {
    if (owner == null || !soul.isAlive()) return;
    ServerLevel level = soul.serverLevel();
    Vec3 anchor = owner.position();
    AABB box = new AABB(anchor, anchor).inflate(GUARD_RADIUS);
    List<LivingEntity> candidates =
        level.getEntitiesOfClass(
            LivingEntity.class,
            box,
            e -> e.isAlive() && e != owner && e != soul && !isSameOwnerSoul(e, owner));
    if (candidates.isEmpty()) {
      // hover near owner
      keepNear(soul, owner.position());
      return;
    }
    LivingEntity target =
        candidates.stream()
            .min(Comparator.comparingDouble(h -> h.distanceToSqr(soul)))
            .orElse(null);
    if (target == null) {
      keepNear(soul, owner.position());
      return;
    }

    // attack if in range
    SoulLook.faceTowards(soul, target.position());
    if (!soul.isUsingItem()) {
      SoulAttackRegistry.attackIfInRange(soul, target);
    }
    // evade if too close
    double d = soul.distanceTo(target);
    double tooClose =
        Math.max(KITE_MIN_DIST, 0.5 + (soul.getBbWidth() + target.getBbWidth()) * 0.5);
    if (d <= tooClose
        && net.tigereye.chestcavity.soul.ai.SoulEvadeHelper.tryEvade(soul, target, d, tooClose)) {
      return;
    }
    // orbit ring
    ringTowards(soul, target.position(), desiredRing(soul, target));
  }

  public static void applyGuardTick(SoulPlayer soul, ServerPlayer owner) {
    if (owner == null || !soul.isAlive()) return;
    ServerLevel level = soul.serverLevel();
    Vec3 anchor = owner.position();
    AABB box = new AABB(anchor, anchor).inflate(GUARD_RADIUS);
    List<LivingEntity> candidates =
        level.getEntitiesOfClass(
            LivingEntity.class,
            box,
            e ->
                e.isAlive()
                    && e != owner
                    && e != soul
                    && (e instanceof Enemy || ConstantMobs.isConsideredHostile(e)));
    if (candidates.isEmpty()) {
      keepNear(soul, anchor);
      return;
    }

    // choose by HP ratio gate similar to order handler
    float myHp = soul.getHealth();
    float ratio = net.tigereye.chestcavity.soul.util.SoulCombatTuning.guardHpRatio();
    LivingEntity target =
        candidates.stream()
            .filter(h -> myHp >= ratio * h.getHealth())
            .min(Comparator.comparingDouble(h -> h.distanceToSqr(soul)))
            .orElse(null);
    if (target == null) {
      keepNear(soul, anchor);
      return;
    }

    SoulLook.faceTowards(soul, target.position());
    if (!soul.isUsingItem()) {
      SoulAttackRegistry.attackIfInRange(soul, target);
    }
    double d = soul.distanceTo(target);
    double tooClose =
        Math.max(KITE_MIN_DIST, 0.5 + (soul.getBbWidth() + target.getBbWidth()) * 0.5);
    if (d <= tooClose
        && net.tigereye.chestcavity.soul.ai.SoulEvadeHelper.tryEvade(soul, target, d, tooClose)) {
      return;
    }
    ringTowards(soul, target.position(), desiredRing(soul, target));
  }

  private static boolean isSameOwnerSoul(LivingEntity entity, ServerPlayer owner) {
    if (entity instanceof SoulPlayer spSoul) {
      return spSoul.getOwnerId().map(owner.getUUID()::equals).orElse(false);
    }
    return false;
  }

  private static void keepNear(SoulPlayer soul, Vec3 pos) {
    double dist = soul.position().distanceTo(pos);
    if (dist > 5.0) {
      SoulNavigationMirror.setGoal(soul, pos, SPEED, 2.0);
    } else {
      SoulNavigationMirror.clearGoal(soul);
    }
    SoulLook.faceTowards(soul, pos);
  }

  private static double desiredRing(SoulPlayer soul, LivingEntity target) {
    double maxRange = SoulAttackRegistry.maxRange(soul, target);
    return Math.max(1.25, Math.min(maxRange - 0.15, KITE_TARGET_DIST));
  }

  private static void ringTowards(SoulPlayer soul, Vec3 center, double desired) {
    var away = soul.position().subtract(center);
    if (away.lengthSqr() > 1.0e-6) {
      var goal = center.add(away.normalize().scale(desired));
      double stopDist = Math.max(0.5, desired - 0.5);
      SoulNavigationMirror.setGoal(soul, goal, SPEED, stopDist);
    } else {
      SoulNavigationMirror.setGoal(soul, center, SPEED, 0.75);
    }
  }
}
