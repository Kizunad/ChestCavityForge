package net.tigereye.chestcavity.compat.guzhenren.item.gu_cai.registry;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_cai.behavior.JianjitengOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.linkage.effect.GuzhenrenLinkageEffectRegistry;

/**
 * Declarative registry for 蛊材（Gu Cai）organ behaviours.
 */
public final class GuCaiOrganRegistry {

    private static final String MOD_ID = "guzhenren";

    private static final ResourceLocation JIANJITENG_BLOCK_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "jianjiteng");

    static {
        GuzhenrenLinkageEffectRegistry.registerSingle(JIANJITENG_BLOCK_ID, context -> {
            context.addSlowTickListener(JianjitengOrganBehavior.INSTANCE);
            JianjitengOrganBehavior.INSTANCE.ensureAttached(context.chestCavity());
        });
    }

    private GuCaiOrganRegistry() {
    }

    /** Forces static initialisation to occur. */
    public static void bootstrap() {
        // no-op
    }
}
