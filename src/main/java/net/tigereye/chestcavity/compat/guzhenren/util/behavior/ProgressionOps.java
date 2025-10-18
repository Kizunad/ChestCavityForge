package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;

/**
 * 统一的“器官进度/升级（FRP）”工具。
 * - 维护点数与阶数（clamp + 阈值触发）
 * - 提供一次性 API，便于在命中/反应/环境钩子里直接加点并检查升级
 * - 不关心具体行为表现（提示/冷却/演出），通过回调交由调用方处理
 */
public final class ProgressionOps {

    private ProgressionOps() {}

    /** 升阶回调：返回 true 表示已消费并处理（例如自定义提示），否则走默认提示路径（若调用方提供）。 */
    @FunctionalInterface
    public interface TierUpCallback {
        boolean onTierUp(ServerPlayer sp, ChestCavityInstance cc, ItemStack organ, int newTier);
    }

    /**
     * 给指定器官增加点数并检查是否满足升阶。
     * @param cc            胸腔实例
     * @param organ         器官物品
     * @param state         该器官的 OrganState（调用方负责获取）
     * @param pointsKey     点数字段键（如 "FireRefinePoints"）
     * @param tierKey       阶数字段键（如 "Tier"）
     * @param add           增加的点数（<=0 则直接返回）
     * @param cap           点数上限（防溢出）
     * @param minTier       最小阶（通常为 1）
     * @param maxTier       最大阶（如 4）
     * @param thresholds    每阶所需点数阈值数组（下标对应目标阶，例如 index=2 表示升到 2 阶的需求）
     * @param spForToast    可选的服务端玩家（用于默认提示），可为 null
     * @param tierUpHandler 自定义升阶处理，可为 null
     * @return true 表示点数发生变化或升阶；false 表示无变化
     */
    public static boolean addPointsAndCheckTier(ChestCavityInstance cc,
                                                ItemStack organ,
                                                OrganState state,
                                                String pointsKey,
                                                String tierKey,
                                                int add,
                                                int cap,
                                                int minTier,
                                                int maxTier,
                                                int[] thresholds,
                                                ServerPlayer spForToast,
                                                TierUpCallback tierUpHandler) {
        if (cc == null || organ == null || organ.isEmpty() || state == null || add <= 0) {
            return false;
        }
        // 点数累积（带上限）
        int current = Math.max(0, state.getInt(pointsKey, 0));
        int capped = Mth.clamp(current + add, 0, Math.max(cap, 0));
        if (capped == current) {
            // 未变化
            return false;
        }
        state.setInt(pointsKey, capped, v -> Mth.clamp(v, 0, Math.max(cap, 0)), 0);

        // 读写阶数（clamp 保底）
        int tier = state.getInt(tierKey, minTier);
        tier = Mth.clamp(tier, minTier, maxTier);
        if (tier != state.getInt(tierKey, minTier)) {
            state.setInt(tierKey, tier, v -> Mth.clamp(v, minTier, maxTier), minTier);
        }

        // 已达上限则不再升阶
        if (tier >= maxTier) {
            return true;
        }

        // 计算下一阶需求；防御下标越界
        int next = Math.min(tier + 1, maxTier);
        int required = thresholdsSafe(thresholds, next, Integer.MAX_VALUE);
        if (capped < required) {
            return true;
        }

        // 升阶
        int newTier = Mth.clamp(tier + 1, minTier, maxTier);
        state.setInt(tierKey, newTier, v -> Mth.clamp(v, minTier, maxTier), minTier);

        // 回调/默认提示
        if (spForToast != null) {
            boolean consumed = tierUpHandler != null && tierUpHandler.onTierUp(spForToast, cc, organ, newTier);
            if (!consumed) {
                // 默认提示走 actionBar，不提供固定文案，交由调用方更进一步定制
                spForToast.displayClientMessage(net.minecraft.network.chat.Component.literal("器官升级至第" + newTier + "阶"), true);
            }
        }
        return true;
    }

    private static int thresholdsSafe(int[] thresholds, int index, int fallback) {
        if (thresholds == null || thresholds.length == 0) return fallback;
        int i = Math.max(0, Math.min(index, thresholds.length - 1));
        return thresholds[i];
    }
}

