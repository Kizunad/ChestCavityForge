package net.tigereye.chestcavity.soul.navigation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.util.SoulLog;

/**
 * Virtual Navigation facade: keeps external API stable while switching from a spawned guide Mob to
 * an in-memory DummyMob that never enters the world.
 */
public final class SoulNavigationMirror {

  private SoulNavigationMirror() {}

  public enum GoalPriority {
    CRITICAL(3),
    HIGH(2),
    NORMAL(1),
    LOW(0);

    private final int weight;

    GoalPriority(int weight) {
      this.weight = weight;
    }

    boolean isHigherThan(GoalPriority other) {
      return this.weight > other.weight;
    }
  }

  private record Goal(Vec3 target, double speed, double stopDistance, GoalPriority priority) {}

  private static final Map<UUID, ISoulNavigator> NAVS = new HashMap<>();
  private static final Map<UUID, Goal> GOALS = new HashMap<>();
  // Reduce navigation log noise unless explicitly enabled via JVM property
  private static final boolean NAV_LOGS = Boolean.getBoolean("chestcavity.debugNav");

  public static void setGoal(SoulPlayer soul, Vec3 target, double speed, double stopDistance) {
    setGoal(soul, target, speed, stopDistance, GoalPriority.NORMAL);
  }

  public static void setGoal(
      SoulPlayer soul, Vec3 target, double speed, double stopDistance, GoalPriority priority) {
    ISoulNavigator nav = ensureNavigator(soul);
    if (nav == null) return;
    UUID id = soul.getUUID();
    Goal prev = GOALS.get(id);
    if (prev != null && prev.priority().isHigherThan(priority)) {
      if (NAV_LOGS) {
        SoulLog.info(
            "[soul][nav] skip_goal soul={} lower_priority target=({}, {}, {}) speed={} stop={} priority={} currentPriority={}",
            id,
            target.x,
            target.y,
            target.z,
            speed,
            stopDistance,
            priority,
            prev.priority());
      }
      return;
    }
    nav.setGoal(soul, target, speed, stopDistance);
    if (soul.level() instanceof ServerLevel level) {
      // 调试粒子：标记当前导航目标（抬高至地表之上以避免埋入方块）
      BlockPos targetPos = BlockPos.containing(target);
      double surface =
          level.getHeight(
                  Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetPos.getX(), targetPos.getZ())
              + 0.25;
      double particleY = Math.max(target.y + 0.25, surface);
      level.sendParticles(
          ParticleTypes.GLOW, target.x, particleY, target.z, 6, 0.2, 0.2, 0.2, 0.01);
    }
    GOALS.put(id, new Goal(target, speed, stopDistance, priority));
    if (NAV_LOGS) {
      boolean changed = true;
      if (prev != null) {
        Vec3 prevTarget = prev.target();
        double dx = prevTarget.x - target.x;
        double dy = prevTarget.y - target.y;
        double dz = prevTarget.z - target.z;
        double dist2 = dx * dx + dy * dy + dz * dz;
        changed =
            dist2 > 0.25
                || Math.abs(prev.speed() - speed) > 1e-6
                || Math.abs(prev.stopDistance() - stopDistance) > 1e-6;
      }}
  }

  public static void clearGoal(SoulPlayer soul) {
    Goal g = GOALS.remove(soul.getUUID());
    ISoulNavigator nav = NAVS.get(soul.getUUID());
    if (nav != null) nav.clearGoal();
    if (g != null && NAV_LOGS) {
      SoulLog.info("[soul][nav] clearGoal soul={}", soul.getUUID());
    }
  }

  public static void serverTick(MinecraftServer server) {
    // Iterate over a stable snapshot of ids to avoid CME when clearing
    var ids = new java.util.ArrayList<>(NAVS.keySet());
    for (UUID entityId : ids) {
      SoulPlayer soul = null;
      for (ServerLevel lvl : server.getAllLevels()) {
        var e = lvl.getEntity(entityId);
        if (e instanceof SoulPlayer sp) {
          soul = sp;
          break;
        }
      }
      if (soul == null) {
        NAVS.remove(entityId);
        GOALS.remove(entityId);
        continue;
      }
      ISoulNavigator nav = NAVS.get(entityId);
      if (nav == null) continue;
      nav.tick(soul);
      // Navigator may clear its goal internally; external goal map is advisory only
    }
  }

  public static void onSoulRemoved(UUID soulId) {
    NAVS.remove(soulId);
    GOALS.remove(soulId);
  }

  /** Legacy per-soul tick used by existing AI handlers. */
  public static void tick(SoulPlayer soul) {
    ISoulNavigator nav = NAVS.get(soul.getUUID());
    if (nav != null) {
      nav.tick(soul);
    }
  }

  public static void clearAll() {
    NAVS.clear();
    GOALS.clear();
  }

  private static ISoulNavigator ensureNavigator(SoulPlayer soul) {
    UUID id = soul.getUUID();
    ISoulNavigator existing = NAVS.get(id);
    if (existing != null) return existing;
    if (!(soul.level() instanceof ServerLevel level)) return null;
    ISoulNavigator created = new BaritoneSoulNavigator(level);
    NAVS.put(id, created);
    return created;
  }

  /** 返回调试行（若为 Baritone 引擎且存在状态）。 */
  public static String debugLine(SoulPlayer soul) {
    ISoulNavigator nav = NAVS.get(soul.getUUID());
    if (nav instanceof BaritoneSoulNavigator b) {
      return b.debugLine();
    }
    return "engine=baritone, planning=false";
  }
}
