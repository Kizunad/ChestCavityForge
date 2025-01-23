package net.tigereye.chestcavity.chestcavities.organs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.util.Pair;

import java.util.HashMap;
import java.util.Map;

public class OrganSerializer {
    //remember: the first ResourceLocation is the entity, the second is the chest cavity type
    public Pair<ResourceLocation, OrganData> read(ResourceLocation id, OrganJsonFormat organJson) {

        if (organJson.itemID == null) {
            throw new JsonSyntaxException("Organ " + id + " must have an item ID");
        }
        //isPsudoOrgan should default to false
        if (organJson.organScores == null) {
            throw new JsonSyntaxException("Organ " + id + " must have organScores");
        }
        OrganData organData = new OrganData();
        ResourceLocation itemID = new ResourceLocation(organJson.itemID);
        organData.pseudoOrgan = organJson.pseudoOrgan;
        organData.organScores = readOrganScoresFromJson(id, organJson.organScores);
        return new Pair<>(itemID,organData);
    }

    private Map<ResourceLocation,Float> readOrganScoresFromJson(ResourceLocation id, JsonArray json){
        Map<ResourceLocation,Float> organScores = new HashMap<>();
        for (JsonElement entry:
                json) {
            try {
                JsonObject obj = entry.getAsJsonObject();
                if (!obj.has("id")) {
                    ChestCavity.LOGGER.error("Missing id component in " + id.toString() + "'s organ scores");
                } else if (!obj.has("value")) {
                    ChestCavity.LOGGER.error("Missing value component in " + id.toString() + "'s organ scores");
                } else {
                    ResourceLocation ability = new ResourceLocation(obj.get("id").getAsString());
                    organScores.put(ability,obj.get("value").getAsFloat());
                }
            }
            catch(Exception e){
                ChestCavity.LOGGER.error("Error parsing " + id.toString() + "'s organ scores!");
            }
        }
        return organScores;
    }
}