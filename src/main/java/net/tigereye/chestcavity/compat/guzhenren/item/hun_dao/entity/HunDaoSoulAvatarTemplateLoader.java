package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.entity;

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

/** Loads soul avatar templates from data packs. */
public final class HunDaoSoulAvatarTemplateLoader extends SimpleJsonResourceReloadListener {

  private static final Gson GSON = new GsonBuilder().setLenient().create();

  public HunDaoSoulAvatarTemplateLoader() {
    super(GSON, HunDaoSoulAvatarTemplates.DATA_FOLDER);
  }

  @Override
  protected void apply(
      Map<ResourceLocation, JsonElement> object,
      ResourceManager resourceManager,
      ProfilerFiller profiler) {
    Map<ResourceLocation, HunDaoSoulAvatarTemplate> templates = new HashMap<>();
    object.forEach(
        (id, element) -> {
          try {
            JsonObject obj = element.getAsJsonObject();
            templates.put(id, HunDaoSoulAvatarTemplate.fromJson(id, obj));
          } catch (Exception ex) {
            ChestCavity.LOGGER.error("[hun_dao][soul_avatar] Failed to parse template {}", id, ex);
          }
        });
    HunDaoSoulAvatarTemplates.update(templates);
  }
}
