package net.tigereye.chestcavity.compat.guzhenren.item.lei_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.lei_dao.behavior.DianLiuguOrganBehavior;
import net.tigereye.chestcavity.linkage.effect.GuzhenrenLinkageEffectRegistry;

/**
 * Declarative registry for 雷道（Lei Dao）organ behaviours.
 */
public final class LeiDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";

    private static final ResourceLocation DIANLIUGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "dianliugu");

    static {
        GuzhenrenLinkageEffectRegistry.registerSingle(DIANLIUGU_ID, context -> {
            context.addSlowTickListener(DianLiuguOrganBehavior.INSTANCE);
            context.addOnHitListener(DianLiuguOrganBehavior.INSTANCE);
            DianLiuguOrganBehavior.INSTANCE.ensureAttached(context.chestCavity());
        });
    }

    private LeiDaoOrganRegistry() {
    }

    /** Forces static initialisation to occur. */
    public static void bootstrap() {
        // no-op
    }
}
