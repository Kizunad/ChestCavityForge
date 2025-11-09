package kizuna.guzhenren_event_ext.common.system_modules;

import com.google.gson.JsonObject;
import net.neoforged.bus.api.Event;

/**
 * Interface for a trigger condition. It checks if a fired event matches the criteria defined in the JSON.
 * @param <T> The type of the event this trigger can handle (e.g., GuzhenrenStatChangeEvent).
 */
@FunctionalInterface
public interface ITrigger<T extends Event> {
    boolean matches(T event, JsonObject definition);
}
