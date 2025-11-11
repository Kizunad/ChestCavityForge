package kizuna.guzhenren_event_ext.common.system_modules.conditions;

import com.google.gson.JsonObject;
import kizuna.guzhenren_event_ext.GuzhenrenEventExtension;
import kizuna.guzhenren_event_ext.common.system_modules.ICondition;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

/**
 * 检查玩家的某一“X道道痕”数值范围。
 * JSON 格式：
 * {
 *   "type": "guzhenren:player_daohen",
 *   "field": "daohen_shuidao", // 或中文文档名，如 "水道道痕"；亦支持 PlayerField 枚举名
 *   "min": 0,                  // 可选
 *   "max": 100                 // 可选
 * }
 */
public class PlayerDaohenCondition implements ICondition {

  @Override
  public boolean check(Player player, JsonObject definition) {
    if (!definition.has("field")) {
      GuzhenrenEventExtension.LOGGER.warn("PlayerDaohenCondition 缺少必填字段 'field'");
      return false;
    }

    String identifier = definition.get("field").getAsString();

    var handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return false;
    }

    double value = handleOpt.get().read(identifier).orElse(Double.NaN);
    if (!Double.isFinite(value)) {
      // 若 identifier 不存在或读取失败
      GuzhenrenEventExtension.LOGGER.debug("PlayerDaohenCondition 无法读取字段 '{}': 非数值或缺失", identifier);
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

