package kizuna.guzhenren_event_ext.common.system.registry;

import kizuna.guzhenren_event_ext.GuzhenrenEventExtension;
import kizuna.guzhenren_event_ext.common.system_modules.IAction;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ActionRegistry {

    private static final ActionRegistry INSTANCE = new ActionRegistry();

    private final Map<String, IAction> registry = new ConcurrentHashMap<>();

    private ActionRegistry() {}

    public static ActionRegistry getInstance() {
        return INSTANCE;
    }

    public void register(String type, IAction action) {
        if (registry.containsKey(type)) {
            GuzhenrenEventExtension.LOGGER.warn("Duplicate action type registration attempted for: {}", type);
            return;
        }
        registry.put(type, action);
    }

    @Nullable
    public IAction get(String type) {
        return registry.get(type);
    }
}
