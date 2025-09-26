package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao;

import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.SwordShadowClone;
import net.tigereye.chestcavity.registration.CCEntities;

/**
 * Registers attribute sets for Jian Dao related entities.
 * Note: This is a Mod-lifecycle listener and must be
 * registered via modEventBus.addListener in the mod constructor.
 */
public final class JiandaoEntityAttributes {

    private JiandaoEntityAttributes() {}

    public static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(CCEntities.SWORD_SHADOW_CLONE.get(), SwordShadowClone.createAttributes().build());
    }
}
