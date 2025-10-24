package net.tigereye.chestcavity.soul.fakeplayer.actions.api;

import java.util.Objects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;

/** Execution context delivered to actions. Keep it lean and server-focused. */
public final class ActionContext {
  private final ServerLevel level;
  private final SoulPlayer soul;
  private final ServerPlayer owner;

  public ActionContext(ServerLevel level, SoulPlayer soul, ServerPlayer owner) {
    this.level = Objects.requireNonNull(level, "level");
    this.soul = Objects.requireNonNull(soul, "soul");
    this.owner = owner; // may be null if detached
  }

  public ServerLevel level() {
    return level;
  }

  public SoulPlayer soul() {
    return soul;
  }

  public ServerPlayer owner() {
    return owner;
  }
}
