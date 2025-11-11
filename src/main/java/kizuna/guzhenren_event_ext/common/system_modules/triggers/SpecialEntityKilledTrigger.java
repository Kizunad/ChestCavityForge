package kizuna.guzhenren_event_ext.common.system_modules.triggers;

import com.google.gson.JsonObject;
import kizuna.guzhenren_event_ext.common.event.api.SpecialEntityKilledEvent;
import kizuna.guzhenren_event_ext.common.system_modules.ITrigger;

/**
 * Trigger：玩家击杀带有特殊标记的实体时触发
 * <p>
 * 该 Trigger 依赖于 {@link SpecialEntityKilledEvent}，当玩家击杀的实体 NBT 中包含
 * {@code guzhenren_event_ext:entity_tag} 时触发。
 * <p>
 * JSON 配置格式：
 * <pre>
 * {
 *   "type": "guzhenren_event_ext:special_entity_killed"
 * }
 * </pre>
 * <p>
 * 事件上下文通过 {@link SpecialEntityKilledEvent#getEntityTag()} 获取标签值。
 * <p>
 * 典型用法：配合 {@link kizuna.guzhenren_event_ext.common.system_modules.conditions.CheckEntityTagCondition}
 * 检查特定标签，实现击杀特定敌人的奖励。
 */
public class SpecialEntityKilledTrigger implements ITrigger<SpecialEntityKilledEvent> {

    @Override
    public boolean matches(SpecialEntityKilledEvent event, JsonObject triggerDef) {
        // 所有带标签的实体击杀都会触发此 Trigger
        // 具体的标签匹配由 Condition 负责
        return event.getEntityTag() != null && !event.getEntityTag().isEmpty();
    }
}
