package net.tigereye.chestcavity.compat.guzhenren.item.mu_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.behavior.LiandaoGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.linkage.effect.GuzhenrenLinkageEffectRegistry;

/**
 * Registry wiring for 木道（Mu Dao） organs such as the 镰刀蛊.
 */
public final class MuDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation LIANDAO_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "liandaogu");

    static {
        GuzhenrenLinkageEffectRegistry.registerSingle(LIANDAO_GU_ID, context -> {
            context.addIncomingDamageListener(LiandaoGuOrganBehavior.INSTANCE);
            LiandaoGuOrganBehavior.INSTANCE.ensureAttached(context.chestCavity());
        });
    }

    private MuDaoOrganRegistry() {
    }

    public static void bootstrap() {
        // no-op, forces class initialisation
    }
}
