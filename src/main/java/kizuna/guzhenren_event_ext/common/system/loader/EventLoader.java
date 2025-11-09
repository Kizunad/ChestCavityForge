package kizuna.guzhenren_event_ext.common.system.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import kizuna.guzhenren_event_ext.GuzhenrenEventExtension;
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
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8)) {
                List<EventDefinition> fileEvents = GSON.fromJson(reader, LIST_TYPE);
                if (fileEvents != null) {
                    // Basic validation
                    fileEvents.forEach(def -> {
                        if (def.id == null || def.id.isEmpty() || def.trigger == null) {
                            GuzhenrenEventExtension.LOGGER.warn("Skipping invalid event definition in file {}: id and trigger are required.", entry.getKey());
                        } else {
                            definitions.add(def);
                        }
                    });
                }
            } catch (Exception e) {
                GuzhenrenEventExtension.LOGGER.error("Failed to load event definition file: {}", entry.getKey(), e);
            }
        }
        return definitions;
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

        PlayerStatWatcher.getInstance().setWatchedStats(statsToWatch);
        if (watchInventory) {
            PlayerInventoryWatcher.getInstance().activate();
        }

        GuzhenrenEventExtension.LOGGER.info("Stat Watcher is monitoring {} stats.", statsToWatch.size());
        GuzhenrenEventExtension.LOGGER.info("Inventory Watcher is {}.", watchInventory ? "ACTIVE" : "INACTIVE");
    }

    public List<EventDefinition> getLoadedEvents() {
        return loadedEvents;
    }
}
