package net.tigereye.chestcavity.engine;

import java.util.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 统一的服务器 Post tick 调度枢纽。 各功能模块可按优先级注册 {@link ServerTickEngine}，枢纽会在服务器 Post tick 时依序调用。
 *
 * <p>优先级数值按升序执行（数值越小越早）。
 */
public final class TickEngineHub {

  /** Reaction 系统默认优先级。 */
  public static final int PRIORITY_REACTION = 100;

  /** DoT 系统默认优先级。 */
  public static final int PRIORITY_DOT = 200;

  private TickEngineHub() {}

  private static final Object LOCK = new Object();
  private static final NavigableMap<Integer, List<ServerTickEngine>> ENGINES = new TreeMap<>();
  private static boolean registeredToBus = false;

  /**
   * 注册新的服务器 tick 引擎。
   *
   * @param priority 优先级（越小越早执行）
   * @param engine 引擎实例
   */
  public static void register(int priority, ServerTickEngine engine) {
    if (engine == null) {
      return;
    }
    synchronized (LOCK) {
      ensureBusHooked();
      ENGINES.computeIfAbsent(priority, k -> new ArrayList<>()).add(engine);
    }
  }

  private static void ensureBusHooked() {
    if (registeredToBus) {
      return;
    }
    NeoForge.EVENT_BUS.addListener(TickEngineHub::onServerTick);
    registeredToBus = true;
  }

  private static void onServerTick(ServerTickEvent.Post event) {
    List<ServerTickEngine> snapshot = buildSnapshot();
    if (snapshot.isEmpty()) {
      return;
    }
    for (ServerTickEngine engine : snapshot) {
      engine.onServerTick(event);
    }
  }

  private static List<ServerTickEngine> buildSnapshot() {
    synchronized (LOCK) {
      if (ENGINES.isEmpty()) {
        return List.of();
      }
      List<ServerTickEngine> snapshot = new ArrayList<>();
      for (Map.Entry<Integer, List<ServerTickEngine>> entry : ENGINES.entrySet()) {
        snapshot.addAll(entry.getValue());
      }
      return snapshot;
    }
  }
}
