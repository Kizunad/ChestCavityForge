package net.tigereye.chestcavity.compat.guzhenren.gufang;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.tigereye.chestcavity.ChestCavity;

public final class GuFangRecipeLoader extends SimpleJsonResourceReloadListener {

  private static final Gson GSON = new GsonBuilder().setLenient().create();

  public GuFangRecipeLoader() {
    super(GSON, "guzhenren/gufang");
  }

  @Override
  protected void apply(
      Map<ResourceLocation, JsonElement> object,
      ResourceManager resourceManager,
      ProfilerFiller profiler) {
    Map<ResourceLocation, GuFangRecipe> recipes = new HashMap<>();
    object.forEach(
        (id, element) -> {
          try {
            JsonObject obj = element.getAsJsonObject();
            recipes.put(id, GuFangRecipe.fromJson(id, obj));
          } catch (Exception ex) {
            ChestCavity.LOGGER.error("[GuFang] Failed to parse recipe {}", id, ex);
          }
        });
    GuFangRecipeRegistry.update(recipes);
    ChestCavity.LOGGER.info("[GuFang] Loaded recipes ({}).", recipes.size());
  }
}
