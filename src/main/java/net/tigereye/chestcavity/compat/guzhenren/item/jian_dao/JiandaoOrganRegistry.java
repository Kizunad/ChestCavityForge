package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.JianYingGuOrganBehavior;
import net.tigereye.chestcavity.linkage.effect.GuzhenrenLinkageEffectRegistry;

/**
 * Declarative registry for sword-path organs.
 */
public final class JiandaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation JIAN_YING_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "jian_ying_gu");

    static {
        GuzhenrenLinkageEffectRegistry.registerSingle(JIAN_YING_GU_ID, context -> {
            context.addOnHitListener(JianYingGuOrganBehavior.INSTANCE);
            JianYingGuOrganBehavior.INSTANCE.ensureAttached(context.chestCavity());
        });
    }

    private JiandaoOrganRegistry() {
    }

    public static void bootstrap() {
        // Trigger static initialiser
    }
}

