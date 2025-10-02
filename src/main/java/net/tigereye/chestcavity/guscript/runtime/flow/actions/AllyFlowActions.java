package net.tigereye.chestcavity.guscript.runtime.flow.actions;

import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowEdgeAction;

/**
 * 同伴、友方单位相关的 Action 实现。
 */
final class AllyFlowActions {

    private AllyFlowActions() {
    }

    static FlowEdgeAction tameNearby(double radius, boolean sit, boolean persist) {
        double r = Math.max(0.0, radius);
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null) return;
                Level level = performer.level();
                if (!(level instanceof ServerLevel server)) return;
                AABB box = new AABB(performer.blockPosition()).inflate(r);
                List<LivingEntity> list = server.getEntitiesOfClass(LivingEntity.class, box);
                for (LivingEntity e : list) {
                    if (e instanceof TamableAnimal tamable) {
                        tamable.tame(performer);
                        tamable.setOrderedToSit(sit);
                        if (persist && e instanceof Mob mob) {
                            mob.setPersistenceRequired();
                        }
                    }
                }
            }

            @Override
            public String describe() { return "tame_nearby(r=" + r + ", sit=" + sit + ")"; }
        };
    }

    static FlowEdgeAction orderGuard(double radius, boolean seekHostiles, double acquireRadius) {
        double r = Math.max(0.0, radius);
        double ar = Math.max(0.0, acquireRadius);
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null) return;
                Level level = performer.level();
                if (!(level instanceof ServerLevel server)) return;
                AABB box = new AABB(performer.blockPosition()).inflate(r);
                List<LivingEntity> allies = server.getEntitiesOfClass(LivingEntity.class, box, e -> FlowActionUtils.isAlly(performer, e));
                for (LivingEntity ally : allies) {
                    if (ally instanceof TamableAnimal tam) {
                        tam.setOrderedToSit(false);
                    }
                    if (seekHostiles && ally instanceof Mob mob) {
                        Vec3 pos = ally.position();
                        AABB search = new AABB(pos, pos).inflate(ar);
                        List<LivingEntity> hostiles = server.getEntitiesOfClass(LivingEntity.class, search, e -> e instanceof Enemy && e.isAlive() && e != performer);
                        LivingEntity nearest = null;
                        double best = Double.POSITIVE_INFINITY;
                        for (LivingEntity h : hostiles) {
                            double d = h.distanceToSqr(ally);
                            if (d < best) { best = d; nearest = h; }
                        }
                        if (nearest != null) {
                            mob.setTarget(nearest);
                        }
                    }
                }
            }

            @Override
            public String describe() { return "order_guard(r=" + r + ", seek=" + seekHostiles + ", ar=" + ar + ")"; }
        };
    }

    static FlowEdgeAction bindOwnerNudao(double radius, boolean alsoTameIfPossible) {
        double r = Math.max(0.0, radius);
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null) return;
                Level level = performer.level();
                if (!(level instanceof ServerLevel server)) return;
                AABB box = new AABB(performer.blockPosition()).inflate(r);
                List<LivingEntity> list = server.getEntitiesOfClass(LivingEntity.class, box);
                for (LivingEntity e : list) {
                    net.tigereye.chestcavity.guzhenren.nudao.GuzhenrenNudaoBridge.openSubject(e)
                            .ifPresent(handle -> handle.setOwner(performer, alsoTameIfPossible));
                }
            }

            @Override
            public String describe() { return "bind_owner_nudao(r=" + r + ", tameIfPossible=" + alsoTameIfPossible + ")"; }
        };
    }

    static FlowEdgeAction assistPlayerAttacks(double allyRadius, int recentTicks, double acquireRadius) {
        double r = Math.max(0.0, allyRadius);
        int window = Math.max(1, recentTicks);
        double ar = Math.max(0.0, acquireRadius);
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null) return;
                Level level = performer.level();
                if (!(level instanceof ServerLevel server)) return;
                LivingEntity recent = performer.getLastHurtMob();
                if (recent == null || !recent.isAlive()) return;
                int ts = performer.getLastHurtMobTimestamp();
                if (performer.tickCount - ts > window) return;
                AABB box = new AABB(performer.blockPosition()).inflate(r);
                List<LivingEntity> allies = server.getEntitiesOfClass(LivingEntity.class, box, e -> FlowActionUtils.isAlly(performer, e));
                for (LivingEntity ally : allies) {
                    if (ally instanceof Mob mob) {
                        if (recent.distanceToSqr(ally) <= ar * ar) {
                            mob.setTarget(recent);
                        }
                    }
                }
            }

            @Override
            public String describe() { return "assist_player_attacks(r=" + r + ", window=" + window + ", ar=" + ar + ")"; }
        };
    }

    static FlowEdgeAction setInvisibleNearby(double radius, boolean invisible, boolean alliesOnly) {
        double r = Math.max(0.0, radius);
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null) return;
                Level level = performer.level();
                if (!(level instanceof ServerLevel server)) {
                    return;
                }
                AABB box = new AABB(performer.blockPosition()).inflate(r);
                List<LivingEntity> entities = server.getEntitiesOfClass(LivingEntity.class, box, candidate -> {
                    if (candidate == null || !candidate.isAlive()) {
                        return false;
                    }
                    if (candidate == performer) {
                        return true;
                    }
                    if (!alliesOnly) {
                        return true;
                    }
                    return FlowActionUtils.isAlly(performer, candidate);
                });
                for (LivingEntity entity : entities) {
                    entity.setInvisible(invisible);
                }
            }

            @Override
            public String describe() { return "set_invisible_nearby(r=" + r + ", alliesOnly=" + alliesOnly + ")"; }
        };
    }
}
