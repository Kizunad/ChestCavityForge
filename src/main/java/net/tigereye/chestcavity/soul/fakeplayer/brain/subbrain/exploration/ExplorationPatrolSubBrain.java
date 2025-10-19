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

/**
 * 探索巡逻子大脑（ExplorationPatrolSubBrain）
 *
 * 作用
 * - 围绕 owner 在一定半径内随机巡逻，周期性重选目标点；
 * - 通过 SoulNavigationMirror 设置行走目标，达到或超时后重新规划；
 * - 使用 MultiCooldown 控制“重规划”节奏，避免过于频繁的目标切换。
 *
 * 参数
 * - PATROL_RADIUS：巡逻半径（水平随机落点）；
 * - STOP_DIST：到达判定半径；
 * - SPEED：移动速度修正；
 * - REPLAN_INTERVAL：最小重选间隔（tick）。
 */
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
        // 兼容无 owner：仅要求 Soul 存活即可运行。
        // 当 owner 为空时，以 Soul 自身位置作为锚点进行就地巡逻。
        return ctx.soul().isAlive();
    }

    @Override
    public void onExit(SubBrainContext ctx) {
        // 退出时清理目标，避免残留路径
        SoulNavigationMirror.clearGoal(ctx.soul());
        ctx.memory().put(MEMORY_TARGET, null);
    }

    private void tickPatrol(SubBrainContext ctx) {
        // owner 可为空；为空时以自身为锚点
        Vec3 anchor = (ctx.owner() != null) ? ctx.owner().position() : ctx.soul().position();
        MultiCooldown cooldowns = ctx.memory().get(MEMORY_COOLDOWNS, ExplorationPatrolSubBrain::createCooldowns);
        MultiCooldown.Entry replan = cooldowns.entry(REPLAN_KEY);
        Vec3 target = ctx.memory().getIfPresent(MEMORY_TARGET);
        long now = ctx.level().getGameTime();
        // 无目标 / 已到达 / 超过重规划间隔 时，挑选新目标
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
        // 在圆盘内均匀采样：角度均匀，半径取 sqrt(u) 以保证面积均匀
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
