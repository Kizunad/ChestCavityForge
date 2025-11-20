package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.soul;

/**
 * Represents the rarity of a soul (魂魄稀有度).
 *
 * <p>Phase 7.3: Originally introduced for client-side display. Moved to a shared package so both
 * runtime logic and the Modern UI panel can reference the same enum.
 */
public enum SoulRarity {
  /** Common rarity (普通). */
  COMMON("soul_rarity.chestcavity.common", "soul_rarity_desc.chestcavity.common"),

  /** Rare rarity (稀有). */
  RARE("soul_rarity.chestcavity.rare", "soul_rarity_desc.chestcavity.rare"),

  /** Epic rarity (史诗). */
  EPIC("soul_rarity.chestcavity.epic", "soul_rarity_desc.chestcavity.epic"),

  /** Legendary rarity (传说). */
  LEGENDARY("soul_rarity.chestcavity.legendary", "soul_rarity_desc.chestcavity.legendary"),

  /** Unidentified rarity (未鉴定). */
  UNIDENTIFIED(
      "soul_rarity.chestcavity.unidentified", "soul_rarity_desc.chestcavity.unidentified");

  private final String translationKey;
  private final String descriptionKey;

  SoulRarity(String translationKey, String descriptionKey) {
    this.translationKey = translationKey;
    this.descriptionKey = descriptionKey;
  }

  /**
   * Get the translation key for this soul rarity.
   *
   * @return the translation key (e.g., {@code "soul_rarity.chestcavity.common"})
   */
  public String getTranslationKey() {
    return translationKey;
  }

  /** Returns the localization key for the rarity description. */
  public String getDescriptionKey() {
    return descriptionKey;
  }

  /**
   * @deprecated Use {@link #getTranslationKey()} and translate on the client.
   */
  @Deprecated
  public String getDisplayName() {
    return translationKey;
  }
}
