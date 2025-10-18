package net.tigereye.chestcavity.soul.fakeplayer;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

/**
 * 灵魂实体生成的结果信息。
 */
public record SoulEntitySpawnResult(
        UUID soulId,
        Entity entity,
        EntityType<?> entityType,
        @Nullable ResourceLocation geckoModelId,
        String reason,
        boolean reusedExisting
) {

    public SoulEntitySpawnResult {
        Objects.requireNonNull(soulId, "soulId");
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(reason, "reason");
    }

    public Optional<SoulPlayer> asSoulPlayer() {
        return entity instanceof SoulPlayer soulPlayer ? Optional.of(soulPlayer) : Optional.empty();
    }

    public boolean isNewSpawn() {
        return !reusedExisting;
    }
}
