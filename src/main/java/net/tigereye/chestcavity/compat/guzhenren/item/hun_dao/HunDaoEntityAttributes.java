package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao;

import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.entity.HunDaoSoulAvatarEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.entity.HunDaoSoulAvatarWorldBossEntity;
import net.tigereye.chestcavity.registration.CCEntities;

/** Registers attribute sets for Hun Dao-specific entities. */
public final class HunDaoEntityAttributes {

  private HunDaoEntityAttributes() {}

  public static void onAttributeCreation(EntityAttributeCreationEvent event) {
    event.put(
        CCEntities.HUN_DAO_SOUL_AVATAR.get(),
        HunDaoSoulAvatarEntity.createMobAttributes().build());
    event.put(
        CCEntities.HUN_DAO_SOUL_AVATAR_BOSS.get(),
        HunDaoSoulAvatarWorldBossEntity.createAttributes().build());
  }
}
