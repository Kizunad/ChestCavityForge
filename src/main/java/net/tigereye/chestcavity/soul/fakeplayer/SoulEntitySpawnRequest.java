package net.tigereye.chestcavity.soul.fakeplayer;

import com.mojang.authlib.GameProfile;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import net.tigereye.chestcavity.soul.profile.SoulProfile;

/**
 * 生成灵魂实体所需的请求参数。
 */
public record SoulEntitySpawnRequest(
        ServerPlayer owner,
        UUID soulId,
        GameProfile identity,
        @Nullable SoulProfile profile,
        EntityType<? extends Entity> entityType,
        @Nullable ResourceLocation geckoModelId,
        String reason,
        boolean forceDerivedIdentity,
        SoulEntitySpawnContext context
) {

    public SoulEntitySpawnRequest {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(soulId, "soulId");
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(reason, "reason");
        context = context == null ? SoulEntitySpawnContext.EMPTY : context;
    }

    public static Builder builder(ServerPlayer owner, UUID soulId, GameProfile identity, String reason) {
        return new Builder(owner, soulId, identity, reason);
    }

    public static final class Builder {
        private final ServerPlayer owner;
        private final UUID soulId;
        private final GameProfile identity;
        private final String reason;
        private SoulProfile profile;
        private EntityType<? extends Entity> entityType = EntityType.PLAYER;
        private ResourceLocation geckoModelId;
        private boolean forceDerivedIdentity;
        private SoulEntitySpawnContext context = SoulEntitySpawnContext.EMPTY;

        private Builder(ServerPlayer owner, UUID soulId, GameProfile identity, String reason) {
            this.owner = Objects.requireNonNull(owner, "owner");
            this.soulId = Objects.requireNonNull(soulId, "soulId");
            this.identity = Objects.requireNonNull(identity, "identity");
            this.reason = Objects.requireNonNull(reason, "reason");
        }

        public Builder profile(@Nullable SoulProfile profile) {
            this.profile = profile;
            return this;
        }

        public Builder entityType(EntityType<? extends Entity> entityType) {
            this.entityType = Objects.requireNonNull(entityType, "entityType");
            return this;
        }

        public Builder geckoModel(@Nullable ResourceLocation geckoModelId) {
            this.geckoModelId = geckoModelId;
            return this;
        }

        public Builder forceDerivedIdentity(boolean value) {
            this.forceDerivedIdentity = value;
            return this;
        }

        public Builder context(@Nullable SoulEntitySpawnContext context) {
            this.context = context == null ? SoulEntitySpawnContext.EMPTY : context;
            return this;
        }

        public SoulEntitySpawnRequest build() {
            return new SoulEntitySpawnRequest(owner, soulId, identity, profile, entityType, geckoModelId, reason, forceDerivedIdentity, context);
        }
    }
}
