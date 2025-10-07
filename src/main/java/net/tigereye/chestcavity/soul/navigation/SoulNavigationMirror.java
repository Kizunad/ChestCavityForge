package net.tigereye.chestcavity.soul.navigation;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.util.SoulLog;
import net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Virtual Navigation facade: keeps external API stable while switching from
 * a spawned guide Mob to an in-memory DummyMob that never enters the world.
 */
public final class SoulNavigationMirror {

    private SoulNavigationMirror() {}

    private record Goal(Vec3 target, double speed, double stopDistance) {}

    private static final Map<UUID, VirtualSoulNavigator> NAVS = new HashMap<>();
    private static final Map<UUID, Goal> GOALS = new HashMap<>();

    public static void setGoal(SoulPlayer soul, Vec3 target, double speed, double stopDistance) {
        VirtualSoulNavigator nav = ensureNavigator(soul);
        if (nav == null) return;
        nav.setGoal(soul, target, speed, stopDistance);
        GOALS.put(soul.getUUID(), new Goal(target, speed, stopDistance));
        SoulLog.info("[soul][nav] setGoal soul={} target=({}, {}, {}) speed={} stop={}",
                soul.getUUID(), target.x, target.y, target.z, speed, stopDistance);
    }

    public static void clearGoal(SoulPlayer soul) {
        Goal g = GOALS.remove(soul.getUUID());
        VirtualSoulNavigator nav = NAVS.get(soul.getUUID());
        if (nav != null) nav.clearGoal();
        if (g != null) {
            SoulLog.info("[soul][nav] clearGoal soul={}", soul.getUUID());
        }
    }

    public static void serverTick(MinecraftServer server) {
        // Iterate over a stable snapshot of ids to avoid CME when clearing
        var ids = new java.util.ArrayList<>(NAVS.keySet());
        for (UUID entityId : ids) {
            SoulPlayer soul = null;
            for (ServerLevel lvl : server.getAllLevels()) {
                var e = lvl.getEntity(entityId);
                if (e instanceof SoulPlayer sp) {
                    soul = sp;
                    break;
                }
            }
            if (soul == null) {
                NAVS.remove(entityId);
                GOALS.remove(entityId);
                continue;
            }
            VirtualSoulNavigator nav = NAVS.get(entityId);
            if (nav == null) continue;
            nav.tick(soul);
            // Navigator may clear its goal internally; external goal map is advisory only
        }
    }

    public static void onSoulRemoved(UUID soulId) {
        NAVS.remove(soulId);
        GOALS.remove(soulId);
    }

    /**
     * Legacy per-soul tick used by existing AI handlers.
     */
    public static void tick(SoulPlayer soul) {
        VirtualSoulNavigator nav = NAVS.get(soul.getUUID());
        if (nav != null) {
            nav.tick(soul);
        }
    }

    public static void clearAll() {
        NAVS.clear();
        GOALS.clear();
    }

    private static VirtualSoulNavigator ensureNavigator(SoulPlayer soul) {
        VirtualSoulNavigator existing = NAVS.get(soul.getUUID());
        if (existing != null) return existing;
        if (!(soul.level() instanceof ServerLevel level)) return null;
        VirtualSoulNavigator created = new VirtualSoulNavigator(level);
        NAVS.put(soul.getUUID(), created);
        return created;
    }
}
