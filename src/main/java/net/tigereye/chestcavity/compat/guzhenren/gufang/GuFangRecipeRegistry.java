package net.tigereye.chestcavity.compat.guzhenren.gufang;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class GuFangRecipeRegistry {

    private static Map<ResourceLocation, GuFangRecipe> RECIPES = new HashMap<>();
    private static Map<String, GuFangRecipe> BY_GUFANG = new HashMap<>();

    private GuFangRecipeRegistry() {}

    public static void update(Map<ResourceLocation, GuFangRecipe> recipes) {
        RECIPES = Map.copyOf(recipes);
        Map<String, GuFangRecipe> by = new HashMap<>();
        for (GuFangRecipe r : recipes.values()) {
            by.put(r.guFangId, r);
        }
        BY_GUFANG = Collections.unmodifiableMap(by);
    }

    public static Optional<GuFangRecipe> find(ResourceLocation id) {
        return Optional.ofNullable(RECIPES.get(id));
    }

    public static Optional<GuFangRecipe> findByGuFangId(String guFangId) {
        if (guFangId == null || guFangId.isBlank()) return Optional.empty();
        return Optional.ofNullable(BY_GUFANG.get(guFangId));
    }
}

