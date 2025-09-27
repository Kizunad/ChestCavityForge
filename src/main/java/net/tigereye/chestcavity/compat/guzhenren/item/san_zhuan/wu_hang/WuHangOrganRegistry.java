package net.tigereye.chestcavity.compat.guzhenren.item.san_zhuan.wu_hang;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.linkage.effect.GuzhenrenLinkageEffectRegistry;

/**
 * Declarative registration for the Wu Hang (五行蛊) organ behaviours.
 */
public final class WuHangOrganRegistry {

    private static final String MOD_ID = "guzhenren";

    private static final ResourceLocation HUOXINGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huoxingu");
    private static final ResourceLocation TUPIGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "tupigu");
    private static final ResourceLocation MUGANGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "mugangu");
    private static final ResourceLocation JINFEIGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "jinfeigu");
    private static final ResourceLocation SHUISHENGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "shuishengu");

    static {
        GuzhenrenLinkageEffectRegistry.registerSingle(HUOXINGU_ID, context ->
                context.addOnFireListener(HuoxinguOrganBehavior.INSTANCE)
        );

        GuzhenrenLinkageEffectRegistry.registerSingle(TUPIGU_ID, context -> {
            context.addOnGroundListener(TupiguOrganBehavior.INSTANCE);
            context.addSlowTickListener(TupiguOrganBehavior.INSTANCE);
        });

        GuzhenrenLinkageEffectRegistry.registerSingle(MUGANGU_ID, context ->
                context.addSlowTickListener(MuganguOrganBehavior.INSTANCE)
        );

        GuzhenrenLinkageEffectRegistry.registerSingle(JINFEIGU_ID, context ->
                context.addSlowTickListener(JinfeiguOrganBehavior.INSTANCE)
        );

        GuzhenrenLinkageEffectRegistry.registerSingle(SHUISHENGU_ID, context -> {
            context.addSlowTickListener(ShuishenguOrganBehavior.INSTANCE);
            context.addIncomingDamageListener(ShuishenguOrganBehavior.INSTANCE);
        });
    }

    private WuHangOrganRegistry() {
    }

    /** Forces static initialisation to occur. */
    public static void bootstrap() {
        // no-op
    }
}
