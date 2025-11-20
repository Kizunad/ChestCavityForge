package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.rarity;

import java.util.Objects;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.soul.SoulRarity;

/** Default immutable implementation of {@link HunDaoSoulRarityProfile}. */
public record DefaultHunDaoSoulRarityProfile(
    SoulRarity rarity,
    HunDaoSoulRarityBonuses humanBonuses,
    HunDaoSoulRarityBonuses soulBeastBonuses)
    implements HunDaoSoulRarityProfile {

  public DefaultHunDaoSoulRarityProfile {
    Objects.requireNonNull(rarity, "rarity");
    Objects.requireNonNull(humanBonuses, "humanBonuses");
    Objects.requireNonNull(soulBeastBonuses, "soulBeastBonuses");
  }

  @Override
  public HunDaoSoulRarityBonuses bonuses(HunDaoSoulForm form) {
    return form == HunDaoSoulForm.SOUL_BEAST ? soulBeastBonuses : humanBonuses;
  }
}
