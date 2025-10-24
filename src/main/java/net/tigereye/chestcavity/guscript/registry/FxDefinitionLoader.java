package net.tigereye.chestcavity.guscript.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.fx.FxDefinition;
import net.tigereye.chestcavity.guscript.fx.FxDefinition.ParticleModule;
import net.tigereye.chestcavity.guscript.fx.FxDefinition.ParticleSettings;
import net.tigereye.chestcavity.guscript.fx.FxDefinition.ScreenShakeModule;
import net.tigereye.chestcavity.guscript.fx.FxDefinition.SoundModule;
import net.tigereye.chestcavity.guscript.fx.FxDefinition.TrailModule;
import net.tigereye.chestcavity.guscript.fx.FxRegistry;

/** Loads FX definitions for GuScript from resource data packs. */
public final class FxDefinitionLoader extends SimpleJsonResourceReloadListener {

  private static final Gson GSON = new GsonBuilder().setLenient().create();

  public FxDefinitionLoader() {
    super(GSON, "guscript/fx");
  }

  @Override
  protected void apply(
      Map<ResourceLocation, JsonElement> object,
      ResourceManager resourceManager,
      ProfilerFiller profiler) {
    Map<ResourceLocation, FxDefinition> definitions = new HashMap<>();
    object.forEach(
        (id, element) -> {
          try {
            JsonObject json = element.getAsJsonObject();
            List<FxDefinition.FxModule> modules = new ArrayList<>();
            JsonArray moduleArray = GsonHelper.getAsJsonArray(json, "modules", new JsonArray());
            for (JsonElement moduleElement : moduleArray) {
              if (!moduleElement.isJsonObject()) {
                continue;
              }
              JsonObject moduleJson = moduleElement.getAsJsonObject();
              String type = GsonHelper.getAsString(moduleJson, "type", "particle");
              switch (type) {
                case "particle" -> modules.add(parseParticleModule(moduleJson));
                case "sound" -> modules.add(parseSoundModule(moduleJson));
                case "screen_shake" -> modules.add(parseScreenShakeModule(moduleJson));
                case "trail" -> modules.add(parseTrailModule(moduleJson));
                default -> ChestCavity.LOGGER.warn(
                    "[GuScript] Unknown FX module type {} in {}", type, id);
              }
            }
            definitions.put(id, new FxDefinition(modules));
          } catch (Exception ex) {
            ChestCavity.LOGGER.error("[GuScript] Failed to parse FX definition {}", id, ex);
          }
        });
    FxRegistry.updateDefinitions(definitions);
  }

  private static ParticleModule parseParticleModule(JsonObject json) {
    ParticleSettings settings = parseParticleSettings(json);
    Vec3 offset = readVec3(json, "offset");
    int count = GsonHelper.getAsInt(json, "count", 6);
    return new ParticleModule(settings, offset, count);
  }

  private static SoundModule parseSoundModule(JsonObject json) {
    ResourceLocation soundId = ResourceLocation.parse(GsonHelper.getAsString(json, "sound"));
    float volume = GsonHelper.getAsFloat(json, "volume", 1.0F);
    float pitch = GsonHelper.getAsFloat(json, "pitch", 1.0F);
    return new SoundModule(soundId, volume, pitch);
  }

  private static ScreenShakeModule parseScreenShakeModule(JsonObject json) {
    float intensity = GsonHelper.getAsFloat(json, "intensity", 0.2F);
    int duration = GsonHelper.getAsInt(json, "duration", 5);
    return new ScreenShakeModule(intensity, duration);
  }

  private static TrailModule parseTrailModule(JsonObject json) {
    ParticleSettings settings = parseParticleSettings(json);
    Vec3 offset = readVec3(json, "offset");
    int segments = GsonHelper.getAsInt(json, "segments", 8);
    double spacing = GsonHelper.getAsDouble(json, "spacing", 0.25D);
    return new TrailModule(settings, offset, segments, spacing);
  }

  private static ParticleSettings parseParticleSettings(JsonObject json) {
    ResourceLocation particleId =
        ResourceLocation.parse(GsonHelper.getAsString(json, "particle", "minecraft:crit"));
    double speed = GsonHelper.getAsDouble(json, "speed", 0.05D);
    Vec3 spread = readVec3(json, "spread", 0.15D);
    Integer primaryColor = null;
    if (json.has("color")) {
      primaryColor = parseColor(json.get("color"));
    }
    if (json.has("from_color")) {
      Integer fromColor = parseColor(json.get("from_color"));
      if (fromColor != null) {
        primaryColor = fromColor;
      }
    }
    Integer secondaryColor = json.has("to_color") ? parseColor(json.get("to_color")) : null;
    float size = GsonHelper.getAsFloat(json, "size", 1.0F);
    return new ParticleSettings(particleId, speed, spread, primaryColor, secondaryColor, size);
  }

  private static Vec3 readVec3(JsonObject json, String key) {
    return readVec3(json, key, 0.0D);
  }

  private static Vec3 readVec3(JsonObject json, String key, double defaultComponent) {
    if (!json.has(key)) {
      return new Vec3(defaultComponent, defaultComponent, defaultComponent);
    }
    JsonElement element = json.get(key);
    if (element.isJsonArray()) {
      JsonArray array = element.getAsJsonArray();
      double x = array.size() > 0 ? array.get(0).getAsDouble() : defaultComponent;
      double y = array.size() > 1 ? array.get(1).getAsDouble() : defaultComponent;
      double z = array.size() > 2 ? array.get(2).getAsDouble() : defaultComponent;
      return new Vec3(x, y, z);
    }
    double value = element.getAsDouble();
    return new Vec3(value, value, value);
  }

  private static Integer parseColor(JsonElement element) {
    if (element == null) {
      return null;
    }
    if (element.isJsonPrimitive()) {
      String raw = element.getAsString().trim();
      try {
        if (raw.startsWith("#")) {
          return Integer.parseInt(raw.substring(1), 16);
        }
        if (raw.startsWith("0x")) {
          return Integer.parseInt(raw.substring(2), 16);
        }
        return Integer.parseInt(raw, 16);
      } catch (NumberFormatException ignored) {
      }
    }
    ChestCavity.LOGGER.warn("[GuScript] Invalid color value {}", element);
    return null;
  }
}
