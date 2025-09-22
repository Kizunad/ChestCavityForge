package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.GuQiangguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.GuzhuguOrganBehavior;

import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.HuGuguOrganBehavior;

import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.LuoXuanGuQiangguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.YuGuguOrganBehavior; // 你需要自己写对应行为
import net.tigereye.chestcavity.compat.guzhenren.linkage.effect.GuzhenrenLinkageEffectRegistry;

/**
 * Declarative registry for 骨道蛊 items. Each behaviour is registered through the
 * {@link GuzhenrenLinkageEffectRegistry} so that listener wiring stays consistent.
 */
public final class GuDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation BONE_BAMBOO_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "gu_zhu_gu");
    private static final ResourceLocation BONE_SPEAR_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "gu_qiang_gu");
    private static final ResourceLocation SPIRAL_BONE_SPEAR_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "luo_xuan_gu_qiang_gu");

    private static final ResourceLocation TIGER_BONE_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "hu_gu_gu");

    private static final ResourceLocation JADE_BONE_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "yu_gu_gu"); // 新增玉骨蛊

    static {
        GuDaoOrganEvents.register();

        GuzhenrenLinkageEffectRegistry.registerSingle(BONE_BAMBOO_ID, context -> {
            context.addSlowTickListener(GuzhuguOrganBehavior.INSTANCE);
            GuzhuguOrganBehavior.INSTANCE.ensureAttached(context.chestCavity());
        });

        GuzhenrenLinkageEffectRegistry.registerSingle(BONE_SPEAR_ID, context -> {
            context.addSlowTickListener(GuQiangguOrganBehavior.INSTANCE);
            context.addOnHitListener(GuQiangguOrganBehavior.INSTANCE);
            GuQiangguOrganBehavior.INSTANCE.ensureAttached(context.chestCavity());
        });

        GuzhenrenLinkageEffectRegistry.registerSingle(SPIRAL_BONE_SPEAR_ID, context -> {
            context.addSlowTickListener(LuoXuanGuQiangguOrganBehavior.INSTANCE);
            context.addOnHitListener(LuoXuanGuQiangguOrganBehavior.INSTANCE);
            LuoXuanGuQiangguOrganBehavior.INSTANCE.ensureAttached(context.chestCavity());
        });


        GuzhenrenLinkageEffectRegistry.registerSingle(TIGER_BONE_ID, context -> {
            context.addSlowTickListener(HuGuguOrganBehavior.INSTANCE);
            context.addIncomingDamageListener(HuGuguOrganBehavior.INSTANCE);
            HuGuguOrganBehavior.INSTANCE.ensureAttached(context.chestCavity());
        });

        GuzhenrenLinkageEffectRegistry.registerSingle(JADE_BONE_ID, context -> {
            context.addSlowTickListener(YuGuguOrganBehavior.INSTANCE);
            YuGuguOrganBehavior.INSTANCE.ensureAttached(context.chestCavity());
            YuGuguOrganBehavior.INSTANCE.onEquip(
                    context.chestCavity(),
                    context.sourceOrgan(),
                    context.staleRemovalContexts()
            );
        });
    }

    private GuDaoOrganRegistry() {
    }

    /** Forces class initialisation so the static registration block runs. */
    public static void bootstrap() {
        // no-op
    }
}
