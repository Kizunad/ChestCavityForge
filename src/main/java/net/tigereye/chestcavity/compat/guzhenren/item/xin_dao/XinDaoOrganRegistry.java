package net.tigereye.chestcavity.compat.guzhenren.item.xin_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.xin_dao.behavior.XingBanDianGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.List;

/**
 * 星道（Xin Dao）器官注册表。
 */
public final class XinDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";

    private static final ResourceLocation XING_BAN_DIAN_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "xing_ban_dian_gu");

    private static final List<OrganIntegrationSpec> SPECS = List.of(
            OrganIntegrationSpec.builder(XING_BAN_DIAN_GU_ID)
                    .addIncomingDamageListener(XingBanDianGuOrganBehavior.INSTANCE)
                    .build()
    );

    private XinDaoOrganRegistry() {}

    public static List<OrganIntegrationSpec> specs() {
        return SPECS;
    }
}

