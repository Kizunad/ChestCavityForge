package net.tigereye.chestcavity.compat.guzhenren.item.yan_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior.FenShenGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior.HuoYiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior.HuoLongGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior.HuoYouGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior.HuorenguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior.HuoTanGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior.HuoxinguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Declarative registration for炎道器官（火心蛊等）。
 */
public final class YanDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation HUOXINGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huoxingu");
    private static final ResourceLocation HUORENGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huorengu");
    private static final ResourceLocation HUO_YI_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huo_gu");
    private static final ResourceLocation HUO_YOU_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huo_you_gu");
    private static final ResourceLocation HUO_LONG_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huolonggu");
    private static final ResourceLocation DAN_QIAO_HUO_TAN_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "dan_qiao_huo_tan_gu");
    private static final ResourceLocation FEN_SHEN_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "fen_shen_gu");

    private YanDaoOrganRegistry() {}

    public static List<OrganIntegrationSpec> specs() {
        // Build lazily to avoid class-loading failures during <clinit>; guard each entry.
        List<OrganIntegrationSpec> list = new ArrayList<>();
        try {
            list.add(OrganIntegrationSpec.builder(HUOXINGU_ID)
                    .addSlowTickListener(HuoxinguOrganBehavior.INSTANCE)
                    .build());
        } catch (Throwable t) {
            ChestCavity.LOGGER.warn("[compat/guzhenren][yan_dao] skip Huoxingu registration due to init error", t);
        }
        try {
            list.add(OrganIntegrationSpec.builder(FEN_SHEN_GU_ID)
                    .addSlowTickListener(FenShenGuOrganBehavior.INSTANCE)
                    .addIncomingDamageListener(FenShenGuOrganBehavior.INSTANCE)
                    .addRemovalListener(FenShenGuOrganBehavior.INSTANCE)
                    .ensureAttached(FenShenGuOrganBehavior.INSTANCE::ensureAttached)
                    .onEquip(FenShenGuOrganBehavior.INSTANCE::onEquip)
                    .build());
        } catch (Throwable t) {
            ChestCavity.LOGGER.warn("[compat/guzhenren][yan_dao] skip FenShenGu registration due to init error", t);
        }
        try {
            list.add(OrganIntegrationSpec.builder(HUO_YI_GU_ID)
                    .addSlowTickListener(HuoYiGuOrganBehavior.INSTANCE)
                    .build());
        } catch (Throwable t) {
            ChestCavity.LOGGER.warn("[compat/guzhenren][yan_dao] skip HuoYiGu registration due to init error", t);
        }
        try {
            list.add(OrganIntegrationSpec.builder(DAN_QIAO_HUO_TAN_GU_ID)
                    .addOnHitListener(HuoTanGuOrganBehavior.INSTANCE)
                    .build());
        } catch (Throwable t) {
            ChestCavity.LOGGER.warn("[compat/guzhenren][yan_dao] skip HuoTanGu registration due to init error", t);
        }
        try {
            list.add(OrganIntegrationSpec.builder(HUO_YOU_GU_ID)
                    .addSlowTickListener(HuoYouGuOrganBehavior.INSTANCE)
                    .addOnHitListener(HuoYouGuOrganBehavior.INSTANCE)
                    .build());
        } catch (Throwable t) {
            ChestCavity.LOGGER.warn("[compat/guzhenren][yan_dao] skip HuoYouGu registration due to init error", t);
        }
        try {
            list.add(OrganIntegrationSpec.builder(HUORENGU_ID)
                    .addSlowTickListener(HuorenguOrganBehavior.INSTANCE)
                    .addOnHitListener(HuorenguOrganBehavior.INSTANCE)
                    .addRemovalListener(HuorenguOrganBehavior.INSTANCE)
                    .ensureAttached(HuorenguOrganBehavior.INSTANCE::ensureAttached)
                    .onEquip(HuorenguOrganBehavior.INSTANCE::onEquip)
                    .build());
        } catch (Throwable t) {
            ChestCavity.LOGGER.warn("[compat/guzhenren][yan_dao] skip Huorengu registration due to init error", t);
        }
        try {
            list.add(OrganIntegrationSpec.builder(HUO_LONG_GU_ID)
                    .addSlowTickListener(HuoLongGuOrganBehavior.INSTANCE)
                    .addOnHitListener(HuoLongGuOrganBehavior.INSTANCE)
                    .addIncomingDamageListener(HuoLongGuOrganBehavior.INSTANCE)
                    .addRemovalListener(HuoLongGuOrganBehavior.INSTANCE)
                    .build());
        } catch (Throwable t) {
            ChestCavity.LOGGER.warn("[compat/guzhenren][yan_dao] skip HuoLongGu registration due to init error", t);
        }
        return list;
    }
}
