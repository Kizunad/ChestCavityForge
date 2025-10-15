package net.tigereye.chestcavity.compat.guzhenren.item.yu_dao.behavior;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.YuanLaoGuHelper;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NetworkUtil;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Shared implementation for 元老蛊系列的被动逻辑。
 */
abstract class AbstractYuanLaoGuBehavior implements OrganSlowTickListener {

    private static final double LOW_ZHENYUAN_RATIO = 0.20;
    private static final double HIGH_ZHENYUAN_RATIO = 0.95;
    private static final double DEFAULT_MAX_STONE_CAP = 10_000.0;
    private static final double EPSILON = 1.0e-6;
    private static final double RESOURCE_CONVERT_PERCENT = 0.01;

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

        if (allowAuxiliaryConversion()) {
            double refreshedBalance = YuanLaoGuHelper.readAmount(organ);
            processAuxiliaryResources(player, cc, organ, handle, cap, refreshedBalance);
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

    /**
     * 是否启用精力/魂魄/念头 ↔ 元石的额外转化流程。默认关闭，以保持现有代际逻辑不变。
     */
    protected boolean allowAuxiliaryConversion() {
        return false;
    }

    /**
     * 指定行为默认启用哪些额外资源的转换。默认全部禁用，仅当子类需要时返回 true。
     */
    protected boolean isAuxiliaryResourceEnabled(AuxiliaryResource resource) {
        return false;
    }

    protected double resolveCap(ItemStack organ) {
        OptionalDouble capacityOpt = YuanLaoGuHelper.readCapacity(organ);
        double rawCap = capacityOpt.isPresent() ? capacityOpt.getAsDouble() : configuredStoneCap();
        if (!Double.isFinite(rawCap) || rawCap <= 0.0) {
            rawCap = configuredStoneCap();
        }
        return Math.min(rawCap, configuredStoneCap());
    }

    private double normaliseBalance(ChestCavityInstance cc, ItemStack organ, double cap) {
        double rawBalance = Math.max(0.0, YuanLaoGuHelper.readAmount(organ));
        double capped = cap > 0.0 ? cap : configuredStoneCap();
        if (capped > 0.0 && rawBalance > capped + EPSILON) {
            double clampedRaw = YuanLaoGuHelper.writeAmountClamped(organ, capped);
            pushOrganUpdate(cc, organ);
            debug("{} 元石余额超出上限 -> 裁剪为 {}", logPrefix(), formatDouble(clampedRaw));
            return clampedRaw;
        }
        return rawBalance;
    }

    private double maybeRegenerate(ChestCavityInstance cc, ItemStack organ, double cap, double currentBalance) {
        double regenRaw = stoneRegenPerSlowTick();
        if (regenRaw <= EPSILON || cap <= 0.0) {
            return currentBalance;
        }
        double available = Math.max(0.0, cap - currentBalance);
        double applied = Math.min(regenRaw, available);
        if (applied <= EPSILON) {
            return currentBalance;
        }
        if (!YuanLaoGuHelper.deposit(organ, applied)) {
            return currentBalance;
        }
        pushOrganUpdate(cc, organ);
        debug("{} 自动恢复元石 +{}", logPrefix(), formatDouble(applied));
        return currentBalance + applied;
    }

    protected void pushOrganUpdate(ChestCavityInstance cc, ItemStack organ) {
        YuanLaoGuHelper.updateDisplayName(organ);
        if (cc != null) {
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }
    }

    private void processAuxiliaryResources(
            Player player,
            ChestCavityInstance cc,
            ItemStack organ,
            GuzhenrenResourceBridge.ResourceHandle handle,
            double cap,
            double initialStoneBalance
    ) {
        double stoneBalance = initialStoneBalance;
        for (AuxiliaryResource resource : AuxiliaryResource.values()) {
            stoneBalance = resource.process(this, player, cc, organ, handle, cap, stoneBalance);
        }
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
        double ratioBase = zhenyuanPerStone();
        double actualPerBase = resolveActualPerBase(handle);
        if (actualPerBase <= 0.0 || !Double.isFinite(actualPerBase)) {
            debug("{} {} 无法解析真元缩放，跳过补充", logPrefix(), player.getScoreboardName());
            return;
        }
        double actualPerStone = actualPerBase * ratioBase;
        if (actualPerStone <= EPSILON || !Double.isFinite(actualPerStone)) {
            debug("{} {} 真元缩放异常，跳过补充", logPrefix(), player.getScoreboardName());
            return;
        }
        double stonesAvailable = stoneBalance;
        if (stonesAvailable <= EPSILON) {
            debug("{} {} 元老蛊内元石不足，无法补充真元", logPrefix(), player.getScoreboardName());
            return;
        }

        double missingActual = Math.max(0.0, maxZhenyuan - currentZhenyuan);
        if (missingActual <= EPSILON) {
            debug("{} {} 真元接近或达到上限，跳过补充", logPrefix(), player.getScoreboardName());
            return;
        }

        double actualSupply = stonesAvailable * actualPerStone;
        double actualTarget = Math.min(missingActual, baseReplenishPerSecond());
        actualTarget = Math.min(actualTarget, actualSupply);
        if (actualTarget <= EPSILON) {
            debug("{} {} 计算补给量过低", logPrefix(), player.getScoreboardName());
            return;
        }

        double stoneRequest = actualTarget / actualPerStone;
        if (stoneRequest <= EPSILON) {
            debug("{} {} 计算补给元石量过低", logPrefix(), player.getScoreboardName());
            return;
        }

        double baseRequest = stoneRequest * ratioBase;

        if (!YuanLaoGuHelper.consume(organ, stoneRequest)) {
            debug("{} {} 元石扣除失败，余额={}", logPrefix(), player.getScoreboardName(), formatDouble(stonesAvailable));
            return;
        }

        OptionalDouble replenish = handle.replenishScaledZhenyuan(baseRequest, true);
        if (replenish.isEmpty()) {
            YuanLaoGuHelper.deposit(organ, stoneRequest);
            debug("{} {} 真元补充失败，返还元石 {}", logPrefix(), player.getScoreboardName(), formatDouble(stoneRequest));
            return;
        }

        double updatedZhenyuan = replenish.getAsDouble();
        double gainedActual = Math.max(0.0, updatedZhenyuan - currentZhenyuan);
        if (gainedActual <= EPSILON) {
            YuanLaoGuHelper.deposit(organ, stoneRequest);
            pushOrganUpdate(cc, organ);
            debug("{} {} 真元增量过小，返还元石 {}", logPrefix(), player.getScoreboardName(), formatDouble(stoneRequest));
            return;
        }

        double stonesSpent = gainedActual / actualPerStone;
        if (!Double.isFinite(stonesSpent)) {
            stonesSpent = stoneRequest;
        }
        if (stonesSpent > stoneRequest + EPSILON) {
            stonesSpent = stoneRequest;
        }
        double refundStones = stoneRequest - stonesSpent;
        if (refundStones > EPSILON) {
            YuanLaoGuHelper.deposit(organ, refundStones);
        }

        pushOrganUpdate(cc, organ);

        debug(
                "{} {} 补充真元: +{} (消耗元石 {})，余额 -> {}",
                logPrefix(),
                player.getScoreboardName(),
                formatDouble(gainedActual),
                formatDouble(stonesSpent),
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
        double ratioBase = zhenyuanPerStone();
        double actualPerBase = resolveActualPerBase(handle);
        if (actualPerBase <= 0.0 || !Double.isFinite(actualPerBase)) {
            debug("{} {} 无法解析真元缩放，跳过吸收", logPrefix(), player.getScoreboardName());
            return;
        }
        double actualPerStone = actualPerBase * ratioBase;
        if (actualPerStone <= EPSILON || !Double.isFinite(actualPerStone)) {
            debug("{} {} 真元缩放异常，跳过吸收", logPrefix(), player.getScoreboardName());
            return;
        }
        double stoneCapacity = Math.max(0.0, cap - stoneBalance);
        if (stoneCapacity <= EPSILON) {
            debug("{} {} 元老蛊空间已满，无法吸收真元", logPrefix(), player.getScoreboardName());
            return;
        }

        double actualCapacity = stoneCapacity * actualPerStone;
        double actualPercentTarget = Math.max(0.0, currentZhenyuan * 0.01);
        double actualLimit = baseAbsorbPerSecond();
        double actualTarget = Math.min(actualPercentTarget, actualLimit);
        actualTarget = Math.min(actualTarget, actualCapacity);
        if (actualTarget <= EPSILON) {
            debug("{} {} 计算吸收量过低", logPrefix(), player.getScoreboardName());
            return;
        }

        double stoneRequest = actualTarget / actualPerStone;
        if (stoneRequest <= EPSILON) {
            debug("{} {} 计算吸收元石量过低", logPrefix(), player.getScoreboardName());
            return;
        }

        double baseRequest = stoneRequest * ratioBase;

        OptionalDouble consume = handle.consumeScaledZhenyuan(baseRequest);
        if (consume.isEmpty()) {
            debug("{} {} 真元扣除失败，无法吸收", logPrefix(), player.getScoreboardName());
            return;
        }

        double updatedZhenyuan = consume.getAsDouble();
        double consumedActual = Math.max(0.0, currentZhenyuan - updatedZhenyuan);
        if (consumedActual <= EPSILON) {
            debug("{} {} 真元扣除量过小 ({})，跳过吸收", logPrefix(), player.getScoreboardName(), formatDouble(consumedActual));
            return;
        }

        double stonesGained = consumedActual / actualPerStone;
        if (!Double.isFinite(stonesGained)) {
            stonesGained = stoneRequest;
        }
        stonesGained = Math.min(stoneCapacity, stonesGained);

        if (!YuanLaoGuHelper.deposit(organ, stonesGained)) {
            double baseRollback = consumedActual / actualPerBase;
            handle.replenishScaledZhenyuan(baseRollback, false);
            debug("{} {} 元石入账失败，已回滚真元", logPrefix(), player.getScoreboardName());
            return;
        }
        pushOrganUpdate(cc, organ);

        debug(
                "{} {} 吸收真元: -{} (存入元石 {})，余额 -> {}",
                logPrefix(),
                player.getScoreboardName(),
                formatDouble(consumedActual),
                formatDouble(stonesGained),
                formatDouble(YuanLaoGuHelper.readAmount(organ))
        );
    }

    protected enum AuxiliaryResource {
        JINGLI(
                "精力",
                BehaviorConfigAccess.getFloat(AbstractYuanLaoGuBehavior.class, "JINGLI_PER_STONE", 0.1f),
                BehaviorConfigAccess.getFloat(AbstractYuanLaoGuBehavior.class, "JINGLI_RECOVERY_EFFICIENCY", 0.2f),
                BehaviorConfigAccess.getFloat(AbstractYuanLaoGuBehavior.class, "JINGLI_RECOVERY_TRIGGER_RATIO", 0.5f),
                "ENABLE_JINGLI_CONVERSION",
                false,
                GuzhenrenResourceBridge.ResourceHandle::getJingli,
                GuzhenrenResourceBridge.ResourceHandle::getMaxJingli,
                (handle, delta) -> handle.adjustJingli(delta, true)
        ),
        HUNPO(
                "魂魄",
                BehaviorConfigAccess.getFloat(AbstractYuanLaoGuBehavior.class, "HUNPO_PER_STONE", 0.05f),
                BehaviorConfigAccess.getFloat(AbstractYuanLaoGuBehavior.class, "HUNPO_RECOVERY_EFFICIENCY", 0.1f),
                BehaviorConfigAccess.getFloat(AbstractYuanLaoGuBehavior.class, "HUNPO_RECOVERY_TRIGGER_RATIO", 0.5f),
                "ENABLE_HUNPO_CONVERSION",
                false,
                GuzhenrenResourceBridge.ResourceHandle::getHunpo,
                GuzhenrenResourceBridge.ResourceHandle::getMaxHunpo,
                (handle, delta) -> handle.adjustHunpo(delta, true)
        ),
        NIANTOU(
                "念头",
                BehaviorConfigAccess.getFloat(AbstractYuanLaoGuBehavior.class, "NIANTOU_PER_STONE", 0.01f),
                BehaviorConfigAccess.getFloat(AbstractYuanLaoGuBehavior.class, "NIANTOU_RECOVERY_EFFICIENCY", 0.05f),
                BehaviorConfigAccess.getFloat(AbstractYuanLaoGuBehavior.class, "NIANTOU_RECOVERY_TRIGGER_RATIO", 0.5f),
                "ENABLE_NIANTOU_CONVERSION",
                false,
                GuzhenrenResourceBridge.ResourceHandle::getNiantou,
                GuzhenrenResourceBridge.ResourceHandle::getMaxNiantou,
                (handle, delta) -> handle.adjustNiantou(delta, true)
        );

        private final String label;
        private final double resourcePerStone;
        private final double recoveryEfficiency;
        private final double recoveryTriggerRatio;
        private final String enableKey;
        private final boolean enableDefault;
        private final Function<GuzhenrenResourceBridge.ResourceHandle, OptionalDouble> currentReader;
        private final Function<GuzhenrenResourceBridge.ResourceHandle, OptionalDouble> maxReader;
        private final BiFunction<GuzhenrenResourceBridge.ResourceHandle, Double, OptionalDouble> adjuster;

        AuxiliaryResource(
                String label,
                float resourcePerStone,
                float recoveryEfficiency,
                float recoveryTriggerRatio,
                String enableKey,
                boolean enableDefault,
                Function<GuzhenrenResourceBridge.ResourceHandle, OptionalDouble> currentReader,
                Function<GuzhenrenResourceBridge.ResourceHandle, OptionalDouble> maxReader,
                BiFunction<GuzhenrenResourceBridge.ResourceHandle, Double, OptionalDouble> adjuster
        ) {
            this.label = label;
            this.resourcePerStone = resourcePerStone;
            this.recoveryEfficiency = recoveryEfficiency;
            this.recoveryTriggerRatio = Mth.clamp(recoveryTriggerRatio, 0.0f, 1.0f);
            this.enableKey = enableKey;
            this.enableDefault = enableDefault;
            this.currentReader = currentReader;
            this.maxReader = maxReader;
            this.adjuster = adjuster;
        }

        double process(
                AbstractYuanLaoGuBehavior behavior,
                Player player,
                ChestCavityInstance cc,
                ItemStack organ,
                GuzhenrenResourceBridge.ResourceHandle handle,
                double cap,
                double stoneBalanceHint
        ) {
            boolean enabledByBehavior = behavior.isAuxiliaryResourceEnabled(this);
            boolean enabledByConfig = BehaviorConfigAccess.getBoolean(AbstractYuanLaoGuBehavior.class, enableKey, enableDefault);
            if (!enabledByBehavior && !enabledByConfig) {
                return Math.max(0.0, Double.isFinite(stoneBalanceHint) ? stoneBalanceHint : YuanLaoGuHelper.readAmount(organ));
            }
            double stoneBalance = Math.max(0.0, Double.isFinite(stoneBalanceHint) ? stoneBalanceHint : YuanLaoGuHelper.readAmount(organ));
            OptionalDouble currentOpt = currentReader.apply(handle);
            OptionalDouble maxOpt = maxReader.apply(handle);
            if (currentOpt.isEmpty() || maxOpt.isEmpty()) {
                return stoneBalance;
            }
            double current = Math.max(0.0, currentOpt.getAsDouble());
            double max = Math.max(0.0, maxOpt.getAsDouble());
            if (!Double.isFinite(current) || !Double.isFinite(max) || max <= EPSILON) {
                return stoneBalance;
            }
            if (resourcePerStone <= EPSILON || !Double.isFinite(resourcePerStone)) {
                return stoneBalance;
            }
            double ratio = current / max;
            if (!Double.isFinite(ratio)) {
                return stoneBalance;
            }
            if (ratio > HIGH_ZHENYUAN_RATIO + EPSILON) {
                return drainExcess(behavior, player, cc, organ, handle, cap, stoneBalance, current);
            }
            double trigger = recoveryTriggerRatio > 0.0 ? recoveryTriggerRatio : LOW_ZHENYUAN_RATIO;
            if (ratio < trigger - EPSILON) {
                return replenish(behavior, player, cc, organ, handle, cap, stoneBalance, current, max);
            }
            return stoneBalance;
        }

        private double drainExcess(
                AbstractYuanLaoGuBehavior behavior,
                Player player,
                ChestCavityInstance cc,
                ItemStack organ,
                GuzhenrenResourceBridge.ResourceHandle handle,
                double cap,
                double stoneBalance,
                double currentValue
        ) {
            double stoneCapacity = Math.max(0.0, cap - stoneBalance);
            if (stoneCapacity <= EPSILON) {
                return stoneBalance;
            }
            double desiredResource = Math.min(currentValue * RESOURCE_CONVERT_PERCENT, stoneCapacity * resourcePerStone);
            if (desiredResource <= EPSILON) {
                return stoneBalance;
            }

            double before = currentValue;
            OptionalDouble adjustedOpt = adjuster.apply(handle, -desiredResource);
            if (adjustedOpt.isEmpty()) {
                return stoneBalance;
            }
            double after = Math.max(0.0, adjustedOpt.getAsDouble());
            double actualSpent = Math.max(0.0, before - after);
            if (actualSpent <= EPSILON) {
                return stoneBalance;
            }
            double stoneGain = actualSpent / resourcePerStone;
            if (!Double.isFinite(stoneGain) || stoneGain <= EPSILON) {
                adjuster.apply(handle, actualSpent);
                return stoneBalance;
            }
            if (!YuanLaoGuHelper.deposit(organ, stoneGain)) {
                adjuster.apply(handle, actualSpent);
                return stoneBalance;
            }
            behavior.pushOrganUpdate(cc, organ);
            double newBalance = YuanLaoGuHelper.readAmount(organ);
            debug(
                    "{} {} {}盈余转化: -{} (存入元石 {})，余额 -> {}",
                    behavior.logPrefix(),
                    player.getScoreboardName(),
                    label,
                    formatDouble(actualSpent),
                    formatDouble(stoneGain),
                    formatDouble(newBalance)
            );
            return newBalance;
        }

        private double replenish(
                AbstractYuanLaoGuBehavior behavior,
                Player player,
                ChestCavityInstance cc,
                ItemStack organ,
                GuzhenrenResourceBridge.ResourceHandle handle,
                double cap,
                double stoneBalance,
                double currentValue,
                double maxValue
        ) {
            double availableStones = YuanLaoGuHelper.readAmount(organ);
            if (availableStones <= EPSILON) {
                return availableStones;
            }
            double missing = Math.max(0.0, maxValue - currentValue);
            if (missing <= EPSILON) {
                return availableStones;
            }
            double efficiency = Math.max(0.0, recoveryEfficiency);
            if (efficiency <= EPSILON) {
                return availableStones;
            }

            double resourceTarget = Math.min(missing, maxValue * RESOURCE_CONVERT_PERCENT);
            if (resourceTarget <= EPSILON) {
                return availableStones;
            }
            double resourceCapacity = availableStones * resourcePerStone * efficiency;
            double resourceGainPlan = Math.min(resourceTarget, resourceCapacity);
            if (resourceGainPlan <= EPSILON) {
                return availableStones;
            }

            double stonesNeeded = resourceGainPlan / (resourcePerStone * efficiency);
            if (!Double.isFinite(stonesNeeded) || stonesNeeded <= EPSILON) {
                return availableStones;
            }
            if (!YuanLaoGuHelper.consume(organ, stonesNeeded)) {
                return availableStones;
            }

            double before = currentValue;
            OptionalDouble adjustedOpt = adjuster.apply(handle, resourceGainPlan);
            if (adjustedOpt.isEmpty()) {
                YuanLaoGuHelper.deposit(organ, stonesNeeded);
                return availableStones;
            }
            double after = Math.max(0.0, adjustedOpt.getAsDouble());
            double actualGain = Math.max(0.0, after - before);
            if (actualGain <= EPSILON) {
                YuanLaoGuHelper.deposit(organ, stonesNeeded);
                return availableStones;
            }

            double netStones = stonesNeeded;
            if (actualGain < resourceGainPlan - EPSILON) {
                double unusedResource = resourceGainPlan - actualGain;
                double refundStones = unusedResource / (resourcePerStone * efficiency);
                if (refundStones > EPSILON && Double.isFinite(refundStones)) {
                    if (YuanLaoGuHelper.deposit(organ, refundStones)) {
                        netStones = Math.max(0.0, stonesNeeded - refundStones);
                    }
                }
            }

            behavior.pushOrganUpdate(cc, organ);
            double newBalance = YuanLaoGuHelper.readAmount(organ);
            debug(
                    "{} {} 元石转化为{}: +{} (消耗元石 {})，余额 -> {}",
                    behavior.logPrefix(),
                    player.getScoreboardName(),
                    label,
                    formatDouble(actualGain),
                    formatDouble(netStones),
                    formatDouble(newBalance)
            );
            return newBalance;
        }
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static void debug(String message, Object... args) {
        if (ChestCavity.LOGGER.isDebugEnabled()) {
            ChestCavity.LOGGER.debug(message, args);
        }
    }

    private double resolveActualPerBase(GuzhenrenResourceBridge.ResourceHandle handle) {
        double jieduan = handle.getJieduan().orElse(0.0);
        double zhuanshu = Math.max(1.0, handle.getZhuanshu().orElse(1.0));
        double denominator = Math.pow(2.0, jieduan + zhuanshu * 4.0) * zhuanshu * 3.0 / 96.0;
        if (!Double.isFinite(denominator) || denominator <= 0.0) {
            return 1.0;
        }
        return 1.0 / denominator;
    }
}
