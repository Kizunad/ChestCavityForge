package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.rarity;

import java.util.Map;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.soul.SoulRarity;

/**
 * Static registry of default rarity profiles. Values can be swapped out in future phases (e.g.,
 * JSON loading) without touching callers.
 */
public final class HunDaoSoulRarityProfiles {

  private static final HunDaoSoulRarityBonuses UNIDENTIFIED_BONUS =
      new HunDaoSoulRarityBonuses(1.0D, 0.0D, 1.0D, 0.0D, 0.0D);

  private static final Map<SoulRarity, HunDaoSoulRarityProfile> PROFILES =
      Map.of(
          SoulRarity.UNIDENTIFIED,
          new DefaultHunDaoSoulRarityProfile(
              SoulRarity.UNIDENTIFIED, UNIDENTIFIED_BONUS, UNIDENTIFIED_BONUS),
          SoulRarity.COMMON,
          profile(
              SoulRarity.COMMON,
              1.02D,
              0.02D,
              1.02D,
              0.10D,
              20.0D),
          SoulRarity.RARE,
          profile(
              SoulRarity.RARE,
              1.05D,
              0.05D,
              1.05D,
              0.20D,
              50.0D),
          SoulRarity.EPIC,
          profile(
              SoulRarity.EPIC,
              1.08D,
              0.10D,
              1.08D,
              0.35D,
              75.0D),
          SoulRarity.LEGENDARY,
          profile(
              SoulRarity.LEGENDARY,
              1.12D,
              0.15D,
              1.12D,
              0.50D,
              100.0D));

  private HunDaoSoulRarityProfiles() {}

  private static HunDaoSoulRarityProfile profile(
      SoulRarity rarity,
      double move,
      double beastReduction,
      double attack,
      double regen,
      double maxBonus) {
    HunDaoSoulRarityBonuses human = new HunDaoSoulRarityBonuses(move, 0.0D, attack, regen, maxBonus);
    HunDaoSoulRarityBonuses beast =
        new HunDaoSoulRarityBonuses(move, beastReduction, attack, regen, 0.0D);
    return new DefaultHunDaoSoulRarityProfile(rarity, human, beast);
  }

  /** Returns the registered profile for a rarity (defaults to {@code UNIDENTIFIED}). */
  public static HunDaoSoulRarityProfile getProfile(SoulRarity rarity) {
    return PROFILES.getOrDefault(rarity, PROFILES.get(SoulRarity.UNIDENTIFIED));
  }
}
