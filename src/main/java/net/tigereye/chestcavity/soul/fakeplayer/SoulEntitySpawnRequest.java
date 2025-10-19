package net.tigereye.chestcavity.soul.fakeplayer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.storage.SoulEntityArchive;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

/**
 * 请求对象：描述一次灵魂实体（含 SoulPlayer 与后续扩展实体）的生成参数。
 *
 * <p>设计目标：
 * <ul>
 *     <li>统一封装生成过程所需的上下文（服务器、优先维度、默认位置、原因等）；</li>
 *     <li>通过 {@link ResourceLocation} 标识生成工厂，便于按需扩展；</li>
 *     <li>支持从 {@link SoulEntityArchive} 读取/消费持久化存档，为“非玩家阵营”实体提供落地方案。</li>
 * </ul>
 *
 * <p>本类采用不可变设计，所有扩展属性通过 {@link ResourceLocation} → {@link Object} 传递，
 * 工厂端按需解析对应类型。</p>
 */
public final class SoulEntitySpawnRequest {

    /**
     * 控制是否在生成前访问 {@link SoulEntityArchive}。
     */
    public enum ArchiveMode {
        /** 不访问存档。 */
        NONE,
        /** 仅读取，不移除存档。 */
        READ_ONLY,
        /** 读取后即移除存档，常用于一次性重建。 */
        CONSUME
    }

    private final MinecraftServer server;
    private final ResourceLocation factoryId;
    private final UUID entityId;
    @Nullable
    private final ServerLevel fallbackLevel;
    private final Vec3 fallbackPosition;
    private final float yaw;
    private final float pitch;
    private final boolean ensureChunkLoaded;
    @Nullable
    private final ServerPlayer owner;
    private final String reason;
    private final ArchiveMode archiveMode;
    private final Map<ResourceLocation, Object> attributes;

    private CompoundTag archivedState;
    private boolean archiveResolved;

    private SoulEntitySpawnRequest(MinecraftServer server,
                                   ResourceLocation factoryId,
                                   UUID entityId,
                                   @Nullable ServerLevel fallbackLevel,
                                   Vec3 fallbackPosition,
                                   float yaw,
                                   float pitch,
                                   boolean ensureChunkLoaded,
                                   @Nullable ServerPlayer owner,
                                   String reason,
                                   ArchiveMode archiveMode,
                                   Map<ResourceLocation, Object> attributes) {
        this.server = Objects.requireNonNull(server, "server");
        this.factoryId = Objects.requireNonNull(factoryId, "factoryId");
        this.entityId = Objects.requireNonNull(entityId, "entityId");
        this.fallbackLevel = fallbackLevel;
        this.fallbackPosition = fallbackPosition == null ? Vec3.ZERO : fallbackPosition;
        this.yaw = yaw;
        this.pitch = pitch;
        this.ensureChunkLoaded = ensureChunkLoaded;
        this.owner = owner;
        this.reason = reason == null || reason.isBlank() ? "unspecified" : reason;
        this.archiveMode = archiveMode == null ? ArchiveMode.NONE : archiveMode;
        this.attributes = Collections.unmodifiableMap(new HashMap<>(attributes));
    }

    public MinecraftServer server() {
        return server;
    }

    public ResourceLocation factoryId() {
        return factoryId;
    }

    public UUID entityId() {
        return entityId;
    }

    public Optional<ServerLevel> fallbackLevel() {
        return Optional.ofNullable(fallbackLevel);
    }

    public Vec3 fallbackPosition() {
        return fallbackPosition;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public boolean ensureChunkLoaded() {
        return ensureChunkLoaded;
    }

    public Optional<ServerPlayer> owner() {
        return Optional.ofNullable(owner);
    }

    public String reason() {
        return reason;
    }

    public ArchiveMode archiveMode() {
        return archiveMode;
    }

    public Map<ResourceLocation, Object> attributes() {
        return attributes;
    }

    public <T> Optional<T> attribute(ResourceLocation key, Class<T> type) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(type, "type");
        Object value = attributes.get(key);
        if (value == null || !type.isInstance(value)) {
            return Optional.empty();
        }
        return Optional.of(type.cast(value));
    }

    /**
     * 按需加载 {@link SoulEntityArchive} 中的快照。调用方获取的是拷贝，可安全修改。
     */
    public Optional<CompoundTag> archivedState() {
        if (archiveMode == ArchiveMode.NONE) {
            return Optional.empty();
        }
        synchronized (this) {
            if (!archiveResolved) {
                archiveResolved = true;
                SoulEntityArchive archive = SoulEntityArchive.get(server);
                Optional<CompoundTag> fetched = switch (archiveMode) {
                    case READ_ONLY -> archive.peek(entityId);
                    case CONSUME -> archive.consume(entityId);
                    case NONE -> Optional.empty();
                };
                archivedState = fetched.map(CompoundTag::copy).orElse(null);
            }
        }
        return archivedState == null ? Optional.empty() : Optional.of(archivedState.copy());
    }

    public static Builder builder(MinecraftServer server, ResourceLocation factoryId, UUID entityId) {
        return new Builder(server, factoryId, entityId);
    }

    /** Builder for {@link SoulEntitySpawnRequest}. */
    public static final class Builder {
        private final MinecraftServer server;
        private final ResourceLocation factoryId;
        private final UUID entityId;
        private ServerLevel fallbackLevel;
        private Vec3 fallbackPosition = Vec3.ZERO;
        private float yaw;
        private float pitch;
        private boolean ensureChunkLoaded = true;
        private ServerPlayer owner;
        private String reason = "unspecified";
        private ArchiveMode archiveMode = ArchiveMode.NONE;
        private final Map<ResourceLocation, Object> attributes = new HashMap<>();

        private Builder(MinecraftServer server, ResourceLocation factoryId, UUID entityId) {
            this.server = Objects.requireNonNull(server, "server");
            this.factoryId = Objects.requireNonNull(factoryId, "factoryId");
            this.entityId = Objects.requireNonNull(entityId, "entityId");
        }

        public Builder withFallbackLevel(@Nullable ServerLevel level) {
            this.fallbackLevel = level;
            return this;
        }

        public Builder withFallbackPosition(Vec3 position) {
            this.fallbackPosition = position == null ? Vec3.ZERO : position;
            return this;
        }

        public Builder withYaw(float yaw) {
            this.yaw = yaw;
            return this;
        }

        public Builder withPitch(float pitch) {
            this.pitch = pitch;
            return this;
        }

        public Builder ensureChunkLoaded(boolean ensure) {
            this.ensureChunkLoaded = ensure;
            return this;
        }

        public Builder withOwner(@Nullable ServerPlayer owner) {
            this.owner = owner;
            return this;
        }

        public Builder withReason(String reason) {
            if (reason == null || reason.isBlank()) {
                this.reason = "unspecified";
            } else {
                this.reason = reason;
            }
            return this;
        }

        public Builder withArchiveMode(ArchiveMode mode) {
            this.archiveMode = mode == null ? ArchiveMode.NONE : mode;
            return this;
        }

        public Builder withAttribute(ResourceLocation key, Object value) {
            Objects.requireNonNull(key, "key");
            if (value == null) {
                attributes.remove(key);
            } else {
                attributes.put(key, value);
            }
            return this;
        }

        public SoulEntitySpawnRequest build() {
            return new SoulEntitySpawnRequest(server,
                    factoryId,
                    entityId,
                    fallbackLevel,
                    fallbackPosition,
                    yaw,
                    pitch,
                    ensureChunkLoaded,
                    owner,
                    reason,
                    archiveMode,
                    attributes);
        }
    }
}

