package net.tigereye.chestcavity.compat.guzhenren.item.xue_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.TiexueguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.XieyanguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.linkage.effect.GuzhenrenLinkageEffectRegistry;

/**
 * Registry wiring for 血道（Xue Dao） organs.
 */
public final class XueDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation TIE_XUE_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "tiexuegu");
    private static final ResourceLocation XIE_YAN_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "xie_yan_gu");

    static {
        GuzhenrenLinkageEffectRegistry.registerSingle(TIE_XUE_GU_ID, context -> {
            context.addSlowTickListener(TiexueguOrganBehavior.INSTANCE);
            context.addRemovalListener(TiexueguOrganBehavior.INSTANCE);
            TiexueguOrganBehavior.INSTANCE.ensureAttached(context.chestCavity());
            TiexueguOrganBehavior.INSTANCE.onEquip(
                    context.chestCavity(),
                    context.sourceOrgan(),
                    context.staleRemovalContexts()
            );
        });

        GuzhenrenLinkageEffectRegistry.registerSingle(XIE_YAN_GU_ID, context -> {
            context.addSlowTickListener(XieyanguOrganBehavior.INSTANCE);
            context.addOnHitListener(XieyanguOrganBehavior.INSTANCE);
            context.addRemovalListener(XieyanguOrganBehavior.INSTANCE);
            XieyanguOrganBehavior.INSTANCE.ensureAttached(context.chestCavity());
            XieyanguOrganBehavior.INSTANCE.onEquip(
                    context.chestCavity(),
                    context.sourceOrgan(),
                    context.staleRemovalContexts()
            );
        });
    }

    private XueDaoOrganRegistry() {
    }

    public static void bootstrap() {
        // no-op, forces static initialisation
    }
}
