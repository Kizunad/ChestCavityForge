package net.tigereye.chestcavity.world.spawn;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 自定义生成定义注册表。
 */
public final class CustomMobSpawnRegistry {

    private static final Map<ResourceLocation, CustomMobSpawnDefinition> DEFINITIONS = new LinkedHashMap<>();

    private CustomMobSpawnRegistry() {}

    public static synchronized CustomMobSpawnDefinition register(CustomMobSpawnDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return DEFINITIONS.put(definition.id(), definition);
    }

    public static synchronized Collection<CustomMobSpawnDefinition> definitions() {
        return Collections.unmodifiableCollection(DEFINITIONS.values());
    }

    public static synchronized CustomMobSpawnDefinition get(ResourceLocation id) {
        return DEFINITIONS.get(id);
    }

    public static synchronized boolean isEmpty() {
        return DEFINITIONS.isEmpty();
    }
}
