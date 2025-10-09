package net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior;

import net.minecraft.resources.ResourceLocation;

/**
 * Behaviour for 黑豕蛊（三转力道，肌肉）。
 */
public final class HeiShiGuOrganBehavior extends AbstractLiYingGuOrganBehavior {

    public static final HeiShiGuOrganBehavior INSTANCE = new HeiShiGuOrganBehavior();

    private static final String MOD_ID = "guzhenren";

    private HeiShiGuOrganBehavior() {
        super(
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "hei_shi_gu"),
                "HeiShiGu"
        );
    }
}
