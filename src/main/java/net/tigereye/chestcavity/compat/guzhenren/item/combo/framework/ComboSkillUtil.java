package net.tigereye.chestcavity.compat.guzhenren.item.combo.framework;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;

/**
 * 组合杀招通用工具
 */
public final class ComboSkillUtil {
    private ComboSkillUtil() {}

    /**
     * 通用资源消耗定义
     */
    public record ResourceCost(
        double zhenyuan, double jingli, double hunpo, double niantou, int hunger, float health) {
        public boolean isZero() {
            return zhenyuan <= 0.0D
                && jingli <= 0.0D
                && hunpo <= 0.0D
                && niantou <= 0.0D
                && hunger <= 0
                && health <= 0.0f;
        }
    }

    /**
     * 尝试支付资源消耗
     * @param player 玩家
     * @param cost 资源成本
     * @param onFail 失败时的回调，用于发送消息
     * @return 是否成功支付
     */
    public static boolean tryPayCost(ServerPlayer player, ResourceCost cost, Consumer<ServerPlayer> onFail) {
        if (cost.isZero()) {
            return true;
        }
        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            onFail.accept(player);
            return false;
        }
        ResourceHandle handle = handleOpt.get();
        double spentZhenyuan = 0.0D;
        boolean spentJingli = false;
        boolean spentHunpo = false;
        boolean spentNiantou = false;
        FoodData foodData = player.getFoodData();
        int prevFood = foodData.getFoodLevel();
        float prevSaturation = foodData.getSaturationLevel();
        float prevHealth = player.getHealth();
        boolean success = false;
        try {
            if (cost.zhenyuan() > 0.0D) {
                OptionalDouble consumed = ResourceOps.tryConsumeScaledZhenyuan(handle, cost.zhenyuan());
                if (consumed.isEmpty()) {
                    onFail.accept(player);
                    return false;
                }
                spentZhenyuan = consumed.getAsDouble();
            }
            if (cost.jingli() > 0.0D) {
                if (handle.adjustJingli(-cost.jingli(), true).isEmpty()) {
                    onFail.accept(player);
                    return false;
                }
                spentJingli = true;
            }
            if (cost.hunpo() > 0.0D) {
                if (handle.adjustHunpo(-cost.hunpo(), true).isEmpty()) {
                    onFail.accept(player);
                    return false;
                }
                spentHunpo = true;
            }
            if (cost.niantou() > 0.0D) {
                if (handle.adjustNiantou(-cost.niantou(), true).isEmpty()) {
                    onFail.accept(player);
                    return false;
                }
                spentNiantou = true;
            }
            if (cost.hunger() > 0) {
                if (foodData.getFoodLevel() < cost.hunger()) {
                    onFail.accept(player);
                    return false;
                }
                foodData.setFoodLevel(foodData.getFoodLevel() - cost.hunger());
            }
            if (cost.health() > 0.0f) {
                if (player.getHealth() <= cost.health() + 1.0f) {
                    onFail.accept(player);
                    return false;
                }
                if (!ResourceOps.drainHealth(player, cost.health())) {
                    onFail.accept(player);
                    return false;
                }
            }
            success = true;
            return true;
        } finally {
            if (!success) {
                if (spentZhenyuan > 0.0D) {
                    handle.adjustZhenyuan(spentZhenyuan, true);
                }
                if (spentJingli) {
                    handle.adjustJingli(cost.jingli(), true);
                }
                if (spentHunpo) {
                    handle.adjustHunpo(cost.hunpo(), true);
                }
                if (spentNiantou) {
                    handle.adjustNiantou(cost.niantou(), true);
                }
                foodData.setFoodLevel(prevFood);
                foodData.setSaturation(prevSaturation);
                player.setHealth(prevHealth);
            }
        }
    }
}
