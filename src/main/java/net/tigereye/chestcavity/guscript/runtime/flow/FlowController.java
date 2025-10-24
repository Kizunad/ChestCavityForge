package net.tigereye.chestcavity.guscript.runtime.flow;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.guscript.runtime.flow.sync.FlowSyncDispatcher;

/** Runtime controller binding a {@link FlowProgram} to a performer. */
public final class FlowController {

  private ServerPlayer performer;
  private final Map<String, Long> cooldowns = new java.util.HashMap<>();
  private final Map<String, Long> longVariables = new java.util.HashMap<>();
  private final Map<String, Double> doubleVariables = new java.util.HashMap<>();
  private final java.util.PriorityQueue<ScheduledTask> scheduledTasks =
      new java.util.PriorityQueue<>();
  private FlowInstance instance;

  /** FIFO queue of pending flow start requests. */
  private final Queue<QueuedStart> pending = new ArrayDeque<>();

  /** Preview context used when revalidating queued guards without instantiating a flow. */
  private boolean previewActive;

  private Map<String, String> previewParams = Map.of();

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
        clearRuntimeState(false);
        instance = null;
        drainQueue(serverLevel.getGameTime());
      }
      return;
    }
    drainQueue(serverLevel.getGameTime());
  }

  public boolean start(
      FlowProgram program,
      LivingEntity target,
      java.util.Map<String, String> flowParams,
      long gameTime) {
    return start(program, target, 1.0, flowParams, gameTime, null);
  }

  public boolean start(
      FlowProgram program,
      LivingEntity target,
      double timeScale,
      java.util.Map<String, String> flowParams,
      long gameTime) {
    return start(program, target, timeScale, flowParams, gameTime, null);
  }

  public boolean start(
      FlowProgram program,
      LivingEntity target,
      double timeScale,
      java.util.Map<String, String> flowParams,
      long gameTime,
      String sourceDescriptor) {
    if (program == null) {
      return false;
    }
    double safeTimeScale = Math.max(0.0D, timeScale);
    Map<String, String> safeParams =
        flowParams == null ? java.util.Map.of() : java.util.Map.copyOf(flowParams);
    String descriptor = sourceDescriptor == null ? program.id().toString() : sourceDescriptor;
    if (isRunning()) {
      if (!queueEnabled()) {
        ChestCavity.LOGGER.info(
            "[Flow] {} ignored new request for {} (source={}) because a flow is already running",
            performerName(),
            program.id(),
            descriptor);
        return false;
      }
      boolean enqueued = enqueue(program, target, safeTimeScale, safeParams, gameTime, descriptor);
      if (enqueued) {
        ChestCavity.LOGGER.info(
            "[Flow] {} enqueued {} (source={}, queueSize={}/{})",
            performerName(),
            program.id(),
            descriptor,
            pending.size(),
            maxQueueLength());
      }
      return enqueued;
    }
    boolean started =
        startInternal(program, target, safeTimeScale, safeParams, gameTime, descriptor);
    if (started) {
      ChestCavity.LOGGER.info(
          "[Flow] {} accepted and started {} (source={}, timeScale={}, params={})",
          performerName(),
          program.id(),
          descriptor,
          formatDouble(safeTimeScale),
          safeParams.isEmpty() ? "{}" : safeParams);
    }
    return started;
  }

  public void handleInput(FlowInput input, long gameTime) {
    if (instance == null || input == null) {
      return;
    }
    instance.handleInput(input, gameTime);
  }

  public boolean requestCancel(String reason, long gameTime) {
    if (instance == null) {
      ChestCavity.LOGGER.debug(
          "[Flow] Cancel request ignored because no instance is running (reason={})", reason);
      return false;
    }
    String sanitized = (reason == null || reason.isBlank()) ? "unspecified" : reason;
    ChestCavity.LOGGER.debug(
        "[Flow] {} received cancel request (program={}, state={}, ticks={}, reason={})",
        performerName(),
        instance.program().id(),
        instance.state(),
        instance.ticksInState(),
        sanitized);
    instance.handleInput(FlowInput.CANCEL, gameTime);
    return true;
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
    if (instance != null) {
      return instance.resolveParam(key).orElse(null);
    }
    if (previewActive && key != null) {
      return previewParams.get(key);
    }
    return null;
  }

  public double resolveFlowParamAsDouble(String key, double defaultValue) {
    if (instance != null) {
      return instance.resolveParamAsDouble(key, defaultValue);
    }
    if (previewActive && key != null) {
      String raw = previewParams.get(key);
      if (raw != null) {
        try {
          double parsed = Double.parseDouble(raw);
          if (!Double.isNaN(parsed) && !Double.isInfinite(parsed)) {
            return parsed;
          }
        } catch (Exception ignored) {
        }
      }
    }
    return defaultValue;
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

  /**
   * @return {@code true} when at least one queued start is waiting to be executed.
   */
  public boolean hasPending() {
    return !pending.isEmpty();
  }

  /** Visible for command feedback and testing to assert queue length. */
  public int pendingSize() {
    return pending.size();
  }

  /**
   * Attempt to start the next eligible flow in the queue.
   *
   * @param gameTime current game time used for guard validation and logging
   */
  void drainQueue(long gameTime) {
    if (instance != null || pending.isEmpty()) {
      return;
    }
    if (!queueEnabled()) {
      pending.clear();
      return;
    }
    int safetyCounter = pending.size();
    while (instance == null && !pending.isEmpty() && safetyCounter-- >= 0) {
      QueuedStart queued = pending.poll();
      if (queued == null) {
        continue;
      }
      if (shouldRevalidateQueuedGuards() && !canStartQueued(queued, gameTime)) {
        int limit = guardRetryLimit();
        queued.guardFailures++;
        boolean retry = queued.guardFailures <= limit;
        ChestCavity.LOGGER.info(
            "[Flow] {} dequeued {} but guards failed (source={}, attempt={}, retry={})",
            performerName(),
            queued.program.id(),
            queued.sourceDescriptor,
            queued.guardFailures,
            retry);
        if (retry) {
          pending.add(queued);
        }
        if (retry) {
          break;
        }
        continue;
      }
      boolean started =
          startInternal(
              queued.program,
              queued.target,
              queued.timeScale,
              queued.flowParams,
              gameTime,
              queued.sourceDescriptor);
      if (started) {
        ChestCavity.LOGGER.info(
            "[Flow] {} dequeued and started {} (source={}, queueSize={} remaining)",
            performerName(),
            queued.program.id(),
            queued.sourceDescriptor,
            pending.size());
      }
    }
    if (safetyCounter < 0 && instance == null && !pending.isEmpty()) {
      ChestCavity.LOGGER.warn(
          "[Flow] {} queue drain aborted due to safety limit (queueSize={})",
          performerName(),
          pending.size());
    }
  }

  void shutdown() {
    clearRuntimeState(true);
    pending.clear();
    instance = null;
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

  @SuppressWarnings("unused")
  private void clearRuntimeState() {
    clearRuntimeState(true);
  }

  private void clearRuntimeState(boolean cancelScheduled) {
    longVariables.clear();
    doubleVariables.clear();
    if (cancelScheduled) {
      scheduledTasks.clear();
    }
    resetPreview();
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

  private boolean startInternal(
      FlowProgram program,
      LivingEntity target,
      double timeScale,
      Map<String, String> flowParams,
      long gameTime,
      String sourceDescriptor) {
    clearRuntimeState(true);
    instance = new FlowInstance(program, performer, target, this, timeScale, flowParams, gameTime);
    FlowSyncDispatcher.syncState(performer, instance);
    if (!instance.attemptStart(gameTime)) {
      ChestCavity.LOGGER.debug(
          "[Flow] Flow {} stayed in {} after start trigger (source={})",
          program.id(),
          instance.state(),
          sourceDescriptor);
    }
    return true;
  }

  private boolean enqueue(
      FlowProgram program,
      LivingEntity target,
      double timeScale,
      Map<String, String> flowParams,
      long gameTime,
      String sourceDescriptor) {
    if (pending.size() >= maxQueueLength()) {
      ChestCavity.LOGGER.info(
          "[Flow] {} dropped {} (source={}) because the queue is full ({})",
          performerName(),
          program.id(),
          sourceDescriptor,
          maxQueueLength());
      return false;
    }
    pending.add(
        new QueuedStart(program, target, timeScale, flowParams, gameTime, sourceDescriptor));
    return true;
  }

  private boolean canStartQueued(QueuedStart queued, long gameTime) {
    var definitionOpt = queued.program.definition(queued.program.initialState());
    if (definitionOpt.isEmpty()) {
      return true;
    }
    List<FlowTransition> transitions = definitionOpt.get().transitionsFor(FlowTrigger.START);
    if (transitions.isEmpty()) {
      return true;
    }
    return withPreview(
        queued.flowParams,
        () -> {
          for (FlowTransition transition : transitions) {
            if (!guardsPass(transition, queued.target, gameTime)) {
              continue;
            }
            return true;
          }
          return false;
        });
  }

  private boolean guardsPass(FlowTransition transition, LivingEntity target, long gameTime) {
    for (FlowGuard guard : transition.guards()) {
      try {
        if (!guard.test(performer, target, this, gameTime)) {
          return false;
        }
      } catch (Exception ex) {
        ChestCavity.LOGGER.error(
            "[Flow] Guard {} threw while validating queued start for performer {}",
            guard.describe(),
            performerName(),
            ex);
        return false;
      }
    }
    return true;
  }

  /**
   * Executes {@code task} while exposing the provided parameters as the current flow context. This
   * allows guard evaluation without constructing a {@link FlowInstance}.
   */
  private <T> T withPreview(Map<String, String> params, Supplier<T> task) {
    boolean previous = previewActive;
    Map<String, String> previousParams = previewParams;
    previewActive = true;
    previewParams = params == null ? Map.of() : params;
    try {
      return task.get();
    } finally {
      previewActive = previous;
      previewParams = previousParams;
    }
  }

  private void resetPreview() {
    previewActive = false;
    previewParams = Map.of();
  }

  private boolean queueEnabled() {
    CCConfig config = ChestCavity.config;
    return config != null
        && config.GUSCRIPT_EXECUTION != null
        && config.GUSCRIPT_EXECUTION.enableFlowQueue;
  }

  private int maxQueueLength() {
    CCConfig config = ChestCavity.config;
    if (config == null || config.GUSCRIPT_EXECUTION == null) {
      return 4;
    }
    return Math.max(1, config.GUSCRIPT_EXECUTION.maxFlowQueueLength);
  }

  private boolean shouldRevalidateQueuedGuards() {
    CCConfig config = ChestCavity.config;
    return config == null
        || config.GUSCRIPT_EXECUTION == null
        || config.GUSCRIPT_EXECUTION.revalidateQueuedGuards;
  }

  private int guardRetryLimit() {
    CCConfig config = ChestCavity.config;
    if (config == null || config.GUSCRIPT_EXECUTION == null) {
      return 0;
    }
    return Math.max(0, config.GUSCRIPT_EXECUTION.queuedGuardRetryLimit);
  }

  private String performerName() {
    return performer != null ? performer.getGameProfile().getName() : "<unbound>";
  }

  private static String formatDouble(double value) {
    return String.format(java.util.Locale.ROOT, "%.3f", value);
  }

  /** Immutable description of a queued flow start request. */
  private static final class QueuedStart {
    private final FlowProgram program;
    private final LivingEntity target;
    private final double timeScale;
    private final Map<String, String> flowParams;
    private final long requestedAtGameTime;
    private final String sourceDescriptor;
    private int guardFailures;

    private QueuedStart(
        FlowProgram program,
        LivingEntity target,
        double timeScale,
        Map<String, String> flowParams,
        long requestedAtGameTime,
        String sourceDescriptor) {
      this.program = Objects.requireNonNull(program, "program");
      this.target = target;
      this.timeScale = Math.max(0.0D, timeScale);
      this.flowParams = flowParams == null ? Map.of() : Map.copyOf(flowParams);
      this.requestedAtGameTime = requestedAtGameTime;
      this.sourceDescriptor = sourceDescriptor == null ? program.id().toString() : sourceDescriptor;
      this.guardFailures = 0;
    }
  }
}
