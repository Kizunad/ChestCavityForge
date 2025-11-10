package net.tigereye.chestcavity.client.modernui.config.docs.provider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.client.modernui.config.docs.DocEntry;
import net.tigereye.chestcavity.client.modernui.config.docs.DocProvider;

public final class ResourceDocProvider implements DocProvider {

  private static final Gson GSON = new GsonBuilder().create();
  private static final String NAMESPACE = "guzhenren";
  private static final String ROOT = "docs";

  @Override
  public String name() {
    return "ResourceDocs";
  }

  @Override
  public Collection<DocEntry> loadAll() {
    Minecraft mc = Minecraft.getInstance();
    if (mc == null) {
      return List.of();
    }
    ResourceManager manager = mc.getResourceManager();
    Map<ResourceLocation, Resource> resources =
        manager.listResources(
            ROOT,
            location ->
                location.getNamespace().equals(NAMESPACE) && location.getPath().endsWith(".json"));
    if (resources.isEmpty()) {
      return List.of();
    }

    List<DocEntry> result = new ArrayList<>();
    for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
      ResourceLocation location = entry.getKey();
      try (var reader =
          new BufferedReader(
              new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8))) {
        JsonElement root = JsonParser.parseReader(reader);
        if (root.isJsonArray()) {
          JsonArray array = root.getAsJsonArray();
          for (JsonElement element : array) {
            if (element.isJsonObject()) {
              DocEntry doc = decode(element.getAsJsonObject(), location);
              if (doc != null) {
                result.add(doc);
              }
            }
          }
        } else if (root.isJsonObject()) {
          DocEntry doc = decode(root.getAsJsonObject(), location);
          if (doc != null) {
            result.add(doc);
          }
        } else {
          ChestCavity.LOGGER.warn("[docs] {} ignored non-object payload", location);
        }
      } catch (Exception ex) {
        ChestCavity.LOGGER.error("[docs] failed to read {}", location, ex);
      }
    }
    return result;
  }

  private DocEntry decode(JsonObject json, ResourceLocation source) {
    String idRaw = asString(json, "id");
    if (idRaw == null || idRaw.isBlank()) {
      ChestCavity.LOGGER.warn("[docs] {} missing id field", source);
      return null;
    }
    ResourceLocation id = ResourceLocation.tryParse(idRaw);
    if (id == null) {
      ChestCavity.LOGGER.warn("[docs] {} has invalid id '{}'", source, idRaw);
      return null;
    }

    String title = asString(json, "title");
    String summary = asString(json, "summary");
    List<String> details = readStringList(json.get("details"));
    List<String> tags = readStringList(json.get("tags"));
    ItemStack icon = resolveIcon(asString(json, "icon"), source);
    ResourceLocation iconTexture = resolveIconTexture(asString(json, "iconTexture"), source);

    // Extract category and subcategory from the resource path
    // Expected format: "docs/category/subcategory/filename.json"
    String[] pathParts = extractCategoryFromPath(source.getPath());
    String category = pathParts[0];
    String subcategory = pathParts[1];

    return new DocEntry(
        id, title, summary, details, tags, icon, iconTexture, category, subcategory);
  }

  /**
   * Extracts category and subcategory from the resource path.
   *
   * @param path Resource path (e.g., "docs/human/bian_hua_dao/shou_pi_gu.json")
   * @return Array of [category, subcategory], empty strings if not found
   */
  private static String[] extractCategoryFromPath(String path) {
    String[] result = {"", ""};
    if (path == null || path.isEmpty()) {
      return result;
    }

    // Remove "docs/" prefix and ".json" suffix
    String trimmed = path;
    if (trimmed.startsWith(ROOT + "/")) {
      trimmed = trimmed.substring((ROOT + "/").length());
    }
    if (trimmed.endsWith(".json")) {
      trimmed = trimmed.substring(0, trimmed.length() - 5);
    }

    // Split by "/" to get path segments
    String[] segments = trimmed.split("/");
    if (segments.length >= 1) {
      result[0] = segments[0]; // category (e.g., "human", "animal")
    }
    if (segments.length >= 2) {
      result[1] = segments[1]; // subcategory (e.g., "bian_hua_dao", "feng_dao")
    }

    return result;
  }

  private static String asString(JsonObject json, String key) {
    if (json == null || !json.has(key)) {
      return null;
    }
    JsonElement element = json.get(key);
    if (element.isJsonNull()) {
      return null;
    }
    if (element.isJsonPrimitive()) {
      return element.getAsString().trim();
    }
    return GSON.toJson(element);
  }

  private static List<String> readStringList(JsonElement element) {
    if (element == null || element.isJsonNull()) {
      return List.of();
    }
    List<String> values = new ArrayList<>();
    if (element.isJsonArray()) {
      for (JsonElement child : element.getAsJsonArray()) {
        if (child != null && child.isJsonPrimitive()) {
          String text = child.getAsString().trim();
          if (!text.isEmpty()) {
            values.add(text);
          }
        }
      }
    } else if (element.isJsonPrimitive()) {
      String text = element.getAsString().trim();
      if (!text.isEmpty()) {
        values.add(text);
      }
    }
    return values.isEmpty() ? List.of() : List.copyOf(values);
  }

  private static ItemStack resolveIcon(String raw, ResourceLocation source) {
    if (raw == null || raw.isBlank()) {
      return ItemStack.EMPTY;
    }
    ResourceLocation itemId = ResourceLocation.tryParse(raw);
    if (itemId == null) {
      ChestCavity.LOGGER.warn("[docs] {} has invalid icon id '{}'", source, raw);
      return ItemStack.EMPTY;
    }
    Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
    if (item == null) {
      ChestCavity.LOGGER.warn("[docs] {} missing item '{}'", source, itemId);
      return ItemStack.EMPTY;
    }
    return new ItemStack(item);
  }

  private static ResourceLocation resolveIconTexture(String raw, ResourceLocation source) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    ResourceLocation textureId = ResourceLocation.tryParse(raw);
    if (textureId == null) {
      ChestCavity.LOGGER.warn("[docs] {} has invalid iconTexture id '{}'", source, raw);
      return null;
    }
    return textureId;
  }
}
