package net.tigereye.chestcavity.guscript.runtime.flow;

import java.util.Locale;

/** Triggers that can drive flow transitions. */
public enum FlowTrigger {
  START,
  AUTO,
  RELEASE,
  CANCEL;

  public static FlowTrigger fromName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Flow trigger name must not be blank");
    }
    return FlowTrigger.valueOf(name.trim().toUpperCase(Locale.ROOT));
  }

  public String serializedName() {
    return name().toLowerCase(Locale.ROOT);
  }
}
