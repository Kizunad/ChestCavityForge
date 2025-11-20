package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.rarity;

import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.soul.SoulRarity;

/**
 * Interface describing how a particular soul rarity maps to numeric bonuses.
 */
public interface HunDaoSoulRarityProfile {

  /** @return the rarity represented by this profile. */
  SoulRarity rarity();

  /**
   * Returns the bonuses for the given soul form.
   *
   * @param form human / soul beast (future forms may be added)
   * @return aggregated bonuses
   */
  HunDaoSoulRarityBonuses bonuses(HunDaoSoulForm form);
}
