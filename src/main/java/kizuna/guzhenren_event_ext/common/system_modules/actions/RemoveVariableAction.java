package kizuna.guzhenren_event_ext.common.system_modules.actions;

import com.google.gson.JsonObject;
import kizuna.guzhenren_event_ext.GuzhenrenEventExtension;
import kizuna.guzhenren_event_ext.common.attachment.EventExtensionWorldData;
import kizuna.guzhenren_event_ext.common.attachment.ModAttachments;
import kizuna.guzhenren_event_ext.common.system_modules.IAction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * 删除变量的 Action
 * <p>
 * JSON 配置格式：
 * {
 *   "type": "guzhenren_event_ext:remove_variable",
 *   "scope": "player",                  // "player" 或 "world"
 *   "variable": "temp.one_time_flag"
 * }
 */
public class RemoveVariableAction implements IAction {

    @Override
    public void execute(Player player, JsonObject definition) {
        // 1. 参数校验
        if (!definition.has("scope") || !definition.has("variable")) {
            GuzhenrenEventExtension.LOGGER.warn("RemoveVariableAction 缺少必要字段");
            return;
        }

        String scope = definition.get("scope").getAsString();
        String variableKey = definition.get("variable").getAsString();

        // 2. 删除变量
        if ("player".equalsIgnoreCase(scope)) {
            ModAttachments.get(player).removeVariable(variableKey);
            GuzhenrenEventExtension.LOGGER.debug("删除玩家变量 '{}'", variableKey);

        } else if ("world".equalsIgnoreCase(scope)) {
            if (player.level() instanceof ServerLevel serverLevel) {
                EventExtensionWorldData.get(serverLevel).removeVariable(variableKey);
                GuzhenrenEventExtension.LOGGER.debug("删除世界变量 '{}'", variableKey);
            } else {
                GuzhenrenEventExtension.LOGGER.warn("无法在客户端删除世界变量");
            }

        } else {
            GuzhenrenEventExtension.LOGGER.warn("未知的 scope: {}", scope);
        }
    }
}
