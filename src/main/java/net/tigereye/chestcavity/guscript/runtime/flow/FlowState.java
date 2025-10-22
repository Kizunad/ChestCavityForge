package net.tigereye.chestcavity.guscript.runtime.flow;

import java.util.Locale;

/** Enumerates the canonical states a flow program can occupy. */
public enum FlowState {
  IDLE,
  CHARGING,
  CHARGED,
  RELEASING,
  COOLDOWN,
  CANCEL;

  public static FlowState fromName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Flow state name must not be blank");
    }
    return FlowState.valueOf(name.trim().toUpperCase(Locale.ROOT));
  }

  public String serializedName() {
    return name().toLowerCase(Locale.ROOT);
  }
}
