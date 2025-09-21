package net.tigereye.chestcavity.compat.guzhenren;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.listeners.OrganOnFireContext;
import net.tigereye.chestcavity.listeners.OrganOnGroundContext;
import net.neoforged.fml.ModList;
import net.tigereye.chestcavity.util.retention.OrganRetentionRules;
import net.tigereye.chestcavity.compat.guzhenren.item.HuoxinguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.TupiguOrganBehavior;

/**
 * Compatibility helpers that inject Guzhenren-specific organ behaviour without direct class dependencies.
 */
public final class GuzhenrenOrganHandlers {

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation HUOXINGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huoxingu");
    private static final ResourceLocation TUPIGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "tupigu");

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
}
