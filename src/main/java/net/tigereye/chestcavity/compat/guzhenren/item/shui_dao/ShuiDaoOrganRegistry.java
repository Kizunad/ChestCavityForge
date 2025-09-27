package net.tigereye.chestcavity.compat.guzhenren.item.shui_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.behavior.LingXianguOrganBehavior;
import net.tigereye.chestcavity.linkage.effect.GuzhenrenLinkageEffectRegistry;

/**
 * Registry wiring for 水道（Shui Dao） organs.
 */
public final class ShuiDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";

    private static final ResourceLocation LING_XIAN_GU_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "ling_xian_gu");

    static {
        GuzhenrenLinkageEffectRegistry.registerSingle(LING_XIAN_GU_ID, context -> {
            context.addSlowTickListener(LingXianguOrganBehavior.INSTANCE);
            LingXianguOrganBehavior.INSTANCE.ensureAttached(context.chestCavity());
        });
    }

    private ShuiDaoOrganRegistry() {
    }

    /** Forces static initialisation. */
    public static void bootstrap() {
        // no-op
    }
}
