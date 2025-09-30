package net.tigereye.chestcavity.debug;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.tigereye.chestcavity.ChestCavity;

import java.util.List;
import java.util.Locale;

/**
 * Debug-only reload listener that counts resources under data/chestcavity/recipes.
 */
public final class RecipeResourceProbe extends SimplePreparableReloadListener<List<ResourceLocation>> {

    private static final boolean ENABLED = true; // flip to false to disable

    @Override
    protected List<ResourceLocation> prepare(ResourceManager manager, ProfilerFiller profiler) {
        if (!ENABLED) return List.of();
        // listResources returns a map; collect keys
        return manager.listResources("recipes", id -> id.getPath().endsWith(".json")).keySet().stream().toList();
    }

    @Override
    protected void apply(List<ResourceLocation> resources, ResourceManager manager, ProfilerFiller profiler) {
        if (!ENABLED) return;
        long chestcavityCount = resources.stream().filter(id -> id.getNamespace().equals(ChestCavity.MODID)).count();
        ChestCavity.LOGGER.info("[Recipes][Probe] Resources under data/*/recipes: total={} chestcavity={} (pack reload)",
                resources.size(), chestcavityCount);
        // Print first few ids for the namespace to aid diagnosis
        resources.stream()
                .filter(id -> id.getNamespace().equals(ChestCavity.MODID))
                .sorted((a, b) -> a.toString().compareTo(b.toString()))
                .limit(5)
                .forEach(id -> ChestCavity.LOGGER.info("[Recipes][Probe] example: {}", id));
    }
}
