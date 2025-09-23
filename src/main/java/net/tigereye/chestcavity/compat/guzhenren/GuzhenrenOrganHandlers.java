package net.tigereye.chestcavity.compat.guzhenren;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.GuDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.san_zhuan.wu_hang.WuHangOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.compat.guzhenren.linkage.effect.GuzhenrenLinkageEffectRegistry;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.util.retention.OrganRetentionRules;

import java.util.List;

/**
 * Compatibility helpers that inject Guzhenren-specific organ behaviour without direct class dependencies.
 */
public final class GuzhenrenOrganHandlers {

    private static final String MOD_ID = "guzhenren";
    private GuzhenrenOrganHandlers() {
    }

    static {
        OrganRetentionRules.registerNamespace(MOD_ID);
    }

    public static void registerListeners(ChestCavityInstance cc, ItemStack stack, List<OrganRemovalContext> staleRemovalContexts) {
        if (stack.isEmpty() || !ModList.get().isLoaded(MOD_ID)) {
            if (ChestCavity.LOGGER.isDebugEnabled() && !stack.isEmpty()) {
                ChestCavity.LOGGER.debug("[Guzhenren] Skipping listener registration for {} because the mod is not loaded", stack);
            }
            return;
        }
        if (ChestCavity.LOGGER.isDebugEnabled()) {
            ChestCavity.LOGGER.debug(
                    "[Guzhenren] Registering listeners for item {} on cavity {}",
                    BuiltInRegistries.ITEM.getKey(stack.getItem()),
                    describeChestCavity(cc)
            );
        }
        GuzhenrenLinkageManager.getContext(cc);
        GuDaoOrganRegistry.bootstrap();
        WuHangOrganRegistry.bootstrap();
        GuzhenrenLinkageEffectRegistry.applyEffects(cc, stack, staleRemovalContexts);
    }

    private static String describeChestCavity(ChestCavityInstance cc) {
        if (cc == null || cc.owner == null) {
            return "<unbound>";
        }
        return cc.owner.getScoreboardName();
    }
}
