package net.tigereye.chestcavity.soul.fakeplayer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.minecraft.world.entity.Entity;
import net.tigereye.chestcavity.soul.container.SoulContainer;
import net.tigereye.chestcavity.soul.profile.SoulProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

/**
 * 灵魂假人（SoulPlayer）生命周期协调器
 *
 * 职责
 * - 统一创建/复原/保存/移除基于 FakePlayer 的“灵魂分身”。
 * - 维护 Soul ↔ Owner 的映射关系，并提供指令/交互所需的查询与建议。
 *
 * 注意
 * - 仅在服务端上下文进行实体创建与传送；广播 PlayerInfo 数据包确保客户端正确渲染。
 */
public final class SoulFakePlayerSpawner {

    /**
     * SoulPlayer lifecycle helpers. This coordinator is the single authority that spawns,
     * rehydrates, saves and removes FakePlayer-backed soul avatars. All call sites should route
     * through these helpers to guarantee ownership validation and the proper PlayerInfo packets
     * reach every client.
     */
    private SoulFakePlayerSpawner() {
    }

    private static final Map<UUID, SoulPlayer> ACTIVE_SOUL_PLAYERS = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> OWNER_ACTIVE_SOUL = new ConcurrentHashMap<>();
    private static final Map<UUID, GameProfile> SOUL_IDENTITIES = new ConcurrentHashMap<>();

    private static final long ID_MASK_MSB = 0x5A5A5A5A5A5A5A5AL;
    private static final long ID_MASK_LSB = 0xA5A5A5A5A5A5A5A5L;

    private static UUID deriveEntityUuid(UUID profileId) {
        return new UUID(profileId.getMostSignificantBits() ^ ID_MASK_MSB,
                profileId.getLeastSignificantBits() ^ ID_MASK_LSB);
    }

    private static void copyProperties(GameProfile from, GameProfile to) {
        var toProps = to.getProperties();
        toProps.clear();
        for (Map.Entry<String, Property> entry : from.getProperties().entries()) {
            toProps.put(entry.getKey(), entry.getValue());
        }
    }

    private static GameProfile cloneProfile(GameProfile source) {
        GameProfile clone = new GameProfile(source.getId(), source.getName());
        copyProperties(source, clone);
        return clone;
    }

    private static GameProfile ensureIdentity(UUID profileId, GameProfile sourceProfile, boolean forceDerivedId) {
        return SOUL_IDENTITIES.compute(profileId, (id, existing) -> {
            if (existing == null) {
                UUID entityId = forceDerivedId ? deriveEntityUuid(id) : sourceProfile.getId();
                if (entityId == null) {
                    entityId = deriveEntityUuid(id);
                }
                if (!forceDerivedId && entityId.equals(id)) {
                    entityId = deriveEntityUuid(id);
                }
                String baseName = sourceProfile.getName();
                if (baseName == null || baseName.isBlank()) {
                    baseName = "Soul" + id.toString().replace("-", "").substring(0, 10);
                }
                String name = baseName.length() > 16 ? baseName.substring(0, 16) : baseName;
                GameProfile identity = new GameProfile(entityId, name);
                copyProperties(sourceProfile, identity);
                return identity;
            }
            copyProperties(sourceProfile, existing);
            return existing;
        });
    }

    private static boolean isOwner(UUID soulId, UUID ownerId) {
        SoulPlayer soul = ACTIVE_SOUL_PLAYERS.get(soulId);
        return soul != null && soul.getOwnerId().map(ownerId::equals).orElse(false);
    }

    private static Optional<SoulPlayer> respawnSoulFromProfile(ServerPlayer owner, UUID profileId,
                                                               GameProfile sourceProfile, boolean forceDerivedId) {
        SoulContainer container = CCAttachments.getSoulContainer(owner);
        SoulProfile profile = container.getOrCreateProfile(profileId);
        GameProfile identity = ensureIdentity(profileId, sourceProfile, forceDerivedId);
        GameProfile clone = cloneProfile(identity);
        SoulPlayer soulPlayer = SoulPlayer.create(owner, clone);
        profile.restore(soulPlayer);

        ServerLevel level = owner.serverLevel();
        var server = level.getServer();
        server.getPlayerList().broadcastAll(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(soulPlayer)));
        if (!level.tryAddFreshEntityWithPassengers(soulPlayer)) {
            server.getPlayerList().broadcastAll(new ClientboundPlayerInfoRemovePacket(List.of(soulPlayer.getUUID())));
            soulPlayer.discard();
            return Optional.empty();
        }
        ACTIVE_SOUL_PLAYERS.put(profileId, soulPlayer);
        return Optional.of(soulPlayer);
    }

    /**
     * 便捷调试：为执行者生成一个测试用的灵魂假人及其可视化外壳，并用新的 SoulProfile 回填状态。
     */
    public static Optional<SpawnResult> spawnTestFakePlayer(ServerPlayer executor) {
        ServerLevel level = executor.serverLevel();
        UUID soulId = UUID.randomUUID();
        String baseName = executor.getGameProfile().getName();
        if (baseName == null || baseName.isBlank()) {
            baseName = "Soul";
        }
        String name = ("Soul" + baseName).replaceAll("[^A-Za-z0-9_]", "");
        if (name.length() > 16) {
            name = name.substring(0, 16);
        }
        GameProfile spawnProfile = new GameProfile(soulId, name);
        copyProperties(executor.getGameProfile(), spawnProfile);

        SoulPlayer soulPlayer = SoulPlayer.create(executor, spawnProfile);
        ensureIdentity(soulId, soulPlayer.getGameProfile(), false);

        SoulContainer container = CCAttachments.getSoulContainer(executor);
        var createdProfile = container.getOrCreateProfile(soulId);
        createdProfile.restore(soulPlayer);

        var server = level.getServer();
        server.getPlayerList().broadcastAll(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(soulPlayer)));

        if (!level.tryAddFreshEntityWithPassengers(soulPlayer)) {
            server.getPlayerList().broadcastAll(new ClientboundPlayerInfoRemovePacket(List.of(soulPlayer.getUUID())));
            soulPlayer.discard();
            SOUL_IDENTITIES.remove(soulId);
            return Optional.empty();
        }

        ACTIVE_SOUL_PLAYERS.put(soulId, soulPlayer);
        soulPlayer.getOwnerId().ifPresent(owner -> OWNER_ACTIVE_SOUL.putIfAbsent(owner, soulId));

        return Optional.of(new SpawnResult(soulPlayer));
    }

    /**
     * 保存指定灵魂假人的当前状态到其 Owner 的 SoulProfile 中。
     */
    public static void saveSoulPlayerState(UUID soulId) {
        SoulPlayer player = ACTIVE_SOUL_PLAYERS.get(soulId);
        if (player == null) {
            return;
        }
        ensureIdentity(soulId, player.getGameProfile(), false);
        player.getOwnerId().ifPresent(owner -> {
            ServerPlayer ownerPlayer = player.serverLevel().getServer().getPlayerList().getPlayer(owner);
            if (ownerPlayer != null) {
                CCAttachments.getExistingSoulContainer(ownerPlayer)
                        .ifPresent(container -> container.getOrCreateProfile(soulId).updateFrom(player));
            }
        });
    }

    public static Optional<SoulPlayer> findSoulPlayer(UUID uuid) {
        return Optional.ofNullable(ACTIVE_SOUL_PLAYERS.get(uuid));
    }

    /**
     * 列出当前存活的灵魂假人基本信息，供调试/指令展示。
     */
    public static List<SoulPlayerInfo> listActive() {
        return ACTIVE_SOUL_PLAYERS.values().stream()
                .map(sp -> new SoulPlayerInfo(
                        sp.getUUID(),
                        sp.getOwnerId().orElse(null),
                        sp.getOwnerId().map(owner -> sp.getUUID().equals(OWNER_ACTIVE_SOUL.get(owner))).orElse(false)))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 将执行者切换到目标灵魂/本体配置：
     * - requestedId 可为灵魂 UUID 或可视化实体 UUID；
     * - 目标属于执行者时，回写当前激活存档并恢复目标存档到假人与执行者本体。
     */
    public static boolean switchTo(ServerPlayer executor, UUID requestedId) {
        SoulContainer container = CCAttachments.getSoulContainer(executor);
        UUID ownerUuid = executor.getUUID();

        if (requestedId.equals(ownerUuid)) {
            container.updateActiveProfile();
            UUID activeSoul = OWNER_ACTIVE_SOUL.remove(ownerUuid);
            if (activeSoul != null) {
                SoulProfile soulProfile = container.getOrCreateProfile(activeSoul);
                soulProfile.updateFrom(executor);
                handleRemoval(activeSoul);
                GameProfile identity = SOUL_IDENTITIES.get(activeSoul);
                boolean forceDerived = identity == null;
                if (identity == null) {
                    identity = executor.getGameProfile();
                }
                respawnSoulFromProfile(executor, activeSoul, identity, forceDerived);
            }
            handleRemoval(ownerUuid);
            ensureIdentity(ownerUuid, executor.getGameProfile(), true);
            container.setActiveProfile(ownerUuid);
            SoulProfile baseProfile = container.getOrCreateProfile(ownerUuid);
            baseProfile.restore(executor);
            return true;
        }

        Optional<UUID> resolved = resolveSoulUuid(requestedId);
        if (resolved.isEmpty()) {
            return false;
        }

        UUID soulId = resolved.get();
        SoulPlayer target = ACTIVE_SOUL_PLAYERS.get(soulId);
        if (target == null) {
            return false;
        }
        if (target.getOwnerId().isPresent() && !target.getOwnerId().get().equals(ownerUuid)) {
            return false;
        }

        container.updateActiveProfile();
        handleRemoval(ownerUuid);
        ensureIdentity(ownerUuid, executor.getGameProfile(), true);
        respawnSoulFromProfile(executor, ownerUuid, executor.getGameProfile(), true);

        SoulProfile soulProfile = container.getOrCreateProfile(soulId);
        soulProfile.updateFrom(target);
        ensureIdentity(soulId, target.getGameProfile(), false);
        handleRemoval(soulId);

        OWNER_ACTIVE_SOUL.put(ownerUuid, soulId);
        container.setActiveProfile(soulId);
        soulProfile.restore(executor);
        return true;
    }

    /**
     * 解析输入 ID：若为灵魂 UUID 直接返回；若为可视化实体 UUID，转换为对应的灵魂 UUID。
     */
    public static Optional<UUID> resolveSoulUuid(UUID id) {
        if (ACTIVE_SOUL_PLAYERS.containsKey(id)) {
            return Optional.of(id);
        }
        return Optional.empty();
    }

    /**
     * Brigadier 补全：为指令提供灵魂/可视化实体 UUID 建议。
     */
    public static CompletableFuture<Suggestions> suggestSoulPlayerUuids(CommandSourceStack source, SuggestionsBuilder builder) {
        if (source.getEntity() instanceof ServerPlayer player) {
            UUID owner = player.getUUID();
            ACTIVE_SOUL_PLAYERS.values().stream()
                    .filter(sp -> sp.getOwnerId().map(owner::equals).orElse(false))
                    .map(SoulPlayer::getUUID)
                    .forEach(uuid -> builder.suggest(uuid.toString()));
            builder.suggest("owner");
        } else {
            ACTIVE_SOUL_PLAYERS.keySet().forEach(uuid -> builder.suggest(uuid.toString()));
        }
        return builder.buildFuture();
    }

    public static boolean remove(UUID id, ServerPlayer executor) {
        Optional<UUID> resolved = resolveSoulUuid(id);
        if (resolved.isEmpty()) {
            return false;
        }
        UUID soulId = resolved.get();
        if (!isOwner(soulId, executor.getUUID())) {
            return false;
        }
        saveSoulPlayerState(soulId);
        handleRemoval(soulId);
        SOUL_IDENTITIES.remove(soulId);
        return true;
    }

    public static void removeByOwner(UUID ownerId) {
        List<UUID> toRemove = ACTIVE_SOUL_PLAYERS.values().stream()
                .filter(sp -> sp.getOwnerId().map(ownerId::equals).orElse(false))
                .map(SoulPlayer::getUUID)
                .collect(Collectors.toList());
        toRemove.forEach(soulId -> {
            saveSoulPlayerState(soulId);
            handleRemoval(soulId);
            SOUL_IDENTITIES.remove(soulId);
        });
    }

    public static void clearAll() {
        List<UUID> ids = new ArrayList<>(ACTIVE_SOUL_PLAYERS.keySet());
        ids.forEach(soulId -> {
            saveSoulPlayerState(soulId);
            handleRemoval(soulId);
        });
        OWNER_ACTIVE_SOUL.clear();
        SOUL_IDENTITIES.clear();
    }

    public static int saveAll(ServerPlayer executor) {
        var server = executor.serverLevel().getServer();
        server.getPlayerList().getPlayers().forEach(player ->
                CCAttachments.getExistingSoulContainer(player)
                        .ifPresent(container -> {
                            container.updateActiveProfile();
                            player.setData(CCAttachments.SOUL_CONTAINER.get(), container);
                        }));
        int saved = 0;
        for (UUID soulId : ACTIVE_SOUL_PLAYERS.keySet()) {
            saveSoulPlayerState(soulId);
            saved++;
        }
        server.getAllLevels().forEach(level -> level.save(null, false, false));
        return saved;
    }

    static void onSoulPlayerRemoved(SoulPlayer soulPlayer) {
        saveSoulPlayerState(soulPlayer.getUUID());
        handleRemoval(soulPlayer.getUUID());
    }

    private static void handleRemoval(UUID soulUuid) {
        SoulPlayer removed = ACTIVE_SOUL_PLAYERS.remove(soulUuid);
        if (removed != null) {
            // Ensure the in-world entity is properly removed to avoid stray visual models.
            if (!removed.isRemoved()) {
                removed.remove(Entity.RemovalReason.DISCARDED);
            }
            removed.serverLevel().getServer().getPlayerList()
                    .broadcastAll(new ClientboundPlayerInfoRemovePacket(List.of(removed.getUUID())));
            removed.getOwnerId().ifPresent(owner -> {
                if (OWNER_ACTIVE_SOUL.get(owner) != null && OWNER_ACTIVE_SOUL.get(owner).equals(soulUuid)) {
                    OWNER_ACTIVE_SOUL.remove(owner);
                }
            });
        }
    }

    public record SpawnResult(SoulPlayer soulPlayer) {}

    public record SoulPlayerInfo(UUID soulUuid,
                                 @Nullable UUID ownerId,
                                 boolean active) {}
}
