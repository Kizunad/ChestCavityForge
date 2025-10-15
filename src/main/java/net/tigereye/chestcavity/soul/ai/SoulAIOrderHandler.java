package net.tigereye.chestcavity.soul.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.util.ConstantMobs;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TeleportOps;
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
 * 灵魂基础指令（FOLLOW/IDLE/GUARD/FORCE_FIGHT）处理器。
 *
 * <p>负责监听容器内持久化的 AI 指令，并在每个 tick 根据指令下达基础移动/战斗行为。</p>
 */
public final class SoulAIOrderHandler implements SoulRuntimeHandler {

    // FOLLOW_TRIGGER_DIST 改为运行时可配（见 SoulFollowTeleportTuning）
    private static final double STOP_DIST = 2.0;            // blocks
    private static final double SPEED = 1.2;               // nav speed
    private static final double GUARD_RADIUS = 16.0;       // blocks
    private static final double TELEPORT_TO_OWNER_DIST = 20.0; // 超过该距离直接传送回到主人身边
    // Kiting preferences
    private static final double KITE_MIN_DIST = 1.5;        // blink/spacing only if closer than this
    private static final double KITE_TARGET_DIST = 6.0;     // preferred distance when kiting

    @Override
    public void onTickEnd(SoulPlayer soul) {
        // If an action is actively driving combat, skip order-based logic
        var mgr = net.tigereye.chestcavity.soul.fakeplayer.actions.state.ActionStateManager.of(soul);
        if (mgr.isActive(new net.tigereye.chestcavity.soul.fakeplayer.actions.api.ActionId(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(net.tigereye.chestcavity.ChestCavity.MODID, "action/force_fight")))
            || mgr.isActive(new net.tigereye.chestcavity.soul.fakeplayer.actions.api.ActionId(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(net.tigereye.chestcavity.ChestCavity.MODID, "action/guard")))) {
            return;
        }
        UUID ownerId = soul.getOwnerId().orElse(null);
        if (ownerId == null) return;
        ServerPlayer owner = soul.serverLevel().getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) return; // offline
        // 若与主人距离过大（>阈值），尝试直接传送至主人身边，避免“无法导航”的极端路径
        tryTeleportNearOwnerIfFar(soul, owner);

        SoulAIOrders.Order order = SoulAIOrders.get(owner, soul.getSoulId());
        switch (order) {
            case FOLLOW -> {
                double dist = soul.distanceTo(owner);
                if (dist > net.tigereye.chestcavity.soul.util.SoulFollowTeleportTuning.followTriggerDist()) {
                    Vec3 target = owner.position();
                    SoulNavigationMirror.setGoal(soul, target, SPEED, STOP_DIST);
                } else {
                    SoulNavigationMirror.clearGoal(soul);
                }
                // face towards owner's current position
                SoulLook.faceTowards(soul, owner.position());
            }
            case GUARD -> handleGuard(soul, owner);
            case FORCE_FIGHT -> handleForceFight(soul, owner);
            case IDLE -> {
                // no-op
            }
        }
        // Navigation is advanced centrally via server tick; no per-entity tick here
        // Opportunistic post-move attack
        if (order == SoulAIOrders.Order.GUARD) {
            postGuardAttack(soul, owner);
        } else if (order == SoulAIOrders.Order.FORCE_FIGHT) {
            postForceFightAttack(soul, owner);
        }
    }

    /**
     * 当 Soul 与主人距离超过阈值时，尝试将其直接传送至主人身边的安全位置。
     * 使用 TeleportOps 做目的地碰撞/高度探测，优先选择“背后-右侧-左侧-正前”四个相邻点。
     */
    private void tryTeleportNearOwnerIfFar(SoulPlayer soul, ServerPlayer owner) {
        if (!soul.level().dimension().equals(owner.level().dimension())) return; // 跨维不处理
        double dist = soul.distanceTo(owner);
        if (!net.tigereye.chestcavity.soul.util.SoulFollowTeleportTuning.teleportEnabled()) return;
        double teleDist = net.tigereye.chestcavity.soul.util.SoulFollowTeleportTuning.teleportDist();
        if (dist <= teleDist) return;
        Vec3 ownerPos = owner.position();
        // 先尝试直接贴身传送至主人当前位置
        if (TeleportOps.blinkTo(soul, ownerPos, /*verticalAttempts*/6, /*step*/0.5D).isPresent()) {
            SoulLook.faceTowards(soul, ownerPos);
            return;
        }
        // 若直接落点失败，再按多个半径尝试围绕主人落位，优先更贴近的半径
        double[] radii = new double[]{0.75, 1.0, 1.25};
        double yawRad = Math.toRadians(owner.getYRot());
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);
        double rx = -fz;
        double rz = fx;
        for (double radius : radii) {
            Vec3[] candidates = new Vec3[]{
                    new Vec3(ownerPos.x - fx * radius, ownerPos.y, ownerPos.z - fz * radius), // 背后
                    new Vec3(ownerPos.x + rx * radius, ownerPos.y, ownerPos.z + rz * radius), // 右侧
                    new Vec3(ownerPos.x - rx * radius, ownerPos.y, ownerPos.z - rz * radius), // 左侧
                    new Vec3(ownerPos.x + fx * radius, ownerPos.y, ownerPos.z + fz * radius)  // 正前
            };
            for (Vec3 target : candidates) {
                if (TeleportOps.blinkTo(soul, target, 6, 0.5D).isPresent()) {
                    SoulLook.faceTowards(soul, ownerPos);
                    return;
                }
            }
        }
        // 仍失败则强制传送至主人脚下（忽略碰撞检查），避免远距离卡死
        soul.teleportTo(ownerPos.x, ownerPos.y, ownerPos.z);
        soul.setDeltaMovement(Vec3.ZERO);
        soul.fallDistance = 0.0F;
        SoulLook.faceTowards(soul, ownerPos);
    }

    /**
     * 激进模式：在半径 16 格内寻找任意活体目标并主动接战。
     */
    private void handleForceFight(SoulPlayer soul, ServerPlayer owner) {
        ServerLevel level = soul.serverLevel();
        Vec3 anchor = owner.position();
        AABB box = new AABB(anchor, anchor).inflate(GUARD_RADIUS);
        // Force-fight: consider ANY living entity except the owner and self (and friendly souls of same owner)
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != owner && e != soul && !isSameOwnerSoul(e, owner));
        if (candidates.isEmpty()) {
            // No entities: hover near owner
            double dist = soul.distanceTo(owner);
            if (dist > net.tigereye.chestcavity.soul.util.SoulFollowTeleportTuning.followTriggerDist()) {
                SoulNavigationMirror.setGoal(soul, anchor, SPEED, STOP_DIST);
            } else {
                SoulNavigationMirror.clearGoal(soul);
            }
            SoulLook.faceTowards(soul, anchor);
            return;
        }

        // Aggressive: pick nearest and pursue/attack unconditionally (no HP ratio check)
        LivingEntity target = candidates.stream()
                .min(Comparator.comparingDouble(h -> h.distanceToSqr(soul)))
                .orElse(null);
        if (target == null) {
            SoulNavigationMirror.clearGoal(soul);
            SoulLook.faceTowards(soul, anchor);
            return;
        }
        SoulLook.faceTowards(soul, target.position());
        boolean attacked = false;
        if (!soul.isUsingItem()) {
            attacked = SoulAttackRegistry.attackIfInRange(soul, target);
        }
        // Maintain a ring around the target to avoid body-collisions while still allowing attacks
        double d = soul.distanceTo(target);
        double maxRange = net.tigereye.chestcavity.soul.combat.SoulAttackRegistry.maxRange(soul, target);
        // Keep orbit slightly inside maxRange so attacks can land while moving
        double desired = Math.max(1.25, Math.min(maxRange - 0.15, 3.0));
        // If extremely close, try an evade blink (consumes 50 jingli, 5s cooldown)
        double tooClose = Math.max(KITE_MIN_DIST, 0.5 + (soul.getBbWidth() + target.getBbWidth()) * 0.5);
        if (d <= tooClose && net.tigereye.chestcavity.soul.ai.SoulEvadeHelper.tryEvade(soul, target, d, tooClose)) {
            return; // evaded this tick
        }
        // Compute a position on the ring around target, pointing away from it
        var away = soul.position().subtract(target.position());
        if (away.lengthSqr() > 1.0e-6) {
            var goal = target.position().add(away.normalize().scale(desired));
            double stopDist = Math.max(0.5, desired - 0.5);
            SoulNavigationMirror.setGoal(soul, goal, SPEED, stopDist);
        } else {
            SoulNavigationMirror.setGoal(soul, target.position(), SPEED, 0.75);
        }
    }

    /**
     * 激进模式的尾随补刀逻辑，补发一次近战攻击尝试。
     */
    private void postForceFightAttack(SoulPlayer soul, ServerPlayer owner) {
        ServerLevel level = soul.serverLevel();
        Vec3 anchor = owner.position();
        AABB box = new AABB(anchor, anchor).inflate(GUARD_RADIUS);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != owner && e != soul && !isSameOwnerSoul(e, owner));
        if (candidates.isEmpty()) return;
        LivingEntity nearest = candidates.stream()
                .min(Comparator.comparingDouble(h -> h.distanceToSqr(soul)))
                .orElse(null);
        if (nearest == null) return;
        double maxRange = SoulAttackRegistry.maxRange(soul, nearest);
        double d = soul.distanceTo(nearest);
        if (d <= maxRange + 0.25 && !soul.isUsingItem()) {
            SoulLook.faceTowards(soul, nearest.position());
            boolean attacked = SoulAttackRegistry.attackIfInRange(soul, nearest);
            if (attacked) {
                SoulNavigationMirror.clearGoal(soul);
            }
        }
    }

    /**
     * 判断目标是否为同一宿主的灵魂，用于避免误伤友方。
     */
    private static boolean isSameOwnerSoul(LivingEntity entity, ServerPlayer owner) {
        if (entity instanceof net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer spSoul) {
            return spSoul.getOwnerId().map(owner.getUUID()::equals).orElse(false);
        }
        return false;
    }

    /**
     * 守卫模式：优先守在宿主身边，遇到威胁时在血量安全前提下主动迎击。
     */
    private void handleGuard(SoulPlayer soul, ServerPlayer owner) {
        ServerLevel level = soul.serverLevel();
        Vec3 anchor = owner.position();
        AABB box = new AABB(anchor, anchor).inflate(GUARD_RADIUS);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != owner && e != soul && (e instanceof Enemy || ConstantMobs.isConsideredHostile(e)));
        if (candidates.isEmpty()) {
            // No hostiles in radius, stay near owner
            double dist = soul.distanceTo(owner);
            if (dist > net.tigereye.chestcavity.soul.util.SoulFollowTeleportTuning.followTriggerDist()) {
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
        // If in attack range, attack; always keep a navigation goal to avoid collision pushing
        SoulLook.faceTowards(soul, target.position());
        boolean attacked = false;
        if (!soul.isUsingItem()) {
            attacked = SoulAttackRegistry.attackIfInRange(soul, target);
        }
        double maxRange2 = net.tigereye.chestcavity.soul.combat.SoulAttackRegistry.maxRange(soul, target);
        double desired2 = Math.max(2.5, Math.min(maxRange2 - 0.25, KITE_TARGET_DIST));
        double d2 = soul.distanceTo(target);
        double tooClose2 = Math.max(KITE_MIN_DIST, 0.5 + (soul.getBbWidth() + target.getBbWidth()) * 0.5);
        if (d2 <= tooClose2 && net.tigereye.chestcavity.soul.ai.SoulEvadeHelper.tryEvade(soul, target, d2, tooClose2)) {
            return; // evaded this tick
        }
        var away2 = soul.position().subtract(target.position());
        if (away2.lengthSqr() > 1.0e-6) {
            var goal2 = target.position().add(away2.normalize().scale(desired2));
            SoulNavigationMirror.setGoal(soul, goal2, SPEED, STOP_DIST);
        } else {
            SoulNavigationMirror.setGoal(soul, target.position(), SPEED, STOP_DIST);
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
        // Keep orbit close enough to keep landing attacks on subsequent ticks
        var away = soul.position().subtract(nearest.position());
        if (away.lengthSqr() > 1.0e-6) {
            double desired = Math.max(1.25, Math.min(maxRange - 0.15, KITE_TARGET_DIST));
            var goal = nearest.position().add(away.normalize().scale(desired));
            double stopDist = Math.max(0.5, desired - 0.5);
            SoulNavigationMirror.setGoal(soul, goal, SPEED, stopDist);
        }
    }
}
