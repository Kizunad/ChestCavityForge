package net.tigereye.chestcavity.soul.navigation;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.navigation.barintegrate.BaritoneFacade;
import net.tigereye.chestcavity.soul.navigation.plan.BaritonePathPlanner;
import net.tigereye.chestcavity.soul.navigation.plan.ISoulPathPlanner;
import net.tigereye.chestcavity.soul.util.SoulLog;

/**
 * Baritone-backed navigator skeleton. 当前阶段只做“可用性检测 + 基本配置”，
 * 实际导航仍委托给 VirtualSoulNavigator，待后续接入 Baritone 路径规划。
 */
final class BaritoneSoulNavigator implements ISoulNavigator {

    private final VirtualSoulNavigator fallback;
    private final boolean baritoneAvailable;
    private final java.util.concurrent.ExecutorService plannerExec = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Soul-Baritone-Planner");
        t.setDaemon(true);
        return t;
    });
    private final ISoulPathPlanner planner = new BaritonePathPlanner();
    private volatile java.util.List<Vec3> waypoints = java.util.Collections.emptyList();
    private volatile int waypointIndex = 0;
    private volatile Vec3 goalTarget;
    private volatile double goalStopDistance = 2.0;
    private volatile double goalSpeedModifier = 1.0;
    private volatile int followStuckTicks = 0;
    private volatile double lastRemain = Double.MAX_VALUE; // linear distance, not squared
    private volatile int replanCount = 0;
    private volatile long lastPlanMs = -1L;
    private volatile boolean planning = false;
    private volatile Vec3 pendingTarget;

    BaritoneSoulNavigator(net.minecraft.server.level.ServerLevel level) {
        // 使用“激进步进”策略作为 Baritone 规划的跟随执行器（autostep 效果）
        this.fallback = new VirtualSoulNavigator(level, VirtualSoulNavigator.StepPolicy.AGGRESSIVE);
        // 反射检测 Baritone，并应用一次安全基线配置
        this.baritoneAvailable = BaritoneFacade.isAvailable();
        if (this.baritoneAvailable) {
            BaritoneFacade.applyBaselineSettings();
            if (SoulLog.DEBUG_LOGS) {
                SoulLog.info("[soul][nav] Baritone detected; baseline settings applied.");
            }
        } else if (SoulLog.DEBUG_LOGS) {
            SoulLog.info("[soul][nav] Baritone not available; falling back to virtual navigator.");
        }
    }

    @Override
    public void setGoal(SoulPlayer soul, Vec3 target, double speedModifier, double stopDistance) {
        // 当前阶段：提交一次规划请求，若不可用/失败，回退到虚拟导航
        this.fallback.setGoal(soul, target, speedModifier, stopDistance);
        this.goalTarget = target;
        this.goalStopDistance = stopDistance;
        this.goalSpeedModifier = speedModifier;
        this.waypointIndex = 0;
        if (this.baritoneAvailable) {
            submitPlan((net.minecraft.server.level.ServerLevel) soul.level(), soul, target);
        }
    }

    @Override
    public void clearGoal() {
        this.fallback.clearGoal();
        this.goalTarget = null;
        this.waypoints = java.util.Collections.emptyList();
        this.waypointIndex = 0;
    }

    @Override
    public void tick(SoulPlayer soul) {
        // 当存在 Baritone 规划结果时，按路径点推进；否则退回虚拟导航
        if (this.goalTarget != null && !this.waypoints.isEmpty() && this.waypointIndex < this.waypoints.size()) {
            followWaypoints(soul);
            // stuck detection while following path
            double remain = Math.sqrt(soul.position().distanceToSqr(this.goalTarget));
            double progressDelta = progressDeltaThreshold(soul); // linear distance threshold
            if ((lastRemain - remain) > progressDelta) {
                lastRemain = remain;
                followStuckTicks = 0;
            } else {
                followStuckTicks++;
            }
            // trigger replan if not progressing for a while
            if (this.baritoneAvailable && !planning && followStuckTicks >= getReplanStuckTicks()) {
                submitPlan((net.minecraft.server.level.ServerLevel) soul.level(), soul, this.goalTarget);
                followStuckTicks = 0;
            }
            return;
        }
        this.fallback.tick(soul);
    }

    private void submitPlan(net.minecraft.server.level.ServerLevel level, SoulPlayer soul, Vec3 target) {
        if (planning) {
            this.pendingTarget = target;
            return;
        }
        planning = true;
        this.pendingTarget = null;
        long timeout = getTimeoutMs();
        plannerExec.submit(() -> {
            long t0 = System.nanoTime();
            try {
                var res = planner.planPath(level, soul, target, timeout);
                this.waypoints = res.orElse(java.util.Collections.emptyList());
                this.waypointIndex = 0;
                this.replanCount++;
            } catch (Throwable t) {
                if (SoulLog.DEBUG_LOGS) {
                    SoulLog.info("[soul][nav][baritone] plan failed: {}", t.toString());
                }
                this.waypoints = java.util.Collections.emptyList();
            } finally {
                this.lastPlanMs = (System.nanoTime() - t0) / 1_000_000L;
                planning = false;
                if (this.pendingTarget != null) {
                    // 目标在规划期间被更改，重新触发
                    Vec3 pt = this.pendingTarget;
                    this.pendingTarget = null;
                    submitPlan(level, soul, pt);
                }
            }
        });
    }

    private static long getTimeoutMs() {
        String t = System.getProperty("chestcavity.soul.baritone.timeoutMs", "2500");
        try { return Math.max(250L, Math.min(10_000L, Long.parseLong(t))); } catch (NumberFormatException e) { return 2500L; }
    }

    private void followWaypoints(SoulPlayer soul) {
        if (this.goalTarget == null) return;
        if (this.waypointIndex >= this.waypoints.size()) return;
        Vec3 next = this.waypoints.get(this.waypointIndex);
        double dist = Math.sqrt(next.distanceToSqr(soul.position()));
        // 到达判定采用实体宽度/速度自适应松弛
        double advSlack = advanceSlackThreshold(soul);
        if (dist <= advSlack) {
            this.waypointIndex++;
            this.fallback.clearGoal();
            if (this.waypointIndex >= this.waypoints.size()) {
                // 到达终点或进入最终停靠范围
                double remain2 = soul.position().distanceToSqr(this.goalTarget);
                if (remain2 <= (this.goalStopDistance * this.goalStopDistance)) {
                    clearGoal();
                }
            }
            return;
        }
        // 使用 autostep 虚拟导航器跟随当前路径点（不直接位移，避免跳跃抖动）
        this.fallback.setGoal(soul, next, this.goalSpeedModifier, advSlack);
        this.fallback.tick(soul);
    }

    private double blocksPerTick(SoulPlayer soul) {
        var attr = soul.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        double base = (attr != null) ? attr.getValue() : 0.1;
        double v = base * this.goalSpeedModifier;
        // 与虚拟导航保持一致：在地面推进阶段按“冲刺”倍率估算阈值
        v *= getRunMultiplier();
        return v;
    }

    private double advanceSlackThreshold(SoulPlayer soul) {
        double sizeTerm = Math.max(0.4, 0.5 * soul.getBbWidth());
        double speedTerm = 0.3 * blocksPerTick(soul);
        double slack = sizeTerm + speedTerm;
        // clamp to reasonable bounds
        if (slack < 0.3) slack = 0.3;
        if (slack > 2.5) slack = 2.5;
        return slack;
    }

    private double progressDeltaThreshold(SoulPlayer soul) {
        double v = blocksPerTick(soul);
        double d = Math.max(0.05, 0.5 * v);
        // clamp
        if (d > 1.5) d = 1.5;
        return d;
    }

    // ---- debug ----
    public String debugLine() {
        int count = waypoints != null ? waypoints.size() : 0;
        return "engine=baritone, planning=" + planning + ", waypoints=" + count + ", idx=" + waypointIndex +
                ", lastPlanMs=" + lastPlanMs + ", replanCount=" + replanCount;
    }

    private static int getReplanStuckTicks() {
        String v = System.getProperty("chestcavity.soul.baritone.replanStuckTicks", "60");
        try { int n = Integer.parseInt(v); return Math.max(20, Math.min(400, n)); } catch (NumberFormatException e) { return 60; }
    }

    private static double getRunMultiplier() {
        String v = System.getProperty("chestcavity.soul.runMultiplier", "1.3");
        try {
            double d = Double.parseDouble(v);
            if (d < 1.0) d = 1.0;
            if (d > 2.0) d = 2.0;
            return d;
        } catch (NumberFormatException e) {
            return 1.3;
        }
    }
}
