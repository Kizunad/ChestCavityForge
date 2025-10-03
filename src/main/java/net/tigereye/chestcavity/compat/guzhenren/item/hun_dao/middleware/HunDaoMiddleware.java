package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.middleware;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.util.DoTManager;
import org.slf4j.Logger;

import java.util.Locale;
import java.util.Optional;

/**
 * 魂道行为与底层系统（DoT、资源）的中间件桥梁。
 * - 负责调度持续伤害（DoT）效果；
 * - 负责魂魄资源与饱食维护；
 * - 预留玩家/非玩家处理入口（占位符）。
 */
public final class HunDaoMiddleware {

    public static final HunDaoMiddleware INSTANCE = new HunDaoMiddleware();

    private static final Logger LOGGER = LogUtils.getLogger();

    private HunDaoMiddleware() {}

    /**
     * 调度“魂焰”持续伤害。
     */
    public void applySoulFlame(Player source, LivingEntity target, double perSecondDamage, int seconds) {
        if (source == null || target == null || !target.isAlive()) {
            return;
        }
        if (perSecondDamage <= 0 || seconds <= 0) {
            return;
        }
        DoTManager.schedulePerSecond(source, target, perSecondDamage, seconds, null, 0.0f, 0.0f);
        LOGGER.debug("[hun_dao][middleware] DoT={}s @{} -> {}", seconds, format(perSecondDamage), target.getName().getString());
    }

    /**
     * 被动资源维护：扣减魂魄并维持饱食/饱和。
     */
    public void passiveUpkeep(Player player, double hunpoLeak) {
        if (player == null) {
            return;
        }
        drainHunpo(player, hunpoLeak, "passive leak");
        maintainSatiation(player);
    }

    /**
     * 玩家处理占位符：便于后续扩展（光环、同步等）。
     */
    public void handlerPlayer(Player player) {
        // 占位：后续可扩展玩家态相关逻辑（如状态同步、音效、粒子）。
    }

    /**
     * 非玩家处理占位符：便于后续扩展。
     */
    public void handlerNonPlayer(LivingEntity entity) {
        // 占位：后续可扩展非玩家实体相关逻辑。
    }

    private void drainHunpo(Player player, double amount, String reason) {
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return;
        }
        GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
        handle.adjustDouble("hunpo", -amount, true, "zuida_hunpo");
        LOGGER.trace("[hun_dao][middleware] drained {} hunpo from {} ({})", format(amount), player.getScoreboardName(), reason);
    }

    private void maintainSatiation(Player player) {
        FoodData foodData = player.getFoodData();
        if (foodData.getFoodLevel() < 18) {
            foodData.eat(1, 0.6f);
        }
        if (foodData.getSaturationLevel() < foodData.getFoodLevel()) {
            foodData.eat(0, 0.4f);
        }
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}

