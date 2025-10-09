package net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior;

import net.minecraft.resources.ResourceLocation;

/**
 * Behaviour for 白豕蛊（三转力道，肌肉）。
 */
public final class BaiShiGuOrganBehavior extends AbstractLiYingGuOrganBehavior {

    public static final BaiShiGuOrganBehavior INSTANCE = new BaiShiGuOrganBehavior();

    private static final String MOD_ID = "guzhenren";

    private BaiShiGuOrganBehavior() {
        super(
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "bai_shi_gu"),
                "BaiShiGu"
        );
    }
}
