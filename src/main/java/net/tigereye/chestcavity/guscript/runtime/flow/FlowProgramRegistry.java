package net.tigereye.chestcavity.guscript.runtime.flow;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global registry of flow programs loaded from data packs.
 */
public final class FlowProgramRegistry {

    private static final Map<ResourceLocation, FlowProgram> PROGRAMS = new ConcurrentHashMap<>();

    private FlowProgramRegistry() {
    }

    public static void update(Map<ResourceLocation, FlowProgram> programs) {
        PROGRAMS.clear();
        PROGRAMS.putAll(programs);
        ChestCavity.LOGGER.info("[Flow] Loaded {} flow programs", PROGRAMS.size());
    }

    public static Optional<FlowProgram> get(ResourceLocation id) {
        return Optional.ofNullable(PROGRAMS.get(id));
    }

    public static Set<ResourceLocation> ids() {
        return Collections.unmodifiableSet(PROGRAMS.keySet());
    }
}
