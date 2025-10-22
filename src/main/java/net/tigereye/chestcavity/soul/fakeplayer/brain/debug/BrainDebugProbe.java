package net.tigereye.chestcavity.soul.fakeplayer.brain.debug;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;

/** Brain 调试探针：既支持简单事件分发，也缓存生存/探索的快照。 */
public final class BrainDebugProbe {

  // 事件分发
  private static final List<Consumer<BrainDebugEvent>> SINKS = new CopyOnWriteArrayList<>();

  // 快照缓存
  private static final Map<UUID, ExplorationTelemetry> EXPLORATION = new ConcurrentHashMap<>();
  private static final Map<UUID, SurvivalTelemetry> SURVIVAL = new ConcurrentHashMap<>();

  private BrainDebugProbe() {}

  // 事件分发 API
  public static void addSink(Consumer<BrainDebugEvent> sink) {
    if (sink != null) SINKS.add(sink);
  }

  public static void removeSink(Consumer<BrainDebugEvent> sink) {
    if (sink != null) SINKS.remove(sink);
  }

  public static void emit(BrainDebugEvent event) {
    if (event == null) return;
    BrainDebugLogger.trace(event.channel(), "%s %s", event.message(), event.attributes());
    for (Consumer<BrainDebugEvent> sink : SINKS) {
      try {
        sink.accept(event);
      } catch (Throwable t) {
        ChestCavity.LOGGER.error("[brain][debug] sink failure", t);
      }
    }
  }

  // 生存/探索快照 API
  public static void recordExploration(SoulPlayer soul, ExplorationTelemetry telemetry) {
    if (soul != null && telemetry != null) EXPLORATION.put(soul.getUUID(), telemetry);
  }

  public static ExplorationTelemetry lastExploration(UUID soulId) {
    return soulId == null ? null : EXPLORATION.get(soulId);
  }

  public static void recordSurvival(SoulPlayer soul, SurvivalTelemetry telemetry) {
    if (soul != null && telemetry != null) SURVIVAL.put(soul.getUUID(), telemetry);
  }

  public static SurvivalTelemetry lastSurvival(UUID soulId) {
    return soulId == null ? null : SURVIVAL.get(soulId);
  }

  public static void clear(UUID soulId) {
    if (soulId == null) return;
    EXPLORATION.remove(soulId);
    SURVIVAL.remove(soulId);
  }
}
