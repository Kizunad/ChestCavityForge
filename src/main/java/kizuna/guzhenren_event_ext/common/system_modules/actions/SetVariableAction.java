package kizuna.guzhenren_event_ext.common.system_modules.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import kizuna.guzhenren_event_ext.GuzhenrenEventExtension;
import kizuna.guzhenren_event_ext.common.attachment.EventExtensionWorldData;
import kizuna.guzhenren_event_ext.common.attachment.ModAttachments;
import kizuna.guzhenren_event_ext.common.system_modules.IAction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * 设置变量值的 Action
 * <p>
 * JSON 配置格式：
 * {
 *   "type": "guzhenren_event_ext:set_variable",
 *   "scope": "player",                  // "player" 或 "world"
 *   "variable": "quest.saved_villager",
 *   "value": true                       // 支持: boolean, int, double, string
 * }
 */
public class SetVariableAction implements IAction {

    @Override
    public void execute(Player player, JsonObject definition) {
        // 1. 参数校验
        if (!definition.has("scope") || !definition.has("variable") || !definition.has("value")) {
            GuzhenrenEventExtension.LOGGER.warn("SetVariableAction 缺少必要字段");
            return;
        }

        String scope = definition.get("scope").getAsString();
        String variableKey = definition.get("variable").getAsString();
        JsonElement valueElement = definition.get("value");

        // 2. 解析值类型
        Object value = parseValue(valueElement);
        if (value == null) {
            GuzhenrenEventExtension.LOGGER.warn("SetVariableAction 无法解析值: {}", valueElement);
            return;
        }

        // 3. 设置变量
        if ("player".equalsIgnoreCase(scope)) {
            ModAttachments.get(player).setVariable(variableKey, value);
            GuzhenrenEventExtension.LOGGER.debug("设置玩家变量 '{}' = {} ({})",
                variableKey, value, value.getClass().getSimpleName());

        } else if ("world".equalsIgnoreCase(scope)) {
            if (player.level() instanceof ServerLevel serverLevel) {
                EventExtensionWorldData.get(serverLevel).setVariable(variableKey, value);
                GuzhenrenEventExtension.LOGGER.debug("设置世界变量 '{}' = {} ({})",
                    variableKey, value, value.getClass().getSimpleName());
            } else {
                GuzhenrenEventExtension.LOGGER.warn("无法在客户端设置世界变量");
            }

        } else {
            GuzhenrenEventExtension.LOGGER.warn("未知的 scope: {}", scope);
        }
    }

    /**
     * 解析 JSON 值为 Java 对象
     * <p>
     * 自动类型推断：
     * - JsonPrimitive.isBoolean() → Boolean
     * - JsonPrimitive.isNumber() → Integer (整数) 或 Double (小数)
     * - JsonPrimitive.isString() → String
     */
    private Object parseValue(JsonElement element) {
        if (!element.isJsonPrimitive()) {
            return null;
        }

        JsonPrimitive primitive = element.getAsJsonPrimitive();

        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        } else if (primitive.isNumber()) {
            // 尝试解析为 int，否则为 double
            double d = primitive.getAsDouble();
            if (d == Math.floor(d) && d >= Integer.MIN_VALUE && d <= Integer.MAX_VALUE) {
                return primitive.getAsInt();
            } else {
                return d;
            }
        } else if (primitive.isString()) {
            return primitive.getAsString();
        }

        return null;
    }
}
