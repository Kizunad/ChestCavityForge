package net.tigereye.chestcavity.guscript.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.action.ActionRegistry;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowEdgeAction;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowGuard;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowProgram;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowProgramRegistry;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowState;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowStateDefinition;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowTransition;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowTrigger;
import net.tigereye.chestcavity.guscript.runtime.flow.actions.FlowActions;
import net.tigereye.chestcavity.guscript.runtime.flow.guards.FlowGuards;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Loads flow programs from data packs.
 */
public final class GuScriptFlowLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setLenient().create();

    public GuScriptFlowLoader() {
        super(GSON, "guscript/flows");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, FlowProgram> programs = new HashMap<>();
        object.forEach((id, element) -> {
            try {
                programs.put(id, parseProgram(id, element.getAsJsonObject()));
            } catch (Exception ex) {
                ChestCavity.LOGGER.error("[Flow] Failed to parse flow program {}", id, ex);
            }
        });
        FlowProgramRegistry.update(programs);
        try {
            String loaded = programs.keySet().stream().map(ResourceLocation::toString).sorted().reduce((a,b) -> a+", "+b).orElse("<none>");
            ChestCavity.LOGGER.info("[Flow] Loaded programs ({}): {}", programs.size(), loaded);
        } catch (Exception ignored) {}
    }

    private static FlowProgram parseProgram(ResourceLocation id, JsonObject json) {
        FlowState initial = FlowState.fromName(GsonHelper.getAsString(json, "initial_state", "idle"));
        JsonObject statesObject = GsonHelper.getAsJsonObject(json, "states");
        Map<FlowState, FlowStateDefinition> definitions = new EnumMap<>(FlowState.class);
        for (Map.Entry<String, JsonElement> entry : statesObject.entrySet()) {
            FlowState state = FlowState.fromName(entry.getKey());
            definitions.put(state, parseStateDefinition(entry.getValue().getAsJsonObject()));
        }
        ensureRequiredStates(definitions, id);
        return new FlowProgram(id, initial, definitions);
    }

    private static FlowStateDefinition parseStateDefinition(JsonObject json) {
        List<FlowTransition> transitions = new ArrayList<>();
        if (json.has("transitions")) {
            for (JsonElement element : json.getAsJsonArray("transitions")) {
                transitions.add(parseTransition(element.getAsJsonObject()));
            }
        }
        List<FlowEdgeAction> enterActions = new ArrayList<>();
        List<Action> pendingEnterScriptActions = new ArrayList<>();
        if (json.has("enter_actions")) {
            for (JsonElement element : json.getAsJsonArray("enter_actions")) {
                JsonObject actionJson = element.getAsJsonObject();
                if (actionJson.has("type")) {
                    if (!pendingEnterScriptActions.isEmpty()) {
                        enterActions.add(FlowActions.runActions(pendingEnterScriptActions));
                        pendingEnterScriptActions = new ArrayList<>();
                    }
                    enterActions.add(parseAction(actionJson));
                } else {
                    pendingEnterScriptActions.add(ActionRegistry.fromJson(actionJson));
                }
            }
        }
        if (!pendingEnterScriptActions.isEmpty()) {
            enterActions.add(FlowActions.runActions(pendingEnterScriptActions));
        }
        List<ResourceLocation> enterFx = new ArrayList<>();
        if (json.has("enter_fx")) {
            for (JsonElement element : json.getAsJsonArray("enter_fx")) {
                if (!element.isJsonPrimitive()) {
                    throw new IllegalArgumentException("Flow state enter_fx entries must be strings");
                }
                enterFx.add(ResourceLocation.parse(element.getAsString()));
            }
        }
        List<FlowEdgeAction> updateActions = new ArrayList<>();
        List<Action> pendingUpdateScriptActions = new ArrayList<>();
        if (json.has("update_actions")) {
            for (JsonElement element : json.getAsJsonArray("update_actions")) {
                JsonObject actionJson = element.getAsJsonObject();
                if (actionJson.has("type")) {
                    if (!pendingUpdateScriptActions.isEmpty()) {
                        updateActions.add(FlowActions.runActions(pendingUpdateScriptActions));
                        pendingUpdateScriptActions = new ArrayList<>();
                    }
                    updateActions.add(parseAction(actionJson));
                } else {
                    pendingUpdateScriptActions.add(ActionRegistry.fromJson(actionJson));
                }
            }
        }
        if (!pendingUpdateScriptActions.isEmpty()) {
            updateActions.add(FlowActions.runActions(pendingUpdateScriptActions));
        }
        int updatePeriod = json.has("update_period") ? json.get("update_period").getAsInt() : 0;
        return new FlowStateDefinition(
                enterActions,
                enterFx,
                transitions,
                updateActions,
                Math.max(0, updatePeriod)
        );
    }

    private static FlowTransition parseTransition(JsonObject json) {
        FlowTrigger trigger = FlowTrigger.fromName(GsonHelper.getAsString(json, "trigger"));
        FlowState target = FlowState.fromName(GsonHelper.getAsString(json, "target"));
        int minTicks = GsonHelper.getAsInt(json, "min_ticks", 0);
        List<FlowGuard> guards = new ArrayList<>();
        if (json.has("guards")) {
            for (JsonElement element : json.getAsJsonArray("guards")) {
                guards.add(parseGuard(element.getAsJsonObject()));
            }
        }
        List<net.tigereye.chestcavity.guscript.runtime.flow.FlowEdgeAction> actions = new ArrayList<>();
        if (json.has("actions")) {
            for (JsonElement element : json.getAsJsonArray("actions")) {
                actions.add(parseAction(element.getAsJsonObject()));
            }
        }
        return new FlowTransition(trigger, target, guards, actions, minTicks);
    }

    private static FlowGuard parseGuard(JsonObject json) {
        String type = GsonHelper.getAsString(json, "type");
        return switch (type) {
            case "resource" -> FlowGuards.hasResource(GsonHelper.getAsString(json, "identifier"), GsonHelper.getAsDouble(json, "minimum"));
            case "resource_below" -> FlowGuards.resourceBelow(GsonHelper.getAsString(json, "identifier"), GsonHelper.getAsDouble(json, "maximum"));
            case "cooldown" -> FlowGuards.cooldownReady(GsonHelper.getAsString(json, "key"));
            case "health_min" -> FlowGuards.healthAtLeast(GsonHelper.getAsDouble(json, "minimum"));
            case "health_below" -> FlowGuards.healthBelow(GsonHelper.getAsDouble(json, "maximum"));
            case "variable_at_most" -> FlowGuards.variableAtMost(
                    GsonHelper.getAsString(json, "name"),
                    GsonHelper.getAsDouble(json, "maximum"),
                    !json.has("value_type") || !GsonHelper.getAsString(json, "value_type").equalsIgnoreCase("long")
            );
            case "variable_at_least" -> FlowGuards.variableAtLeast(
                    GsonHelper.getAsString(json, "name"),
                    GsonHelper.getAsDouble(json, "minimum"),
                    !json.has("value_type") || !GsonHelper.getAsString(json, "value_type").equalsIgnoreCase("long")
            );
            default -> throw new IllegalArgumentException("Unknown flow guard type: " + type);
        };
    }

    private static net.tigereye.chestcavity.guscript.runtime.flow.FlowEdgeAction parseAction(JsonObject json) {
        String type = GsonHelper.getAsString(json, "type");
        return switch (type) {
            case "consume_resource" -> FlowActions.consumeResource(GsonHelper.getAsString(json, "identifier"), GsonHelper.getAsDouble(json, "amount"));
            case "consume_health" -> FlowActions.consumeHealth(GsonHelper.getAsDouble(json, "amount"));
            case "apply_effect" -> FlowActions.applyEffect(
                    ResourceLocation.parse(GsonHelper.getAsString(json, "id")),
                    GsonHelper.getAsInt(json, "duration"),
                    GsonHelper.getAsInt(json, "amplifier", 0),
                    GsonHelper.getAsBoolean(json, "showParticles", false),
                    GsonHelper.getAsBoolean(json, "showIcon", false)
            );
            case "true_damage" -> FlowActions.trueDamage(GsonHelper.getAsDouble(json, "amount"));
            case "explode" -> FlowActions.explode((float) GsonHelper.getAsDouble(json, "power"));
            case "set_cooldown" -> FlowActions.setCooldown(GsonHelper.getAsString(json, "key"), GsonHelper.getAsLong(json, "ticks"));
            case "trigger_actions" -> FlowActions.runActions(parseActions(json));
            case "apply_attribute" -> FlowActions.applyAttributeModifier(
                    ResourceLocation.parse(GsonHelper.getAsString(json, "attribute")),
                    ResourceLocation.parse(GsonHelper.getAsString(json, "modifier")),
                    parseOperation(GsonHelper.getAsString(json, "operation")),
                    GsonHelper.getAsDouble(json, "amount")
            );
            case "remove_attribute" -> FlowActions.removeAttributeModifier(
                    ResourceLocation.parse(GsonHelper.getAsString(json, "attribute")),
                    ResourceLocation.parse(GsonHelper.getAsString(json, "modifier"))
            );
            case "area_effect" -> FlowActions.areaEffect(
                    ResourceLocation.parse(GsonHelper.getAsString(json, "id")),
                    GsonHelper.getAsInt(json, "duration"),
                    GsonHelper.getAsInt(json, "amplifier", 0),
                    GsonHelper.getAsDouble(json, "radius"),
                    json.has("radius_variable") ? GsonHelper.getAsString(json, "radius_variable") : null,
                    GsonHelper.getAsBoolean(json, "hostiles_only", true),
                    GsonHelper.getAsBoolean(json, "include_self", false),
                    GsonHelper.getAsBoolean(json, "show_particles", false),
                    GsonHelper.getAsBoolean(json, "show_icon", false)
            );
            case "dampen_projectiles" -> FlowActions.dampenProjectiles(
                    GsonHelper.getAsDouble(json, "radius"),
                    json.has("radius_variable") ? GsonHelper.getAsString(json, "radius_variable") : null,
                    GsonHelper.getAsDouble(json, "factor"),
                    GsonHelper.getAsInt(json, "cap", 8)
            );
            case "highlight_hostiles" -> FlowActions.highlightHostiles(
                    GsonHelper.getAsDouble(json, "radius"),
                    json.has("radius_variable") ? GsonHelper.getAsString(json, "radius_variable") : null,
                    GsonHelper.getAsInt(json, "duration")
            );
            case "set_variable" -> FlowActions.setVariable(
                    GsonHelper.getAsString(json, "name"),
                    GsonHelper.getAsDouble(json, "value"),
                    parseVariableIsDouble(json)
            );
            case "add_variable" -> FlowActions.addVariable(
                    GsonHelper.getAsString(json, "name"),
                    GsonHelper.getAsDouble(json, "value"),
                    parseVariableIsDouble(json)
            );
            case "add_variable_from_variable" -> FlowActions.addVariableFromVariable(
                    GsonHelper.getAsString(json, "name"),
                    GsonHelper.getAsString(json, "source"),
                    GsonHelper.getAsDouble(json, "scale", 1.0D),
                    parseVariableIsDouble(json)
            );
            case "clamp_variable" -> FlowActions.clampVariable(
                    GsonHelper.getAsString(json, "name"),
                    GsonHelper.getAsDouble(json, "min", Double.NEGATIVE_INFINITY),
                    GsonHelper.getAsDouble(json, "max", Double.POSITIVE_INFINITY),
                    parseVariableIsDouble(json)
            );
            case "set_variable_from_param" -> FlowActions.setVariableFromParam(
                    GsonHelper.getAsString(json, "param"),
                    GsonHelper.getAsString(json, "name"),
                    GsonHelper.getAsDouble(json, "default", 0.0D)
            );
            case "copy_variable" -> FlowActions.copyVariable(
                    GsonHelper.getAsString(json, "from"),
                    GsonHelper.getAsString(json, "to"),
                    GsonHelper.getAsDouble(json, "scale", 1.0D),
                    GsonHelper.getAsDouble(json, "offset", 0.0D),
                    parseVariableIsDouble(json)
            );
            case "emit_fx" -> FlowActions.emitFx(
                    GsonHelper.getAsString(json, "fx"),
                    GsonHelper.getAsFloat(json, "base_intensity", 1.0F),
                    json.has("scale_variable") ? GsonHelper.getAsString(json, "scale_variable") : null,
                    GsonHelper.getAsDouble(json, "default_scale", 1.0D)
            );
            case "replace_blocks_sphere" -> {
                ReplacementParseResult replacements = parseReplacementBlocks(json);
                yield FlowActions.replaceBlocksSphere(
                        GsonHelper.getAsDouble(json, "radius", 0.0D),
                        json.has("radius_param") ? GsonHelper.getAsString(json, "radius_param") : null,
                        json.has("radius_variable") ? GsonHelper.getAsString(json, "radius_variable") : null,
                        GsonHelper.getAsDouble(json, "max_hardness", 0.0D),
                        GsonHelper.getAsBoolean(json, "include_fluids", false),
                        GsonHelper.getAsBoolean(json, "drop_blocks", false),
                        replacements.blocks(),
                        replacements.weights(),
                        parseBlockList(json, "forbidden_blocks"),
                        GsonHelper.getAsBoolean(json, "place_snow_layers", false),
                        GsonHelper.getAsInt(json, "snow_layers_min", 1),
                        GsonHelper.getAsInt(json, "snow_layers_max", 1),
                        GsonHelper.getAsString(json, "origin", "performer")
                );
            }
            case "tame_nearby" -> FlowActions.tameNearby(
                    GsonHelper.getAsDouble(json, "radius", 4.0D),
                    GsonHelper.getAsBoolean(json, "sit", false),
                    GsonHelper.getAsBoolean(json, "persist", true)
            );
            case "order_guard" -> FlowActions.orderGuard(
                    GsonHelper.getAsDouble(json, "radius", 8.0D),
                    GsonHelper.getAsBoolean(json, "seek_hostiles", true),
                    GsonHelper.getAsDouble(json, "acquire_radius", 16.0D)
            );
            case "bind_owner_nudao" -> FlowActions.bindOwnerNudao(
                    GsonHelper.getAsDouble(json, "radius", 4.0D),
                    GsonHelper.getAsBoolean(json, "tame_if_possible", true)
            );
            case "assist_player_attacks" -> FlowActions.assistPlayerAttacks(
                    GsonHelper.getAsDouble(json, "ally_radius", 8.0D),
                    GsonHelper.getAsInt(json, "recent_ticks", 40),
                    GsonHelper.getAsDouble(json, "acquire_radius", 16.0D)
            );
            default -> throw new IllegalArgumentException("Unknown flow action type: " + type);
        };
    }

    private static List<Action> parseActions(JsonObject json) {
        List<Action> actions = new ArrayList<>();
        if (json.has("actions")) {
            for (JsonElement element : json.getAsJsonArray("actions")) {
                actions.add(ActionRegistry.fromJson(element.getAsJsonObject()));
            }
        }
        return actions;
    }

    private static AttributeModifier.Operation parseOperation(String raw) {
        if (raw == null) {
            return AttributeModifier.Operation.ADD_VALUE;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ADD_VALUE", "ADDITION", "ADD" -> AttributeModifier.Operation.ADD_VALUE;
            case "MULTIPLY_BASE", "MULTIPLY_BASELINE", "BASE_MULTIPLY", "ADD_MULTIPLIED_BASE" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "MULTIPLY_TOTAL", "TOTAL_MULTIPLY", "ADD_MULTIPLIED_TOTAL" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
    }

    private static List<ResourceLocation> parseBlockList(JsonObject json, String key) {
        List<ResourceLocation> blocks = new ArrayList<>();
        if (json == null || key == null || !json.has(key)) {
            return blocks;
        }
        JsonArray array = GsonHelper.getAsJsonArray(json, key);
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive()) {
                continue;
            }
            String raw = element.getAsString();
            ResourceLocation id = ResourceLocation.tryParse(raw);
            if (id != null) {
                blocks.add(id);
            } else {
                ChestCavity.LOGGER.warn("[Flow] Ignoring invalid block id {} in {}", raw, key);
            }
        }
        return blocks;
    }

    private static ReplacementParseResult parseReplacementBlocks(JsonObject json) {
        List<ResourceLocation> blocks = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        if (json == null || !json.has("replacements")) {
            return new ReplacementParseResult(blocks, weights);
        }
        JsonArray array = GsonHelper.getAsJsonArray(json, "replacements");
        for (JsonElement element : array) {
            if (element.isJsonPrimitive()) {
                String raw = element.getAsString();
                ResourceLocation id = ResourceLocation.tryParse(raw);
                if (id != null) {
                    blocks.add(id);
                    weights.add(1);
                } else {
                    ChestCavity.LOGGER.warn("[Flow] Ignoring invalid replacement block id {}", raw);
                }
                continue;
            }
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            String blockId = GsonHelper.getAsString(entry, "block", "");
            ResourceLocation id = ResourceLocation.tryParse(blockId);
            if (id == null) {
                ChestCavity.LOGGER.warn("[Flow] Ignoring invalid replacement block id {}", blockId);
                continue;
            }
            int weight = Math.max(0, GsonHelper.getAsInt(entry, "weight", 1));
            if (weight <= 0) {
                continue;
            }
            blocks.add(id);
            weights.add(weight);
        }
        return new ReplacementParseResult(blocks, weights);
    }

    private static boolean parseVariableIsDouble(JsonObject json) {
        String type = GsonHelper.getAsString(json, "value_type", "double");
        return !type.equalsIgnoreCase("long") && !type.equalsIgnoreCase("int");
    }

    private static void ensureRequiredStates(Map<FlowState, FlowStateDefinition> definitions, ResourceLocation id) {
        for (FlowState state : FlowState.values()) {
            if (!definitions.containsKey(state)) {
                ChestCavity.LOGGER.warn("[Flow] Program {} is missing definition for state {}", id, state);
            }
        }
    }

    private record ReplacementParseResult(List<ResourceLocation> blocks, List<Integer> weights) {
    }
}
