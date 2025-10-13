package net.tigereye.chestcavity.compat.guzhenren.item.yu_dao.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.YuanLaoGuHelper;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NetworkUtil;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Shared implementation for 元老蛊系列的被动逻辑。
 */
abstract class AbstractYuanLaoGuBehavior implements OrganSlowTickListener {

    private static final double LOW_ZHENYUAN_RATIO = 0.20;
    private static final double HIGH_ZHENYUAN_RATIO = 0.95;
    private static final double DEFAULT_MAX_STONE_CAP = 10_000.0;
    private static final double EPSILON = 1.0e-6;

    @Override
    public final void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }
        if (organ == null || organ.isEmpty() || !matchesOrgan(organ)) {
            return;
        }

        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            debug("{} {} 无法打开 Guzhenren 资源附件", logPrefix(), player.getScoreboardName());
            return;
        }
        GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();

        OptionalDouble currentOpt = handle.getZhenyuan();
        OptionalDouble maxOpt = handle.getMaxZhenyuan();
        if (currentOpt.isEmpty() || maxOpt.isEmpty()) {
            debug("{} {} 缺失真元字段，跳过元老蛊判定", logPrefix(), player.getScoreboardName());
            return;
        }

        double maxZhenyuan = maxOpt.getAsDouble();
        if (maxZhenyuan <= EPSILON) {
            debug("{} {} 最大真元 <= 0，跳过元老蛊判定", logPrefix(), player.getScoreboardName());
            return;
        }

        double currentZhenyuan = Math.max(0.0, currentOpt.getAsDouble());
        double ratio = currentZhenyuan / maxZhenyuan;

        double cap = resolveCap(organ);
        double stoneBalance = normaliseBalance(cc, organ, cap);
        stoneBalance = maybeRegenerate(cc, organ, cap, stoneBalance);

        if (ratio < LOW_ZHENYUAN_RATIO - EPSILON) {
            attemptReplenish(player, cc, organ, handle, currentZhenyuan, maxZhenyuan, stoneBalance);
        } else if (ratio > HIGH_ZHENYUAN_RATIO + EPSILON) {
            attemptAbsorb(player, cc, organ, handle, currentZhenyuan, stoneBalance, cap);
        } else {
            debug("{} {} 真元比 {} 在安全区间，跳过元老蛊被动", logPrefix(), player.getScoreboardName(), formatDouble(ratio));
        }
    }

    protected abstract String logPrefix();

    protected abstract boolean matchesOrgan(ItemStack stack);

    protected abstract double zhenyuanPerStone();

    protected double baseReplenishPerSecond() {
        return 200.0;
    }

    protected double baseAbsorbPerSecond() {
        return 100.0;
    }

    protected double configuredStoneCap() {
        return DEFAULT_MAX_STONE_CAP;
    }

    protected double stoneRegenPerSlowTick() {
        return 0.0;
    }

    private double resolveCap(ItemStack organ) {
        OptionalDouble capacityOpt = YuanLaoGuHelper.readCapacity(organ);
        double cap = capacityOpt.isPresent() ? capacityOpt.getAsDouble() : configuredStoneCap();
        if (!Double.isFinite(cap) || cap <= 0.0) {
            cap = configuredStoneCap();
        }
        return Math.min(cap, configuredStoneCap());
    }

    private double normaliseBalance(ChestCavityInstance cc, ItemStack organ, double cap) {
        double balance = Math.max(0.0, YuanLaoGuHelper.readAmount(organ));
        if (cap > 0.0 && balance > cap + EPSILON) {
            double clamped = YuanLaoGuHelper.writeAmountClamped(organ, cap);
            YuanLaoGuHelper.updateDisplayName(organ);
            if (cc != null) {
                NetworkUtil.sendOrganSlotUpdate(cc, organ);
            }
            debug("{} 元石余额超出上限 -> 裁剪为 {}", logPrefix(), formatDouble(clamped));
            return clamped;
        }
        return balance;
    }

    private double maybeRegenerate(ChestCavityInstance cc, ItemStack organ, double cap, double currentBalance) {
        double regen = stoneRegenPerSlowTick();
        if (regen <= EPSILON || cap <= 0.0) {
            return currentBalance;
        }
        double available = Math.max(0.0, cap - currentBalance);
        double applied = Math.min(regen, available);
        if (applied <= EPSILON) {
            return currentBalance;
        }
        if (!YuanLaoGuHelper.deposit(organ, applied)) {
            return currentBalance;
        }
        YuanLaoGuHelper.updateDisplayName(organ);
        if (cc != null) {
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }
        debug("{} 自动恢复元石 +{}", logPrefix(), formatDouble(applied));
        return currentBalance + applied;
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
            debug("{} {} 元老蛊内元石不足，无法补充真元", logPrefix(), player.getScoreboardName());
            return;
        }

        double availableZhenyuan = stoneBalance * zhenyuanPerStone();
        double missing = Math.max(0.0, maxZhenyuan - currentZhenyuan);
        if (missing <= EPSILON) {
            debug("{} {} 真元接近或达到上限，跳过补充", logPrefix(), player.getScoreboardName());
            return;
        }

        double amountToReplenish = Math.min(baseReplenishPerSecond(), availableZhenyuan);
        amountToReplenish = Math.min(amountToReplenish, missing);
        if (amountToReplenish <= EPSILON) {
            debug("{} {} 计算补给量过低 -> {}", logPrefix(), player.getScoreboardName(), formatDouble(amountToReplenish));
            return;
        }

        double stoneCost = amountToReplenish / zhenyuanPerStone();
        if (!YuanLaoGuHelper.consume(organ, stoneCost)) {
            debug("{} {} 元石扣除失败，余额={}", logPrefix(), player.getScoreboardName(), formatDouble(stoneBalance));
            return;
        }

        double spentZhenyuan = stoneCost * zhenyuanPerStone();
        OptionalDouble result = ResourceOps.tryAdjustZhenyuan(handle, amountToReplenish, false);
        if (result.isEmpty()) {
            YuanLaoGuHelper.deposit(organ, stoneCost);
            debug("{} {} 真元补充失败 -> 返还元石 {}", logPrefix(), player.getScoreboardName(), formatDouble(stoneCost));
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
                    double refundStones = overshoot / zhenyuanPerStone();
                    YuanLaoGuHelper.deposit(organ, refundStones);
                    spentZhenyuan = Math.max(0.0, spentZhenyuan - overshoot);
                    debug("{} {} 真元补充超出上限 -> 返还元石 {}", logPrefix(), player.getScoreboardName(), formatDouble(refundStones));
                }
            }
        }

        if (gained <= EPSILON) {
            if (spentZhenyuan > EPSILON) {
                YuanLaoGuHelper.deposit(organ, spentZhenyuan / zhenyuanPerStone());
            }
            YuanLaoGuHelper.updateDisplayName(organ);
            if (cc != null) {
                NetworkUtil.sendOrganSlotUpdate(cc, organ);
            }
            debug("{} {} 真元已满或增量过小，返还消耗", logPrefix(), player.getScoreboardName());
            return;
        }

        if (spentZhenyuan - gained > EPSILON) {
            double refundZhenyuan = spentZhenyuan - gained;
            double refundStones = refundZhenyuan / zhenyuanPerStone();
            YuanLaoGuHelper.deposit(organ, refundStones);
            spentZhenyuan -= refundZhenyuan;
            debug("{} {} 真元补充未用尽 -> 返还元石 {}", logPrefix(), player.getScoreboardName(), formatDouble(refundStones));
        }

        YuanLaoGuHelper.updateDisplayName(organ);
        if (cc != null) {
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }

        debug(
                "{} {} 补充真元: +{} (消耗 {})，余额 -> {}",
                logPrefix(),
                player.getScoreboardName(),
                formatDouble(gained),
                formatDouble((spentZhenyuan / zhenyuanPerStone())),
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
            debug("{} {} 元老蛊空间已满，无法吸收真元", logPrefix(), player.getScoreboardName());
            return;
        }

        double availableZhenyuan = availableSpace * zhenyuanPerStone();
        double amountToAbsorb = Math.min(baseAbsorbPerSecond(), availableZhenyuan);
        amountToAbsorb = Math.min(amountToAbsorb, currentZhenyuan);
        if (amountToAbsorb <= EPSILON) {
            debug("{} {} 计算吸收量过低 -> {}", logPrefix(), player.getScoreboardName(), formatDouble(amountToAbsorb));
            return;
        }

        OptionalDouble result = ResourceOps.tryConsumeScaledZhenyuan(handle, amountToAbsorb);
        if (result.isEmpty()) {
            debug("{} {} 真元扣除失败，无法吸收", logPrefix(), player.getScoreboardName());
            return;
        }

        double updatedZhenyuan = result.getAsDouble();
        double consumed = Math.max(0.0, currentZhenyuan - updatedZhenyuan);
        if (consumed <= EPSILON) {
            debug("{} {} 真元扣除量过小 ({})，跳过吸收", logPrefix(), player.getScoreboardName(), formatDouble(consumed));
            return;
        }

        double stonesToDeposit = consumed / zhenyuanPerStone();
        if (!YuanLaoGuHelper.deposit(organ, stonesToDeposit)) {
            ResourceOps.tryAdjustZhenyuan(handle, consumed, false);
            debug("{} {} 元石入账失败，已回滚真元", logPrefix(), player.getScoreboardName());
            return;
        }
        YuanLaoGuHelper.updateDisplayName(organ);
        if (cc != null) {
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }

        debug(
                "{} {} 吸收真元: -{} (存入 {})，余额 -> {}",
                logPrefix(),
                player.getScoreboardName(),
                formatDouble(consumed),
                formatDouble(stonesToDeposit),
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
