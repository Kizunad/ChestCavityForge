package net.tigereye.chestcavity.linkage.effect;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageManager;
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
import net.tigereye.chestcavity.util.ChestCavityUtil;

/**
 * Relocated from the compat.guzhenren linkage package.
 *
 * <p>Default implementation that wires listener registration into the owning chest cavity.
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
      List<OrganRemovalContext> staleRemovalContexts) {
    this.chestCavity = Objects.requireNonNull(chestCavity, "chestCavity");
    this.sourceOrgan = Objects.requireNonNull(sourceOrgan, "sourceOrgan");
    this.requirements = Collections.unmodifiableMap(requirements);
    this.matchingCounts = Collections.unmodifiableMap(matchingCounts);
    this.matchingOrgans = Collections.unmodifiableList(matchingOrgans);
    this.linkageContext = LinkageManager.getContext(chestCavity);
    this.staleRemovalContexts =
        Objects.requireNonNull(staleRemovalContexts, "staleRemovalContexts");
    if (ChestCavity.LOGGER.isDebugEnabled()) {
      ChestCavity.LOGGER.debug(
          "[Guzhenren] Constructed effect context for {} via organ {} (requirements {})",
          describeChestCavity(chestCavity),
          describeStack(sourceOrgan),
          requirements);
    }
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
    logListenerRegistration("slow-tick", organ, listener);
  }

  @Override
  public void addOnHitListener(ItemStack organ, OrganOnHitListener listener) {
    if (listener == null) {
      return;
    }
    chestCavity.onHitListeners.add(new OrganOnHitContext(organ, listener));
    logListenerRegistration("on-hit", organ, listener);
  }

  @Override
  public void addIncomingDamageListener(ItemStack organ, OrganIncomingDamageListener listener) {
    if (listener == null) {
      return;
    }
    chestCavity.onDamageListeners.add(new OrganIncomingDamageContext(organ, listener));
    logListenerRegistration("incoming-damage", organ, listener);
  }

  @Override
  public void addOnFireListener(ItemStack organ, OrganOnFireListener listener) {
    if (listener == null) {
      return;
    }
    chestCavity.onFireListeners.add(new OrganOnFireContext(organ, listener));
    logListenerRegistration("on-fire", organ, listener);
  }

  @Override
  public void addOnGroundListener(ItemStack organ, OrganOnGroundListener listener) {
    if (listener == null) {
      return;
    }
    chestCavity.onGroundListeners.add(new OrganOnGroundContext(organ, listener));
    logListenerRegistration("on-ground", organ, listener);
  }

  @Override
  public void addHealListener(ItemStack organ, OrganHealListener listener) {
    if (listener == null) {
      return;
    }
    chestCavity.onHealListeners.add(new OrganHealContext(organ, listener));
    logListenerRegistration("heal", organ, listener);
  }

  @Override
  public void addRemovalListener(ItemStack organ, OrganRemovalListener listener) {
    if (listener == null) {
      return;
    }
    int slotIndex = ChestCavityUtil.findOrganSlot(chestCavity, organ);
    chestCavity.onRemovedListeners.add(new OrganRemovalContext(slotIndex, organ, listener));
    staleRemovalContexts.removeIf(
        old -> ChestCavityUtil.matchesRemovalContext(old, slotIndex, organ, listener));
    logListenerRegistration("removal", organ, listener);
  }

  private void logListenerRegistration(String type, ItemStack organ, Object listener) {
    if (!ChestCavity.LOGGER.isDebugEnabled()) {
      return;
    }
    ChestCavity.LOGGER.debug(
        "[Guzhenren] Registered {} listener {} for organ {} on {}",
        type,
        listener.getClass().getSimpleName(),
        describeStack(organ),
        describeChestCavity(chestCavity));
  }

  private static String describeStack(ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
      return "<empty>";
    }
    return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
  }

  private static String describeChestCavity(ChestCavityInstance cc) {
    if (cc == null || cc.owner == null) {
      return "<unbound>";
    }
    return cc.owner.getScoreboardName();
  }
}
