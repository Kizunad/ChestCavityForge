package net.tigereye.chestcavity.compat.guzhenren;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageContext;
import net.tigereye.chestcavity.listeners.OrganOnFireContext;
import net.tigereye.chestcavity.listeners.OrganOnGroundContext;
import net.tigereye.chestcavity.listeners.OrganSlowTickContext;
import net.neoforged.fml.ModList;
import net.tigereye.chestcavity.util.retention.OrganRetentionRules;
import net.tigereye.chestcavity.compat.guzhenren.item.HuoxinguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.JinfeiguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.MuganguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.ShuishenguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.TupiguOrganBehavior;

/**
 * Compatibility helpers that inject Guzhenren-specific organ behaviour without direct class dependencies.
 */
public final class GuzhenrenOrganHandlers {

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation HUOXINGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huoxingu");
    private static final ResourceLocation TUPIGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "tupigu");
    private static final ResourceLocation MUGANGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "mugangu");
    private static final ResourceLocation JINFEIGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "jinfeigu");
    private static final ResourceLocation SHUISHENGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "shuishengu");

    private GuzhenrenOrganHandlers() {
    }

    static {
        OrganRetentionRules.registerNamespace(MOD_ID);
    }

    public static void registerListeners(ChestCavityInstance cc, ItemStack stack) {
        if (stack.isEmpty() || !ModList.get().isLoaded(MOD_ID)) {
            return;
        }

        if (isHuoxingu(stack)) {
            cc.onFireListeners.add(new OrganOnFireContext(stack, HuoxinguOrganBehavior.INSTANCE));
        }
        if (isTupigu(stack)) {
            cc.onGroundListeners.add(new OrganOnGroundContext(stack, TupiguOrganBehavior.INSTANCE));
            cc.onSlowTickListeners.add(new OrganSlowTickContext(stack, TupiguOrganBehavior.INSTANCE));
        }
        if (isMugangu(stack)) {
            cc.onSlowTickListeners.add(new OrganSlowTickContext(stack, MuganguOrganBehavior.INSTANCE));
        }
        if (isJinfeigu(stack)) {
            cc.onSlowTickListeners.add(new OrganSlowTickContext(stack, JinfeiguOrganBehavior.INSTANCE));
        }
        if (isShuishengu(stack)) {
            cc.onSlowTickListeners.add(new OrganSlowTickContext(stack, ShuishenguOrganBehavior.INSTANCE));
            cc.onDamageListeners.add(new OrganIncomingDamageContext(stack, ShuishenguOrganBehavior.INSTANCE));
        }
    }

    private static boolean isHuoxingu(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId != null && itemId.equals(HUOXINGU_ID);
    }

    private static boolean isTupigu(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId != null && itemId.equals(TUPIGU_ID);
    }

    private static boolean isMugangu(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId != null && itemId.equals(MUGANGU_ID);
    }

    private static boolean isJinfeigu(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId != null && itemId.equals(JINFEIGU_ID);
    }

    private static boolean isShuishengu(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId != null && itemId.equals(SHUISHENGU_ID);
    }
}
