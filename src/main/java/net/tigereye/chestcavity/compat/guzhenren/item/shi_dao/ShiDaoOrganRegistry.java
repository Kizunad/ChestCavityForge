package net.tigereye.chestcavity.compat.guzhenren.item.shi_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.shi_dao.behavior.JiuChongOrganBehavior;
import net.tigereye.chestcavity.linkage.effect.GuzhenrenLinkageEffectRegistry;

/**
 * Registry wiring for 食道（Shi Dao） organs such as the 酒虫.
 */
public final class ShiDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation JIU_CHONG_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "jiu_chong");

    static {
        GuzhenrenLinkageEffectRegistry.registerSingle(JIU_CHONG_ID, context -> {
            context.addSlowTickListener(JiuChongOrganBehavior.INSTANCE);
            context.addOnHitListener(JiuChongOrganBehavior.INSTANCE);
            context.addIncomingDamageListener(JiuChongOrganBehavior.INSTANCE);
            JiuChongOrganBehavior.INSTANCE.ensureAttached(context.chestCavity());
        });
    }

    private ShiDaoOrganRegistry() {
    }

    public static void bootstrap() {
        // no-op, just triggers class initialisation
    }
}
