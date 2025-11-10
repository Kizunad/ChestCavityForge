package kizuna.guzhenren_event_ext.common.system;

import kizuna.guzhenren_event_ext.common.config.Gamerules;
import kizuna.guzhenren_event_ext.common.event.api.GuzhenrenStatChangeEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStatWatcher {

    private static final PlayerStatWatcher INSTANCE = new PlayerStatWatcher();

    private final Map<UUID, Map<String, Double>> lastKnownStats = new ConcurrentHashMap<>();
    private Set<String> watchedStats = Collections.emptySet();
    private int pollingInterval = 40; // Default to 2 seconds
    private int tickCounter = 0;

    private PlayerStatWatcher() {}

    public static PlayerStatWatcher getInstance() {
        return INSTANCE;
    }

    public void setWatchedStats(Set<String> stats) {
        this.watchedStats = Collections.unmodifiableSet(stats);
        // Clear cache when watched stats change to avoid holding onto unnecessary data
        this.lastKnownStats.clear();
    }

    public void setPollingInterval(int interval) {
        this.pollingInterval = Math.max(1, interval);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        this.tick(event.getServer());
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            this.lastKnownStats.remove(event.getEntity().getUUID());
        }
    }

    private void tick(MinecraftServer server) {
        if (!server.getGameRules().getBoolean(Gamerules.RULE_EVENT_EXTENSION_ENABLED)) {
            return;
        }
        if (watchedStats.isEmpty()) {
            return;
        }

        tickCounter++;
        if (tickCounter < pollingInterval) {
            return;
        }
        tickCounter = 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            checkPlayer(player);
        }
    }

    private void checkPlayer(ServerPlayer player) {
        GuzhenrenResourceBridge.open(player).ifPresent(handle -> {
            Map<String, Double> oldStats = lastKnownStats.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>());
            Map<String, Double> newStats = new ConcurrentHashMap<>();

            for (String statId : watchedStats) {
                handle.read(statId).ifPresent(currentValue -> {
                    Double oldValue = oldStats.get(statId);
                    newStats.put(statId, currentValue);

                    if (oldValue != null && Math.abs(currentValue - oldValue) > 1e-9) {
                        // Value has changed, fire event
                        NeoForge.EVENT_BUS.post(new GuzhenrenStatChangeEvent(player, statId, oldValue, currentValue, handle));
                    }
                });
            }
            // Update the cache with the new values
            lastKnownStats.put(player.getUUID(), newStats);
        });
    }
}
