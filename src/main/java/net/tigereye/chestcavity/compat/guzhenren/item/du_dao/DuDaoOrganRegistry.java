package net.tigereye.chestcavity.compat.guzhenren.item.du_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.du_dao.behavior.ChouPiGuOrganBehavior;
import net.tigereye.chestcavity.linkage.effect.GuzhenrenLinkageEffectRegistry;

/**
 * Declarative registration for 毒道蛊 (Du Dao) organ behaviours.
 */
public final class DuDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";

    private static final ResourceLocation CHOU_PI_GU_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "chou_pi_gu");

    static {
        DuDaoOrganEvents.register();

        GuzhenrenLinkageEffectRegistry.registerSingle(CHOU_PI_GU_ID, context -> {
            context.addSlowTickListener(ChouPiGuOrganBehavior.INSTANCE);
            context.addIncomingDamageListener(ChouPiGuOrganBehavior.INSTANCE);
            ChouPiGuOrganBehavior.INSTANCE.ensureAttached(context.chestCavity());
        });
    }

    private DuDaoOrganRegistry() {
    }

    /** Forces static initialisation so the registration block executes. */
    public static void bootstrap() {
        // no-op
    }
}
