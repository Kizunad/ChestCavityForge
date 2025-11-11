package kizuna.guzhenren_event_ext.common.system.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import kizuna.guzhenren_event_ext.GuzhenrenEventExtension;
import kizuna.guzhenren_event_ext.common.config.ModConfig;
import kizuna.guzhenren_event_ext.common.system.PlayerInventoryWatcher;
import kizuna.guzhenren_event_ext.common.system.PlayerStatWatcher;
import kizuna.guzhenren_event_ext.common.system.def.EventDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EventLoader extends SimplePreparableReloadListener<List<EventDefinition>> {

    private static final EventLoader INSTANCE = new EventLoader();
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = "events"; // Path within the mod's data namespace
    private static final Type LIST_TYPE = new TypeToken<List<EventDefinition>>() {}.getType();

    private List<EventDefinition> loadedEvents = new ArrayList<>();

    private EventLoader() {}

    public static EventLoader getInstance() {
        return INSTANCE;
    }

    @Override
    protected List<EventDefinition> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        List<EventDefinition> definitions = new ArrayList<>();
        // The namespace is automatically handled by the resource manager
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(DIRECTORY, (location) -> location.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try {
                String json = new String(entry.getValue().open().readAllBytes(), StandardCharsets.UTF_8);

                // 1) 优先按数组解析
                List<EventDefinition> fileEvents = null;
                try {
                    fileEvents = GSON.fromJson(json, LIST_TYPE);
                } catch (Exception ignore) {
                    // fallthrough
                }

                if (fileEvents == null) {
                    // 2) 兼容：单对象解析（便于子目录内使用单个对象文件）
                    try {
                        EventDefinition single = GSON.fromJson(json, EventDefinition.class);
                        if (single != null) {
                            fileEvents = new ArrayList<>();
                            fileEvents.add(single);
                        }
                    } catch (Exception ignore) {
                        // fallthrough
                    }
                }

                if (fileEvents == null) {
                    GuzhenrenEventExtension.LOGGER.warn("Ignoring event file (unparseable): {}", entry.getKey());
                    continue;
                }

                // 3) 规范化并校验
                for (EventDefinition def : fileEvents) {
                    normalizeDefinition(def);
                    if (def == null || def.id == null || def.id.isEmpty() || def.trigger == null) {
                        GuzhenrenEventExtension.LOGGER.warn(
                            "Skipping invalid event definition in file {}: id and trigger are required.",
                            entry.getKey());
                        continue;
                    }
                    definitions.add(def);
                }

            } catch (Exception e) {
                GuzhenrenEventExtension.LOGGER.error("Failed to load event definition file: {}", entry.getKey(), e);
            }
        }
        return definitions;
    }

    /**
     * 规范化：兼容将 "id" 用作类型键的写法，统一转成 "type"；确保条件与动作数组非空时元素也被规范化。
     */
    private void normalizeDefinition(EventDefinition def) {
        if (def == null) return;
        // trigger: id -> type
        if (def.trigger != null) {
            if (!def.trigger.has("type") && def.trigger.has("id") && def.trigger.get("id").isJsonPrimitive()) {
                def.trigger.addProperty("type", def.trigger.get("id").getAsString());
            }
        }
        // conditions
        if (def.conditions != null) {
            for (var cond : def.conditions) {
                if (cond != null && !cond.has("type") && cond.has("id") && cond.get("id").isJsonPrimitive()) {
                    cond.addProperty("type", cond.get("id").getAsString());
                }
            }
        }
        // actions
        if (def.actions != null) {
            for (var act : def.actions) {
                if (act != null && !act.has("type") && act.has("id") && act.get("id").isJsonPrimitive()) {
                    act.addProperty("type", act.get("id").getAsString());
                }
            }
        }
    }

    @Override
    protected void apply(List<EventDefinition> definitions, ResourceManager resourceManager, ProfilerFiller profiler) {
        this.loadedEvents = definitions;
        GuzhenrenEventExtension.LOGGER.info("Loaded {} event definitions.", definitions.size());

        // Activate watchers based on loaded triggers
        Set<String> statsToWatch = new HashSet<>();
        boolean watchInventory = false;

        for (EventDefinition def : definitions) {
            if (def.trigger != null && def.trigger.has("type")) {
                String type = def.trigger.get("type").getAsString();
                if ("guzhenren_event_ext:player_stat_change".equals(type) && def.trigger.has("stat")) {
                    statsToWatch.add(def.trigger.get("stat").getAsString());
                } else if ("guzhenren_event_ext:player_obtained_item".equals(type)) {
                    watchInventory = true;
                }
            }
        }

        // Apply configuration values to watchers
        PlayerStatWatcher.getInstance().setWatchedStats(statsToWatch);
        PlayerStatWatcher.getInstance().setPollingInterval(ModConfig.getStatWatcherInterval());

        if (watchInventory) {
            PlayerInventoryWatcher.getInstance().activate();
            PlayerInventoryWatcher.getInstance().setPollingInterval(ModConfig.getInventoryWatcherInterval());
        }

        GuzhenrenEventExtension.LOGGER.info("Stat Watcher is monitoring {} stats (interval: {} ticks).", statsToWatch.size(), ModConfig.getStatWatcherInterval());
        GuzhenrenEventExtension.LOGGER.info("Inventory Watcher is {} (interval: {} ticks).", watchInventory ? "ACTIVE" : "INACTIVE", ModConfig.getInventoryWatcherInterval());
    }

    public List<EventDefinition> getLoadedEvents() {
        return loadedEvents;
    }
}
