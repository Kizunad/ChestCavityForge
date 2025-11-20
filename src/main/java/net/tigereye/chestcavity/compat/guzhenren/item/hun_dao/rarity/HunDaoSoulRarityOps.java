package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.rarity;

import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.soul.SoulRarity;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.storage.HunDaoSoulState;
import net.tigereye.chestcavity.registration.CCAttachments;

/**
 * Runtime helper for reading/updating soul rarity values and retrieving precomputed bonuses.
 */
public final class HunDaoSoulRarityOps {

  public static final HunDaoSoulRarityOps INSTANCE = new HunDaoSoulRarityOps();

  private HunDaoSoulRarityOps() {}

  /** Resolves the player's persisted rarity (defaults to {@link SoulRarity#UNIDENTIFIED}). */
  public SoulRarity getRarity(LivingEntity entity) {
    if (entity == null) {
      return SoulRarity.UNIDENTIFIED;
    }
    return CCAttachments.getExistingHunDaoSoulState(entity)
        .map(HunDaoSoulState::getSoulRarity)
        .orElse(SoulRarity.UNIDENTIFIED);
  }

  /** Persists a new rarity into the player's soul state. */
  public void setRarity(LivingEntity entity, SoulRarity rarity) {
    if (entity == null) {
      return;
    }
    HunDaoSoulState state = CCAttachments.getHunDaoSoulState(entity);
    state.setSoulRarity(rarity);
  }

  /** Convenience accessor for the active rarity profile. */
  public HunDaoSoulRarityProfile getProfile(LivingEntity entity) {
    return HunDaoSoulRarityProfiles.getProfile(getRarity(entity));
  }

  /** Returns numeric bonuses for the entity in the specified form. */
  public HunDaoSoulRarityBonuses getBonuses(LivingEntity entity, HunDaoSoulForm form) {
    return getProfile(entity).bonuses(form);
  }
}
