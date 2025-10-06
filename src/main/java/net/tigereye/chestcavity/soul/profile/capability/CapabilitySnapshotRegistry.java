package net.tigereye.chestcavity.soul.profile.capability;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class CapabilitySnapshotRegistry {

    private static final Map<ResourceLocation, Supplier<CapabilitySnapshot>> REGISTRY = new LinkedHashMap<>();

    private CapabilitySnapshotRegistry() {
    }

    public static void register(ResourceLocation id, Supplier<CapabilitySnapshot> factory) {
        if (REGISTRY.containsKey(id)) {
            ChestCavity.LOGGER.warn("[soul] Overriding capability snapshot registration for id={}", id);
        }
        REGISTRY.put(id, factory);
    }

    public static Collection<ResourceLocation> keys() {
        return java.util.Collections.unmodifiableSet(REGISTRY.keySet());
    }

    public static Optional<CapabilitySnapshot> create(ResourceLocation id) {
        Supplier<CapabilitySnapshot> factory = REGISTRY.get(id);
        if (factory == null) {
            return Optional.empty();
        }
        return Optional.of(factory.get());
    }
}
