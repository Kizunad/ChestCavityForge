package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.IncreaseEffectContributor;
import net.tigereye.chestcavity.linkage.IncreaseEffectLedger;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;

/** Helpers around IncreaseEffectLedger + LinkageChannel operations. */
public final class LedgerOps {

  private LedgerOps() {}

  /** Ensure the channel exists and attach optional policies. */
  public static LinkageChannel ensureChannel(
      ActiveLinkageContext context, ResourceLocation channelId, ClampPolicy... policies) {
    if (context == null || channelId == null) {
      return null;
    }
    LinkageChannel channel = context.getOrCreateChannel(channelId);
    if (policies != null) {
      for (ClampPolicy policy : policies) {
        if (policy != null) {
          channel.addPolicy(policy);
        }
      }
    }
    return channel;
  }

  /** Ensure the channel exists on the provided chest cavity. */
  public static LinkageChannel ensureChannel(
      ChestCavityInstance cc, ResourceLocation channelId, ClampPolicy... policies) {
    return ensureChannel(context(cc), channelId, policies);
  }

  /** Lookup a channel if it exists without creating it. */
  public static Optional<LinkageChannel> lookupChannel(
      ActiveLinkageContext context, ResourceLocation channelId) {
    if (context == null || channelId == null) {
      return Optional.empty();
    }
    return context.lookupChannel(channelId);
  }

  /** Lookup a channel if it exists without creating it. */
  public static Optional<LinkageChannel> lookupChannel(
      ChestCavityInstance cc, ResourceLocation channelId) {
    return lookupChannel(context(cc), channelId);
  }

  /** Resolve the linkage context associated with the chest cavity. */
  public static ActiveLinkageContext context(ChestCavityInstance cc) {
    if (cc == null) {
      return null;
    }
    return LinkageManager.getContext(cc);
  }

  /** Register the organ as a contributor for one or more channels. */
  public static void registerContributor(
      ChestCavityInstance cc,
      ItemStack organ,
      IncreaseEffectContributor contributor,
      ResourceLocation... channelIds) {
    if (cc == null || organ == null || organ.isEmpty() || contributor == null) {
      return;
    }
    ActiveLinkageContext context = context(cc);
    if (context == null) {
      return;
    }
    IncreaseEffectLedger ledger = context.increaseEffects();
    ledger.registerContributor(organ, contributor, channelIds);
  }

  /** Adjust both channel value and ledger entry, then verify/rebuild if needed. */
  public static void adjust(
      ChestCavityInstance cc,
      ItemStack organ,
      ResourceLocation channelId,
      double delta,
      ClampPolicy policy,
      boolean verify) {
    if (cc == null || organ == null || organ.isEmpty() || channelId == null || delta == 0.0) {
      return;
    }
    ActiveLinkageContext context = context(cc);
    if (context == null) {
      return;
    }
    // Ensure channel & policy
    LinkageChannel channel = ensureChannel(context, channelId, policy);
    if (channel != null) {
      channel.adjust(delta);
    }
    // Record in ledger
    IncreaseEffectLedger ledger = context.increaseEffects();
    ledger.adjust(organ, channelId, delta);
    if (verify) {
      ledger.verifyAndRebuildIfNeeded();
    }
  }

  /**
   * Set absolute contribution for an organ and reconcile the channel (delta-applied), then
   * optionally verify.
   */
  public static void set(
      ChestCavityInstance cc,
      ItemStack organ,
      ResourceLocation channelId,
      int stackCount,
      double effect,
      ClampPolicy policy,
      boolean verify) {
    if (cc == null || organ == null || organ.isEmpty() || channelId == null) {
      return;
    }
    ActiveLinkageContext context = context(cc);
    if (context == null) {
      return;
    }
    IncreaseEffectLedger ledger = context.increaseEffects();
    double previous = ledger.adjust(organ, channelId, 0.0);
    double delta = effect - previous;
    LinkageChannel channel = ensureChannel(context, channelId, policy);
    if (channel != null && delta != 0.0) {
      channel.adjust(delta);
    }
    ledger.set(organ, channelId, Math.max(1, stackCount), effect);
    if (verify) {
      ledger.verifyAndRebuildIfNeeded();
    }
  }

  /**
   * Remove organ contribution and reconcile the channel by subtracting the previous value;
   * optionally verify.
   */
  public static void remove(
      ChestCavityInstance cc,
      ItemStack organ,
      ResourceLocation channelId,
      ClampPolicy policy,
      boolean verify) {
    if (cc == null || organ == null || organ.isEmpty() || channelId == null) {
      return;
    }
    ActiveLinkageContext context = LinkageManager.getContext(cc);
    if (context == null) {
      return;
    }
    IncreaseEffectLedger ledger = context.increaseEffects();
    double previous = ledger.adjust(organ, channelId, 0.0);
    if (previous != 0.0) {
      LinkageChannel channel = ensureChannel(context, channelId, policy);
      if (channel != null) {
        channel.adjust(-previous);
      }
    }
    ledger.remove(organ, channelId);
    if (verify) {
      ledger.verifyAndRebuildIfNeeded();
    }
  }
}
