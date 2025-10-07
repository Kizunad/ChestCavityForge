package net.tigereye.chestcavity.soul.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.util.ConstantMobs;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.navigation.SoulNavigationMirror;
import net.tigereye.chestcavity.soul.registry.SoulRuntimeHandler;
import net.tigereye.chestcavity.soul.util.SoulLook;
import net.tigereye.chestcavity.soul.combat.SoulAttackRegistry;
import net.tigereye.chestcavity.soul.combat.SoulFleeRegistry;
import net.tigereye.chestcavity.soul.combat.FleeContext;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Minimal FOLLOW/IDLE handler: when a soul is ordered to FOLLOW and is farther than 5 blocks
 * from the owner's current controlled body (ServerPlayer), it pathfinds to that position.
 */
public final class SoulAIOrderHandler implements SoulRuntimeHandler {

    private static final double FOLLOW_TRIGGER_DIST = 5.0; // blocks
    private static final double STOP_DIST = 2.0;            // blocks
    private static final double SPEED = 1.2;               // nav speed
    private static final double GUARD_RADIUS = 16.0;       // blocks

    @Override
    public void onTickEnd(SoulPlayer soul) {
        UUID ownerId = soul.getOwnerId().orElse(null);
        if (ownerId == null) return;
        ServerPlayer owner = soul.serverLevel().getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) return; // offline

        SoulAIOrders.Order order = SoulAIOrders.get(owner, soul.getSoulId());
        switch (order) {
            case FOLLOW -> {
                double dist = soul.distanceTo(owner);
                if (dist > FOLLOW_TRIGGER_DIST) {
                    Vec3 target = owner.position();
                    SoulNavigationMirror.setGoal(soul, target, SPEED, STOP_DIST);
                } else {
                    SoulNavigationMirror.clearGoal(soul);
                }
                // face towards owner's current position
                SoulLook.faceTowards(soul, owner.position());
            }
            case GUARD -> handleGuard(soul, owner);
            case IDLE -> {
                // no-op
            }
        }
        // Move first this tick so distance checks reflect new position
        SoulNavigationMirror.tick(soul);
        // Opportunistic post-move attack for GUARD
        if (order == SoulAIOrders.Order.GUARD) {
            postGuardAttack(soul, owner);
        }
    }

    private void handleGuard(SoulPlayer soul, ServerPlayer owner) {
        ServerLevel level = soul.serverLevel();
        Vec3 anchor = owner.position();
        AABB box = new AABB(anchor, anchor).inflate(GUARD_RADIUS);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != owner && e != soul && (e instanceof Enemy || ConstantMobs.isConsideredHostile(e)));
        if (candidates.isEmpty()) {
            // No hostiles in radius, stay near owner
            double dist = soul.distanceTo(owner);
            if (dist > FOLLOW_TRIGGER_DIST) {
                SoulNavigationMirror.setGoal(soul, anchor, SPEED, STOP_DIST);
            } else {
                SoulNavigationMirror.clearGoal(soul);
            }
            // look at anchor
            SoulLook.faceTowards(soul, anchor);
            return;
        }

        float myHp = soul.getHealth();
        float ratio = net.tigereye.chestcavity.soul.util.SoulCombatTuning.guardHpRatio();
        // Abort if exists a hostile with enemyHp*2 > myHp (i.e., myHp < 2*enemyHp)
        boolean risky = candidates.stream().anyMatch(h -> (h.getHealth() * ratio) > myHp);
        if (risky) {
            // attempt flee from the nearest/highest threat
            LivingEntity hazard = candidates.stream()
                    .max(Comparator.comparingDouble(LivingEntity::getHealth))
                    .orElse(null);
            if (hazard != null) {
                SoulFleeRegistry.tryFlee(FleeContext.of(soul, hazard, anchor));
                SoulLook.faceTowards(soul, hazard.position());
            } else {
                SoulNavigationMirror.clearGoal(soul);
                SoulLook.faceTowards(soul, anchor);
            }
            return;
        }

        // Pick nearest hostile that we are allowed to attack (myHp > 2*enemyHp)
        LivingEntity target = candidates.stream()
                .filter(h -> myHp >= ratio * h.getHealth())
                .min(Comparator.comparingDouble(h -> h.distanceToSqr(soul)))
                .orElse(null);
        if (target == null) {
            SoulNavigationMirror.clearGoal(soul);
            SoulLook.faceTowards(soul, anchor);
            return;
        }
        // If in attack range, use attack registry; otherwise, path towards the target
        SoulLook.faceTowards(soul, target.position());
        boolean attacked = false;
        if (!soul.isUsingItem()) {
            attacked = SoulAttackRegistry.attackIfInRange(soul, target);
        }
        if (!attacked) {
            SoulNavigationMirror.setGoal(soul, target.position(), SPEED, STOP_DIST);
        } else {
            // while attacking, do not move goal to avoid pushback fighting navigation
            SoulNavigationMirror.clearGoal(soul);
        }
    }

    private void postGuardAttack(SoulPlayer soul, ServerPlayer owner) {
        ServerLevel level = soul.serverLevel();
        Vec3 anchor = owner.position();
        AABB box = new AABB(anchor, anchor).inflate(GUARD_RADIUS);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != owner && e != soul && (e instanceof Enemy || ConstantMobs.isConsideredHostile(e)));
        if (candidates.isEmpty()) return;
        LivingEntity nearest = candidates.stream()
                .min(Comparator.comparingDouble(h -> h.distanceToSqr(soul)))
                .orElse(null);
        if (nearest == null) return;
        double maxRange = net.tigereye.chestcavity.soul.combat.SoulAttackRegistry.maxRange(soul, nearest);
        double d = soul.distanceTo(nearest);
        if (d <= maxRange + 0.25 && !soul.isUsingItem()) {
            SoulLook.faceTowards(soul, nearest.position());
            boolean attacked = net.tigereye.chestcavity.soul.combat.SoulAttackRegistry.attackIfInRange(soul, nearest);
            if (attacked) {
                SoulNavigationMirror.clearGoal(soul);
            }
        }
    }
}
