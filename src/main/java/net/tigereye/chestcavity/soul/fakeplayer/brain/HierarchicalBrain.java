package net.tigereye.chestcavity.soul.fakeplayer.brain;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.BrainSharedMemory;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrainContext;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrainMemory;

/**
 * A {@link Brain} implementation that drives one or more sub-brains in a fixed order. Each
 * sub-brain receives its own isolated memory store and participates based on its {@link
 * SubBrain#shouldTick(SubBrainContext)} gate.
 */
public class HierarchicalBrain implements Brain {

  private final String id;
  private final BrainMode mode;
  private final List<SubBrain> pipeline;

  private final Map<UUID, Map<String, SubBrainMemory>> memories = new ConcurrentHashMap<>();
  private final Map<UUID, Map<String, Boolean>> activeStates = new ConcurrentHashMap<>();
  private final Map<UUID, BrainSharedMemory> sharedMemories = new ConcurrentHashMap<>();

  public HierarchicalBrain(String id, BrainMode mode, List<SubBrain> pipeline) {
    this.id = id;
    this.mode = mode;
    this.pipeline = List.copyOf(pipeline);
  }

  @Override
  public String id() {
    return id;
  }

  @Override
  public BrainMode mode() {
    return mode;
  }

  @Override
  public void onEnter(BrainContext ctx) {
    UUID soulId = ctx.soul().getUUID();
    var mems = new ConcurrentHashMap<String, SubBrainMemory>();
    memories.put(soulId, mems);
    var states = new ConcurrentHashMap<String, Boolean>();
    activeStates.put(soulId, states);
    sharedMemories.put(soulId, new BrainSharedMemory());
    for (SubBrain sub : pipeline) {
      mems.put(sub.id(), new SubBrainMemory());
      states.put(sub.id(), Boolean.FALSE);
    }
  }

  @Override
  public void onExit(BrainContext ctx) {
    UUID soulId = ctx.soul().getUUID();
    var states = activeStates.remove(soulId);
    if (states != null) {
      for (SubBrain sub : pipeline) {
        if (Boolean.TRUE.equals(states.get(sub.id()))) {
          SubBrainContext subCtx = contextFor(ctx, sub);
          sub.onExit(subCtx);
        }
      }
    }
    var mems = memories.remove(soulId);
    if (mems != null) {
      mems.values().forEach(SubBrainMemory::clear);
    }
    var shared = sharedMemories.remove(soulId);
    if (shared != null) {
      shared.clear();
    }
  }

  @Override
  public void tick(BrainContext ctx) {
    UUID soulId = ctx.soul().getUUID();
    var states = activeStates.computeIfAbsent(soulId, unused -> new ConcurrentHashMap<>());
    for (SubBrain sub : pipeline) {
      SubBrainContext subCtx = contextFor(ctx, sub);
      boolean shouldTick = safeShouldTick(sub, subCtx);
      boolean wasActive = Boolean.TRUE.equals(states.get(sub.id()));
      if (shouldTick && !wasActive) {
        sub.onEnter(subCtx);
        states.put(sub.id(), Boolean.TRUE);
      } else if (!shouldTick && wasActive) {
        sub.onExit(subCtx);
        states.put(sub.id(), Boolean.FALSE);
        subCtx.memory().clear();
      }
      if (shouldTick) {
        sub.tick(subCtx);
      }
    }
  }

  private boolean safeShouldTick(SubBrain sub, SubBrainContext ctx) {
    try {
      return sub.shouldTick(ctx);
    } catch (RuntimeException ex) {
      // Defensive: prevent a bad sub-brain from crashing the orchestrator.
      net.tigereye.chestcavity.ChestCavity.LOGGER.error(
          "SubBrain {} failed shouldTick", sub.id(), ex);
      return false;
    }
  }

  private SubBrainContext contextFor(BrainContext ctx, SubBrain sub) {
    UUID soulId = ctx.soul().getUUID();
    var mems = memories.computeIfAbsent(soulId, unused -> new ConcurrentHashMap<>());
    var mem = mems.computeIfAbsent(sub.id(), unused -> new SubBrainMemory());
    var shared = sharedMemories.computeIfAbsent(soulId, unused -> new BrainSharedMemory());
    return new SubBrainContext(ctx, sub, mem, shared);
  }
}
