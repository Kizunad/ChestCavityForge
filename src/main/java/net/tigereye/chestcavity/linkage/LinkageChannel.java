package net.tigereye.chestcavity.linkage;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

/**
 * Relocated from the compat.guzhenren linkage package.
 *
 * <p>Holds the numeric state shared between organs. Channels are lightweight and only perform work
 * when listeners or policies are attached.
 */
public final class LinkageChannel {

  private final ActiveLinkageContext context;
  private final ResourceLocation id;
  private final List<LinkagePolicy> policies = new ArrayList<>();
  private final List<LinkageSubscriber> subscribers = new ArrayList<>();
  private double value;

  LinkageChannel(ActiveLinkageContext context, ResourceLocation id) {
    this.context = context;
    this.id = id;
  }

  public ResourceLocation id() {
    return id;
  }

  public double get() {
    return value;
  }

  /** Writes an absolute value into the channel after passing through policies. */
  public double set(double newValue) {
    LivingEntity entity = context.getEntity();
    ChestCavityInstance cc = context.getChestCavity();
    double processed = newValue;
    for (LinkagePolicy policy : policies) {
      processed = policy.apply(this, value, processed, context, entity, cc);
    }
    double previous = value;
    if (Double.compare(previous, processed) != 0) {
      value = processed;
      notifySubscribers(previous);
      if (ChestCavity.LOGGER.isDebugEnabled()) {
        ChestCavity.LOGGER.debug(
            "[Guzhenren] Channel {} updated {} -> {}",
            id,
            String.format("%.3f", previous),
            String.format("%.3f", value));
      }
    }
    return value;
  }

  /** Adds a delta to the channel value. */
  public double adjust(double delta) {
    if (delta == 0.0) {
      return value;
    }
    double result = set(value + delta);
    if (ChestCavity.LOGGER.isTraceEnabled()) {
      ChestCavity.LOGGER.trace(
          "[Guzhenren] Channel {} adjusted by {} (now {})",
          id,
          String.format("%.3f", delta),
          String.format("%.3f", result));
    }
    return result;
  }

  /** Invoked by the owning context every slow tick to execute time-based policies. */
  void tick(LivingEntity entity, ChestCavityInstance cc) {
    if (policies.isEmpty()) {
      return;
    }
    for (LinkagePolicy policy : policies) {
      policy.tick(this, context, entity, cc);
    }
  }

  public LinkageChannel addPolicy(LinkagePolicy policy) {
    if (policy != null && !policies.contains(policy)) {
      policies.add(policy);
      if (ChestCavity.LOGGER.isTraceEnabled()) {
        ChestCavity.LOGGER.trace(
            "[Guzhenren] Channel {} attached policy {}", id, policy.getClass().getSimpleName());
      }
    }
    return this;
  }

  public LinkageChannel addSubscriber(LinkageSubscriber subscriber) {
    if (subscriber != null && !subscribers.contains(subscriber)) {
      subscribers.add(subscriber);
      if (ChestCavity.LOGGER.isTraceEnabled()) {
        ChestCavity.LOGGER.trace(
            "[Guzhenren] Channel {} attached subscriber {}",
            id,
            subscriber.getClass().getSimpleName());
      }
    }
    return this;
  }

  private void notifySubscribers(double previous) {
    if (subscribers.isEmpty()) {
      return;
    }
    LivingEntity entity = context.getEntity();
    ChestCavityInstance cc = context.getChestCavity();
    for (LinkageSubscriber subscriber : subscribers) {
      subscriber.onChannelUpdated(this, previous, context, entity, cc);
    }
  }
}
