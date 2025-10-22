package net.tigereye.chestcavity.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;

/** Utility for sampling GeckoLib animation position keyframes from animation json files. */
public final class AnimationPathHelper {

  private AnimationPathHelper() {}

  private static final Map<ResourceLocation, Map<String, Map<String, List<AnimationKeyframe>>>>
      CACHE = new ConcurrentHashMap<>();

  public static List<AnimationKeyframe> loadPositionKeyframes(
      ResourceManager resourceManager,
      ResourceLocation animationFile,
      String animationName,
      String boneName) {
    if (resourceManager == null
        || animationFile == null
        || animationName == null
        || boneName == null) {
      return List.of();
    }
    Map<String, Map<String, List<AnimationKeyframe>>> animations =
        CACHE.computeIfAbsent(animationFile, key -> new ConcurrentHashMap<>());
    Map<String, List<AnimationKeyframe>> bones =
        animations.computeIfAbsent(animationName, key -> new ConcurrentHashMap<>());
    return bones.computeIfAbsent(
        boneName, key -> loadUncached(resourceManager, animationFile, animationName, boneName));
  }

  private static List<AnimationKeyframe> loadUncached(
      ResourceManager resourceManager,
      ResourceLocation animationFile,
      String animationName,
      String boneName) {
    Optional<Resource> resourceOpt;
    try {
      resourceOpt = resourceManager.getResource(animationFile);
    } catch (Exception ex) {
      ChestCavity.LOGGER.warn(
          "[AnimationPathHelper] Failed to access animation resource {}", animationFile, ex);
      return List.of();
    }
    if (resourceOpt.isEmpty()) {
      ChestCavity.LOGGER.warn(
          "[AnimationPathHelper] Animation resource {} not found", animationFile);
      return List.of();
    }
    Resource resource = resourceOpt.get();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
      JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
      JsonObject animations = GsonHelper.getAsJsonObject(root, "animations");
      if (!animations.has(animationName)) {
        ChestCavity.LOGGER.warn(
            "[AnimationPathHelper] Animation {} missing entry {}", animationFile, animationName);
        return List.of();
      }
      JsonObject animation = animations.getAsJsonObject(animationName);
      JsonObject bones = GsonHelper.getAsJsonObject(animation, "bones");
      if (!bones.has(boneName)) {
        ChestCavity.LOGGER.warn(
            "[AnimationPathHelper] Animation {} bone {} not found in {}",
            animationFile,
            boneName,
            animationName);
        return List.of();
      }
      JsonObject bone = bones.getAsJsonObject(boneName);
      if (!bone.has("position")) {
        ChestCavity.LOGGER.warn(
            "[AnimationPathHelper] Animation {} missing position data for bone {}",
            animationFile,
            boneName);
        return List.of();
      }
      JsonObject position = bone.getAsJsonObject("position");
      List<AnimationKeyframe> frames = new ArrayList<>();
      for (Map.Entry<String, JsonElement> entry : position.entrySet()) {
        String key = entry.getKey();
        double time;
        try {
          time = Double.parseDouble(key);
        } catch (NumberFormatException ex) {
          ChestCavity.LOGGER.warn(
              "[AnimationPathHelper] Invalid keyframe time '{}' in {}", key, animationFile);
          continue;
        }
        JsonObject obj = GsonHelper.convertToJsonObject(entry.getValue(), "keyframe");
        JsonArray vec = GsonHelper.getAsJsonArray(obj, "vector");
        double x = vec.size() > 0 ? vec.get(0).getAsDouble() : 0.0D;
        double y = vec.size() > 1 ? vec.get(1).getAsDouble() : 0.0D;
        double z = vec.size() > 2 ? vec.get(2).getAsDouble() : 0.0D;
        frames.add(new AnimationKeyframe(time, new Vec3(x, y, z)));
      }
      frames.sort(Comparator.comparingDouble(AnimationKeyframe::time));
      return List.copyOf(frames);
    } catch (Exception ex) {
      ChestCavity.LOGGER.warn(
          "[AnimationPathHelper] Failed to read animation {}", animationFile, ex);
      return List.of();
    }
  }

  public record AnimationKeyframe(double time, Vec3 position) {}
}
