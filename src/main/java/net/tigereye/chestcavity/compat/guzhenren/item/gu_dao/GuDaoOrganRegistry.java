package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.GuQiangguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.GuzhuguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.YuGuguOrganBehavior; // 你需要自己写对应行为
import net.tigereye.chestcavity.listeners.OrganOnHitContext;
import net.tigereye.chestcavity.listeners.OrganSlowTickContext;

/**
 * Registry for 骨道蛊 items.
 */
public final class GuDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation BONE_BAMBOO_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "gu_zhu_gu");
    private static final ResourceLocation BONE_SPEAR_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "gu_qiang_gu");
    private static final ResourceLocation JADE_BONE_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "yu_gu_gu"); // 新增玉骨蛊

    static {
        GuDaoOrganEvents.register();
    }

    private GuDaoOrganRegistry() {
    }

    public static boolean register(ChestCavityInstance cc, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return false;
        }

        if (itemId.equals(BONE_BAMBOO_ID)) {
            cc.onSlowTickListeners.add(new OrganSlowTickContext(stack, GuzhuguOrganBehavior.INSTANCE));
            GuzhuguOrganBehavior.INSTANCE.ensureAttached(cc);
            return true;
        }
        if (itemId.equals(BONE_SPEAR_ID)) {
            cc.onSlowTickListeners.add(new OrganSlowTickContext(stack, GuQiangguOrganBehavior.INSTANCE));
            cc.onHitListeners.add(new OrganOnHitContext(stack, GuQiangguOrganBehavior.INSTANCE));
            GuQiangguOrganBehavior.INSTANCE.ensureAttached(cc);
            return true;
        }
        if (itemId.equals(JADE_BONE_ID)) {
            cc.onSlowTickListeners.add(new OrganSlowTickContext(stack, YuGuguOrganBehavior.INSTANCE));
            YuGuguOrganBehavior.INSTANCE.ensureAttached(cc);
            return true;
        }

        return false;
    }
}
