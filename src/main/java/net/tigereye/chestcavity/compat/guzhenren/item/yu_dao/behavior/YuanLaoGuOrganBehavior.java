package net.tigereye.chestcavity.compat.guzhenren.item.yu_dao.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.YuanLaoGuHelper;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NetworkUtil;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Slow tick behaviour for 元老蛊（一转）。 Handles passive真元充填与吸收。
 */
public enum YuanLaoGuOrganBehavior implements OrganSlowTickListener {
    INSTANCE;

    private static final double LOW_ZHENYUAN_RATIO = 0.20;
    private static final double HIGH_ZHENYUAN_RATIO = 0.95;

    private static final double MAX_STONE_CAP = 10_000.0;
    private static final double BASE_REPLENISH_PER_SEC = 200.0;
    private static final double BASE_ABSORB_PER_SEC = 100.0;
    private static final double EPSILON = 1.0e-6;

    private static final String LOG_PREFIX = "[compat/guzhenren][yu_dao][yuan_lao_gu]";

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }
        if (organ == null || organ.isEmpty() || !YuanLaoGuHelper.isYuanLaoGu(organ)) {
            return;
        }

        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            debug("{} {} 无法打开 Guzhenren 资源附件", LOG_PREFIX, player.getScoreboardName());
            return;
        }
        GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();

        OptionalDouble currentOpt = handle.getZhenyuan();
        OptionalDouble maxOpt = handle.getMaxZhenyuan();
        if (currentOpt.isEmpty() || maxOpt.isEmpty()) {
            debug("{} {} 缺失真元字段，跳过元老蛊判定", LOG_PREFIX, player.getScoreboardName());
            return;
        }
        double maxZhenyuan = maxOpt.getAsDouble();
        if (maxZhenyuan <= EPSILON) {
            debug("{} {} 最大真元 <= 0，跳过元老蛊判定", LOG_PREFIX, player.getScoreboardName());
            return;
        }

        double currentZhenyuan = Math.max(0.0, currentOpt.getAsDouble());
        double ratio = currentZhenyuan / maxZhenyuan;

        double effectiveCap = resolveCap(organ);
        double stoneBalance = normaliseBalance(cc, organ, effectiveCap);

        if (ratio < LOW_ZHENYUAN_RATIO - EPSILON) {
            attemptReplenish(player, cc, organ, handle, currentZhenyuan, maxZhenyuan, stoneBalance);
        } else if (ratio > HIGH_ZHENYUAN_RATIO + EPSILON) {
            attemptAbsorb(player, cc, organ, handle, currentZhenyuan, stoneBalance, effectiveCap);
        } else {
            debug("{} {} 真元比 {} 在安全区间，跳过元老蛊被动", LOG_PREFIX, player.getScoreboardName(), formatDouble(ratio));
        }
    }

    private static double resolveCap(ItemStack organ) {
        OptionalDouble capacityOpt = YuanLaoGuHelper.readCapacity(organ);
        double cap = capacityOpt.isPresent() ? capacityOpt.getAsDouble() : MAX_STONE_CAP;
        if (!Double.isFinite(cap) || cap <= 0.0) {
            cap = MAX_STONE_CAP;
        }
        return Math.min(cap, MAX_STONE_CAP);
    }

    private static double normaliseBalance(ChestCavityInstance cc, ItemStack organ, double cap) {
        double balance = Math.max(0.0, YuanLaoGuHelper.readAmount(organ));
        if (cap > 0.0 && balance > cap + EPSILON) {
            double clamped = YuanLaoGuHelper.writeAmountClamped(organ, cap);
            YuanLaoGuHelper.updateDisplayName(organ);
            if (cc != null) {
                NetworkUtil.sendOrganSlotUpdate(cc, organ);
            }
            debug("{} 元石余额超出上限 -> 裁剪为 {}", LOG_PREFIX, formatDouble(clamped));
            return clamped;
        }
        return balance;
    }

    private void attemptReplenish(
            Player player,
            ChestCavityInstance cc,
            ItemStack organ,
            GuzhenrenResourceBridge.ResourceHandle handle,
            double currentZhenyuan,
            double maxZhenyuan,
            double stoneBalance
    ) {
        if (stoneBalance <= EPSILON) {
            debug("{} {} 元老蛊内元石不足，无法补充真元", LOG_PREFIX, player.getScoreboardName());
            return;
        }

        double missing = Math.max(0.0, maxZhenyuan - currentZhenyuan);
        if (missing <= EPSILON) {
            debug("{} {} 真元接近或达到上限，跳过补充", LOG_PREFIX, player.getScoreboardName());
            return;
        }

        double amountToReplenish = Math.min(BASE_REPLENISH_PER_SEC, stoneBalance);
        amountToReplenish = Math.min(amountToReplenish, missing);
        if (amountToReplenish <= EPSILON) {
            debug("{} {} 计算补给量过低 -> {}", LOG_PREFIX, player.getScoreboardName(), formatDouble(amountToReplenish));
            return;
        }

        if (!YuanLaoGuHelper.consume(organ, amountToReplenish)) {
            debug("{} {} 元石扣除失败，余额={}", LOG_PREFIX, player.getScoreboardName(), formatDouble(stoneBalance));
            return;
        }

        double spentFromOrgan = amountToReplenish;
        OptionalDouble result = handle.adjustZhenyuan(amountToReplenish, false);
        if (result.isEmpty()) {
            YuanLaoGuHelper.deposit(organ, amountToReplenish);
            debug("{} {} 真元补充失败 -> 返还元石 {}", LOG_PREFIX, player.getScoreboardName(), formatDouble(amountToReplenish));
            return;
        }

        double updatedZhenyuan = result.getAsDouble();
        OptionalDouble maxOpt = handle.getMaxZhenyuan();
        double effectiveMax = maxOpt.orElse(maxZhenyuan);
        double gained = Math.max(0.0, updatedZhenyuan - currentZhenyuan);

        if (maxOpt.isPresent() && updatedZhenyuan > effectiveMax + EPSILON) {
            double overshoot = updatedZhenyuan - effectiveMax;
            OptionalDouble clampResult = handle.setZhenyuan(effectiveMax);
            if (clampResult.isPresent()) {
                updatedZhenyuan = clampResult.getAsDouble();
                gained = Math.max(0.0, updatedZhenyuan - currentZhenyuan);
                if (overshoot > EPSILON) {
                    YuanLaoGuHelper.deposit(organ, overshoot);
                    spentFromOrgan = Math.max(0.0, spentFromOrgan - overshoot);
                    debug("{} {} 真元补充超出上限 -> 返还元石 {}", LOG_PREFIX, player.getScoreboardName(), formatDouble(overshoot));
                }
            }
        }

        if (gained <= EPSILON) {
            if (spentFromOrgan > EPSILON) {
                YuanLaoGuHelper.deposit(organ, spentFromOrgan);
            }
            YuanLaoGuHelper.updateDisplayName(organ);
            if (cc != null) {
                NetworkUtil.sendOrganSlotUpdate(cc, organ);
            }
            debug("{} {} 真元已满或增量过小，返还消耗", LOG_PREFIX, player.getScoreboardName());
            return;
        }

        if (spentFromOrgan - gained > EPSILON) {
            double refund = spentFromOrgan - gained;
            YuanLaoGuHelper.deposit(organ, refund);
            spentFromOrgan -= refund;
            debug("{} {} 真元补充未用尽 -> 返还元石 {}", LOG_PREFIX, player.getScoreboardName(), formatDouble(refund));
        }

        YuanLaoGuHelper.updateDisplayName(organ);
        if (cc != null) {
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }

        debug(
                "{} {} 补充真元: +{} (消耗 {})，余额 -> {}",
                LOG_PREFIX,
                player.getScoreboardName(),
                formatDouble(gained),
                formatDouble(spentFromOrgan),
                formatDouble(YuanLaoGuHelper.readAmount(organ))
        );
    }

    private void attemptAbsorb(
            Player player,
            ChestCavityInstance cc,
            ItemStack organ,
            GuzhenrenResourceBridge.ResourceHandle handle,
            double currentZhenyuan,
            double stoneBalance,
            double cap
    ) {
        double availableSpace = Math.max(0.0, cap - stoneBalance);
        if (availableSpace <= EPSILON) {
            debug("{} {} 元老蛊空间已满，无法吸收真元", LOG_PREFIX, player.getScoreboardName());
            return;
        }

        double amountToAbsorb = Math.min(BASE_ABSORB_PER_SEC, availableSpace);
        amountToAbsorb = Math.min(amountToAbsorb, currentZhenyuan);
        if (amountToAbsorb <= EPSILON) {
            debug("{} {} 计算吸收量过低 -> {}", LOG_PREFIX, player.getScoreboardName(), formatDouble(amountToAbsorb));
            return;
        }

        // 使用按境界缩放的真元扣除，而非直接调整，保证与 Guzhenren 标准消耗一致
        OptionalDouble result = handle.consumeScaledZhenyuan(amountToAbsorb);
        if (result.isEmpty()) {
            debug("{} {} 真元扣除失败，无法吸收", LOG_PREFIX, player.getScoreboardName());
            return;
        }

        double updatedZhenyuan = result.getAsDouble();
        double consumed = Math.max(0.0, currentZhenyuan - updatedZhenyuan);
        if (consumed <= EPSILON) {
            debug("{} {} 真元扣除量过小 ({})，跳过吸收", LOG_PREFIX, player.getScoreboardName(), formatDouble(consumed));
            return;
        }

        if (!YuanLaoGuHelper.deposit(organ, consumed)) {
            handle.adjustZhenyuan(consumed, false);
            debug("{} {} 元石入账失败，已回滚真元", LOG_PREFIX, player.getScoreboardName());
            return;
        }
        YuanLaoGuHelper.updateDisplayName(organ);
        if (cc != null) {
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }

        debug(
                "{} {} 吸收真元: -{} (存入 {})，余额 -> {}",
                LOG_PREFIX,
                player.getScoreboardName(),
                formatDouble(consumed),
                formatDouble(consumed),
                formatDouble(YuanLaoGuHelper.readAmount(organ))
        );
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static void debug(String message, Object... args) {
        if (ChestCavity.LOGGER.isDebugEnabled()) {
            ChestCavity.LOGGER.debug(message, args);
        }
    }
}
