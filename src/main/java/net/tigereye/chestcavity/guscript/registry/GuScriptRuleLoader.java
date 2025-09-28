package net.tigereye.chestcavity.guscript.registry;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.ast.GuNodeKind;
import net.tigereye.chestcavity.guscript.ast.OperatorGuNode;
import net.tigereye.chestcavity.guscript.runtime.action.ActionRegistry;
import net.tigereye.chestcavity.guscript.runtime.reduce.ReactionRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads reaction rules from data packs.
 */
public final class GuScriptRuleLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setLenient().create();

    public GuScriptRuleLoader() {
        super(GSON, "guscript/rules");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        List<ReactionRule> rules = new ArrayList<>();
        object.forEach((id, element) -> {
            try {
                rules.add(parseRule(id, element.getAsJsonObject()));
            } catch (Exception ex) {
                ChestCavity.LOGGER.error("[GuScript] Failed to parse reaction rule {}", id, ex);
            }
        });
        rules.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
        GuScriptRegistry.updateReactionRules(rules);
    }

    private static ReactionRule parseRule(ResourceLocation id, JsonObject json) {
        int arity = json.has("arity") ? json.get("arity").getAsInt() : 2;
        int priority = json.has("priority") ? json.get("priority").getAsInt() : 0;

        Map<String, Integer> required = new HashMap<>();
        if (json.has("required")) {
            JsonObject obj = json.getAsJsonObject("required");
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                required.put(entry.getKey(), entry.getValue().getAsInt());
            }
        }

        Set<String> inhibitors = new HashSet<>();
        if (json.has("inhibitors")) {
            JsonArray arr = json.getAsJsonArray("inhibitors");
            for (JsonElement el : arr) {
                inhibitors.add(el.getAsString());
            }
        }

        JsonObject result = json.getAsJsonObject("result");
        if (result == null) {
            throw new JsonParseException("Reaction rule missing result block");
        }
        String name = result.get("name").getAsString();
        String operatorId = result.has("operator_id") ? result.get("operator_id").getAsString() : id.toString();
        GuNodeKind kind = result.has("kind") ? parseKind(result.get("kind").getAsString()) : GuNodeKind.OPERATOR;

        Multiset<String> tags = HashMultiset.create();
        if (result.has("tags")) {
            JsonArray arr = result.getAsJsonArray("tags");
            for (JsonElement el : arr) {
                tags.add(el.getAsString());
            }
        }

        List<Action> actions = new ArrayList<>();
        if (result.has("actions")) {
            JsonArray arr = result.getAsJsonArray("actions");
            for (JsonElement el : arr) {
                actions.add(ActionRegistry.fromJson(el.getAsJsonObject()));
            }
        }

        final Integer order = result.has("order") ? result.get("order").getAsInt() : null;
        final JsonObject exports = result.has("export_modifiers") ? result.getAsJsonObject("export_modifiers") : null;
        final boolean exportMultiplier = exports != null && exports.has("multiplier") && exports.get("multiplier").getAsBoolean();
        final boolean exportFlat = exports != null && exports.has("flat") && exports.get("flat").getAsBoolean();
        final ResourceLocation flowId = parseFlowId(result);
        final Map<String, String> flowParams = parseFlowParams(result);

        return ReactionRule.builder(id.toString())
                .arity(arity)
                .priority(priority)
                .requiredTags(required)
                .inhibitors(inhibitors)
                .operator((ruleId, inputs) -> new OperatorGuNode(operatorId, name, kind, tags, actions, inputs, order, exportMultiplier, exportFlat, flowId, flowParams))
                .build();
    }

    private static ResourceLocation parseFlowId(JsonObject result) {
        if (!result.has("flow_id")) {
            return null;
        }
        String raw = result.get("flow_id").getAsString();
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) {
            throw new JsonParseException("Invalid flow_id: " + raw);
        }
        return id;
    }

    private static Map<String, String> parseFlowParams(JsonObject result) {
        if (!result.has("flow_params")) {
            return Map.of();
        }
        JsonObject obj = result.getAsJsonObject("flow_params");
        if (obj == null || obj.entrySet().isEmpty()) {
            return Map.of();
        }
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            JsonElement value = entry.getValue();
            if (value == null || value.isJsonNull()) {
                continue;
            }
            String serialized;
            if (value.isJsonPrimitive()) {
                serialized = value.getAsString();
            } else {
                serialized = GSON.toJson(value);
            }
            params.put(entry.getKey(), serialized);
        }
        return params.isEmpty() ? Map.of() : Map.copyOf(params);
    }

    private static GuNodeKind parseKind(String raw) {
        String normalized = raw.toUpperCase();
        try {
            return GuNodeKind.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new JsonParseException("Unknown GuNodeKind: " + raw);
        }
    }
}
