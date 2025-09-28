package net.tigereye.chestcavity.guscript.runtime.action;

import com.google.gson.JsonObject;
import net.tigereye.chestcavity.guscript.actions.AddDamageMultiplierAction;
import net.tigereye.chestcavity.guscript.actions.AddFlatDamageAction;
import net.tigereye.chestcavity.guscript.actions.ConsumeHealthAction;
import net.tigereye.chestcavity.guscript.actions.ConsumeZhenyuanAction;
import net.tigereye.chestcavity.guscript.actions.EmitProjectileAction;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.actions.ExportFlatModifierAction;
import net.tigereye.chestcavity.guscript.actions.ExportMultiplierModifierAction;

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
}
