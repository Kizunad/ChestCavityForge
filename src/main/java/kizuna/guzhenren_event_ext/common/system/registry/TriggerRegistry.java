package kizuna.guzhenren_event_ext.common.system.registry;

import kizuna.guzhenren_event_ext.GuzhenrenEventExtension;
import kizuna.guzhenren_event_ext.common.system_modules.ITrigger;
import net.neoforged.bus.api.Event;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TriggerRegistry {

    private static final TriggerRegistry INSTANCE = new TriggerRegistry();

    private final Map<String, ITrigger<? extends Event>> registry = new ConcurrentHashMap<>();

    private TriggerRegistry() {}

    public static TriggerRegistry getInstance() {
        return INSTANCE;
    }

    public <T extends Event> void register(String type, ITrigger<T> trigger) {
        if (registry.containsKey(type)) {
            GuzhenrenEventExtension.LOGGER.warn("Duplicate trigger type registration attempted for: {}", type);
            return;
        }
        registry.put(type, trigger);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends Event> ITrigger<T> get(String type) {
        return (ITrigger<T>) registry.get(type);
    }
}
