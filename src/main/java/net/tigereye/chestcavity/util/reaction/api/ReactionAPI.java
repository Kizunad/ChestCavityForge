package net.tigereye.chestcavity.util.reaction.api;

import java.util.Objects;

/** Reaction 对外统一入口，提供可替换的 {@link ReactionService} 实现。 */
public final class ReactionAPI {
  private static volatile ReactionService SERVICE = null;

  private ReactionAPI() {}

  public static ReactionService get() {
    return Objects.requireNonNull(SERVICE, "Reaction service not initialized");
  }

  public static void set(ReactionService service) {
    SERVICE = Objects.requireNonNull(service);
  }
}
