package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior.passive;

// file moved from HunDaoHeartBehavior.java to match public class name

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior.common.HunDaoBehaviorContextHelper;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.runtime.HunDaoRuntimeContext;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.tuning.HunDaoTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.IncreaseEffectLedger;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/** Passive behavior handler for the Xiao Hun Gu heart organ. */
public final class XiaoHunGuBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganSlowTickListener, OrganRemovalListener {

  public static final XiaoHunGuBehavior INSTANCE = new XiaoHunGuBehavior();

  private static final Logger LOGGER = LogUtils.getLogger();
  private static final String MOD_ID = "guzhenren";
  private static final String MODULE_NAME = "xiao_hun_gu";

  private static final ResourceLocation HUN_DAO_INCREASE_EFFECT =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/hun_dao_increase_effect");

  private static final String STATE_ROOT_KEY = "HunDaoHeart";
  private static final String KEY_LAST_SYNC_TICK = "last_sync_tick";

  private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

  private XiaoHunGuBehavior() {}

  /**
   * Ensures that any necessary data linkages are established.
   *
   * @param cc The chest cavity instance.
   */
  public void ensureAttached(ChestCavityInstance cc) {
    if (cc == null) {
      return;
    }
    ActiveLinkageContext context = LinkageManager.getContext(cc);
    ensureChannel(context, HUN_DAO_INCREASE_EFFECT);
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
    registerRemovalHook(cc, organ, this, staleRemovalContexts);
    ensureAttached(cc);
    ActiveLinkageContext context = LinkageManager.getContext(cc);
    LinkageChannel channel =
        context.getOrCreateChannel(HUN_DAO_INCREASE_EFFECT).addPolicy(NON_NEGATIVE);
    refreshIncreaseContribution(cc, organ, true);
    sendSlotUpdate(cc, organ);
  }

  /** {@inheritDoc} */
  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (entity.level().isClientSide()) {
      return;
    }
    if (entity instanceof Player player) {
      handlerPlayer(player, cc, organ);
    } else {
      handlerNonPlayer(entity, cc, organ);
    }
  }

  private void handlerPlayer(Player player, ChestCavityInstance cc, ItemStack organ) {
    if (cc == null) {
      return;
    }
    ensureAttached(cc);

    // Use runtime context for all resource operations
    HunDaoRuntimeContext runtimeContext = HunDaoBehaviorContextHelper.getContext(player);

    ActiveLinkageContext context = LinkageManager.getContext(cc);
    LinkageChannel channel =
        context.getOrCreateChannel(HUN_DAO_INCREASE_EFFECT).addPolicy(NON_NEGATIVE);
    IncreaseEffectLedger ledger = context.increaseEffects();
    ledger.verifyAndRebuildIfNeeded();
    double contribution = ledger.adjust(organ, HUN_DAO_INCREASE_EFFECT, 0.0);
    if (contribution <= 0.0) {
      return; // only one Xiao Hun Gu may apply its effect
    }
    double bonus = Math.max(0.0, channel.get());
    double baseAmount = HunDaoTuning.XiaoHunGu.RECOVER * (1.0 + bonus);
    if (baseAmount <= 0.0) {
      return;
    }
    double scarMultiplier = computeScarMultiplier(runtimeContext, player);
    double amount = baseAmount * scarMultiplier;

    // Access resource ops through runtime context
    runtimeContext.getResourceOps().adjustDouble(player, "hunpo", amount, true, "zuida_hunpo");

    HunDaoBehaviorContextHelper.debugLog(
        MODULE_NAME,
        player,
        "+{} hunpo (bonus={} scar_mult={})",
        HunDaoBehaviorContextHelper.format(amount),
        HunDaoBehaviorContextHelper.format(bonus),
        HunDaoBehaviorContextHelper.format(scarMultiplier));

    OrganState state = organState(organ, STATE_ROOT_KEY);
    OrganStateOps.setLong(
        state, cc, organ, KEY_LAST_SYNC_TICK, player.level().getGameTime(), value -> value, 0L);
  }

  private void handlerNonPlayer(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    HunDaoBehaviorContextHelper.debugLog(
        MODULE_NAME,
        "skip nonplayer entity={} type={}",
        entity.getStringUUID(),
        entity.getType().toShortString());
  }

  /** {@inheritDoc} */
  @Override
  public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (cc == null) {
      return;
    }
    ActiveLinkageContext context = LinkageManager.getContext(cc);
    IncreaseEffectLedger ledger = context.increaseEffects();
    ledger.verifyAndRebuildIfNeeded();
    double previous = ledger.adjust(organ, HUN_DAO_INCREASE_EFFECT, 0.0);
    refreshIncreaseContribution(cc, organ, false);
    if (previous > 0.0) {
      promoteReplacement(cc, organ);
    }
  }

  private void refreshIncreaseContribution(
      ChestCavityInstance cc, ItemStack organ, boolean requestPrimary) {
    if (cc == null || organ == null || organ.isEmpty()) {
      return;
    }
    ActiveLinkageContext context = LinkageManager.getContext(cc);
    IncreaseEffectLedger ledger = context.increaseEffects();
    ledger.verifyAndRebuildIfNeeded();
    double previous = ledger.adjust(organ, HUN_DAO_INCREASE_EFFECT, 0.0);
    double totalWithoutSelf = Math.max(0.0, ledger.total(HUN_DAO_INCREASE_EFFECT) - previous);
    boolean activate = requestPrimary && totalWithoutSelf <= 0.0;
    double target = activate ? HunDaoTuning.XiaoHunGu.RECOVER_BONUS : 0.0;
    double delta = target - previous;
    if (target > 0.0) {
      LedgerOps.set(
          cc,
          organ,
          HUN_DAO_INCREASE_EFFECT,
          Math.max(1, organ.getCount()),
          target,
          NON_NEGATIVE,
          true);
    } else {
      LedgerOps.remove(cc, organ, HUN_DAO_INCREASE_EFFECT, NON_NEGATIVE, true);
    }
  }

  private void promoteReplacement(ChestCavityInstance cc, ItemStack removed) {
    if (cc == null || cc.inventory == null) {
      return;
    }
    ActiveLinkageContext context = LinkageManager.getContext(cc);
    IncreaseEffectLedger ledger = context.increaseEffects();
    ledger.verifyAndRebuildIfNeeded();
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack candidate = cc.inventory.getItem(i);
      if (candidate == null || candidate.isEmpty() || candidate == removed) {
        continue;
      }
      if (candidate.getItem() != removed.getItem()) {
        continue;
      }
      refreshIncreaseContribution(cc, candidate, true);
      break;
    }
  }

  private double computeScarMultiplier(HunDaoRuntimeContext context, Player player) {
    if (context == null || player == null) {
      return 1.0;
    }
    long now = player.level().getGameTime();
    double scar = Math.max(0.0, context.getScarOps().effectiveCached(player, now));
    return 1.0 + scar;
  }
}
