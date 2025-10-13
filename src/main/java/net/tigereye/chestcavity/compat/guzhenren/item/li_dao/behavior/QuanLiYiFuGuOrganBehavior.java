package net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;


import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.AbstractLiDaoOrganBehavior;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper.ConsumptionResult;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper.Mode;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

/**
 * Behaviour for 全力以赴蛊（三转力道，心脏）。
 */
public final class QuanLiYiFuGuOrganBehavior extends AbstractLiDaoOrganBehavior implements OrganSlowTickListener {

    public static final QuanLiYiFuGuOrganBehavior INSTANCE = new QuanLiYiFuGuOrganBehavior();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "quan_li_yi_fu_gu");
    private static final String STATE_ROOT = "quan_li_yi_fu_gu";
    private static final String COOLDOWN_KEY = "cooldown";

    private static final double BASE_ZHENYUAN_COST = 500.0;
    private static final double BASE_JINGLI_RESTORE = 5.0;
    private static final double MUSCLE_BONUS_PER_SLOT = 0.5;
    private static final double MUSCLE_BONUS_CAP_BASE = 25.0;
    private static final int PULSE_INTERVAL_SECONDS = BehaviorConfigAccess.getInt(QuanLiYiFuGuOrganBehavior.class, "PULSE_INTERVAL_SECONDS", 15);
    private static final int RETRY_INTERVAL_SECONDS = BehaviorConfigAccess.getInt(QuanLiYiFuGuOrganBehavior.class, "RETRY_INTERVAL_SECONDS", 3);

    private static final int MAX_STORED_COOLDOWN = BehaviorConfigAccess.getInt(QuanLiYiFuGuOrganBehavior.class, "MAX_STORED_COOLDOWN", PULSE_INTERVAL_SECONDS);

    private QuanLiYiFuGuOrganBehavior() {
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

        OrganState state = organState(organ, STATE_ROOT);
        if (decrementCooldown(state, cc, organ)) {
            return;
        }

        ConsumptionResult payment = ResourceOps.consumeStrict(player, BASE_ZHENYUAN_COST, 0.0);
        if (!payment.succeeded()) {
            setCooldownSeconds(state, cc, organ, RETRY_INTERVAL_SECONDS);
            return;
        }

        double jingliRestore = computeJingliRestore(cc);
        if (jingliRestore <= 0.0) {
            refund(player, payment);
            setCooldownSeconds(state, cc, organ, RETRY_INTERVAL_SECONDS);
            return;
        }

        if (ResourceOps.tryAdjustJingli(player, jingliRestore, true).isEmpty()) {
            refund(player, payment);
            setCooldownSeconds(state, cc, organ, RETRY_INTERVAL_SECONDS);
            return;
        }

        setCooldownSeconds(state, cc, organ, PULSE_INTERVAL_SECONDS);
    }

    private double computeJingliRestore(ChestCavityInstance cc) {
        double increase = Math.max(0.0, liDaoIncrease(cc));
        double scale = 1.0 + increase;
        double base = BASE_JINGLI_RESTORE * scale;
        int muscles = Math.max(0, countMuscles(cc));
        double perMuscle = MUSCLE_BONUS_PER_SLOT * scale;
        double bonusCap = Math.max(0.0, MUSCLE_BONUS_CAP_BASE + scale);
        double bonus = Math.min(muscles * perMuscle, bonusCap);
        return Math.max(0.0, base + bonus);
    }

    private boolean decrementCooldown(OrganState state, ChestCavityInstance cc, ItemStack organ) {
        int remaining = Math.max(0, state.getInt(COOLDOWN_KEY, 0));
        if (remaining <= 0) {
            return false;
        }
        int next = Math.max(0, remaining - 1);
        if (next != remaining) {
            OrganStateOps.setIntSync(cc, organ, STATE_ROOT, COOLDOWN_KEY, next, v -> Math.max(0, Math.min(v, MAX_STORED_COOLDOWN)), 0);
        }
        return true;
    }

    private void setCooldownSeconds(OrganState state, ChestCavityInstance cc, ItemStack organ, int seconds) {
        int stored;
        if (seconds <= 0) {
            stored = 0;
        } else {
            stored = Math.max(0, seconds - 1);
        }
        stored = Math.min(stored, MAX_STORED_COOLDOWN);
        OrganStateOps.setIntSync(cc, organ, STATE_ROOT, COOLDOWN_KEY, stored, v -> Math.max(0, Math.min(v, MAX_STORED_COOLDOWN)), 0);
    }

    private void refund(Player player, ConsumptionResult payment) {
        if (player != null && payment != null && payment.mode() == Mode.PLAYER_RESOURCES) {
            GuzhenrenResourceCostHelper.refund(player, payment);
        }
    }

    private boolean isPrimaryOrgan(ChestCavityInstance cc, ItemStack organ) {
        if (cc == null || cc.inventory == null || organ == null || organ.isEmpty()) {
            return false;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack slotStack = cc.inventory.getItem(i);
            if (slotStack == null || slotStack.isEmpty()) {
                continue;
            }
            if (!matchesOrgan(slotStack, ORGAN_ID)) {
                continue;
            }
            return slotStack == organ;
        }
        return false;
    }
}
