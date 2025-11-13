package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao;

import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.PersistentGuCultivatorClone;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.SwordShadowClone;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.registration.CCEntities;

/**
 * Registers attribute sets for Jian Dao related entities. Note: This is a Mod-lifecycle listener
 * and must be registered via modEventBus.addListener in the mod constructor.
 */
public final class JiandaoEntityAttributes {

  private JiandaoEntityAttributes() {}

  public static void onAttributeCreation(EntityAttributeCreationEvent event) {
    event.put(CCEntities.SWORD_SHADOW_CLONE.get(), SwordShadowClone.createAttributes().build());
    event.put(CCEntities.FLYING_SWORD.get(), FlyingSwordEntity.createAttributes().build());
    // 专用派生类型（正道/人兽葬生）同样共享 FlyingSwordEntity 的基础属性集
    event.put(
        CCEntities.FLYING_SWORD_ZHENG_DAO.get(), FlyingSwordEntity.createAttributes().build());
    event.put(
        CCEntities.FLYING_SWORD_REN_SHOU_ZANG_SHENG.get(),
        FlyingSwordEntity.createAttributes().build());
    // 多重剑影蛊：持久化蛊修分身
    event.put(
        CCEntities.PERSISTENT_GU_CULTIVATOR_CLONE.get(),
        PersistentGuCultivatorClone.createAttributes().build());
  }
}
