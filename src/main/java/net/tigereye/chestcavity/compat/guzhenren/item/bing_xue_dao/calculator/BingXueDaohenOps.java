package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.calculator;

import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.registration.CCItems;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class BingXueDaohenOps {

    private static final List<Function<ChestCavityInstance, Double>> providers = new ArrayList<>();

    /**
     * Registers a new provider for the 'daohen' calculation.
     * This allows other modules to add sources for the 'bing_xue_dao' path marks.
     *
     * @param provider A function that takes a ChestCavityInstance and returns a double.
     */
    public static void registerProvider(Function<ChestCavityInstance, Double> provider) {
        providers.add(provider);
    }

    /**
     * Computes the 'daohen' (path marks) for the 'bing_xue_dao' school in real-time.
     * The primary source is the number of stacked 'shuang_xi_gu' organs.
     * The calculation is extensible via registered providers.
     *
     * @param cc The ChestCavityInstance of the entity.
     * @return The calculated 'daohen' value, always >= 0.
     */
    public static double compute(ChestCavityInstance cc) {
        if (cc == null) {
            return 0.0;
        }

        // Base provider: count shuang_xi_gu stacks
        int shuangXiGuCount = 0;
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack organ = cc.inventory.getItem(i);
            if (organ.getItem() == CCItems.GUZHENREN_SHUANG_XI_GU) {
                shuangXiGuCount += organ.getCount();
            }
        }

        double daohen = calculateDaohen(shuangXiGuCount, ChestCavity.config.GUZHENREN_BING_XUE_DAO.SHUANG_XI_GU.increasePerStack);

        // Aggregate from other registered providers
        for (Function<ChestCavityInstance, Double> provider : providers) {
            daohen += provider.apply(cc);
        }

        return Math.max(0.0, daohen);
    }

    public static double calculateDaohen(int stackCount, double increasePerStack) {
        return stackCount * increasePerStack;
    }
}
