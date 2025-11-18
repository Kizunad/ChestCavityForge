package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client;

/**
 * Represents the rarity of a soul (魂魄稀有度).
 *
 * <p>Phase 7: Soul rarity enumeration for Modern UI panel display.
 */
public enum SoulRarity {
  /** Common rarity (普通). */
  COMMON("Common"),

  /** Rare rarity (稀有). */
  RARE("Rare"),

  /** Epic rarity (史诗). */
  EPIC("Epic"),

  /** Legendary rarity (传说). */
  LEGENDARY("Legendary"),

  /** Unidentified rarity (未鉴定). */
  UNIDENTIFIED("Unidentified");

  private final String displayName;

  SoulRarity(String displayName) {
    this.displayName = displayName;
  }

  /**
   * Get the display name of this soul rarity.
   *
   * @return the display name (e.g., "Common", "Rare", "Epic", "Legendary", "Unidentified")
   */
  public String getDisplayName() {
    return displayName;
  }
}
