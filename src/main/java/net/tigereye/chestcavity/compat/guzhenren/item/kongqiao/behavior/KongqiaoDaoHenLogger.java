package net.tigereye.chestcavity.compat.guzhenren.item.kongqiao.behavior;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLEnvironment;
import net.tigereye.chestcavity.compat.guzhenren.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.compat.guzhenren.network.GuzhenrenNetworkBridge;
import net.tigereye.chestcavity.compat.guzhenren.network.GuzhenrenPayloadListener;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Listens for Guzhenren player variable syncs and logs Dao Hen changes.
 */
public final class KongqiaoDaoHenLogger implements GuzhenrenPayloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double EPSILON = 1.0e-4;
    private static final String LOG_PREFIX = "[compat/guzhenren][kongqiao]";

    private static final KongqiaoDaoHenLogger INSTANCE = new KongqiaoDaoHenLogger();
    private static final AtomicBoolean INITIALISED = new AtomicBoolean(false);
    private static final int POLL_INTERVAL_TICKS = 20; // ~1s at 20 TPS
    private static int tickCounter = 0;

    private static final Map<UUID, Map<String, Double>> lastSnapshots = new ConcurrentHashMap<>();

    private KongqiaoDaoHenLogger() {
    }

    public static void bootstrap() {
        LOGGER.debug("{} bootstrap invoked (dist={}, initialised={})", LOG_PREFIX, FMLEnvironment.dist, INITIALISED.get());
        if (!FMLEnvironment.dist.isClient()) {
            LOGGER.debug("{} abort bootstrap: running on non-client dist", LOG_PREFIX);
            return;
        }
        if (INITIALISED.compareAndSet(false, true)) {
            GuzhenrenNetworkBridge.registerListener(INSTANCE);
            NeoForge.EVENT_BUS.addListener(KongqiaoDaoHenLogger::onClientTick);
            LOGGER.info("{} Dao Hen logger initialised and listener registered", LOG_PREFIX);
        } else {
            LOGGER.debug("{} bootstrap skipped: already initialised", LOG_PREFIX);
        }
    }

    @Override
    public void onPlayerVariablesSynced(Player player, GuzhenrenResourceBridge.ResourceHandle handle, Map<String, Double> snapshot) {
        LOGGER.debug("{} onPlayerVariablesSynced entry (player={}, snapshotKeys={})", LOG_PREFIX,
                player == null ? "<null>" : player.getScoreboardName(), snapshot == null ? -1 : snapshot.size());
        if (player == null || snapshot == null || snapshot.isEmpty()) {
            LOGGER.debug("{} exit: missing player/snapshot (player={}, snapshotNull={}, snapshotEmpty={})", LOG_PREFIX,
                    player == null, snapshot == null, snapshot != null && snapshot.isEmpty());
            return;
        }

        Map<String, Double> tracked = filterDaoHen(snapshot);
        if (tracked.isEmpty()) {
            LOGGER.debug("{} exit: no daohen_* keys present. Snapshot keys={}", LOG_PREFIX, snapshot.keySet());
            return;
        }

        UUID playerId = player.getUUID();
        Map<String, Double> previous = lastSnapshots.get(playerId);
        if (previous == null) {
            LOGGER.debug("{} no previous snapshot cached; logging initial values", LOG_PREFIX);
            logInitialSnapshot(player, tracked);
        } else {
            LOGGER.debug("{} previous snapshot found ({} keys); computing delta", LOG_PREFIX, previous.size());
            logDiff(player, previous, tracked);
        }
        lastSnapshots.put(playerId, Collections.unmodifiableMap(new HashMap<>(tracked)));
        LOGGER.debug("{} snapshot cached for player {} ({} tracked keys)", LOG_PREFIX, player.getScoreboardName(), tracked.size());
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        // Run on physical client only
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        if (++tickCounter % POLL_INTERVAL_TICKS != 0) {
            return;
        }
        final Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) {
            LOGGER.trace("{} skip poll: mc/player/level null", LOG_PREFIX);
            return;
        }
        var player = mc.player;
        GuzhenrenResourceBridge.open(player).ifPresent(handle -> {
            var snapshot = handle.snapshotAll();
            if (snapshot.isEmpty()) {
                LOGGER.trace("{} poll snapshot empty for {}", LOG_PREFIX, player.getScoreboardName());
                return;
            }
            var tracked = filterDaoHen(snapshot);
            if (tracked.isEmpty()) {
                return;
            }
            var previous = lastSnapshots.get(player.getUUID());
            if (previous == null) {
                LOGGER.debug("{} poll: seeding initial DaoHen snapshot ({} keys)", LOG_PREFIX, tracked.size());
                logInitialSnapshot(player, tracked);
            } else {
                logDiff(player, previous, tracked);
            }
            lastSnapshots.put(player.getUUID(), Collections.unmodifiableMap(new HashMap<>(tracked)));
        });
    }

    private static Map<String, Double> filterDaoHen(Map<String, Double> snapshot) {
        Map<String, Double> filtered = new HashMap<>();
        for (Map.Entry<String, Double> entry : snapshot.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("daohen_") || key.startsWith("dahen_")) {
                filtered.put(key, entry.getValue());
            }
        }
        return filtered;
    }

    private static void logInitialSnapshot(Player player, Map<String, Double> values) {
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            LOGGER.info("{} {} {} = {}", LOG_PREFIX, player.getScoreboardName(), entry.getKey(), format(entry.getValue()));
        }
    }

    private static void logDiff(Player player, Map<String, Double> previous, Map<String, Double> current) {
        boolean anyLogged = false;
        for (Map.Entry<String, Double> entry : current.entrySet()) {
            String key = entry.getKey();
            double newValue = entry.getValue();
            double oldValue = previous.getOrDefault(key, Double.NaN);
            if (Double.isNaN(oldValue) || Math.abs(newValue - oldValue) > EPSILON) {
                double delta = Double.isNaN(oldValue) ? newValue : newValue - oldValue;
                LOGGER.info("{} {} {} -> {} (Δ {})", LOG_PREFIX, player.getScoreboardName(), key,
                        format(newValue), format(delta));
                anyLogged = true;
            }
        }
        if (!anyLogged) {
            LOGGER.debug("{} diff computed but no changes exceeded epsilon={}", LOG_PREFIX, EPSILON);
        }
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
