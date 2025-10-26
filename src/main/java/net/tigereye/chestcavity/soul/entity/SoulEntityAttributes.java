package net.tigereye.chestcavity.soul.entity;

import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.tigereye.chestcavity.registration.CCEntities;
import net.tigereye.chestcavity.soul.playerghost.PlayerGhostEntity;

/** 注册灵魂系自定义实体的属性。 */
public final class SoulEntityAttributes {

  private SoulEntityAttributes() {}

  public static void onAttributeCreation(EntityAttributeCreationEvent event) {
    event.put(CCEntities.SOUL_CLAN.get(), SoulClanEntity.createAttributes().build());
    event.put(CCEntities.TEST_SOUL.get(), TestSoulEntity.createAttributes().build());
    event.put(CCEntities.PLAYER_GHOST.get(), PlayerGhostEntity.createAttributes().build());
  }
}
