package net.tigereye.chestcavity.guscript.registry;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.registry.GuScriptRegistry.LeafDefinition;
import net.tigereye.chestcavity.guscript.runtime.action.ActionRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads leaf definitions that map items to GuScript leaf nodes.
 */
public final class GuScriptLeafLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setLenient().create();

    public GuScriptLeafLoader() {
        super(GSON, "guscript/leaves");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, LeafDefinition> leaves = new HashMap<>();
        object.forEach((id, element) -> {
            try {
                JsonObject json = element.getAsJsonObject();
                ResourceLocation itemId = ResourceLocation.parse(GsonHelper.getAsString(json, "item"));
                String name = GsonHelper.getAsString(json, "name", itemId.toString());
                Multiset<String> tags = HashMultiset.create();
                if (json.has("tags")) {
                    for (JsonElement tag : json.getAsJsonArray("tags")) {
                        tags.add(tag.getAsString());
                    }
                }
                List<Action> actions = new ArrayList<>();
                if (json.has("actions")) {
                    for (JsonElement actionElement : json.getAsJsonArray("actions")) {
                        actions.add(ActionRegistry.fromJson(actionElement.getAsJsonObject()));
                    }
                }
                leaves.put(itemId, new LeafDefinition(name, tags, actions));
            } catch (Exception ex) {
                ChestCavity.LOGGER.error("[GuScript] Failed to parse leaf definition {}", id, ex);
            }
        });
        GuScriptRegistry.updateLeaves(leaves);
    }
}
