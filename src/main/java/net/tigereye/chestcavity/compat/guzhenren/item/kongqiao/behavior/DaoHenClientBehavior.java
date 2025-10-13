package net.tigereye.chestcavity.compat.guzhenren.item.kongqiao.behavior;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;


import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.tigereye.chestcavity.guzhenren.network.GuzhenrenNetworkBridge;
import net.tigereye.chestcavity.guzhenren.network.GuzhenrenPayloadListener;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client-only DaoHen logging hooks. Registers payload listener and 1s polling tick.
 */
@OnlyIn(Dist.CLIENT)
final class DaoHenClientBehavior implements GuzhenrenPayloadListener {
    private static final AtomicBoolean CLIENT_INITIALISED = new AtomicBoolean(false);
    private static final Map<UUID, Map<String, Double>> CLIENT_SNAPSHOTS = new ConcurrentHashMap<>();
    private static final int POLL_INTERVAL_TICKS = BehaviorConfigAccess.getInt(DaoHenClientBehavior.class, "POLL_INTERVAL_TICKS", 20); // ~1s at 20 TPS
    private static int clientTickCounter = 0;

    private static final DaoHenClientBehavior INSTANCE = new DaoHenClientBehavior();

    private DaoHenClientBehavior() {
    }

    static void bootstrap() {
        if (!CLIENT_INITIALISED.compareAndSet(false, true)) {
            DaoHenBehavior.LOGGER.debug("{} DaoHenBehavior client bootstrap skipped (already initialised)", DaoHenBehavior.LOG_PREFIX);
            return;
        }
        boolean listenerAdded = GuzhenrenNetworkBridge.registerListener(INSTANCE);
        NeoForge.EVENT_BUS.addListener(DaoHenClientBehavior::onClientTick);
        DaoHenBehavior.LOGGER.info("{} DaoHenBehavior client hooks initialised (listenerAdded={}, poll={}t)",
                DaoHenBehavior.LOG_PREFIX, listenerAdded, POLL_INTERVAL_TICKS);
    }

    @Override
    public void onPlayerVariablesSynced(Player player, GuzhenrenResourceBridge.ResourceHandle handle, Map<String, Double> snapshot) {
        if (player == null || snapshot == null || snapshot.isEmpty()) {
            return;
        }
        Map<String, Double> tracked = DaoHenBehavior.filterDaoHen(snapshot);
        if (tracked.isEmpty()) {
            return;
        }
        DaoHenBehavior.processSnapshot(player, tracked, CLIENT_SNAPSHOTS, true);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        if (++clientTickCounter % POLL_INTERVAL_TICKS != 0) {
            return;
        }
        final Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) {
            return;
        }
        Player player = mc.player;
        GuzhenrenResourceBridge.open(player).ifPresent(handle -> {
            var snapshot = handle.snapshotAll();
            if (snapshot.isEmpty()) {
                return;
            }
            var tracked = DaoHenBehavior.filterDaoHen(snapshot);
            if (tracked.isEmpty()) {
                return;
            }
            DaoHenBehavior.processSnapshot(player, tracked, CLIENT_SNAPSHOTS, true);
        });
    }
}
