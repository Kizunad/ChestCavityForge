package net.tigereye.chestcavity.guscript.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record EmitProjectileAction(String projectileId, double damage, Map<String, ParameterSource> parameters) implements Action {
    public static final String ID = "emit.projectile";

    public EmitProjectileAction(String projectileId, double damage) {
        this(projectileId, damage, Map.of());
    }

    public EmitProjectileAction {
        Objects.requireNonNull(projectileId, "projectileId");
        if (projectileId.isBlank()) {
            throw new IllegalArgumentException("projectileId must not be blank");
        }
        if (Double.isNaN(damage) || Double.isInfinite(damage)) {
            throw new IllegalArgumentException("damage must be finite");
        }
        parameters = parameters == null || parameters.isEmpty() ? Map.of() : Collections.unmodifiableMap(parameters);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "发射 " + projectileId + " (伤害 " + damage + ")";
    }

    @Override
    public void execute(GuScriptContext context) {
        double finalDamage = context.applyDamageModifiers(damage);
        CompoundTag tag = parameters.isEmpty() ? null : buildParameterTag(context);
        context.bridge().emitProjectile(projectileId, finalDamage, tag);
    }

    private CompoundTag buildParameterTag(GuScriptContext context) {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<String, ParameterSource> entry : parameters.entrySet()) {
            String key = entry.getKey();
            ParameterSource source = entry.getValue();
            if (key == null || key.isBlank() || source == null) {
                continue;
            }
            source.write(tag, key, context);
        }
        return tag.isEmpty() ? null : tag;
    }

    public static Map<String, ParameterSource> parseParameters(JsonObject json) {
        if (json == null || !json.has("parameters")) {
            return Map.of();
        }
        JsonObject raw = json.getAsJsonObject("parameters");
        if (raw == null || raw.entrySet().isEmpty()) {
            return Map.of();
        }
        Map<String, ParameterSource> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : raw.entrySet()) {
            ParameterSource source = ParameterSource.fromJson(entry.getValue());
            if (source != null) {
                values.put(entry.getKey(), source);
            }
        }
        return values.isEmpty() ? Map.of() : Collections.unmodifiableMap(values);
    }

    public record ParameterSource(String flowParam, Double constantValue, Double defaultValue, boolean integerHint) {

        private static final double EPSILON = 1.0E-6;

        public static ParameterSource ofConstant(double value, boolean integerHint) {
            return new ParameterSource(null, value, null, integerHint);
        }

        public static ParameterSource ofFlowParam(String name, Double fallback, boolean integerHint) {
            if (name == null || name.isBlank()) {
                return null;
            }
            return new ParameterSource(name, null, fallback, integerHint);
        }

        public static ParameterSource fromJson(JsonElement element) {
            if (element == null || element.isJsonNull()) {
                return null;
            }
            boolean integerHint = false;
            if (element.isJsonPrimitive()) {
                if (element.getAsJsonPrimitive().isNumber()) {
                    double value = element.getAsDouble();
                    integerHint = Math.abs(value - Math.rint(value)) < EPSILON;
                    return ofConstant(value, integerHint);
                }
                String flowParam = element.getAsString();
                return ofFlowParam(flowParam, null, false);
            }
            if (!element.isJsonObject()) {
                return null;
            }
            JsonObject obj = element.getAsJsonObject();
            Double constant = obj.has("value") ? obj.get("value").getAsDouble() : null;
            Double fallback = obj.has("default") ? obj.get("default").getAsDouble() : null;
            String flowParam = obj.has("flow_param") ? obj.get("flow_param").getAsString() : null;
            if (obj.has("integer") && obj.get("integer").getAsBoolean()) {
                integerHint = true;
            }
            if (flowParam != null && !flowParam.isBlank()) {
                return ofFlowParam(flowParam, constant != null ? constant : fallback, integerHint);
            }
            if (constant != null) {
                if (fallback != null && Math.abs(constant - Math.rint(constant)) < EPSILON) {
                    integerHint = true;
                }
                return ofConstant(constant, integerHint);
            }
            if (fallback != null) {
                return ofConstant(fallback, integerHint);
            }
            return null;
        }

        void write(CompoundTag tag, String key, GuScriptContext context) {
            double value = resolve(context);
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                return;
            }
            if (integerHint) {
                int clamped = Mth.clamp((int) Math.round(value), Integer.MIN_VALUE, Integer.MAX_VALUE);
                tag.putInt(key, clamped);
            } else {
                tag.putDouble(key, value);
            }
        }

        private double resolve(GuScriptContext context) {
            if (flowParam != null && !flowParam.isBlank()) {
                double fallback = defaultValue != null ? defaultValue : (constantValue != null ? constantValue : 0.0D);
                return context.resolveParameterAsDouble(flowParam, fallback);
            }
            if (constantValue != null) {
                return constantValue;
            }
            return defaultValue != null ? defaultValue : 0.0D;
        }
    }
}
