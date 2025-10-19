package net.tigereye.chestcavity.soul.entity;

import java.util.Collections;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.soul.entity.data.TestSoulWorldData;

/**
 * 负责 Test 生物的世界级上限与存活计数。
 *
 * <p>使用 WeakHashMap 以服务器实例划分状态，保证游戏在同一进程内切换存档时，
 * 不会共享上限设置。</p>
 */
public final class TestSoulManager {

    private static final Map<MinecraftServer, ServerState> STATE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private TestSoulManager() {
    }

    public static boolean canSpawn(ServerLevel level) {
        ServerState state = state(level);
        int limit = resolveData(level).getMaxCount();
        return state.active.size() < limit;
    }

    public static void register(TestSoulEntity entity) {
        if (entity == null || !(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        state(serverLevel).active.add(entity.getUUID());
    }

    public static void unregister(TestSoulEntity entity) {
        if (entity == null || !(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        state(serverLevel).active.remove(entity.getUUID());
    }

    public static int getActiveCount(ServerLevel level) {
        return state(level).active.size();
    }

    public static Collection<UUID> getActiveIds(ServerLevel level) {
        return List.copyOf(state(level).active);
    }

    public static int getMaxCount(ServerLevel level) {
        return resolveData(level).getMaxCount();
    }

    public static int setMaxCount(ServerLevel level, int value) {
        TestSoulWorldData data = resolveData(level);
        data.setMaxCount(value);
        return data.getMaxCount();
    }

    private static ServerState state(ServerLevel level) {
        MinecraftServer server = level.getServer();
        return STATE.computeIfAbsent(server, ignored -> new ServerState());
    }

    private static TestSoulWorldData resolveData(ServerLevel level) {
        MinecraftServer server = level.getServer();
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        ServerLevel target = overworld != null ? overworld : level;
        return TestSoulWorldData.get(target);
    }

    private static final class ServerState {
        private final Set<UUID> active = Collections.newSetFromMap(new ConcurrentHashMap<>());
    }
}
