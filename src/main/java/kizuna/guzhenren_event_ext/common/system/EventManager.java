package kizuna.guzhenren_event_ext.common.system;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kizuna.guzhenren_event_ext.GuzhenrenEventExtension;
import kizuna.guzhenren_event_ext.common.attachment.ModAttachments;
import kizuna.guzhenren_event_ext.common.event.api.GuzhenrenPlayerEvent;
import kizuna.guzhenren_event_ext.common.event.api.GuzhenrenStatChangeEvent;
import kizuna.guzhenren_event_ext.common.event.api.PlayerObtainedItemEvent;
import kizuna.guzhenren_event_ext.common.system.def.EventDefinition;
import kizuna.guzhenren_event_ext.common.system.loader.EventLoader;
import kizuna.guzhenren_event_ext.common.system.registry.ActionRegistry;
import kizuna.guzhenren_event_ext.common.system.registry.ConditionRegistry;
import kizuna.guzhenren_event_ext.common.system.registry.TriggerRegistry;
import kizuna.guzhenren_event_ext.common.system_modules.IAction;
import kizuna.guzhenren_event_ext.common.system_modules.ICondition;
import kizuna.guzhenren_event_ext.common.system_modules.ITrigger;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;

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
