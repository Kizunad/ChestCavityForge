package net.tigereye.chestcavity.guscript.registry;

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
import net.tigereye.chestcavity.guscript.runtime.action.ActionRegistry;
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
import java.util.Map;

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
        List<Action> enterActions = new ArrayList<>();
        if (json.has("enter_actions")) {
            for (JsonElement element : json.getAsJsonArray("enter_actions")) {
                enterActions.add(ActionRegistry.fromJson(element.getAsJsonObject()));
            }
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
        List<Action> updateActions = new ArrayList<>();
        if (json.has("update_actions")) {
            for (JsonElement element : json.getAsJsonArray("update_actions")) {
                updateActions.add(ActionRegistry.fromJson(element.getAsJsonObject()));
            }
        }
        int updatePeriod = json.has("update_period") ? json.get("update_period").getAsInt() : 0;
        return new FlowStateDefinition(
                enterActions.isEmpty() ? List.of() : List.of(FlowActions.runActions(enterActions)),
                enterFx,
                transitions,
                updateActions.isEmpty() ? List.of() : List.of(FlowActions.runActions(updateActions)),
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

    private static void ensureRequiredStates(Map<FlowState, FlowStateDefinition> definitions, ResourceLocation id) {
        for (FlowState state : FlowState.values()) {
            if (!definitions.containsKey(state)) {
                ChestCavity.LOGGER.warn("[Flow] Program {} is missing definition for state {}", id, state);
            }
        }
    }
}
