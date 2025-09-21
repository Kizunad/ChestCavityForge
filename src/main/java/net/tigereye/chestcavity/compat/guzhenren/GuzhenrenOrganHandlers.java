package net.tigereye.chestcavity.compat.guzhenren;

import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.neoforged.fml.ModList;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.GuDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.san_zhuan.wu_hang.WuHangOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.util.retention.OrganRetentionRules;

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

    public static void registerListeners(ChestCavityInstance cc, ItemStack stack) {
        if (stack.isEmpty() || !ModList.get().isLoaded(MOD_ID)) {
            return;
        }
        GuzhenrenLinkageManager.getContext(cc);
        GuDaoOrganRegistry.register(cc, stack);
        WuHangOrganRegistry.register(cc, stack);
    }
}
