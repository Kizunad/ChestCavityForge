package net.tigereye.chestcavity.soul.fakeplayer;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.tigereye.chestcavity.soul.storage.SoulEntityArchive;
import net.tigereye.chestcavity.soul.util.SoulLog;

/** 工厂注册中心：按 {@link ResourceLocation} 分派生成逻辑。 */
public final class SoulEntityFactories {

  private static final Map<ResourceLocation, SoulEntityFactory> FACTORIES =
      new ConcurrentHashMap<>();

  private SoulEntityFactories() {}

  public static void register(ResourceLocation id, SoulEntityFactory factory) {
    if (id == null || factory == null) {
      throw new IllegalArgumentException("id and factory must not be null");
    }
    SoulEntityFactory previous = FACTORIES.put(id, factory);
    if (previous != null && previous != factory) {
      SoulLog.warn(
          "[soul] factory-overwrite id={} prev={} new={}",
          id,
          previous.getClass().getName(),
          factory.getClass().getName());
    } else {
      SoulLog.info("[soul] factory-register id={} impl={} ", id, factory.getClass().getName());
    }
  }

  public static void unregister(ResourceLocation id) {
    if (id == null) {
      return;
    }
    FACTORIES.remove(id);
  }

  public static Optional<SoulEntityFactory> get(ResourceLocation id) {
    return Optional.ofNullable(FACTORIES.get(id));
  }

  public static Optional<SoulEntitySpawnResult> spawn(SoulEntitySpawnRequest request) {
    SoulEntityFactory factory = FACTORIES.get(request.factoryId());
    if (factory == null) {
      SoulLog.warn(
          "[soul] spawn-abort reason=factory-missing id={} entity={} cause=noFactory",
          request.factoryId(),
          request.entityId());
      return Optional.empty();
    }
    request
        .fallbackLevel()
        .ifPresent(
            level -> {
              if (request.ensureChunkLoaded()) {
                try {
                  level.getChunkAt(BlockPos.containing(request.fallbackPosition()));
                } catch (Throwable throwable) {
                  SoulLog.error(
                      "[soul] spawn-chunk-load-failed id={} dim={} pos=({},{},{})",
                      throwable,
                      request.factoryId(),
                      level.dimension().location(),
                      request.fallbackPosition().x,
                      request.fallbackPosition().y,
                      request.fallbackPosition().z);
                }
              }
            });
    try {
      return factory.spawn(request);
    } catch (Throwable throwable) {
      SoulLog.error(
          "[soul] spawn-factory-error id={} entity={} reason={}",
          throwable,
          request.factoryId(),
          request.entityId(),
          request.reason());
      return Optional.empty();
    }
  }

  public static void persist(MinecraftServer server, UUID entityId, CompoundTag tag) {
    SoulEntityArchive.get(server).put(entityId, tag);
  }

  public static Optional<CompoundTag> peek(MinecraftServer server, UUID entityId) {
    return SoulEntityArchive.get(server).peek(entityId);
  }

  public static void discard(MinecraftServer server, UUID entityId) {
    SoulEntityArchive.get(server).remove(entityId);
  }
}
