package kizuna.guzhenren_event_ext.common.system_modules.conditions;

import com.google.gson.JsonObject;
import kizuna.guzhenren_event_ext.GuzhenrenEventExtension;
import kizuna.guzhenren_event_ext.common.system_modules.ICondition;
import net.minecraft.world.entity.player.Player;

/**
 * Condition：检查被击杀实体的标签是否匹配
 * <p>
 * 该 Condition 需要配合 {@link kizuna.guzhenren_event_ext.common.system_modules.triggers.SpecialEntityKilledTrigger}
 * 使用，用于匹配特定标签的实体击杀事件。
 * <p>
 * JSON 配置格式：
 * <pre>
 * {
 *   "type": "guzhenren_event_ext:check_entity_tag",
 *   "tag": "blood_moon_boss"  // 必需：要匹配的标签值
 * }
 * </pre>
 * <p>
 * 注意：该 Condition 依赖于事件上下文中的 {@code entity_tag} 字段，
 * 该字段由 {@link kizuna.guzhenren_event_ext.common.event.api.SpecialEntityKilledEvent} 提供。
 * <p>
 * 由于当前架构限制，该 Condition 通过玩家的临时数据（ThreadLocal 或其他机制）获取上下文信息。
 * 这里我们采用一个临时方案：通过玩家 PersistentData 的临时字段传递。
 */
public class CheckEntityTagCondition implements ICondition {

    /**
     * 临时存储当前击杀事件的实体标签的 NBT 键
     * 该键在事件处理前设置，处理后清除
     */
    public static final String TEMP_ENTITY_TAG_KEY = "guzhenren_event_ext:temp_killed_entity_tag";

    @Override
    public boolean check(Player player, JsonObject definition) {
        // 1. 参数校验
        if (!definition.has("tag")) {
            GuzhenrenEventExtension.LOGGER.warn("CheckEntityTagCondition 缺少 'tag' 字段");
            return false;
        }

        String requiredTag = definition.get("tag").getAsString();

        // 2. 从玩家临时数据中获取当前击杀的实体标签
        if (!player.getPersistentData().contains(TEMP_ENTITY_TAG_KEY)) {
            // 没有实体标签上下文，可能不是击杀事件
            return false;
        }

        String actualTag = player.getPersistentData().getString(TEMP_ENTITY_TAG_KEY);

        // 3. 比较标签
        boolean matches = requiredTag.equals(actualTag);

        GuzhenrenEventExtension.LOGGER.debug(
            "CheckEntityTagCondition: required='{}', actual='{}', matches={}",
            requiredTag, actualTag, matches
        );

        return matches;
    }
}
