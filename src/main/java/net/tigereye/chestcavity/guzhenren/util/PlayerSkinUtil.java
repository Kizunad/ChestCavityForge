package net.tigereye.chestcavity.guzhenren.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Utility helpers for extracting lightweight player skin metadata without introducing client-only
 * dependencies. The decoded information can be reused by both server- and client-side features.
 *
 * <p>Relocated from the compatibility package so Guzhenren rendering helpers live under the
 * consolidated integration namespace.
 */
public final class PlayerSkinUtil {

  private static final ResourceLocation DEFAULT_TEXTURE =
      ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/steve.png");

  private PlayerSkinUtil() {}

  /**
   * Captures a snapshot of a player's current skin metadata. If the skin information cannot be
   * resolved the snapshot falls back to the default Steve texture and classic model.
   */
  public static SkinSnapshot capture(Player player) {
    if (player == null) {
      return SkinSnapshot.defaultSnapshot();
    }

    GameProfile profile = player.getGameProfile();
    if (profile == null) {
      return SkinSnapshot.defaultSnapshot();
    }

    UUID id = profile.getId();
    String name = profile.getName();

    ResourceLocation texture = DEFAULT_TEXTURE;
    String model = SkinSnapshot.MODEL_DEFAULT;
    String skinUrl = null;
    String propertyValue = null;
    String propertySignature = null;

    PropertyMap properties = profile.getProperties();
    if (properties != null && !properties.isEmpty()) {
      Property textureProperty = properties.get("textures").stream().findFirst().orElse(null);
      if (textureProperty != null) {
        propertyValue = textureProperty.value();
        propertySignature = textureProperty.signature();
        SkinPayload payload = decode(textureProperty.value());
        if (payload != null) {
          if (payload.texture() != null) {
            texture = payload.texture();
          }
          if (payload.model() != null) {
            model = payload.model();
          }
          skinUrl = payload.url();
        }
      }
    }

    return new SkinSnapshot(
        id,
        name,
        texture,
        model,
        skinUrl,
        propertyValue,
        propertySignature,
        1.0f,
        1.0f,
        1.0f,
        1.0f);
  }

  /** Produces a tinted copy of the provided snapshot using the supplied RGBA components. */
  public static SkinSnapshot withTint(
      SkinSnapshot original, float red, float green, float blue, float alpha) {
    if (original == null) {
      return new SkinSnapshot(
          null,
          null,
          DEFAULT_TEXTURE,
          SkinSnapshot.MODEL_DEFAULT,
          null,
          null,
          null,
          red,
          green,
          blue,
          alpha);
    }
    return new SkinSnapshot(
        original.playerId(),
        original.playerName(),
        original.texture(),
        original.model(),
        original.skinUrl(),
        original.propertyValue(),
        original.propertySignature(),
        red,
        green,
        blue,
        alpha);
  }

  private static SkinPayload decode(String encoded) {
    if (encoded == null || encoded.isBlank()) {
      return null;
    }
    try {
      String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
      JsonObject root = JsonParser.parseString(decoded).getAsJsonObject();
      JsonObject textures = root.getAsJsonObject("textures");
      if (textures == null) {
        return null;
      }
      JsonObject skin = textures.getAsJsonObject("SKIN");
      if (skin == null) {
        return null;
      }
      String url = Optional.ofNullable(skin.get("url")).map(JsonElement::getAsString).orElse(null);
      String model =
          Optional.ofNullable(skin.getAsJsonObject("metadata"))
              .map(meta -> meta.get("model"))
              .map(JsonElement::getAsString)
              .orElse(null);
      ResourceLocation texture = null;
      if (url != null) {
        if (url.isBlank()) {
          url = null;
        } else {
          texture = urlToResource(url);
        }
      }
      return new SkinPayload(texture, model == null ? null : model.toLowerCase(Locale.ROOT), url);
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }

  private static ResourceLocation urlToResource(String url) {
    if (url == null || url.isBlank()) {
      return null;
    }
    String s = url.trim();
    // Strip known prefixes (both http and https)
    s = s.replaceFirst("^https?://textures\\.minecraft\\.net/texture/", "");
    // If still looks like a URL/path, reduce to last segment
    int hashIdx = s.lastIndexOf('/') + 1;
    if (hashIdx > 0 && hashIdx < s.length()) {
      s = s.substring(hashIdx);
    }
    // Drop query/fragment if any
    int q = s.indexOf('?');
    if (q >= 0) s = s.substring(0, q);
    int h = s.indexOf('#');
    if (h >= 0) s = s.substring(0, h);
    s = s.toLowerCase(Locale.ROOT);

    // Validate allowed chars; Mojang texture IDs are hex. If invalid/empty, hash the URL.
    if (s.isBlank() || !s.matches("[a-z0-9._-]+")) {
      s = sha1Hex(url);
    }
    return ResourceLocation.fromNamespaceAndPath("minecraft", "skins/" + s);
  }

  private static String sha1Hex(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        sb.append(Character.forDigit((b >> 4) & 0xF, 16));
        sb.append(Character.forDigit(b & 0xF, 16));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      // Extremely unlikely; fallback to a sanitized base64
      String base =
          Base64.getUrlEncoder()
              .withoutPadding()
              .encodeToString(input.getBytes(StandardCharsets.UTF_8));
      // Reduce to allowed chars (url-safe base64 already ok), just lowercase to be safe
      return base.toLowerCase(Locale.ROOT);
    }
  }

  private record SkinPayload(ResourceLocation texture, String model, String url) {}

  /** Snapshot of the relevant skin metadata along with a configurable tint. */
  public record SkinSnapshot(
      UUID playerId,
      String playerName,
      ResourceLocation texture,
      String model,
      String skinUrl,
      String propertyValue,
      String propertySignature,
      float red,
      float green,
      float blue,
      float alpha) {
    public static final String MODEL_DEFAULT = "default";

    public SkinSnapshot {
      texture = Objects.requireNonNullElse(texture, DEFAULT_TEXTURE);
      model = model == null || model.isBlank() ? MODEL_DEFAULT : model.toLowerCase(Locale.ROOT);
    }

    private static SkinSnapshot defaultSnapshot() {
      return new SkinSnapshot(
          null, null, DEFAULT_TEXTURE, MODEL_DEFAULT, null, null, null, 1.0f, 1.0f, 1.0f, 1.0f);
    }
  }
}
