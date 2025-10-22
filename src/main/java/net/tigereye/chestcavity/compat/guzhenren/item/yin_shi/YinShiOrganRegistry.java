package net.tigereye.chestcavity.compat.guzhenren.item.yin_shi;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.yin_shi.behavior.YinShiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Declarative registration for 隐石蛊。
 */
public final class YinShiOrganRegistry {

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "yin_shi_gu");

    private YinShiOrganRegistry() {
    }

    public static List<OrganIntegrationSpec> specs() {
        List<OrganIntegrationSpec> list = new ArrayList<>();
        try {
            list.add(OrganIntegrationSpec.builder(ORGAN_ID)
                    .addSlowTickListener(YinShiGuOrganBehavior.INSTANCE)
                    .addIncomingDamageListener(YinShiGuOrganBehavior.INSTANCE)
                    .addOnHitListener(YinShiGuOrganBehavior.INSTANCE)
                    .addRemovalListener(YinShiGuOrganBehavior.INSTANCE)
                    .ensureAttached(YinShiGuOrganBehavior.INSTANCE::ensureAttached)
                    .onEquip(YinShiGuOrganBehavior.INSTANCE::onEquip)
                    .build());
        } catch (Throwable t) {
            ChestCavity.LOGGER.warn("[compat/guzhenren][yin_shi] skip YinShiGu registration due to init error", t);
        }
        return list;
    }
}
