package net.tigereye.chestcavity.guscript.runtime.flow;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.runtime.flow.sync.FlowSyncDispatcher;

import java.util.Map;
import java.util.Objects;

/**
 * Runtime controller binding a {@link FlowProgram} to a performer.
 */
public final class FlowController {

    private ServerPlayer performer;
    private final Map<String, Long> cooldowns = new java.util.HashMap<>();
    private final Map<String, Long> longVariables = new java.util.HashMap<>();
    private final Map<String, Double> doubleVariables = new java.util.HashMap<>();
    private final java.util.PriorityQueue<ScheduledTask> scheduledTasks = new java.util.PriorityQueue<>();
    private FlowInstance instance;

    public FlowController(ServerPlayer performer) {
        this.performer = Objects.requireNonNull(performer, "performer");
    }

    FlowController() {
        this.performer = null;
    }

    public ServerPlayer performer() {
        return performer;
    }

    public void updatePerformer(ServerPlayer performer) {
        if (performer == null || this.performer == performer) {
            return;
        }
        this.performer = performer;
        if (instance != null) {
            instance.rebindPerformer(performer);
        }
    }


    public boolean isRunning() {
        return instance != null && !instance.isFinished();
    }

    public void tick(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        runScheduledTasks(serverLevel);
        if (instance != null) {
            instance.tick(serverLevel);
            if (instance.isFinished()) {
                FlowSyncDispatcher.syncStopped(performer, instance);
                clearRuntimeState();
                instance = null;
            }
        }
    }

    public void start(FlowProgram program, LivingEntity target, java.util.Map<String, String> flowParams, long gameTime) {
        start(program, target, 1.0, flowParams, gameTime);
    }

    public void start(FlowProgram program, LivingEntity target, double timeScale, java.util.Map<String, String> flowParams, long gameTime) {
        if (program == null) {
            return;
        }
        if (isRunning()) {
            ChestCavity.LOGGER.debug("[Flow] Ignoring start for {} because flow {} already running", performer.getGameProfile().getName(), instance.program().id());
            return;
        }
        clearRuntimeState();
        java.util.Map<String, String> safeParams = flowParams == null ? java.util.Map.of() : java.util.Map.copyOf(flowParams);
        instance = new FlowInstance(program, performer, target, this, Math.max(0.0, timeScale), safeParams, gameTime);
        FlowSyncDispatcher.syncState(performer, instance);
        if (!instance.attemptStart(gameTime)) {
            ChestCavity.LOGGER.debug("[Flow] Flow {} stayed in {} after start trigger", program.id(), instance.state());
        }
    }

    public void handleInput(FlowInput input, long gameTime) {
        if (instance == null || input == null) {
            return;
        }
        instance.handleInput(input, gameTime);
    }

    public void handleStateChanged(FlowInstance flowInstance) {
        FlowSyncDispatcher.syncState(performer, flowInstance);
    }

    public boolean isCooldownReady(String key, long gameTime) {
        Long cooldown = cooldowns.get(key);
        return cooldown == null || cooldown <= gameTime;
    }

    public void setCooldown(String key, long readyTick) {
        cooldowns.put(key, readyTick);
    }

    public void setLong(String key, long value) {
        if (key != null) {
            longVariables.put(key, value);
        }
    }

    public long addLong(String key, long delta) {
        if (key == null) {
            return 0L;
        }
        long value = longVariables.getOrDefault(key, 0L) + delta;
        longVariables.put(key, value);
        return value;
    }

    public long getLong(String key, long defaultValue) {
        if (key == null) {
            return defaultValue;
        }
        return longVariables.getOrDefault(key, defaultValue);
    }

    public void setDouble(String key, double value) {
        if (key != null && !Double.isNaN(value) && !Double.isInfinite(value)) {
            doubleVariables.put(key, value);
        }
    }

    public double addDouble(String key, double delta) {
        if (key == null || Double.isNaN(delta) || Double.isInfinite(delta)) {
            return 0.0D;
        }
        double value = doubleVariables.getOrDefault(key, 0.0D) + delta;
        doubleVariables.put(key, value);
        return value;
    }

    public double getDouble(String key, double defaultValue) {
        if (key == null) {
            return defaultValue;
        }
        return doubleVariables.getOrDefault(key, defaultValue);
    }

    public void clampDouble(String key, double min, double max) {
        if (key == null) {
            return;
        }
        double value = getDouble(key, 0.0D);
        double clamped = net.minecraft.util.Mth.clamp(value, min, max);
        doubleVariables.put(key, clamped);
    }

    public void clampLong(String key, long min, long max) {
        if (key == null) {
            return;
        }
        long value = getLong(key, 0L);
        long clamped = java.lang.Math.max(min, java.lang.Math.min(max, value));
        longVariables.put(key, clamped);
    }

    public String resolveFlowParam(String key) {
        if (instance == null) {
            return null;
        }
        return instance.resolveParam(key).orElse(null);
    }

    public double resolveFlowParamAsDouble(String key, double defaultValue) {
        if (instance == null) {
            return defaultValue;
        }
        return instance.resolveParamAsDouble(key, defaultValue);
    }

    public void schedule(long executeAtTick, Runnable runnable) {
        if (runnable == null) {
            return;
        }
        scheduledTasks.add(new ScheduledTask(Math.max(0L, executeAtTick), runnable));
    }

    FlowInstance currentInstance() {
        return instance;
    }

    private void runScheduledTasks(ServerLevel level) {
        long gameTime = level.getGameTime();
        while (!scheduledTasks.isEmpty() && scheduledTasks.peek().executeAt <= gameTime) {
            ScheduledTask task = scheduledTasks.poll();
            if (task == null || task.runnable == null) {
                continue;
            }
            try {
                task.runnable.run();
            } catch (Exception ex) {
                ChestCavity.LOGGER.error("[Flow] Scheduled task failed", ex);
            }
        }
    }

    private void clearRuntimeState() {
        longVariables.clear();
        doubleVariables.clear();
        scheduledTasks.clear();
    }

    private static final class ScheduledTask implements Comparable<ScheduledTask> {
        private final long executeAt;
        private final Runnable runnable;

        private ScheduledTask(long executeAt, Runnable runnable) {
            this.executeAt = executeAt;
            this.runnable = runnable;
        }

        @Override
        public int compareTo(ScheduledTask other) {
            return Long.compare(this.executeAt, other.executeAt);
        }
    }
}
