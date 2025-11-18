package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client;

/**
 * Represents the rarity of a soul (魂魄稀有度).
 *
 * <p>Phase 7.3: Soul rarity enumeration for Modern UI panel display with i18n support.
 */
public enum SoulRarity {
  /** Common rarity (普通). */
  COMMON("soul_rarity.chestcavity.common"),

  /** Rare rarity (稀有). */
  RARE("soul_rarity.chestcavity.rare"),

  /** Epic rarity (史诗). */
  EPIC("soul_rarity.chestcavity.epic"),

  /** Legendary rarity (传说). */
  LEGENDARY("soul_rarity.chestcavity.legendary"),

  /** Unidentified rarity (未鉴定). */
  UNIDENTIFIED("soul_rarity.chestcavity.unidentified");

  private final String translationKey;

  SoulRarity(String translationKey) {
    this.translationKey = translationKey;
  }

  /**
   * Get the translation key for this soul rarity.
   *
   * @return the translation key (e.g., "soul_rarity.chestcavity.common")
   */
  public String getTranslationKey() {
    return translationKey;
  }

  /**
   * Get the display name of this soul rarity (deprecated, use getTranslationKey() instead).
   *
   * @return the translation key
   * @deprecated Use {@link #getTranslationKey()} and translate on client side
   */
  @Deprecated
  public String getDisplayName() {
    return translationKey;
  }
}
