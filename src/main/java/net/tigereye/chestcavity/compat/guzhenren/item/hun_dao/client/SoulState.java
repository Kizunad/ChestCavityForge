package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client;

/**
 * Represents the state of a soul (魂魄状态).
 *
 * <p>Phase 7.3: Soul state enumeration for Modern UI panel display with i18n support.
 */
public enum SoulState {
  /** Soul is active (活跃). */
  ACTIVE("soul_state.chestcavity.active"),

  /** Soul is resting (休眠). */
  REST("soul_state.chestcavity.rest"),

  /** Soul state is unknown (未知). */
  UNKNOWN("soul_state.chestcavity.unknown");

  private final String translationKey;

  SoulState(String translationKey) {
    this.translationKey = translationKey;
  }

  /**
   * Get the translation key for this soul state.
   *
   * @return the translation key (e.g., "soul_state.chestcavity.active")
   */
  public String getTranslationKey() {
    return translationKey;
  }

  /**
   * Get the display name of this soul state (deprecated, use getTranslationKey() instead).
   *
   * @return the translation key
   * @deprecated Use {@link #getTranslationKey()} and translate on client side
   */
  @Deprecated
  public String getDisplayName() {
    return translationKey;
  }
}
