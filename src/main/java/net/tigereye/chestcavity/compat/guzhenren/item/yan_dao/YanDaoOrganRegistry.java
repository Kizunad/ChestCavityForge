package net.tigereye.chestcavity.compat.guzhenren.item.yan_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior.FenShenGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior.HuoYiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior.HuorenguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior.HuoxinguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.List;

/**
 * Declarative registration for炎道器官（火心蛊等）。
 */
public final class YanDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation HUOXINGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huoxingu");
    private static final ResourceLocation HUORENGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huorengu");
    private static final ResourceLocation HUO_YI_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huo_gu");
    private static final ResourceLocation FEN_SHEN_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "fen_shen_gu");

    private static final List<OrganIntegrationSpec> SPECS = List.of(
            OrganIntegrationSpec.builder(HUOXINGU_ID)
                    .addSlowTickListener(HuoxinguOrganBehavior.INSTANCE)
                    .build(),
            OrganIntegrationSpec.builder(FEN_SHEN_GU_ID)
                    .addSlowTickListener(FenShenGuOrganBehavior.INSTANCE)
                    .addIncomingDamageListener(FenShenGuOrganBehavior.INSTANCE)
                    .build(),
            OrganIntegrationSpec.builder(HUO_YI_GU_ID)
                    .addSlowTickListener(HuoYiGuOrganBehavior.INSTANCE)
                    .build(),
            OrganIntegrationSpec.builder(HUORENGU_ID)
                    .addSlowTickListener(HuorenguOrganBehavior.INSTANCE)
                    .addOnHitListener(HuorenguOrganBehavior.INSTANCE)
                    .addRemovalListener(HuorenguOrganBehavior.INSTANCE)
                    .ensureAttached(HuorenguOrganBehavior.INSTANCE::ensureAttached)
                    .onEquip(HuorenguOrganBehavior.INSTANCE::onEquip)
                    .build()
    );

    private YanDaoOrganRegistry() {}

    public static List<OrganIntegrationSpec> specs() {
        return SPECS;
    }
}
