package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.HunDaoOrganRegistry;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.compat.guzhenren.util.IntimidationHelper;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.soulbeast.state.SoulBeastStateManager;
import org.slf4j.Logger;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * 大魂蛊（心脏）被动行为：
 * <ul>
 *     <li>每秒恢复 2 点魂魄与 1 点念头；</li>
 *     <li>若胸腔内存在小魂蛊且持有者非魂兽，则按胸腔内魂道蛊虫数量提供最多 20% 的魂魄恢复加成（魂意）；</li>
 *     <li>若持有者处于魂兽状态，则提供威灵：魂道攻击魂魄消耗 -10，并震慑生命值低于当前魂魄值的敌对生物。</li>
 * </ul>
 */
public final class DaHunGuBehavior extends AbstractGuzhenrenOrganBehavior implements OrganSlowTickListener {

    public static final DaHunGuBehavior INSTANCE = new DaHunGuBehavior();

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final double BASE_HUNPO_RECOVERY_PER_SECOND = 2.0;
    private static final double BASE_NIANTOU_RECOVERY_PER_SECOND = 1.0;
    private static final double SOUL_INTENT_PER_ORGAN = 0.01;
    private static final double SOUL_INTENT_MAX = 0.20;
    private static final double WEILING_ATTACK_COST_REDUCTION = 10.0;
    private static final double WEILING_RADIUS = 8.0D;
    private static final int WEILING_EFFECT_DURATION_TICKS = 100;

    private static final String STATE_ROOT_KEY = "HunDaoDaHunGu";
    private static final String KEY_LAST_SYNC_TICK = "last_sync_tick";

    private DaHunGuBehavior() {
    }

    public void ensureAttached(ChestCavityInstance cc) {
        // 大魂蛊当前不需要额外的联动通道，预留入口以便未来扩展。
    }

    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        sendSlotUpdate(cc, organ);
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }
        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return;
        }
        ResourceHandle handle = handleOpt.get();
        boolean soulBeast = SoulBeastStateManager.isActive(player);
        double soulIntentBonus = (!soulBeast && hasXiaoHunGu(cc)) ? computeSoulIntentBonus(cc) : 0.0;
        double hunpoGain = BASE_HUNPO_RECOVERY_PER_SECOND * (1.0 + soulIntentBonus);
        if (hunpoGain > 0.0) {
            handle.adjustDouble("hunpo", hunpoGain, true, "zuida_hunpo");
            LOGGER.debug("[compat/guzhenren][hun_dao][da_hun_gu] +{} hunpo (soul_intent_bonus={}) -> {}",
                    format(hunpoGain), format(soulIntentBonus), describePlayer(player));
        }
        if (BASE_NIANTOU_RECOVERY_PER_SECOND > 0.0) {
            handle.adjustDouble("niantou", BASE_NIANTOU_RECOVERY_PER_SECOND, true, "niantou_zuida");
        }
        if (soulBeast && hasDaHunGu(cc)) {
            double currentHunpo = handle.read("hunpo").orElse(0.0);
            applyWeiling(player, currentHunpo);
        }
        OrganState state = organState(organ, STATE_ROOT_KEY);
        state.setLong(KEY_LAST_SYNC_TICK, entity.level().getGameTime());
    }

    private boolean hasXiaoHunGu(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return false;
        }
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (HunDaoOrganRegistry.XIAO_HUN_GU_ID.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private double computeSoulIntentBonus(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return 0.0;
        }
        Set<ResourceLocation> organIds = HunDaoOrganRegistry.organIds();
        if (organIds.isEmpty()) {
            return 0.0;
        }
        int total = 0;
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!organIds.contains(id)) {
                continue;
            }
            total += Math.max(1, stack.getCount());
        }
        if (total <= 0) {
            return 0.0;
        }
        double bonus = Math.min(SOUL_INTENT_MAX, total * SOUL_INTENT_PER_ORGAN);
        LOGGER.debug("[compat/guzhenren][hun_dao][da_hun_gu] soul intent bonus computed: organs={} bonus={}", total,
                format(bonus));
        return bonus;
    }

    private void applyWeiling(Player player, double hunpoValue) {
        if (hunpoValue <= 0.0) {
            return;
        }
        IntimidationHelper.Settings settings = new IntimidationHelper.Settings(
                hunpoValue,
                IntimidationHelper.AttitudeScope.HOSTILE,
                MobEffects.WEAKNESS,
                WEILING_EFFECT_DURATION_TICKS,
                0,
                false,
                true,
                true,
                false
        );
        int affected = IntimidationHelper.intimidateNearby(player, WEILING_RADIUS, settings);
        if (affected > 0) {
            LOGGER.debug("[compat/guzhenren][hun_dao][da_hun_gu] weiling intimidated {} targets (hunpo={})", affected,
                    format(hunpoValue));
        }
    }

    private String describePlayer(Player player) {
        return player == null ? "<unknown>" : player.getScoreboardName();
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public static boolean hasDaHunGu(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return false;
        }
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (HunDaoOrganRegistry.DA_HUN_GU_ID.equals(id)) {
                return true;
            }
        }
        return false;
    }

    public static double attackHunpoCostReduction(Player player, ChestCavityInstance cc) {
        if (player == null || !SoulBeastStateManager.isActive(player)) {
            return 0.0;
        }
        return hasDaHunGu(cc) ? WEILING_ATTACK_COST_REDUCTION : 0.0;
    }
}
