package net.tigereye.chestcavity.soul.util;

import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.tigereye.chestcavity.registration.CCEntities;
import net.tigereye.chestcavity.soul.entity.SoulChunkLoaderEntity;

/**
 * 通用区块加载工具，可复用在不同实体。
 */
public final class EntityChunkLoadOps {

    private EntityChunkLoadOps() {
    }

    public static SoulChunkLoaderEntity ensureChunkLoader(Entity owner, SoulChunkLoaderEntity existing, int radius) {
        if (owner == null || owner.level().isClientSide) {
            return existing;
        }
        ServerLevel level = (ServerLevel) owner.level();
        SoulChunkLoaderEntity loader = existing;
        if (loader == null || !loader.isAlive()) {
            loader = CCEntities.SOUL_CHUNK_LOADER.get().create(level);
            if (loader == null) {
                return null;
            }
            loader.setTicketRadius(radius);
            loader.moveTo(owner.position());
            loader.setUUID(UUID.randomUUID());
            level.addFreshEntity(loader);
        } else {
            loader.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
            loader.setTicketRadius(radius);
        }
        return loader;
    }
}
