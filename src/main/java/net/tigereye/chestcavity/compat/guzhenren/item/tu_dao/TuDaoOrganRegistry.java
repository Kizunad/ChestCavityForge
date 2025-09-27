package net.tigereye.chestcavity.compat.guzhenren.item.tu_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.tu_dao.behavior.ShiPiGuOrganBehavior;
import net.tigereye.chestcavity.linkage.effect.GuzhenrenLinkageEffectRegistry;

/**
 * Declarative registration for 土道蛊 (Tu Dao) organ behaviours.
 */
public final class TuDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";

    private static final ResourceLocation SHI_PI_GU_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "shi_pi_gu");

    static {
        GuzhenrenLinkageEffectRegistry.registerSingle(SHI_PI_GU_ID, context -> {
            context.addSlowTickListener(ShiPiGuOrganBehavior.INSTANCE);
            context.addIncomingDamageListener(ShiPiGuOrganBehavior.INSTANCE);
            ShiPiGuOrganBehavior.INSTANCE.ensureAttached(context.chestCavity());
        });
    }

    private TuDaoOrganRegistry() {
    }

    /** Forces static initialisation to occur. */
    public static void bootstrap() {
        // no-op
    }
}
