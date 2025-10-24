package net.tigereye.chestcavity.compat.guzhenren.item.guang_dao;

import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.guang_dao.entity.XiaoGuangIllusionEntity;
import net.tigereye.chestcavity.registration.CCEntities;

/** Registers attribute sets for 光道相关实体。 */
public final class GuangDaoEntityAttributes {

  private GuangDaoEntityAttributes() {}

  public static void onAttributeCreation(EntityAttributeCreationEvent event) {
    event.put(
        CCEntities.XIAO_GUANG_ILLUSION.get(), XiaoGuangIllusionEntity.createAttributes().build());
  }
}
