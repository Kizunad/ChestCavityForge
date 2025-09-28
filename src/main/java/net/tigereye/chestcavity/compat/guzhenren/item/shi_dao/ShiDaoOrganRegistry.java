package net.tigereye.chestcavity.compat.guzhenren.item.shi_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.shi_dao.behavior.FanDaiCaoGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.shi_dao.behavior.JiuChongOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.List;

/**
 * Registry wiring for 食道（Shi Dao） organs such as the 酒虫.
 */
public final class ShiDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation JIU_CHONG_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "jiu_chong");
    private static final ResourceLocation FAN_DAI_CAO_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "fan_dai_cao_gu");

    private static final List<OrganIntegrationSpec> SPECS = List.of(
            OrganIntegrationSpec.builder(FAN_DAI_CAO_GU_ID)
                    .addSlowTickListener(FanDaiCaoGuOrganBehavior.INSTANCE)
                    .build(),
            OrganIntegrationSpec.builder(JIU_CHONG_ID)
                    .addSlowTickListener(JiuChongOrganBehavior.INSTANCE)
                    .addOnHitListener(JiuChongOrganBehavior.INSTANCE)
                    .addIncomingDamageListener(JiuChongOrganBehavior.INSTANCE)
                    .ensureAttached(JiuChongOrganBehavior.INSTANCE::ensureAttached)
                    .build()
    );

    private ShiDaoOrganRegistry() {
    }

    public static List<OrganIntegrationSpec> specs() {
        return SPECS;
    }
}
