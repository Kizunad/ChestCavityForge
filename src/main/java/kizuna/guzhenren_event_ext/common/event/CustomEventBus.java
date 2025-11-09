package kizuna.guzhenren_event_ext.common.event;

import net.neoforged.bus.api.EventBus;
import net.neoforged.bus.api.IEventBus;

public class CustomEventBus {

    /**
     * Our dedicated event bus. Events posted here are internal to the event extension system.
     */
    public static final IEventBus EVENT_BUS = new EventBus();

    public static void register(Object target) {
        EVENT_BUS.register(target);
    }
}
