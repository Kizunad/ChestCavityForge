package kizuna.guzhenren_event_ext.common.system_modules.conditions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kizuna.guzhenren_event_ext.GuzhenrenEventExtension;
import kizuna.guzhenren_event_ext.common.attachment.EventExtensionWorldData;
import kizuna.guzhenren_event_ext.common.attachment.ModAttachments;
import kizuna.guzhenren_event_ext.common.system_modules.ICondition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * 检查变量值的 Condition
 * <p>
 * JSON 配置格式：
 * {
 *   "type": "guzhenren_event_ext:check_variable",
 *   "scope": "player",                  // "player" 或 "world"
 *   "variable": "quest.main_story_chapter",
 *   "condition": ">=",                  // "==", "!=", ">", ">=", "<", "<=", "exists", "not_exists"
 *   "value": 3                          // 对比值（exists/not_exists 时可选）
 * }
 */
public class CheckVariableCondition implements ICondition {

    @Override
    public boolean check(Player player, JsonObject definition) {
        // 1. 参数校验
        if (!definition.has("scope") || !definition.has("variable") || !definition.has("condition")) {
            GuzhenrenEventExtension.LOGGER.warn("CheckVariableCondition 缺少必要字段");
            return false;
        }

        String scope = definition.get("scope").getAsString();
        String variableKey = definition.get("variable").getAsString();
        String conditionType = definition.get("condition").getAsString();

        // 2. 获取变量值
        Object variableValue = null;

        if ("player".equalsIgnoreCase(scope)) {
            variableValue = ModAttachments.get(player).getVariable(variableKey);
        } else if ("world".equalsIgnoreCase(scope)) {
            if (player.level() instanceof ServerLevel serverLevel) {
                variableValue = EventExtensionWorldData.get(serverLevel).getVariable(variableKey);
            } else {
                GuzhenrenEventExtension.LOGGER.warn("无法在客户端获取世界变量");
                return false;
            }
        } else {
            GuzhenrenEventExtension.LOGGER.warn("未知的 scope: {}", scope);
            return false;
        }

        // 3. 执行条件判断
        return evaluateCondition(variableValue, conditionType, definition.get("value"));
    }

    /**
     * 评估条件
     */
    private boolean evaluateCondition(Object variableValue, String conditionType, JsonElement expectedValueElement) {
        switch (conditionType.toLowerCase()) {
            case "exists":
                return variableValue != null;

            case "not_exists":
                return variableValue == null;

            case "==":
            case "!=":
            case ">":
            case ">=":
            case "<":
            case "<=":
                if (variableValue == null || expectedValueElement == null) {
                    return false;
                }
                return compareValues(variableValue, conditionType, expectedValueElement);

            default:
                GuzhenrenEventExtension.LOGGER.warn("未知的 condition 类型: {}", conditionType);
                return false;
        }
    }

    /**
     * 比较值（类型安全）
     */
    private boolean compareValues(Object variableValue, String operator, JsonElement expectedValueElement) {
        // Boolean 比较
        if (variableValue instanceof Boolean) {
            if (!expectedValueElement.isJsonPrimitive()) {
                return false;
            }
            boolean expected = expectedValueElement.getAsBoolean();
            boolean actual = (Boolean) variableValue;

            if ("==".equals(operator)) {
                return actual == expected;
            }
            if ("!=".equals(operator)) {
                return actual != expected;
            }
            // boolean 不支持大小比较
            GuzhenrenEventExtension.LOGGER.warn("Boolean 类型不支持比较运算符: {}", operator);
            return false;
        }

        // 数值比较
        if (variableValue instanceof Number) {
            if (!expectedValueElement.isJsonPrimitive()) {
                return false;
            }
            double expected = expectedValueElement.getAsDouble();
            double actual = ((Number) variableValue).doubleValue();

            switch (operator) {
                case "==":
                    return Math.abs(actual - expected) < 1e-9;
                case "!=":
                    return Math.abs(actual - expected) >= 1e-9;
                case ">":
                    return actual > expected;
                case ">=":
                    return actual >= expected;
                case "<":
                    return actual < expected;
                case "<=":
                    return actual <= expected;
                default:
                    return false;
            }
        }

        // String 比较
        if (variableValue instanceof String) {
            if (!expectedValueElement.isJsonPrimitive()) {
                return false;
            }
            String expected = expectedValueElement.getAsString();
            String actual = (String) variableValue;

            if ("==".equals(operator)) {
                return actual.equals(expected);
            }
            if ("!=".equals(operator)) {
                return !actual.equals(expected);
            }

            // String 字典序比较
            int cmp = actual.compareTo(expected);
            switch (operator) {
                case ">":
                    return cmp > 0;
                case ">=":
                    return cmp >= 0;
                case "<":
                    return cmp < 0;
                case "<=":
                    return cmp <= 0;
                default:
                    return false;
            }
        }

        GuzhenrenEventExtension.LOGGER.warn("不支持的变量类型: {}", variableValue.getClass().getName());
        return false;
    }
}
