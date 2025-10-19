package net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.exploration;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.soul.fakeplayer.brain.debug.BrainDebugEvent;
import net.tigereye.chestcavity.soul.fakeplayer.brain.debug.BrainDebugProbe;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.BrainActionStep;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrainContext;
import net.tigereye.chestcavity.soul.navigation.SoulNavigationMirror;

/** Simple exploration loop that wanders near the owner. */
public final class ExplorationPatrolSubBrain extends SubBrain {

    private static final String MEMORY_TARGET = "target";
    private static final String MEMORY_COOLDOWNS = "cooldowns";
    private static final String REPLAN_KEY = "replan";
    private static final double PATROL_RADIUS = 10.0;
    private static final double STOP_DIST = 2.0;
    private static final double SPEED = 1.0;
    private static final int REPLAN_INTERVAL = 60;

    public ExplorationPatrolSubBrain() {
        super("exploration.patrol");
        addStep(BrainActionStep.always(this::tickPatrol));
    }

    @Override
    public boolean shouldTick(SubBrainContext ctx) {
        return ctx.soul().isAlive() && ctx.owner() != null;
    }

    @Override
    public void onExit(SubBrainContext ctx) {
        SoulNavigationMirror.clearGoal(ctx.soul());
        ctx.memory().put(MEMORY_TARGET, null);
    }

    private void tickPatrol(SubBrainContext ctx) {
        Vec3 anchor = ctx.owner().position();
        MultiCooldown cooldowns = ctx.memory().get(MEMORY_COOLDOWNS, ExplorationPatrolSubBrain::createCooldowns);
        MultiCooldown.Entry replan = cooldowns.entry(REPLAN_KEY);
        Vec3 target = ctx.memory().getIfPresent(MEMORY_TARGET);
        long now = ctx.level().getGameTime();
        if (target == null || reached(ctx, target) || replan.isReady(now)) {
            target = pickNewTarget(ctx, anchor);
            ctx.memory().put(MEMORY_TARGET, target);
            replan.setReadyAt(now + REPLAN_INTERVAL);
            BrainDebugProbe.emit(BrainDebugEvent.builder("exploration")
                    .message("patrol_target")
                    .attribute("x", target.x)
                    .attribute("y", target.y)
                    .attribute("z", target.z)
                    .build());
        }
        SoulNavigationMirror.setGoal(ctx.soul(), target, SPEED, STOP_DIST);
    }

    private boolean reached(SubBrainContext ctx, Vec3 target) {
        return ctx.soul().position().distanceToSqr(target) <= STOP_DIST * STOP_DIST;
    }

    private Vec3 pickNewTarget(SubBrainContext ctx, Vec3 anchor) {
        var random = ctx.level().random;
        double angle = random.nextDouble() * Math.PI * 2.0;
        double radius = PATROL_RADIUS * Math.sqrt(random.nextDouble());
        double dx = Math.cos(angle) * radius;
        double dz = Math.sin(angle) * radius;
        return new Vec3(anchor.x + dx, anchor.y, anchor.z + dz);
    }

    private static MultiCooldown createCooldowns() {
        OrganState state = OrganState.of(new ItemStack(Items.MAP), "brain.explore");
        return MultiCooldown.builder(state)
                .withLongClamp(value -> Math.max(0L, value), 0L)
                .build();
    }
}
