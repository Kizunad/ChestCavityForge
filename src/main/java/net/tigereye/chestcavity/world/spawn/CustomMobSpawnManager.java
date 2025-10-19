package net.tigereye.chestcavity.world.spawn;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.tigereye.chestcavity.ChestCavity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * 运行时生成管理器。
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class CustomMobSpawnManager {

    private static final Map<MinecraftServer, Map<ResourceLocation, EntityTracker>> SERVER_TRACKERS = new WeakHashMap<>();

    private CustomMobSpawnManager() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (CustomMobSpawnRegistry.isEmpty()) {
            return;
        }
        Map<ResourceLocation, EntityTracker> trackerMap = SERVER_TRACKERS.computeIfAbsent(server, s -> new HashMap<>());
        for (CustomMobSpawnDefinition definition : CustomMobSpawnRegistry.definitions()) {
            EntityTracker tracker = trackerMap.computeIfAbsent(definition.id(), key -> new EntityTracker(definition));
            tracker.tick(server);
        }
    }

    private static final class EntityTracker {
        private final CustomMobSpawnDefinition definition;
        private final RandomSource random = RandomSource.create();
        private final List<TrackedMob> tracked = new ArrayList<>();
        private int cooldown;

        private EntityTracker(CustomMobSpawnDefinition definition) {
            this.definition = definition;
            this.cooldown = definition.attemptIntervalTicks();
        }

        private void tick(MinecraftServer server) {
            cleanup();
            tickTracked();
            if (--cooldown <= 0) {
                cooldown = definition.attemptIntervalTicks();
                attemptSpawn(server);
            }
        }

        private void cleanup() {
            Iterator<TrackedMob> iterator = tracked.iterator();
            while (iterator.hasNext()) {
                TrackedMob mob = iterator.next();
                if (!mob.isAlive()) {
                    iterator.remove();
                }
            }
        }

        private void tickTracked() {
            Iterator<TrackedMob> iterator = tracked.iterator();
            while (iterator.hasNext()) {
                TrackedMob entry = iterator.next();
                if (!entry.tick()) {
                    iterator.remove();
                }
            }
        }

        private void attemptSpawn(MinecraftServer server) {
            if (definition.maxAlive() >= 0 && tracked.size() >= definition.maxAlive()) {
                return;
            }
            if (random.nextDouble() > definition.spawnChance()) {
                return;
            }
            int batchSize = definition.spawnBatchSize();
            for (int i = 0; i < batchSize; i++) {
                if (definition.maxAlive() >= 0 && tracked.size() >= definition.maxAlive()) {
                    break;
                }
                Optional<SpawnLocation> spawnLocation = definition.locationProvider().find(server, definition, random);
                if (spawnLocation.isEmpty()) {
                    break;
                }
                SpawnLocation location = spawnLocation.get();
                ServerLevel level = location.level();
                if (level == null) {
                    continue;
                }
                EntityType<? extends Mob> type = definition.entityType().get();
                if (type == null) {
                    continue;
                }
                Mob mob = type.create(level);
                if (mob == null) {
                    continue;
                }
                Vec3 pos = location.position();
                mob.moveTo(pos.x, pos.y, pos.z, location.yaw(), location.pitch());
                if (definition.forcePersistence()) {
                    mob.setPersistenceRequired();
                }
                if (!definition.spawnValidator().test(definition, level, pos, random)) {
                    mob.discard();
                    continue;
                }
                if (!level.tryAddFreshEntityWithPassengers(mob)) {
                    mob.discard();
                    continue;
                }
                SpawnedMobContext context = new SpawnedMobContext(definition, server, level, mob, pos);
                for (SpawnConfigurator configurator : definition.configurators()) {
                    try {
                        configurator.configure(context);
                    } catch (Throwable throwable) {
                        ChestCavity.LOGGER.error("[spawn] configurator error id={} mob={}", definition.id(), mob.getUUID(), throwable);
                    }
                }
                if (!definition.brainConfigurators().isEmpty()) {
                    var brain = mob.getBrain();
                    for (BrainConfigurator brainConfigurator : definition.brainConfigurators()) {
                        try {
                            brainConfigurator.configure(context, brain);
                        } catch (Throwable throwable) {
                            ChestCavity.LOGGER.error("[spawn] brain-configurator error id={} mob={}", definition.id(), mob.getUUID(), throwable);
                        }
                    }
                }
                TrackedMob trackedMob = new TrackedMob(mob, definition.messageEmitter().orElse(null));
                tracked.add(trackedMob);
            }
        }
    }

    private static final class TrackedMob {
        private final WeakReference<Mob> mobRef;
        private final CustomMobMessageEmitter.RuntimeState messageState;

        private TrackedMob(Mob mob, CustomMobMessageEmitter emitter) {
            this.mobRef = new WeakReference<>(mob);
            this.messageState = emitter == null ? null : new CustomMobMessageEmitter.RuntimeState(emitter);
            if (messageState != null && mob.level() instanceof ServerLevel serverLevel) {
                messageState.broadcastOnSpawn(serverLevel, mob);
            }
        }

        private boolean tick() {
            Mob mob = mobRef.get();
            if (mob == null || mob.isRemoved() || mob.isDeadOrDying()) {
                return false;
            }
            if (messageState != null && mob.level() instanceof ServerLevel serverLevel) {
                return messageState.tick(serverLevel, mob);
            }
            return true;
        }

        private boolean isAlive() {
            Mob mob = mobRef.get();
            return mob != null && !mob.isRemoved() && !mob.isDeadOrDying();
        }
    }
}
