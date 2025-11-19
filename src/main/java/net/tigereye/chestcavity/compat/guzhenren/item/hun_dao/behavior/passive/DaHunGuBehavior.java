package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior.passive;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.HunDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior.common.HunDaoBehaviorContextHelper;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.runtime.HunDaoRuntimeContext;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.tuning.HunDaoTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.IntimidationHelper;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.registration.CCStatusEffects;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Passive behavior handler for the Da Hun Gu heart organ.
 *
 * <ul>
 *   <li>每秒恢复 2 点魂魄与 1 点念头；
 *   <li>若胸腔内存在小魂蛊且持有者非魂兽，则按胸腔内魂道蛊虫数量提供最多 20% 的魂魄恢复加成（魂意）；
 *   <li>若持有者处于魂兽状态，则提供威灵：魂道攻击魂魄消耗 -10，并震慑生命值低于当前魂魄值的敌对生物。
 * </ul>
 */
public final class DaHunGuBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganSlowTickListener {

  public static final DaHunGuBehavior INSTANCE = new DaHunGuBehavior();

  private static final Logger LOGGER = LogUtils.getLogger();
  private static final String MODULE_NAME = "da_hun_gu";

  private static final double SOUL_INTENT_PER_ORGAN = 0.0075;
  private static final double SOUL_INTENT_MAX = 0.15;
  private static final double WEILING_ATTACK_COST_REDUCTION = 10.0;
  private static final int WEILING_EFFECT_DURATION_TICKS =
      BehaviorConfigAccess.getInt(DaHunGuBehavior.class, "WEILING_EFFECT_DURATION_TICKS", 100);

  private static final String STATE_ROOT_KEY = "HunDaoDaHunGu";
  private static final String KEY_LAST_SYNC_TICK = "last_sync_tick";

  private DaHunGuBehavior() {}

  /**
   * Ensures that any necessary data linkages are established.
   *
   * @param cc The chest cavity instance.
   */
  public void ensureAttached(ChestCavityInstance cc) {
    // 大魂蛊当前不需要额外的联动通道，预留入口以便未来扩展。
  }

  /**
   * Called when the organ is equipped.
   *
   * @param cc The chest cavity instance.
   * @param organ The organ being equipped.
   * @param staleRemovalContexts A list of stale removal contexts.
   */
  public void onEquip(
      ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
    if (cc == null || organ == null || organ.isEmpty()) {
      return;
    }
    sendSlotUpdate(cc, organ);
  }

  /** {@inheritDoc} */
  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof Player player) || entity.level().isClientSide()) {
      return;
    }

    // Use runtime context for all resource operations
    HunDaoRuntimeContext runtimeContext = HunDaoBehaviorContextHelper.getContext(player);

    boolean soulBeast = SoulBeastStateManager.isActive(player);
    double soulIntentBonus = (!soulBeast && hasXiaoHunGu(cc)) ? computeSoulIntentBonus(cc) : 0.0;
    double hunpoGain = HunDaoTuning.DaHunGu.RECOVER * (1.0 + soulIntentBonus);
    if (hunpoGain > 0.0) {
      runtimeContext.getResourceOps().adjustDouble(player, "hunpo", hunpoGain, true, "zuida_hunpo");
      HunDaoBehaviorContextHelper.debugLog(
          MODULE_NAME,
          player,
          "+{} hunpo (soul_intent_bonus={})",
          HunDaoBehaviorContextHelper.format(hunpoGain),
          HunDaoBehaviorContextHelper.format(soulIntentBonus));
    }
    if (HunDaoTuning.DaHunGu.NIANTOU > 0.0) {
      runtimeContext
          .getResourceOps()
          .adjustDouble(player, "niantou", HunDaoTuning.DaHunGu.NIANTOU, true, "niantou_zuida");
    }
    if (soulBeast && hasDaHunGu(cc)) {
      double currentHunpo = runtimeContext.getResourceOps().readHunpo(player);
      applyWeiling(player, currentHunpo);
    }
    OrganState state = organState(organ, STATE_ROOT_KEY);
    OrganStateOps.setLong(
        state, cc, organ, KEY_LAST_SYNC_TICK, entity.level().getGameTime(), value -> value, 0L);
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
    HunDaoBehaviorContextHelper.debugLog(
        MODULE_NAME,
        "soul intent bonus computed: organs={} bonus={}",
        total,
        HunDaoBehaviorContextHelper.format(bonus));
    return bonus;
  }

  private void applyWeiling(Player player, double hunpoValue) {
    if (hunpoValue <= 0.0) {
      return;
    }
    IntimidationHelper.Settings settings =
        new IntimidationHelper.Settings(
            hunpoValue,
            IntimidationHelper.AttitudeScope.HOSTILE,
            CCStatusEffects.SOUL_BEAST_INTIMIDATED,
            WEILING_EFFECT_DURATION_TICKS,
            0,
            false,
            true,
            true,
            false);
    int affected =
        IntimidationHelper.intimidateNearby(player, HunDaoTuning.Effects.DETER_RADIUS, settings);
    if (affected > 0) {
      HunDaoBehaviorContextHelper.debugLog(
          MODULE_NAME,
          "weiling intimidated {} targets (hunpo={})",
          affected,
          HunDaoBehaviorContextHelper.format(hunpoValue));
    }
  }

  /**
   * Checks if the given chest cavity contains a Da Hun Gu.
   *
   * @param cc The chest cavity instance to check.
   * @return {@code true} if the chest cavity contains a Da Hun Gu, {@code false} otherwise.
   */
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

  /**
   * Calculates the hunpo cost reduction for attacks.
   *
   * @param player The player.
   * @param cc The chest cavity instance.
   * @return The hunpo cost reduction.
   */
  public static double attackHunpoCostReduction(Player player, ChestCavityInstance cc) {
    if (player == null || !SoulBeastStateManager.isActive(player)) {
      return 0.0;
    }
    return hasDaHunGu(cc) ? WEILING_ATTACK_COST_REDUCTION : 0.0;
  }
}
