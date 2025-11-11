package kizuna.guzhenren_event_ext.common.system;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kizuna.guzhenren_event_ext.GuzhenrenEventExtension;
import kizuna.guzhenren_event_ext.common.attachment.ModAttachments;
import kizuna.guzhenren_event_ext.common.event.api.GuzhenrenPlayerEvent;
import kizuna.guzhenren_event_ext.common.event.api.GuzhenrenStatChangeEvent;
import kizuna.guzhenren_event_ext.common.event.api.PlayerObtainedItemEvent;
import kizuna.guzhenren_event_ext.common.event.api.SpecialEntityKilledEvent;
import kizuna.guzhenren_event_ext.common.system.def.EventDefinition;
import kizuna.guzhenren_event_ext.common.system.loader.EventLoader;
import kizuna.guzhenren_event_ext.common.system.registry.ActionRegistry;
import kizuna.guzhenren_event_ext.common.system.registry.ConditionRegistry;
import kizuna.guzhenren_event_ext.common.system.registry.TriggerRegistry;
import kizuna.guzhenren_event_ext.common.system_modules.IAction;
import kizuna.guzhenren_event_ext.common.system_modules.ICondition;
import kizuna.guzhenren_event_ext.common.system_modules.ITrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public class EventManager {

    private static final EventManager INSTANCE = new EventManager();

    private EventManager() {}

    public static EventManager getInstance() {
        return INSTANCE;
    }

    @SubscribeEvent
    public void onStatChange(GuzhenrenStatChangeEvent event) {
        processEvent(event, "guzhenren_event_ext:player_stat_change");
    }

    @SubscribeEvent
    public void onItemObtained(PlayerObtainedItemEvent event) {
        processEvent(event, "guzhenren_event_ext:player_obtained_item");
    }

    /**
     * 监听实体死亡事件，检查是否为玩家击杀的特殊标记实体
     */
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();

        // 检查击杀者是否为玩家
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }

        // 检查被击杀实体是否有特殊标记
        if (!victim.getPersistentData().contains("guzhenren_event_ext:entity_tag")) {
            return;
        }

        String entityTag = victim.getPersistentData().getString("guzhenren_event_ext:entity_tag");

        if (entityTag.isEmpty()) {
            return;
        }

        GuzhenrenEventExtension.LOGGER.debug(
            "Player {} killed special entity with tag: {}",
            killer.getName().getString(),
            entityTag
        );

        // 创建并发布自定义事件
        SpecialEntityKilledEvent customEvent = new SpecialEntityKilledEvent(killer, entityTag);
        NeoForge.EVENT_BUS.post(customEvent);
    }

    @SubscribeEvent
    public void onSpecialEntityKilled(SpecialEntityKilledEvent event) {
        // 为 CheckEntityTagCondition 提供临时上下文
        Player player = (Player) event.getEntity();
        player.getPersistentData().putString(
            kizuna.guzhenren_event_ext.common.system_modules.conditions.CheckEntityTagCondition.TEMP_ENTITY_TAG_KEY,
            event.getEntityTag()
        );

        try {
            processEvent(event, "guzhenren_event_ext:special_entity_killed");
        } finally {
            // 清除临时数据
            player.getPersistentData().remove(
                kizuna.guzhenren_event_ext.common.system_modules.conditions.CheckEntityTagCondition.TEMP_ENTITY_TAG_KEY
            );
        }
    }

    private <T extends Event> void processEvent(T event, String triggerType) {
        if (!(event instanceof GuzhenrenPlayerEvent playerEvent)) {
            return;
        }
        Player player = (Player) playerEvent.getEntity();

        for (EventDefinition def : EventLoader.getInstance().getLoadedEvents()) {
            if (!def.enabled || def.trigger == null) {
                continue;
            }

            JsonElement typeElement = def.trigger.get("type");
            if (typeElement == null || !typeElement.isJsonPrimitive() || !triggerType.equals(typeElement.getAsString())) {
                continue;
            }

            // 1. Check Trigger
            ITrigger<T> trigger = TriggerRegistry.getInstance().get(triggerType);
            if (trigger == null || !trigger.matches(event, def.trigger)) {
                continue;
            }

            // 2. Check TriggerOnce
            if (def.triggerOnce) {
                if (ModAttachments.get(player).hasTriggered(def.id)) {
                    continue;
                }
            }

            // 3. Check Conditions
            boolean allConditionsMet = true;
            if (def.conditions != null) {
                for (JsonObject conditionDef : def.conditions) {
                    JsonElement conditionTypeElement = conditionDef.get("type");
                    if (conditionTypeElement == null || !conditionTypeElement.isJsonPrimitive()) continue;
                    String conditionType = conditionTypeElement.getAsString();

                    ICondition condition = ConditionRegistry.getInstance().get(conditionType);
                    if (condition == null) {
                        GuzhenrenEventExtension.LOGGER.warn("Unknown condition type '{}' in event '{}'", conditionType, def.id);
                        allConditionsMet = false;
                        break;
                    }
                    if (!condition.check(player, conditionDef)) {
                        allConditionsMet = false;
                        break;
                    }
                }
            }

            if (!allConditionsMet) {
                continue;
            }

            // 4. Execute Actions
            GuzhenrenEventExtension.LOGGER.debug("Executing actions for event '{}' for player {}", def.id, player.getName().getString());
            if (def.actions != null) {
                for (JsonObject actionDef : def.actions) {
                    JsonElement actionTypeElement = actionDef.get("type");
                    if (actionTypeElement == null || !actionTypeElement.isJsonPrimitive()) continue;
                    String actionType = actionTypeElement.getAsString();

                    IAction action = ActionRegistry.getInstance().get(actionType);
                    if (action == null) {
                        GuzhenrenEventExtension.LOGGER.warn("Unknown action type '{}' in event '{}'", actionType, def.id);
                        continue;
                    }
                    try {
                        action.execute(player, actionDef);
                    } catch (Exception e) {
                        GuzhenrenEventExtension.LOGGER.error("Error executing action '{}' for event '{}'", actionType, def.id, e);
                    }
                }
            }

            // 5. Mark as triggered if 'trigger_once' is true
            if (def.triggerOnce) {
                ModAttachments.get(player).addTriggeredEvent(def.id);
            }
        }
    }
}
