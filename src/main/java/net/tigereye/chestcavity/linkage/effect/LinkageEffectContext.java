package net.tigereye.chestcavity.linkage.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.listeners.OrganHealListener;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnFireListener;
import net.tigereye.chestcavity.listeners.OrganOnGroundListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

import java.util.List;
import java.util.Map;

/**
 * Relocated from the compat.guzhenren linkage package.
 *
 * Provides helper utilities for linkage effects so behaviour registration is concise
 * and consistent. The context is scoped to the current chest cavity evaluation pass.
 */
public interface LinkageEffectContext {

    /** Returns the chest cavity this effect operates on. */
    ChestCavityInstance chestCavity();

    /** Returns the organ stack that triggered the registration. */
    ItemStack sourceOrgan();

    /**
     * Returns a view of all organ stacks participating in the effect. The list may contain
     * multiple stacks when the requirement spans several organ items.
     */
    List<ItemStack> matchingOrgans();

    /** Minimum organ counts required for the effect. */
    Map<ResourceLocation, Integer> requirements();

    /** Actual organ counts currently present in the chest cavity for the requirement set. */
    Map<ResourceLocation, Integer> matchingCounts();

    /** Exposes the linkage context used to look up or create channels. */
    ActiveLinkageContext linkageContext();

    /** Mutable view of stale removal contexts from the evaluation pass. */
    List<OrganRemovalContext> staleRemovalContexts();

    /** Registers a slow tick listener for the provided organ stack. */
    void addSlowTickListener(ItemStack organ, OrganSlowTickListener listener);

    /** Convenience overload binding the listener to {@link #sourceOrgan()}. */
    default void addSlowTickListener(OrganSlowTickListener listener) {
        addSlowTickListener(sourceOrgan(), listener);
    }

    /** Registers an on-hit listener for the provided organ stack. */
    void addOnHitListener(ItemStack organ, OrganOnHitListener listener);

    default void addOnHitListener(OrganOnHitListener listener) {
        addOnHitListener(sourceOrgan(), listener);
    }

    /** Registers an incoming damage listener for the provided organ stack. */
    void addIncomingDamageListener(ItemStack organ, OrganIncomingDamageListener listener);

    default void addIncomingDamageListener(OrganIncomingDamageListener listener) {
        addIncomingDamageListener(sourceOrgan(), listener);
    }

    /** Registers an on-fire listener for the provided organ stack. */
    void addOnFireListener(ItemStack organ, OrganOnFireListener listener);

    default void addOnFireListener(OrganOnFireListener listener) {
        addOnFireListener(sourceOrgan(), listener);
    }

    /** Registers an on-ground listener for the provided organ stack. */
    void addOnGroundListener(ItemStack organ, OrganOnGroundListener listener);

    default void addOnGroundListener(OrganOnGroundListener listener) {
        addOnGroundListener(sourceOrgan(), listener);
    }

    /** Registers a heal listener for the provided organ stack. */
    void addHealListener(ItemStack organ, OrganHealListener listener);

    default void addHealListener(OrganHealListener listener) {
        addHealListener(sourceOrgan(), listener);
    }

    /** Registers a removal listener for the provided organ stack. */
    void addRemovalListener(ItemStack organ, OrganRemovalListener listener);

    default void addRemovalListener(OrganRemovalListener listener) {
        addRemovalListener(sourceOrgan(), listener);
    }
}
