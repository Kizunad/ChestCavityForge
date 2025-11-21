package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;

/**
 * Data-driven template describing how to spawn a Hun Dao soul avatar.
 */
public record HunDaoSoulAvatarTemplate(
    ResourceLocation id,
    boolean captureSkin,
    float tintR,
    float tintG,
    float tintB,
    float tintAlpha,
    int lifetimeTicks) {

  private static final float DEFAULT_TINT_R = 0.6f;
  private static final float DEFAULT_TINT_G = 0.6f;
  private static final float DEFAULT_TINT_B = 1.0f;
  private static final float DEFAULT_TINT_A = 0.5f;

  public static HunDaoSoulAvatarTemplate defaults(ResourceLocation id) {
    return new HunDaoSoulAvatarTemplate(id, true, DEFAULT_TINT_R, DEFAULT_TINT_G, DEFAULT_TINT_B, DEFAULT_TINT_A, -1);
  }

  public static HunDaoSoulAvatarTemplate fromJson(ResourceLocation id, JsonObject root) {
    if (root == null) {
      return defaults(id);
    }
    boolean captureSkin = root.has("capture_skin") && root.get("capture_skin").getAsBoolean();
    float r = DEFAULT_TINT_R;
    float g = DEFAULT_TINT_G;
    float b = DEFAULT_TINT_B;
    float a = DEFAULT_TINT_A;
    if (root.has("tint")) {
      try {
        JsonArray array = root.getAsJsonArray("tint");
        if (array.size() >= 4) {
          r = clamp((float) array.get(0).getAsDouble());
          g = clamp((float) array.get(1).getAsDouble());
          b = clamp((float) array.get(2).getAsDouble());
          a = clamp((float) array.get(3).getAsDouble());
        }
      } catch (Exception ex) {
        ChestCavity.LOGGER.warn("[hun_dao][soul_avatar] Failed to parse tint for {}: {}", id, ex.getMessage());
      }
    }
    int lifetime = root.has("lifetime_ticks") ? root.get("lifetime_ticks").getAsInt() : -1;
    if (lifetime < -1) {
      lifetime = -1;
    }
    return new HunDaoSoulAvatarTemplate(id, captureSkin, r, g, b, a, lifetime);
  }

  private static float clamp(float value) {
    if (value < 0.0f) {
      return 0.0f;
    }
    if (value > 1.0f) {
      return 1.0f;
    }
    return value;
  }
}
