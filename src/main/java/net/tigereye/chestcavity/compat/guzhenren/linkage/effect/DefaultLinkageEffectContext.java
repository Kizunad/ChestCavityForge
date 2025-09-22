package net.tigereye.chestcavity.compat.guzhenren.linkage.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.listeners.OrganHealContext;
import net.tigereye.chestcavity.listeners.OrganHealListener;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageContext;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnFireContext;
import net.tigereye.chestcavity.listeners.OrganOnFireListener;
import net.tigereye.chestcavity.listeners.OrganOnGroundContext;
import net.tigereye.chestcavity.listeners.OrganOnGroundListener;
import net.tigereye.chestcavity.listeners.OrganOnHitContext;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickContext;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Default implementation that wires listener registration into the owning chest cavity.
 */
final class DefaultLinkageEffectContext implements LinkageEffectContext {

    private final ChestCavityInstance chestCavity;
    private final ItemStack sourceOrgan;
    private final List<ItemStack> matchingOrgans;
    private final Map<ResourceLocation, Integer> requirements;
    private final Map<ResourceLocation, Integer> matchingCounts;
    private final ActiveLinkageContext linkageContext;
    private final List<OrganRemovalContext> staleRemovalContexts;

    DefaultLinkageEffectContext(
            ChestCavityInstance chestCavity,
            ItemStack sourceOrgan,
            Map<ResourceLocation, Integer> requirements,
            List<ItemStack> matchingOrgans,
            Map<ResourceLocation, Integer> matchingCounts,
            List<OrganRemovalContext> staleRemovalContexts
    ) {
        this.chestCavity = Objects.requireNonNull(chestCavity, "chestCavity");
        this.sourceOrgan = Objects.requireNonNull(sourceOrgan, "sourceOrgan");
        this.requirements = Collections.unmodifiableMap(requirements);
        this.matchingCounts = Collections.unmodifiableMap(matchingCounts);
        this.matchingOrgans = Collections.unmodifiableList(matchingOrgans);
        this.linkageContext = GuzhenrenLinkageManager.getContext(chestCavity);
        this.staleRemovalContexts = Objects.requireNonNull(staleRemovalContexts, "staleRemovalContexts");
    }

    @Override
    public ChestCavityInstance chestCavity() {
        return chestCavity;
    }

    @Override
    public ItemStack sourceOrgan() {
        return sourceOrgan;
    }

    @Override
    public List<ItemStack> matchingOrgans() {
        return matchingOrgans;
    }

    @Override
    public Map<ResourceLocation, Integer> requirements() {
        return requirements;
    }

    @Override
    public Map<ResourceLocation, Integer> matchingCounts() {
        return matchingCounts;
    }

    @Override
    public ActiveLinkageContext linkageContext() {
        return linkageContext;
    }

    @Override
    public List<OrganRemovalContext> staleRemovalContexts() {
        return staleRemovalContexts;
    }

    @Override
    public void addSlowTickListener(ItemStack organ, OrganSlowTickListener listener) {
        if (listener == null) {
            return;
        }
        chestCavity.onSlowTickListeners.add(new OrganSlowTickContext(organ, listener));
    }

    @Override
    public void addOnHitListener(ItemStack organ, OrganOnHitListener listener) {
        if (listener == null) {
            return;
        }
        chestCavity.onHitListeners.add(new OrganOnHitContext(organ, listener));
    }

    @Override
    public void addIncomingDamageListener(ItemStack organ, OrganIncomingDamageListener listener) {
        if (listener == null) {
            return;
        }
        chestCavity.onDamageListeners.add(new OrganIncomingDamageContext(organ, listener));
    }

    @Override
    public void addOnFireListener(ItemStack organ, OrganOnFireListener listener) {
        if (listener == null) {
            return;
        }
        chestCavity.onFireListeners.add(new OrganOnFireContext(organ, listener));
    }

    @Override
    public void addOnGroundListener(ItemStack organ, OrganOnGroundListener listener) {
        if (listener == null) {
            return;
        }
        chestCavity.onGroundListeners.add(new OrganOnGroundContext(organ, listener));
    }

    @Override
    public void addHealListener(ItemStack organ, OrganHealListener listener) {
        if (listener == null) {
            return;
        }
        chestCavity.onHealListeners.add(new OrganHealContext(organ, listener));
    }

    @Override
    public void addRemovalListener(ItemStack organ, OrganRemovalListener listener) {
        if (listener == null) {
            return;
        }
        chestCavity.onRemovedListeners.add(new OrganRemovalContext(organ, listener));
    }
}
