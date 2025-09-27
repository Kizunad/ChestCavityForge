package net.tigereye.chestcavity.compat.guzhenren.item.common;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.util.ChestCavityUtil;
import net.tigereye.chestcavity.util.NetworkUtil;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;

/**
 * Shared utilities for Guzhenren organ behaviours to avoid copy-pasted plumbing.
 */
public abstract class AbstractGuzhenrenOrganBehavior {

    protected AbstractGuzhenrenOrganBehavior() {
    }

    protected boolean matchesOrgan(ItemStack stack, ItemLike canonicalItem, ResourceLocation... organIds) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (canonicalItem != null && stack.is(canonicalItem.asItem())) {
            return true;
        }
        if (organIds == null || organIds.length == 0) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        for (ResourceLocation candidate : organIds) {
            if (candidate != null && candidate.equals(id)) {
                return true;
            }
        }
        return false;
    }

    protected boolean matchesOrgan(ItemStack stack, ResourceLocation... organIds) {
        return matchesOrgan(stack, null, organIds);
    }

    protected RemovalRegistration registerRemovalHook(
            ChestCavityInstance cc,
            ItemStack organ,
            OrganRemovalListener listener,
            List<OrganRemovalContext> staleRemovalContexts
    ) {
        if (cc == null || organ == null || organ.isEmpty() || listener == null) {
            return RemovalRegistration.EMPTY;
        }
        int slotIndex = ChestCavityUtil.findOrganSlot(cc, organ);

        boolean restoredFromStale = false;
        if (staleRemovalContexts != null) {
            for (int i = 0; i < staleRemovalContexts.size(); ) {
                OrganRemovalContext old = staleRemovalContexts.get(i);
                if (ChestCavityUtil.matchesRemovalContext(old, slotIndex, organ, listener)) {
                    staleRemovalContexts.remove(i);
                    restoredFromStale = true;
                    continue;
                }
                i++;
            }
        }

        boolean presentInCurrentList = cc.onRemovedListeners.stream()
                .anyMatch(existing -> ChestCavityUtil.matchesRemovalContext(existing, slotIndex, organ, listener));
        if (!presentInCurrentList) {
            cc.onRemovedListeners.add(new OrganRemovalContext(slotIndex, organ, listener));
        }
        return new RemovalRegistration(slotIndex, restoredFromStale || presentInCurrentList);
    }

    protected LinkageChannel ensureChannel(ActiveLinkageContext context, ResourceLocation channelId) {
        if (context == null || channelId == null) {
            return null;
        }
        return context.getOrCreateChannel(channelId);
    }

    protected void ensureChannels(ActiveLinkageContext context, Collection<ResourceLocation> channelIds) {
        if (context == null || channelIds == null) {
            return;
        }
        for (ResourceLocation channelId : channelIds) {
            ensureChannel(context, channelId);
        }
    }

    protected void sendSlotUpdate(ChestCavityInstance cc, ItemStack organ) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }

    protected OrganState organState(ItemStack stack, String rootKey) {
        return OrganState.of(stack, rootKey);
    }

    protected void logStateChange(
            Logger logger,
            String prefix,
            ItemStack stack,
            String key,
            OrganState.Change<?> change
    ) {
        if (logger == null || !logger.isDebugEnabled() || change == null || !change.changed()) {
            return;
        }
        logger.debug("{} Updated {} for {} from {} to {}", prefix, key, describeStack(stack), change.previous(), change.current());
    }

    protected String describeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "<empty>";
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return stack.getCount() + "x " + id;
    }

    protected record RemovalRegistration(int slotIndex, boolean alreadyRegistered) {
        private static final RemovalRegistration EMPTY = new RemovalRegistration(-1, false);
    }
}
