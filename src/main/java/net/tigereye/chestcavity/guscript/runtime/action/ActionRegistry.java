package net.tigereye.chestcavity.guscript.runtime.action;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.tigereye.chestcavity.guscript.actions.AddDamageMultiplierAction;
import net.tigereye.chestcavity.guscript.actions.AddFlatDamageAction;
import net.tigereye.chestcavity.guscript.actions.ConsumeHealthAction;
import net.tigereye.chestcavity.guscript.actions.ConsumeZhenyuanAction;
import net.tigereye.chestcavity.guscript.actions.EmitProjectileAction;
import net.tigereye.chestcavity.guscript.actions.TriggerFxAction;
import net.tigereye.chestcavity.guscript.actions.SpawnEntityAction;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.actions.ExportFlatModifierAction;
import net.tigereye.chestcavity.guscript.actions.ExportMultiplierModifierAction;
import net.tigereye.chestcavity.guscript.actions.ExportTimeScaleFlatAction;
import net.tigereye.chestcavity.guscript.actions.ExportTimeScaleMultiplierAction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Simple registry turning JSON specs into strongly typed actions.
 */
public final class ActionRegistry {

    private static final Map<String, Function<JsonObject, Action>> FACTORIES = new HashMap<>();
    private static boolean defaultsRegistered = false;

    private ActionRegistry() {}

    public static void registerDefaults() {
        if (defaultsRegistered) {
            return;
        }
        defaultsRegistered = true;
        register(ConsumeZhenyuanAction.ID, json -> new ConsumeZhenyuanAction(json.get("amount").getAsInt()));
        register(ConsumeHealthAction.ID, json -> new ConsumeHealthAction(json.get("amount").getAsInt()));
        register(EmitProjectileAction.ID, json -> new EmitProjectileAction(
                json.get("projectileId").getAsString(),
                json.get("damage").getAsDouble()
        ));
        register(AddDamageMultiplierAction.ID, json -> new AddDamageMultiplierAction(json.get("amount").getAsDouble()));
        register(AddFlatDamageAction.ID, json -> new AddFlatDamageAction(json.get("amount").getAsDouble()));
        register(ExportMultiplierModifierAction.ID, json -> new ExportMultiplierModifierAction(json.get("amount").getAsDouble()));
        register(ExportFlatModifierAction.ID, json -> new ExportFlatModifierAction(json.get("amount").getAsDouble()));
        register(ExportTimeScaleMultiplierAction.ID, json -> new ExportTimeScaleMultiplierAction(json.get("amount").getAsDouble()));
        register(ExportTimeScaleFlatAction.ID, json -> new ExportTimeScaleFlatAction(json.get("amount").getAsDouble()));
        register(TriggerFxAction.ID, json -> TriggerFxAction.from(
                ResourceLocation.parse(json.get("fxId").getAsString()),
                readVec3(json, "originOffset"),
                readVec3(json, "targetOffset"),
                json.has("intensity") ? json.get("intensity").getAsFloat() : 1.0F
        ));
        register(SpawnEntityAction.ID, json -> new SpawnEntityAction(
                ResourceLocation.parse(json.get("entityId").getAsString()),
                readVec3(json, "offset"),
                !json.has("noAi") || json.get("noAi").getAsBoolean()
        ));
        register(net.tigereye.chestcavity.guscript.actions.AdjustGuzhenrenResourceAction.ID, json ->
                new net.tigereye.chestcavity.guscript.actions.AdjustGuzhenrenResourceAction(
                        json.get("identifier").getAsString(),
                        json.get("amount").getAsDouble()
                ));
        register(net.tigereye.chestcavity.guscript.actions.GainHealthAction.ID, json ->
                new net.tigereye.chestcavity.guscript.actions.GainHealthAction(
                        json.get("amount").getAsDouble()
                ));
        register(net.tigereye.chestcavity.guscript.actions.MindShockAction.ID, json ->
                new net.tigereye.chestcavity.guscript.actions.MindShockAction(
                        json.get("damage").getAsDouble(),
                        json.has("range") ? json.get("range").getAsDouble() : 16.0D
                ));
        register(net.tigereye.chestcavity.guscript.actions.EmitBloodBoneBombAction.ID, json ->
                new net.tigereye.chestcavity.guscript.actions.EmitBloodBoneBombAction(
                        json.has("base") ? json.get("base").getAsDouble() : 80.0
                ));
        register(net.tigereye.chestcavity.guscript.actions.ConditionalTargetAction.ID, json ->
                new net.tigereye.chestcavity.guscript.actions.ConditionalTargetAction(
                        json.has("range") ? json.get("range").getAsDouble() : Double.POSITIVE_INFINITY,
                        !json.has("excludeSelf") || json.get("excludeSelf").getAsBoolean(),
                        !json.has("requireAlive") || json.get("requireAlive").getAsBoolean(),
                        !json.has("requirePerformer") || json.get("requirePerformer").getAsBoolean(),
                        readActions(json)
                ));
        register(net.tigereye.chestcavity.guscript.actions.IfResourceAction.ID, json ->
                new net.tigereye.chestcavity.guscript.actions.IfResourceAction(
                        json.get("identifier").getAsString(),
                        json.get("minimum").getAsDouble(),
                        readActions(json)
                ));
        register(net.tigereye.chestcavity.guscript.actions.AdjustLinkageChannelAction.ID, json ->
                new net.tigereye.chestcavity.guscript.actions.AdjustLinkageChannelAction(
                        ResourceLocation.parse(json.get("channel").getAsString()),
                        json.get("amount").getAsDouble()
                ));
        register(net.tigereye.chestcavity.guscript.actions.AdjustLinkageChannelTemporaryAction.ID, json ->
                new net.tigereye.chestcavity.guscript.actions.AdjustLinkageChannelTemporaryAction(
                        ResourceLocation.parse(json.get("channel").getAsString()),
                        json.get("amount").getAsDouble(),
                        json.has("duration") ? json.get("duration").getAsInt() : 1200
                ));
    }

    public static void register(String id, Function<JsonObject, Action> factory) {
        Objects.requireNonNull(id, "Action id");
        Objects.requireNonNull(factory, "Action factory");
        FACTORIES.put(id, factory);
    }

    public static Action fromJson(JsonObject json) {
        registerDefaults();
        String id = json.get("id").getAsString();
        Function<JsonObject, Action> factory = FACTORIES.get(id);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown action id: " + id);
        }
        return factory.apply(json);
    }

    private static Vec3 readVec3(JsonObject json, String key) {
        if (!json.has(key)) {
            return Vec3.ZERO;
        }
        JsonArray array = json.getAsJsonArray(key);
        if (array == null || array.size() == 0) {
            return Vec3.ZERO;
        }
        double x = array.size() > 0 ? array.get(0).getAsDouble() : 0.0D;
        double y = array.size() > 1 ? array.get(1).getAsDouble() : 0.0D;
        double z = array.size() > 2 ? array.get(2).getAsDouble() : 0.0D;
        return new Vec3(x, y, z);
    }

    private static java.util.List<Action> readActions(JsonObject json) {
        if (!json.has("actions") || !json.get("actions").isJsonArray()) {
            return java.util.List.of();
        }
        JsonArray array = json.getAsJsonArray("actions");
        java.util.List<Action> actions = new java.util.ArrayList<>(array.size());
        for (var element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            actions.add(fromJson(element.getAsJsonObject()));
        }
        return actions;
    }
}
