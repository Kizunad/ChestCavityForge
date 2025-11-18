package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client;

/**
 * Represents the state of a soul (魂魄状态).
 *
 * <p>Phase 7: Soul state enumeration for Modern UI panel display.
 */
public enum SoulState {
  /** Soul is active (活跃). */
  ACTIVE("Active"),

  /** Soul is resting (休眠). */
  REST("Rest"),

  /** Soul state is unknown (未知). */
  UNKNOWN("Unknown");

  private final String displayName;

  SoulState(String displayName) {
    this.displayName = displayName;
  }

  /**
   * Get the display name of this soul state.
   *
   * @return the display name (e.g., "Active", "Rest", "Unknown")
   */
  public String getDisplayName() {
    return displayName;
  }
}
