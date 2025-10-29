package net.tigereye.chestcavity.soul.navigation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.fakeplayer.service.SoulIdentityViews;
import net.tigereye.chestcavity.soul.util.SoulLog;

/**
 * Lightweight driver that exercises {@link SoulGoalPlanner#collectEntityGoals} results by steering
 * a SoulPlayer through each collected goal sequentially. For every goal the soul will pathfind
 * towards the target, perform a jump once it reaches the stop distance, wait a short delay, and
 * continue with the next goal until exhausted.
 *
 * <p>This helper is intentionally isolated to debug commands so it can be removed or replaced
 * without affecting the production navigation loop.
 */
public final class SoulNavigationTestHarness {

  private SoulNavigationTestHarness() {}

  private static final Map<UUID, Task> TASKS = new HashMap<>();

  // 允许在目标停止距离之外额外的到达余量（单位：方块）。
  // 可通过 -Dchestcavity.soul.testArrivalSlack=1.25 调整，默认 0.9 格。
  private static double arrivalSlackBlocks() {
    String v = System.getProperty("chestcavity.soul.testArrivalSlack");
    if (v == null) {
      return 0.9;
    }
    try {
      double d = Double.parseDouble(v);
      if (d < 0) {
        return 0.0;
      }
      if (d > 4) {
        return 4.0;
      }
      return d;
    } catch (NumberFormatException e) {
      return 0.9;
    }
  }

  /**
   * Queues a new test run for the given SoulPlayer. Any previous run for the same soul will be
   * replaced.
   *
   * @param soul active SoulPlayer instance
   * @param goals ordered navigation goals to visit
   * @return true when the run was scheduled
   */
  public static boolean schedule(SoulPlayer soul, List<SoulGoalPlanner.NavigationGoal> goals) {
    if (soul == null || goals == null || goals.isEmpty()) {
      return false;
    }
    UUID id = soul.getUUID();
    SoulNavigationMirror.clearGoal(soul);
    TASKS.put(id, new Task(id, goals));
    if (SoulLog.DEBUG_LOGS) {
      SoulLog.info(
          "[soul][test] Scheduled TestCollectEntityGoals soul={} ({}) goals={}",
          id,
          displayName(soul),
          goals.size());
    }
    return true;
  }

  /** Advances all running test runs. Called once per server tick. */
  public static void tick(MinecraftServer server) {
    if (TASKS.isEmpty()) {
      return;
    }
    Iterator<Map.Entry<UUID, Task>> it = TASKS.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<UUID, Task> entry = it.next();
      Task task = entry.getValue();
      SoulPlayer soul = task.resolve(server);
      if (soul == null) {
        if (SoulLog.DEBUG_LOGS) {
          SoulLog.info(
              "[soul][test] Aborting TestCollectEntityGoals for missing soul {}", entry.getKey());
        }
        it.remove();
        continue;
      }
      task.tick(soul);
      if (task.isDone()) {
        SoulNavigationMirror.clearGoal(soul);
        it.remove();
        if (SoulLog.DEBUG_LOGS) {
          SoulLog.info(
              "[soul][test] Completed TestCollectEntityGoals soul={} ({})",
              soul.getUUID(),
              displayName(soul));
        }
      }
    }
  }

  /** Stops and removes the test run for the specified soul, if any. */
  public static void cancel(UUID soulId) {
    TASKS.remove(soulId);
  }

  private static String displayName(SoulPlayer soul) {
    if (soul == null) {
      return "#????????";
    }
    ServerPlayer owner =
        soul.getOwnerId()
            .map(id -> soul.serverLevel().getServer().getPlayerList().getPlayer(id))
            .orElse(null);
    return SoulIdentityViews.resolveDisplayName(owner, soul.getSoulId());
  }

  private static final class Task {
    private final UUID soulId;
    private final List<SoulGoalPlanner.NavigationGoal> goals;

    private int goalIndex = -1;
    private Vec3 currentTarget;
    private double currentStop;
    private double currentSpeed;
    private int waitTicks;
    private int stuckTicks;
    private double lastDist2 = Double.MAX_VALUE;
    private Phase phase = Phase.IDLE;

    private Task(UUID soulId, List<SoulGoalPlanner.NavigationGoal> goals) {
      this.soulId = soulId;
      this.goals = List.copyOf(new ArrayList<>(goals));
    }

    private SoulPlayer resolve(MinecraftServer server) {
      for (ServerLevel level : server.getAllLevels()) {
        var entity = level.getEntity(this.soulId);
        if (entity instanceof SoulPlayer soul) {
          return soul;
        }
      }
      return null;
    }

    private void tick(SoulPlayer soul) {
      switch (phase) {
        case IDLE -> startNext(soul);
        case MOVING -> tickMoving(soul);
        case WAITING_AFTER_JUMP -> tickWait();
        case FINISHED -> {
          // no-op
        }
      }
    }

    private void startNext(SoulPlayer soul) {
      goalIndex++;
      if (goalIndex >= goals.size()) {
        phase = Phase.FINISHED;
        return;
      }
      SoulGoalPlanner.NavigationGoal goal = goals.get(goalIndex);
      this.currentTarget = goal.goal();
      this.currentStop = Math.max(0.0, goal.stopDistance());
      this.currentSpeed = goal.speed();
      this.waitTicks = 0;
      this.stuckTicks = 0;
      this.lastDist2 = Double.MAX_VALUE;
      this.phase = Phase.MOVING;
      SoulNavigationMirror.setGoal(soul, this.currentTarget, this.currentSpeed, this.currentStop);
      if (SoulLog.DEBUG_LOGS) {
        SoulLog.info(
            "[soul][test] -> goal {} / {} target=({}, {}, {}) stop={} speed={} soul={} ({})",
            goalIndex + 1,
            goals.size(),
            String.format(Locale.ROOT, "%.2f", currentTarget.x),
            String.format(Locale.ROOT, "%.2f", currentTarget.y),
            String.format(Locale.ROOT, "%.2f", currentTarget.z),
            String.format(Locale.ROOT, "%.2f", currentStop),
            String.format(Locale.ROOT, "%.2f", currentSpeed),
            soul.getUUID(),
            displayName(soul));
      }
    }

    private void tickMoving(SoulPlayer soul) {
      if (currentTarget == null) {
        startNext(soul);
        return;
      }
      SoulGoalPlanner.NavigationGoal navGoal = goals.get(goalIndex);
      LivingEntity liveTarget = navGoal.target();
      if (liveTarget != null && liveTarget.isAlive()) {
        this.currentTarget = liveTarget.position();
      }
      SoulNavigationMirror.setGoal(soul, this.currentTarget, this.currentSpeed, this.currentStop);
      double dist2 = soul.position().distanceToSqr(currentTarget);
      // 结合实体体积与可配置余量，放宽到达判定，避免因碰撞/台阶等原因卡住
      double slack = Math.max(arrivalSlackBlocks(), 0.5 * soul.getBbWidth());
      double reach = Math.max(currentStop, 0.0) + slack;
      double threshold = reach * reach;
      if (dist2 <= threshold) {
        SoulNavigationMirror.clearGoal(soul);
        attemptJump(soul);
        phase = Phase.WAITING_AFTER_JUMP;
        waitTicks = 10;
        return;
      }
      if (dist2 + 0.05 < lastDist2) {
        lastDist2 = dist2;
        stuckTicks = 0;
      } else {
        stuckTicks++;
      }
      if (stuckTicks > 200) {
        if (SoulLog.DEBUG_LOGS) {
          SoulLog.info(
              "[soul][test] goal {} stuck after {} ticks, skipping soul={} ({})",
              goalIndex + 1,
              stuckTicks,
              this.soulId,
              displayName(soul));
        }
        SoulNavigationMirror.clearGoal(soul);
        phase = Phase.WAITING_AFTER_JUMP;
        waitTicks = 5;
      }
    }

    private void tickWait() {
      if (waitTicks > 0) {
        waitTicks--;
        return;
      }
      phase = Phase.IDLE;
    }

    private void attemptJump(SoulPlayer soul) {
      try {
        soul.forceJump();
      } catch (Throwable t) {
        Vec3 motion = soul.getDeltaMovement();
        soul.setDeltaMovement(motion.x, Math.max(motion.y, 0.42D), motion.z);
      }
    }

    private boolean isDone() {
      return phase == Phase.FINISHED;
    }

    private enum Phase {
      IDLE,
      MOVING,
      WAITING_AFTER_JUMP,
      FINISHED
    }
  }
}
