package net.tigereye.chestcavity.soul.fakeplayer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 描述一次生成调用的结果。
 */
public record SoulEntitySpawnResult(UUID entityId,
                                    Entity entity,
                                    ResourceLocation factoryId,
                                    boolean restoredFromPersistentState,
                                    String reason) {

    public SoulEntitySpawnResult {
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(factoryId, "factoryId");
        reason = reason == null || reason.isBlank() ? "unspecified" : reason;
    }

    public Optional<SoulPlayer> asSoulPlayer() {
        return entity instanceof SoulPlayer soulPlayer ? Optional.of(soulPlayer) : Optional.empty();
    }
}

