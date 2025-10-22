package net.tigereye.chestcavity.guscript.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.fx.gecko.GeckoFxDefinition;
import net.tigereye.chestcavity.guscript.fx.gecko.GeckoFxDefinition.BlendMode;
import net.tigereye.chestcavity.guscript.fx.gecko.GeckoFxRegistry;

/** Loads GeckoLib FX definitions for GuScript flows. */
public final class GeckoFxDefinitionLoader extends SimpleJsonResourceReloadListener {

  private static final Gson GSON = new GsonBuilder().setLenient().create();

  public GeckoFxDefinitionLoader() {
    super(GSON, "geckofx");
  }

  @Override
  protected void apply(
      Map<ResourceLocation, JsonElement> object,
      ResourceManager resourceManager,
      ProfilerFiller profiler) {
    Map<ResourceLocation, GeckoFxDefinition> collected = new HashMap<>();
    object.forEach(
        (id, element) -> {
          try {
            if (!element.isJsonObject()) {
              ChestCavity.LOGGER.warn("[GuScript] Gecko FX {} is not a JSON object", id);
              return;
            }
            JsonObject json = element.getAsJsonObject();
            ResourceLocation model = parseResource(json, "model");
            ResourceLocation texture = parseResource(json, "texture");
            ResourceLocation animation = parseResource(json, "animation");
            String defaultAnimation =
                json.has("default_animation")
                    ? GsonHelper.getAsString(json, "default_animation").trim()
                    : null;
            float defaultScale = GsonHelper.getAsFloat(json, "default_scale", 1.0F);
            int defaultTint = parseColor(json, "default_tint", 0xFFFFFF);
            float defaultAlpha = GsonHelper.getAsFloat(json, "default_alpha", 1.0F);
            BlendMode blendMode =
                BlendMode.fromString(
                    GsonHelper.getAsString(json, "blend", "translucent"), BlendMode.TRANSLUCENT);
            GeckoFxDefinition definition =
                new GeckoFxDefinition(
                    id,
                    model,
                    texture,
                    animation,
                    defaultAnimation,
                    defaultScale,
                    defaultTint,
                    defaultAlpha,
                    blendMode);
            collected.put(id, definition);
          } catch (Exception ex) {
            ChestCavity.LOGGER.error("[GuScript] Failed to parse Gecko FX definition {}", id, ex);
          }
        });
    GeckoFxRegistry.updateDefinitions(collected);
  }

  private static ResourceLocation parseResource(JsonObject json, String key) {
    if (!json.has(key)) {
      throw new IllegalArgumentException("Missing required Gecko FX property  + key + ");
    }
    return ResourceLocation.parse(GsonHelper.getAsString(json, key));
  }

  private static int parseColor(JsonObject json, String key, int fallback) {
    if (!json.has(key)) {
      return fallback;
    }
    String raw = GsonHelper.getAsString(json, key).trim().toLowerCase(Locale.ROOT);
    try {
      if (raw.startsWith("#")) {
        return (int) Long.parseLong(raw.substring(1), 16);
      }
      if (raw.startsWith("0x")) {
        return (int) Long.parseLong(raw.substring(2), 16);
      }
      return (int) Long.parseLong(raw, 16);
    } catch (NumberFormatException ex) {
      ChestCavity.LOGGER.warn("[GuScript] Invalid Gecko FX tint {}", raw);
      return fallback;
    }
  }
}
