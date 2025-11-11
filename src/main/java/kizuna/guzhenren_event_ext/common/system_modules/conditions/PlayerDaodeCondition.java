package kizuna.guzhenren_event_ext.common.system_modules.conditions;

import com.google.gson.JsonObject;
import kizuna.guzhenren_event_ext.common.system_modules.ICondition;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

/**
 * 检查玩家的蛊真人「道德」数值（绝对值范围）。
 * JSON 格式：
 * {
 *   "type": "guzhenren:player_daode",
 *   "min": -100.0,   // 可选
 *   "max":  100.0    // 可选
 * }
 */
public class PlayerDaodeCondition implements ICondition {

  @Override
  public boolean check(Player player, JsonObject definition) {
    var handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return false;
    }
    double value = handleOpt.get().getDaode().orElse(Double.NaN);
    if (!Double.isFinite(value)) {
      return false;
    }

    if (definition.has("min")) {
      double min = definition.get("min").getAsDouble();
      if (value < min) {
        return false;
      }
    }
    if (definition.has("max")) {
      double max = definition.get("max").getAsDouble();
      if (value > max) {
        return false;
      }
    }
    return true;
  }
}

