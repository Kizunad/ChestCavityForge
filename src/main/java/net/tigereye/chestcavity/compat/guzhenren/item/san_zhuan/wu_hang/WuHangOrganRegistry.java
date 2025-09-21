package net.tigereye.chestcavity.compat.guzhenren.item.san_zhuan.wu_hang;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageContext;
import net.tigereye.chestcavity.listeners.OrganOnFireContext;
import net.tigereye.chestcavity.listeners.OrganOnGroundContext;
import net.tigereye.chestcavity.listeners.OrganSlowTickContext;

/**
 * Centralised registration for the Wu Hang (五行蛊) organ behaviours.
 */
public final class WuHangOrganRegistry {

    private static final String MOD_ID = "guzhenren";

    private static final ResourceLocation HUOXINGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huoxingu");
    private static final ResourceLocation TUPIGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "tupigu");
    private static final ResourceLocation MUGANGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "mugangu");
    private static final ResourceLocation JINFEIGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "jinfeigu");
    private static final ResourceLocation SHUISHENGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "shuishengu");

    private WuHangOrganRegistry() {
    }

    public static boolean register(ChestCavityInstance cc, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return false;
        }
        boolean handled = false;
        if (itemId.equals(HUOXINGU_ID)) {
            cc.onFireListeners.add(new OrganOnFireContext(stack, HuoxinguOrganBehavior.INSTANCE));
            handled = true;
        }
        if (itemId.equals(TUPIGU_ID)) {
            cc.onGroundListeners.add(new OrganOnGroundContext(stack, TupiguOrganBehavior.INSTANCE));
            cc.onSlowTickListeners.add(new OrganSlowTickContext(stack, TupiguOrganBehavior.INSTANCE));
            handled = true;
        }
        if (itemId.equals(MUGANGU_ID)) {
            cc.onSlowTickListeners.add(new OrganSlowTickContext(stack, MuganguOrganBehavior.INSTANCE));
            handled = true;
        }
        if (itemId.equals(JINFEIGU_ID)) {
            cc.onSlowTickListeners.add(new OrganSlowTickContext(stack, JinfeiguOrganBehavior.INSTANCE));
            handled = true;
        }
        if (itemId.equals(SHUISHENGU_ID)) {
            cc.onSlowTickListeners.add(new OrganSlowTickContext(stack, ShuishenguOrganBehavior.INSTANCE));
            cc.onDamageListeners.add(new OrganIncomingDamageContext(stack, ShuishenguOrganBehavior.INSTANCE));
            handled = true;
        }
        return handled;
    }
}
