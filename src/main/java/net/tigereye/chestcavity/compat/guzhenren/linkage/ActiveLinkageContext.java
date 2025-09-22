package net.tigereye.chestcavity.compat.guzhenren.linkage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Runtime state container that keeps track of linkage channels, policies and trigger endpoints
 * for a specific {@link ChestCavityInstance}. All coordination between Guzhenren organs happens
 * through this context.
 */
public final class ActiveLinkageContext {

    private final ChestCavityInstance chestCavity;
    private final Map<ResourceLocation, LinkageChannel> channels = new LinkedHashMap<>();
    private final EnumMap<TriggerType, List<TriggerEndpoint>> triggers = new EnumMap<>(TriggerType.class);
    private CompoundTag deferredLoadData;

    ActiveLinkageContext(ChestCavityInstance chestCavity) {
        this.chestCavity = chestCavity;
    }

    /** Returns the owning chest cavity. */
    public ChestCavityInstance getChestCavity() {
        return chestCavity;
    }

    /** Convenience accessor for the living entity that owns the chest cavity. */
    public LivingEntity getEntity() {
        return chestCavity.owner;
    }

    /**
     * Retrieves or creates a channel by id. The channel is initialised with neutral defaults,
     * call {@link #configureChannel(ResourceLocation, Consumer)} if you need to customise it right away.
     */
    public LinkageChannel getOrCreateChannel(ResourceLocation id) {
        flushDeferredLoad();
        return channels.computeIfAbsent(id, key -> new LinkageChannel(this, key));
    }

    /**
     * Fetches an existing channel if present.
     */
    public Optional<LinkageChannel> lookupChannel(ResourceLocation id) {
        return Optional.ofNullable(channels.get(id));
    }

    /**
     * Configures the channel with the supplied callback (creating it when missing) and then returns it.
     */
    public LinkageChannel configureChannel(ResourceLocation id, Consumer<LinkageChannel> configurator) {
        LinkageChannel channel = getOrCreateChannel(id);
        configurator.accept(channel);
        return channel;
    }

    /** Registers a trigger endpoint to be fired when the matching {@link TriggerType} is broadcast. */
    public void registerTrigger(TriggerEndpoint endpoint) {
        triggers.computeIfAbsent(endpoint.type(), unused -> new ArrayList<>()).add(endpoint);
    }

    /** Broadcasts a trigger manually; primarily used for active triggers. */
    public void broadcast(TriggerType type) {
        fireTriggers(type);
    }

    /** Internal slow-tick entry point invoked by the manager once per cadence. */
    void onSlowTick() {
        LivingEntity entity = getEntity();
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        flushDeferredLoad();
        for (LinkageChannel channel : channels.values()) {
            channel.tick(entity, chestCavity);
        }
        fireTriggers(TriggerType.SLOW_TICK);
    }

    private void fireTriggers(TriggerType type) {
        List<TriggerEndpoint> endpoints = triggers.get(type);
        if (endpoints == null || endpoints.isEmpty()) {
            return;
        }
        LivingEntity entity = getEntity();
        if (entity == null) {
            return;
        }
        long gameTime = entity.level().getGameTime();
        for (TriggerEndpoint endpoint : endpoints) {
            endpoint.tryFire(gameTime, entity, chestCavity, this);
        }
    }

    /** @return true when no channels currently store values. */
    boolean isEmpty() {
        if (!channels.isEmpty()) {
            for (LinkageChannel channel : channels.values()) {
                if (channel.get() != 0.0) {
                    return false;
                }
            }
        }
        return deferredLoadData == null || deferredLoadData.isEmpty();
    }

    /** Serialises all channel values into a tag for persistence. */
    CompoundTag writeToTag() {
        if (channels.isEmpty()) {
            return deferredLoadData == null ? new CompoundTag() : deferredLoadData.copy();
        }
        CompoundTag tag = new CompoundTag();
        channels.forEach((id, channel) -> {
            double value = channel.get();
            if (value != 0.0) {
                tag.putDouble(id.toString(), value);
            }
        });
        return tag;
    }

    /** Restores channel values from the provided tag, applying them once channels are ready. */
    void readFromTag(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return;
        }
        if (!channels.isEmpty()) {
            tag.getAllKeys().forEach(key -> {
                ResourceLocation id = parseId(key);
                if (id != null) {
                    double value = tag.getDouble(key);
                    getOrCreateChannel(id).set(value);
                }
            });
        } else {
            // Delay applying until channels are constructed via organ registration.
            deferredLoadData = tag.copy();
        }
    }

    /** Applies any deferred data once at least one slow-tick pass has built channels. */
    private void flushDeferredLoad() {
        if (deferredLoadData == null || deferredLoadData.isEmpty()) {
            return;
        }
        CompoundTag copy = deferredLoadData;
        deferredLoadData = null;
        copy.getAllKeys().forEach(key -> {
            ResourceLocation id = parseId(key);
            if (id != null) {
                double value = copy.getDouble(key);
                getOrCreateChannel(id).set(value);
            }
        });
    }

    private static ResourceLocation parseId(String raw) {
        try {
            return ResourceLocation.parse(raw);
        } catch (IllegalArgumentException ex) {
            ChestCavity.LOGGER.warn("Ignoring invalid Guzhenren linkage id '{}' during load", raw, ex);
            return null;
        }
    }
}
