package net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.orientation.OrientationMode;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.orientation.UpMode;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.profile.SwordVisualProfile.AlignMode;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.profile.SwordVisualProfile.GlintMode;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.profile.SwordVisualProfile.RendererKind;

/** 新视觉配置加载器：assets/[ns]/sword_visuals/*.json */
public final class SwordVisualProfileLoader extends SimpleJsonResourceReloadListener {
  private static final Gson GSON = new GsonBuilder().setLenient().create();

  public SwordVisualProfileLoader() {
    super(GSON, "sword_visuals");
  }

  @Override
  protected void apply(
      Map<ResourceLocation, JsonElement> object,
      ResourceManager resourceManager,
      ProfilerFiller profiler) {
    Map<String, SwordVisualProfile> collected = new HashMap<>();
    object.forEach(
        (id, element) -> {
          try {
            if (!element.isJsonObject()) {
              ChestCavity.LOGGER.warn("[SwordVisualProfile] {} is not a JSON object", id);
              return;
            }
            JsonObject json = element.getAsJsonObject();
            String key = GsonHelper.getAsString(json, "key");
            boolean enabled = GsonHelper.getAsBoolean(json, "enabled", false);
            RendererKind renderer = parseRenderer(GsonHelper.getAsString(json, "renderer", "item"));
            var model =
                json.has("model")
                    ? ResourceLocation.parse(GsonHelper.getAsString(json, "model"))
                    : null;
            var animation =
                json.has("animation")
                    ? ResourceLocation.parse(GsonHelper.getAsString(json, "animation"))
                    : null;
            List<ResourceLocation> textures = new ArrayList<>();
            if (json.has("textures") && json.get("textures").isJsonArray()) {
              for (var e : json.getAsJsonArray("textures"))
                textures.add(ResourceLocation.parse(e.getAsString()));
            }
            AlignMode align = parseAlign(GsonHelper.getAsString(json, "align", "velocity"));
            float preRoll = GsonHelper.getAsFloat(json, "pre_roll", -45.0f);
            float yawOff = GsonHelper.getAsFloat(json, "yaw_offset", -90.0f);
            float pitchOff = GsonHelper.getAsFloat(json, "pitch_offset", 0.0f);
            float scale = GsonHelper.getAsFloat(json, "scale", 1.0f);
            OrientationMode orientationMode =
                parseOrientationMode(GsonHelper.getAsString(json, "orientation_mode", "basis"));
            UpMode upMode = parseUpMode(GsonHelper.getAsString(json, "up_mode", "world_y"));
            GlintMode glint = parseGlint(GsonHelper.getAsString(json, "glint", "inherit"));
            List<String> matchKeys = new ArrayList<>();
            if (json.has("match_model_keys") && json.get("match_model_keys").isJsonArray()) {
              for (var e : json.getAsJsonArray("match_model_keys")) matchKeys.add(e.getAsString());
            }
            SwordVisualProfile p =
                new SwordVisualProfile(
                    key,
                    enabled,
                    renderer,
                    model,
                    textures,
                    animation,
                    align,
                    preRoll,
                    yawOff,
                    pitchOff,
                    scale,
                    orientationMode,
                    upMode,
                    glint,
                    matchKeys.isEmpty() ? List.of(key) : matchKeys);
            collected.put(key, p);
          } catch (Exception ex) {
            ChestCavity.LOGGER.error("[SwordVisualProfile] Failed to parse {}", id, ex);
          }
        });
    SwordVisualProfileRegistry.replaceAll(collected);
    ChestCavity.LOGGER.info("[SwordVisualProfile] Loaded {} profiles", collected.size());
  }

  private static RendererKind parseRenderer(String s) {
    s = s.toLowerCase();
    return switch (s) {
      case "gecko" -> RendererKind.GECKO;
      default -> RendererKind.ITEM;
    };
  }

  private static AlignMode parseAlign(String s) {
    s = s.toLowerCase();
    return switch (s) {
      case "target" -> AlignMode.TARGET;
      case "owner" -> AlignMode.OWNER;
      case "none" -> AlignMode.NONE;
      default -> AlignMode.VELOCITY;
    };
  }

  private static GlintMode parseGlint(String s) {
    s = s.toLowerCase();
    return switch (s) {
      case "force_on" -> GlintMode.FORCE_ON;
      case "force_off" -> GlintMode.FORCE_OFF;
      default -> GlintMode.INHERIT;
    };
  }

  private static OrientationMode parseOrientationMode(String s) {
    s = s.toLowerCase();
    return switch (s) {
      case "legacy_euler", "legacy" -> OrientationMode.LEGACY_EULER;
      default -> OrientationMode.BASIS;
    };
  }

  private static UpMode parseUpMode(String s) {
    s = s.toLowerCase();
    return switch (s) {
      case "owner_up", "owner" -> UpMode.OWNER_UP;
      default -> UpMode.WORLD_Y;
    };
  }
}
