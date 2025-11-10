package net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.override;

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
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.orientation.OrientationMode;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.orientation.UpMode;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.override.SwordModelOverrideDef.AlignMode;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.override.SwordModelOverrideDef.RendererKind;

/**
 * 客户端模型覆盖定义加载器。
 *
 * <p>扫描 assets/[namespace]/sword_models/*.json 并构建覆盖注册表。
 */
public final class SwordModelOverrideLoader extends SimpleJsonResourceReloadListener {
  private static final Gson GSON = new GsonBuilder().setLenient().create();

  public SwordModelOverrideLoader() {
    super(GSON, "sword_models");
  }

  @Override
  protected void apply(
      Map<ResourceLocation, JsonElement> object,
      ResourceManager resourceManager,
      ProfilerFiller profiler) {
    Map<String, SwordModelOverrideDef> collected = new HashMap<>();
    object.forEach(
        (id, element) -> {
          try {
            if (!element.isJsonObject()) {
              ChestCavity.LOGGER.warn("[SwordModelOverride] {} is not a JSON object", id);
              return;
            }
            JsonObject json = element.getAsJsonObject();
            String key = GsonHelper.getAsString(json, "key");
            RendererKind renderer =
                parseRenderer(GsonHelper.getAsString(json, "renderer", "gecko"));

            ResourceLocation model =
                json.has("model")
                    ? ResourceLocation.parse(GsonHelper.getAsString(json, "model"))
                    : null;
            java.util.List<ResourceLocation> textures = new java.util.ArrayList<>();
            if (json.has("textures") && json.get("textures").isJsonArray()) {
              for (var e : json.getAsJsonArray("textures")) {
                textures.add(ResourceLocation.parse(e.getAsString()));
              }
            }
            ResourceLocation texture =
                json.has("texture")
                    ? ResourceLocation.parse(GsonHelper.getAsString(json, "texture"))
                    : null;
            ResourceLocation animation =
                json.has("animation")
                    ? ResourceLocation.parse(GsonHelper.getAsString(json, "animation"))
                    : null;
            ResourceLocation displayItem =
                json.has("display_item")
                    ? ResourceLocation.parse(GsonHelper.getAsString(json, "display_item"))
                    : null;

            AlignMode align = parseAlign(GsonHelper.getAsString(json, "align", "velocity"));
            float preRoll = GsonHelper.getAsFloat(json, "pre_roll", -45.0F);
            float yawOffset = GsonHelper.getAsFloat(json, "yaw_offset", -90.0F);
            float pitchOffset = GsonHelper.getAsFloat(json, "pitch_offset", 0.0F);
            float scale = GsonHelper.getAsFloat(json, "scale", 1.0F);
            OrientationMode orientationMode =
                parseOrientationMode(GsonHelper.getAsString(json, "orientation_mode", "basis"));
            UpMode upMode = parseUpMode(GsonHelper.getAsString(json, "up_mode", "world_y"));

            SwordModelOverrideDef def;
            if (!textures.isEmpty()) {
              def =
                  new SwordModelOverrideDef(
                      key,
                      renderer,
                      model,
                      textures,
                      animation,
                      displayItem,
                      align,
                      preRoll,
                      yawOffset,
                      pitchOffset,
                      scale,
                      orientationMode,
                      upMode);
            } else {
              def =
                  new SwordModelOverrideDef(
                      key,
                      renderer,
                      model,
                      texture,
                      animation,
                      displayItem,
                      align,
                      preRoll,
                      yawOffset,
                      pitchOffset,
                      scale);
              // 手动设置orientation字段（因为旧构造器不支持）
              try {
                var f1 = SwordModelOverrideDef.class.getDeclaredField("orientationMode");
                var f2 = SwordModelOverrideDef.class.getDeclaredField("upMode");
                f1.setAccessible(true);
                f2.setAccessible(true);
                f1.set(def, orientationMode);
                f2.set(def, upMode);
              } catch (Exception ignored) {
              }
            }
            collected.put(key, def);
          } catch (Exception ex) {
            ChestCavity.LOGGER.error("[SwordModelOverride] Failed to parse definition {}", id, ex);
          }
        });
    SwordModelOverrideRegistry.replaceAll(collected);
    ChestCavity.LOGGER.info("[SwordModelOverride] Loaded {} overrides", collected.size());
  }

  private static RendererKind parseRenderer(String raw) {
    String s = raw.toLowerCase(Locale.ROOT).trim();
    return switch (s) {
      case "item" -> RendererKind.ITEM;
      default -> RendererKind.GECKO;
    };
  }

  private static AlignMode parseAlign(String raw) {
    String s = raw.toLowerCase(Locale.ROOT).trim();
    return switch (s) {
      case "target" -> AlignMode.TARGET;
      case "none" -> AlignMode.NONE;
      default -> AlignMode.VELOCITY;
    };
  }

  private static OrientationMode parseOrientationMode(String raw) {
    String s = raw.toLowerCase(Locale.ROOT).trim();
    return switch (s) {
      case "legacy_euler", "legacy" -> OrientationMode.LEGACY_EULER;
      default -> OrientationMode.BASIS;
    };
  }

  private static UpMode parseUpMode(String raw) {
    String s = raw.toLowerCase(Locale.ROOT).trim();
    return switch (s) {
      case "owner_up", "owner" -> UpMode.OWNER_UP;
      default -> UpMode.WORLD_Y;
    };
  }
}
