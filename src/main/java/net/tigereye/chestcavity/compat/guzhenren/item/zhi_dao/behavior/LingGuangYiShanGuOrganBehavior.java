package net.tigereye.chestcavity.compat.guzhenren.item.zhi_dao.behavior;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.zhi_dao.ZhiDaoOrganRegistry;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper.ConsumptionResult;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import org.slf4j.Logger;

import java.util.Locale;
import java.util.OptionalDouble;

/**
 * Behaviour for 灵光一闪蛊（Zhi Dao, spine）。
 * <ul>
 *     <li>Consumes 1000 zhenyuan each slow tick;</li>
 *     <li>Restores 3念头/秒 up to the "念头容量" soft cap;</li>
 *     <li>If the equipped player has 逍遥智心体体质 (tizhi == 8)，restoration gains an extra 0.12 念头/秒;</li>
 *     <li>The restoration bonus does not stack across multiple copies.</li>
 * </ul>
 */
public final class LingGuangYiShanGuOrganBehavior extends AbstractGuzhenrenOrganBehavior implements OrganSlowTickListener {

    public static final LingGuangYiShanGuOrganBehavior INSTANCE = new LingGuangYiShanGuOrganBehavior();

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final double BASE_ZHENYUAN_COST_PER_SECOND = 1000.0;
    private static final double BASE_NIANTOU_RECOVERY_PER_SECOND = 1.0;
    private static final double TIZHI_RECOVERY_BONUS_PER_SECOND = 0.12;
    private static final double REQUIRED_TIZHI = 8.0;
    private static final double EPSILON = 1.0E-4;

    private LingGuangYiShanGuOrganBehavior() {
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!isPrimaryOrgan(cc, organ)) {
            return;
        }

        ConsumptionResult payment = GuzhenrenResourceCostHelper.consumeStrict(player, BASE_ZHENYUAN_COST_PER_SECOND, 0.0);
        if (!payment.succeeded()) {
            logDebug("zhenyuan payment failed", player, 0.0, 0.0, payment);
            return;
        }

        OptionalDouble handleOpt = GuzhenrenResourceBridge.open(player)
                .map(handle -> applyRecovery(player, handle))
                .orElse(OptionalDouble.empty());

        if (handleOpt.isEmpty()) {
            GuzhenrenResourceCostHelper.refund(player, payment);
            logDebug("recovery aborted", player, 0.0, 0.0, payment);
        }
    }

    private OptionalDouble applyRecovery(Player player, ResourceHandle handle) {
        double recovery = computeRecoveryPerSecond(handle);
        if (recovery <= 0.0) {
            return OptionalDouble.empty();
        }

        OptionalDouble currentOpt = handle.read("niantou");
        OptionalDouble capacityOpt = handle.read("niantou_rongliang");
        OptionalDouble hardCapOpt = handle.read("niantou_zuida");

        double amount = recovery;
        if (currentOpt.isPresent()) {
            double current = currentOpt.getAsDouble();
            double limit = Double.POSITIVE_INFINITY;
            if (capacityOpt.isPresent()) {
                limit = Math.min(limit, capacityOpt.getAsDouble());
            }
            if (hardCapOpt.isPresent()) {
                limit = Math.min(limit, hardCapOpt.getAsDouble());
            }
            if (Double.isFinite(limit)) {
                double room = limit - current;
                if (!(room > EPSILON)) {
                    return OptionalDouble.empty();
                }
                amount = Math.min(amount, room);
            }
            if (!(amount > EPSILON)) {
                return OptionalDouble.empty();
            }
        }

        if (!(amount > EPSILON)) {
            return OptionalDouble.empty();
        }

        double appliedAmount = amount;
        OptionalDouble adjusted = handle.adjustDouble("niantou", appliedAmount, true, "niantou_zuida");
        adjusted.ifPresent(result -> logDebug("recovered", player, appliedAmount, result, null));
        return adjusted;
    }

    private double computeRecoveryPerSecond(ResourceHandle handle) {
        double recovery = BASE_NIANTOU_RECOVERY_PER_SECOND;
        double tizhi = handle.read("tizhi").orElse(Double.NaN);
        if (Double.isFinite(tizhi) && Math.abs(tizhi - REQUIRED_TIZHI) < EPSILON) {
            recovery += TIZHI_RECOVERY_BONUS_PER_SECOND;
        }
        return recovery;
    }

    private boolean isPrimaryOrgan(ChestCavityInstance cc, ItemStack organ) {
        if (cc == null || cc.inventory == null || organ == null || organ.isEmpty()) {
            return false;
        }
        ResourceLocation targetId = ZhiDaoOrganRegistry.LING_GUANG_YI_SHAN_GU_ID;
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack slotStack = cc.inventory.getItem(i);
            if (slotStack == null || slotStack.isEmpty()) {
                continue;
            }
            if (!matchesOrgan(slotStack, targetId)) {
                continue;
            }
            return slotStack == organ;
        }
        return false;
    }

    private void logDebug(String action, Player player, double delta, double result, ConsumptionResult payment) {
        if (!LOGGER.isDebugEnabled()) {
            return;
        }
        String playerName = player == null ? "<unknown>" : player.getScoreboardName();
        String message = String.format(
                Locale.ROOT,
                "[compat/guzhenren][zhi_dao][ling_guang_yi_shan_gu] %s player=%s delta=%s result=%s payment=%s",
                action,
                playerName,
                format(delta),
                format(result),
                payment == null ? "<none>" : payment
        );
        LOGGER.debug(message);
    }

    private String format(double value) {
        if (!Double.isFinite(value)) {
            return "<nan>";
        }
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
