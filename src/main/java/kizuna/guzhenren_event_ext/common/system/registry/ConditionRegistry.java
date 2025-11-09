package kizuna.guzhenren_event_ext.common.system.registry;

import kizuna.guzhenren_event_ext.GuzhenrenEventExtension;
import kizuna.guzhenren_event_ext.common.system_modules.ICondition;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConditionRegistry {

    private static final ConditionRegistry INSTANCE = new ConditionRegistry();

    private final Map<String, ICondition> registry = new ConcurrentHashMap<>();

    private ConditionRegistry() {}

    public static ConditionRegistry getInstance() {
        return INSTANCE;
    }

    public void register(String type, ICondition condition) {
        if (registry.containsKey(type)) {
            GuzhenrenEventExtension.LOGGER.warn("Duplicate condition type registration attempted for: {}", type);
            return;
        }
        registry.put(type, condition);
    }

    @Nullable
    public ICondition get(String type) {
        return registry.get(type);
    }
}
