package net.tigereye.chestcavity.chestcavities.types.json;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.ChestCavityInventory;
import net.tigereye.chestcavity.chestcavities.types.GeneratedChestCavityType;
import net.tigereye.chestcavity.chestcavities.types.RandomChestCavityFiller;

public class ChestCavityTypeSerializer {
  public GeneratedChestCavityType read(ResourceLocation id, ChestCavityTypeJsonFormat cctJson) {
    // ChestCavityTypeJsonFormat cctJson = new Gson().fromJson(json,
    // ChestCavityTypeJsonFormat.class);

    if (cctJson.defaultChestCavity == null) {
      throw new JsonSyntaxException("Chest Cavity Types must have a default chest cavity!");
    }

    if (cctJson.exceptionalOrgans == null) cctJson.exceptionalOrgans = new JsonArray();
    if (cctJson.baseOrganScores == null) cctJson.baseOrganScores = new JsonArray();
    if (cctJson.forbiddenSlots == null) cctJson.forbiddenSlots = new JsonArray();
    // bossChestCavity defaults to false
    // playerChestCavity defaults to false
    // dropRateMultiplier defaults to true

    GeneratedChestCavityType cct = new GeneratedChestCavityType();
    cct.setForbiddenSlots(readForbiddenSlotsFromJson(id, cctJson));
    cct.setDefaultChestCavity(readDefaultChestCavityFromJson(id, cctJson, cct.getForbiddenSlots()));
    cct.setBaseOrganScores(readBaseOrganScoresFromJson(id, cctJson));
    cct.setExceptionalOrganList(readExceptionalOrgansFromJson(id, cctJson));
    cct.setRandomFillers(readRandomGeneratorsFromJson(id, cctJson));
    cct.setDropRateMultiplier(cctJson.dropRateMultiplier);
    cct.setPlayerChestCavity(cctJson.playerChestCavity);
    cct.setBossChestCavity(cctJson.bossChestCavity);

    /*
    Ingredient input = Ingredient.fromJson(recipeJson.ingredient);
    Item outputItem = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(recipeJson.result))
            .orElseThrow(() -> new JsonSyntaxException("No such item " + recipeJson.result));
    ItemStack output = new ItemStack(outputItem, recipeJson.count);
    */

    return cct;
  }

  private ChestCavityInventory readDefaultChestCavityFromJson(
      ResourceLocation id, ChestCavityTypeJsonFormat cctJson, List<Integer> forbiddenSlots) {
    ChestCavityInventory inv = new ChestCavityInventory();
    int i = 0;
    for (JsonElement entry : cctJson.defaultChestCavity) {
      ++i;
      try {
        JsonObject obj = entry.getAsJsonObject();
        if (!obj.has("item")) {
          ChestCavity.LOGGER.error(
              "Missing item component in entry no."
                  + i
                  + " in "
                  + id.toString()
                  + "'s default chest cavity");
        } else if (!obj.has("position")) {
          ChestCavity.LOGGER.error(
              "Missing position component in entry no. "
                  + i
                  + " in "
                  + id.toString()
                  + "'s default chest cavity");
        } else {
          ResourceLocation itemID = ResourceLocation.parse(obj.get("item").getAsString());
          Optional<Item> itemOptional = BuiltInRegistries.ITEM.getOptional(itemID);
          if (itemOptional.isPresent()) {
            Item item = itemOptional.get();
            ItemStack stack;
            if (obj.has("count")) {
              int count = obj.get("count").getAsInt();
              stack = new ItemStack(item, count);
            } else {
              stack = new ItemStack(item, item.getDefaultMaxStackSize());
            }
            int pos = obj.get("position").getAsInt();
            if (pos >= inv.getContainerSize()) {
              ChestCavity.LOGGER.error(
                  "Position component is out of bounds in entry no. "
                      + i
                      + " in "
                      + id.toString()
                      + "'s default chest cavity");
            } else if (forbiddenSlots.contains(pos)) {
              ChestCavity.LOGGER.error(
                  "Position component is forbidden in entry no. "
                      + i
                      + " in "
                      + id.toString()
                      + "'s default chest cavity");
            } else {
              inv.setItem(pos, stack);
            }
          } else {
            ChestCavity.LOGGER.error(
                "Unknown "
                    + itemID.toString()
                    + " in entry no. "
                    + i
                    + " in "
                    + id.toString()
                    + "'s default chest cavity");
          }
        }
      } catch (Exception e) {
        ChestCavity.LOGGER.error(
            "Error parsing entry no. " + i + " in " + id.toString() + "'s default chest cavity");
      }
    }
    return inv;
  }

  private Map<ResourceLocation, Float> readBaseOrganScoresFromJson(
      ResourceLocation id, ChestCavityTypeJsonFormat cctJson) {
    return readOrganScoresFromJson(id, cctJson.baseOrganScores);
  }

  private Map<Ingredient, Map<ResourceLocation, Float>> readExceptionalOrgansFromJson(
      ResourceLocation id, ChestCavityTypeJsonFormat cctJson) {
    Map<Ingredient, Map<ResourceLocation, Float>> exceptionalOrgans = new HashMap<>();

    int i = 0;
    for (JsonElement entry : cctJson.exceptionalOrgans) {
      ++i;
      try {
        JsonObject obj = entry.getAsJsonObject();
        if (!obj.has("ingredient")) {
          ChestCavity.LOGGER.error(
              "Missing ingredient component in entry no."
                  + i
                  + " in "
                  + id.toString()
                  + "'s exceptional organs");
        } else if (!obj.has("value")) {
          ChestCavity.LOGGER.error(
              "Missing value component in entry no. "
                  + i
                  + " in "
                  + id.toString()
                  + "'s exceptional organs");
        } else {
          final int entryNumber = i;
          final String typeId = id.toString();
          Ingredient ingredient =
              Ingredient.CODEC
                  .parse(JsonOps.INSTANCE, obj.get("ingredient"))
                  .resultOrPartial(
                      error ->
                          ChestCavity.LOGGER.error(
                              "Error parsing ingredient in entry no. "
                                  + entryNumber
                                  + " in "
                                  + typeId
                                  + "'s exceptional organs: "
                                  + error))
                  .orElse(Ingredient.EMPTY);
          if (ingredient.isEmpty()) {
            ChestCavity.LOGGER.error(
                "Empty ingredient in entry no. " + i + " in " + id + "'s exceptional organs");
          } else {
            exceptionalOrgans.put(
                ingredient, readOrganScoresFromJson(id, obj.get("value").getAsJsonArray()));
          }
        }
      } catch (Exception e) {
        ChestCavity.LOGGER.error(
            "Error parsing entry no. " + i + " in " + id.toString() + "'s exceptional organs");
      }
    }

    return exceptionalOrgans;
  }

  private List<RandomChestCavityFiller> readRandomGeneratorsFromJson(
      ResourceLocation id, ChestCavityTypeJsonFormat cctJson) {
    if (cctJson.randomGenerators == null) {
      return Collections.emptyList();
    }
    List<RandomChestCavityFiller> fillers = new ArrayList<>();
    int index = 0;
    for (JsonElement element : cctJson.randomGenerators) {
      ++index;
      if (!element.isJsonObject()) {
        ChestCavity.LOGGER.error(
            "Invalid entry type in {}'s random generators at position {}", id, index);
        continue;
      }
      JsonObject object = element.getAsJsonObject();
      if (!object.has("id")) {
        ChestCavity.LOGGER.error(
            "Missing id component in entry no. {} in {}'s random generators", index, id);
        continue;
      }
      ResourceLocation generatorId;
      try {
        generatorId = ResourceLocation.parse(object.get("id").getAsString());
      } catch (IllegalArgumentException exception) {
        ChestCavity.LOGGER.error(
            "Invalid id '{}' in entry no. {} in {}'s random generators",
            object.get("id").getAsString(),
            index,
            id);
        continue;
      }

      int minCount = 1;
      int maxCount = 1;
      if (object.has("count")) {
        JsonElement countElement = object.get("count");
        if (countElement.isJsonPrimitive() && countElement.getAsJsonPrimitive().isNumber()) {
          minCount = maxCount = countElement.getAsInt();
        } else if (countElement.isJsonObject()) {
          JsonObject countObject = countElement.getAsJsonObject();
          if (countObject.has("min")) {
            minCount = countObject.get("min").getAsInt();
          } else {
            minCount = 0;
          }
          if (countObject.has("max")) {
            maxCount = countObject.get("max").getAsInt();
          } else {
            maxCount = minCount;
          }
        } else {
          ChestCavity.LOGGER.error(
              "Invalid count definition in entry no. {} in {}'s random generators", index, id);
          continue;
        }
      }

      if (minCount < 0 || maxCount < 0) {
        ChestCavity.LOGGER.warn(
            "Negative count in entry no. {} in {}'s random generators; clamping to zero",
            index,
            id);
        minCount = Math.max(0, minCount);
        maxCount = Math.max(0, maxCount);
      }
      if (minCount > maxCount) {
        ChestCavity.LOGGER.warn(
            "Min greater than max in entry no. {} in {}'s random generators; swapping values",
            index,
            id);
        int temp = minCount;
        minCount = maxCount;
        maxCount = temp;
      }

      // 解析 chance 参数，默认为 100（总是生成）
      int chance = 100;
      if (object.has("chance")) {
        JsonElement chanceElement = object.get("chance");
        if (chanceElement.isJsonPrimitive() && chanceElement.getAsJsonPrimitive().isNumber()) {
          chance = chanceElement.getAsInt();
          if (chance < 0 || chance > 100) {
            ChestCavity.LOGGER.warn(
                "Chance value {} is out of range (0-100) in entry no. {} in {}'s random generators; clamping to valid range",
                chance,
                index,
                id);
            chance = Math.max(0, Math.min(100, chance));
          }
        } else {
          ChestCavity.LOGGER.error(
              "Invalid chance definition in entry no. {} in {}'s random generators", index, id);
          continue;
        }
      }

      if (!object.has("items") || !object.get("items").isJsonArray()) {
        ChestCavity.LOGGER.error(
            "Missing items array in entry no. {} in {}'s random generators", index, id);
        continue;
      }

      List<Item> items = new ArrayList<>();
      for (JsonElement itemElement : object.getAsJsonArray("items")) {
        if (!itemElement.isJsonPrimitive()) {
          ChestCavity.LOGGER.error(
              "Invalid item entry in entry no. {} in {}'s random generators", index, id);
          continue;
        }
        ResourceLocation itemId;
        try {
          itemId = ResourceLocation.parse(itemElement.getAsString());
        } catch (IllegalArgumentException exception) {
          ChestCavity.LOGGER.error(
              "Invalid item id '{}' in entry no. {} in {}'s random generators",
              itemElement.getAsString(),
              index,
              id);
          continue;
        }
        Optional<Item> itemOptional = BuiltInRegistries.ITEM.getOptional(itemId);
        if (itemOptional.isEmpty()) {
          ChestCavity.LOGGER.error(
              "Unknown {} in entry no. {} in {}'s random generators", itemId, index, id);
          continue;
        }
        items.add(itemOptional.get());
      }
      if (items.isEmpty()) {
        ChestCavity.LOGGER.error(
            "No valid items found in entry no. {} in {}'s random generators", index, id);
        continue;
      }
      fillers.add(new RandomChestCavityFiller(generatorId, minCount, maxCount, items, chance));
    }
    return Collections.unmodifiableList(fillers);
  }

  private Map<ResourceLocation, Float> readOrganScoresFromJson(
      ResourceLocation id, JsonArray json) {
    Map<ResourceLocation, Float> organScores = new HashMap<>();
    for (JsonElement entry : json) {
      try {
        JsonObject obj = entry.getAsJsonObject();
        if (!obj.has("id")) {
          ChestCavity.LOGGER.error("Missing id component in " + id.toString() + "'s organ scores");
        } else if (!obj.has("value")) {
          ChestCavity.LOGGER.error(
              "Missing value component in " + id.toString() + "'s organ scores");
        } else {
          ResourceLocation ability = ResourceLocation.parse(obj.get("id").getAsString());
          organScores.put(ability, obj.get("value").getAsFloat());
        }
      } catch (Exception e) {
        ChestCavity.LOGGER.error("Error parsing " + id.toString() + "'s organ scores!");
      }
    }
    return organScores;
  }

  private List<Integer> readForbiddenSlotsFromJson(
      ResourceLocation id, ChestCavityTypeJsonFormat cctJson) {
    List<Integer> list = new ArrayList<>();
    for (JsonElement entry : cctJson.forbiddenSlots) {
      try {
        int slot = entry.getAsInt();
        list.add(slot);
      } catch (Exception e) {
        ChestCavity.LOGGER.error("Error parsing " + id.toString() + "'s organ scores!");
      }
    }
    return list;
  }
}
