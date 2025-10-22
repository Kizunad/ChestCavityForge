package net.tigereye.chestcavity.soul.fakeplayer.actions.state;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.fakeplayer.actions.api.*;
import net.tigereye.chestcavity.soul.fakeplayer.actions.scheduler.ActionScheduler;

/**
 * Minimal per-soul action registry supporting priorities and concurrency. Not persisted yet;
 * suitable as a runtime coordinator.
 */
public final class ActionStateManager {
  private static final Map<UUID, ActionStateManager> INSTANCES = new ConcurrentHashMap<>();

  public static ActionStateManager of(SoulPlayer soul) {
    return INSTANCES.computeIfAbsent(soul.getUUID(), id -> new ActionStateManager());
  }

  private final Map<ActionId, ActionRuntime> active = new ConcurrentHashMap<>();

  public Collection<ActionRuntime> active() {
    return active.values();
  }

  public boolean isActive(ActionId id) {
    return active.containsKey(id);
  }

  /** Cancel all exclusive (non-concurrent) actions before starting a new exclusive one. */
  private void preemptFor(
      Action newAction, ServerLevel level, SoulPlayer soul, ServerPlayer owner) {
    if (newAction.allowConcurrent()) return;
    for (var entry : new java.util.ArrayList<>(active.entrySet())) {
      ActionId aid = entry.getKey();
      ActionRuntime rt = entry.getValue();
      Action current =
          net.tigereye.chestcavity.soul.fakeplayer.actions.registry.ActionRegistry.find(aid);
      if (current != null && !current.allowConcurrent()) {
        try {
          current.cancel(new ActionContext(level, soul, owner));
        } catch (Throwable ignored) {
        }
        active.remove(aid);
      }
    }
  }

  public boolean tryStart(ServerLevel level, SoulPlayer soul, Action action, ServerPlayer owner) {
    if (isActive(action.id())) return false;
    ActionContext ctx = new ActionContext(level, soul, owner);
    if (!action.canRun(ctx)) return false;
    preemptFor(action, level, soul, owner);
    action.start(ctx);
    long now = level.getGameTime();
    long next = action.nextReadyAt(ctx, now);
    active.put(action.id(), new ActionRuntime(action.id(), now, next, "start"));
    ActionScheduler.schedule(
        level,
        soul,
        action.id(),
        (Runnable) () -> tick(level, soul, action, owner),
        (int) Math.max(0, next - now));
    return true;
  }

  public void cancel(ServerLevel level, SoulPlayer soul, Action action, ServerPlayer owner) {
    var rt = active.remove(action.id());
    if (rt == null) return;
    action.cancel(new ActionContext(level, soul, owner));
  }

  private void tick(ServerLevel level, SoulPlayer soul, Action action, ServerPlayer owner) {
    var rt = active.get(action.id());
    if (rt == null) return;
    ActionContext ctx = new ActionContext(level, soul, owner);
    ActionResult result = action.tick(ctx);
    if (result == ActionResult.RUNNING) {
      long now = level.getGameTime();
      long next = action.nextReadyAt(ctx, now);
      rt.nextReadyAt = next;
      ActionScheduler.schedule(
          level,
          soul,
          action.id(),
          (Runnable) () -> tick(level, soul, action, owner),
          (int) Math.max(0, next - now));
    } else {
      active.remove(action.id());
    }
  }
}
