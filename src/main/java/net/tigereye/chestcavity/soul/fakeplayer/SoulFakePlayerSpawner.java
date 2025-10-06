package net.tigereye.chestcavity.soul.fakeplayer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.minecraft.world.entity.Entity;
import net.tigereye.chestcavity.soul.container.SoulContainer;
import net.tigereye.chestcavity.soul.profile.SoulProfile;
import net.tigereye.chestcavity.soul.profile.capability.CapabilityPipeline;
import net.tigereye.chestcavity.soul.util.SoulLog;
import net.tigereye.chestcavity.soul.util.SoulPersistence;
import net.tigereye.chestcavity.soul.util.SoulProfileOps;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import net.tigereye.chestcavity.soul.storage.SoulOfflineStore;

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
    // Maps the in-world SoulPlayer entity UUID -> Soul profile UUID
    private static final Map<UUID, UUID> ENTITY_TO_SOUL = new ConcurrentHashMap<>();
    // Reentrancy guard for removal to avoid cascading double-calls (die -> remove)
    private static final java.util.Set<UUID> REMOVING_ENTITIES = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());
    // Track entities spawned via our legal paths and their reasons
    private static final java.util.Set<UUID> LEGIT_ENTITY_SPAWNS = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<UUID, String> ENTITY_SPAWN_REASON = new ConcurrentHashMap<>();

    /**
     * Verify owner shell cardinality against autospawn list after a bulk stage (e.g., login restore).
     * Logs ERROR if actual shells exceed expected, with details for diagnostics.
     */
    public static void verifyOwnerShellCardinality(ServerPlayer owner, String stage) {
        UUID ownerId = owner.getUUID();
        var container = CCAttachments.getSoulContainer(owner);
        long expected = container.getKnownSoulIds().stream()
                .filter(id -> !id.equals(ownerId))
                .filter(container::isAutospawn)
                .count();
        List<SoulPlayer> ownedShells = ACTIVE_SOUL_PLAYERS.values().stream()
                .filter(sp -> sp.getOwnerId().map(ownerId::equals).orElse(false))
                .toList();
        long actual = ownedShells.size();
        if (actual > expected) {
            String details = ownedShells.stream()
                    .map(sp -> String.format("entity=%s profile=%s dim=%s pos=(%.2f,%.2f,%.2f) legit=%s reason=%s",
                            sp.getUUID(), sp.getSoulId(), sp.level().dimension().location(), sp.getX(), sp.getY(), sp.getZ(),
                            LEGIT_ENTITY_SPAWNS.contains(sp.getUUID()), ENTITY_SPAWN_REASON.getOrDefault(sp.getUUID(), "unknown")))
                    .collect(Collectors.joining("; "));
            SoulLog.error("[soul] invariant-violation stage={} owner={} expectedShells={} actualShells={} details=[{}]",
                    new IllegalStateException("shell-cardinality-exceeded"), stage, ownerId, expected, actual, details);
        } else {
            SoulLog.info("[soul] verify-cardinality stage={} owner={} expectedShells={} actualShells={}", stage, ownerId, expected, actual);
        }
    }

    public static boolean isOwnerPossessingSoul(UUID ownerId, UUID soulId) {
        if (ownerId == null || soulId == null) {
            return false;
        }
        return isOwnerPossessingSoul(ownerId, soulId, OWNER_ACTIVE_SOUL.get(ownerId));
    }

    private static boolean isOwnerPossessingSoul(UUID ownerId, UUID soulId, UUID currentActiveSoul) {
        return ownerId != null && soulId != null && currentActiveSoul != null && currentActiveSoul.equals(soulId);
    }

    private static void clearActivePossession(UUID ownerId, UUID soulId) {
        if (ownerId == null || soulId == null) {
            return;
        }
        OWNER_ACTIVE_SOUL.computeIfPresent(ownerId, (key, current) ->
                isOwnerPossessingSoul(key, soulId, current) ? null : current);
    }

    public static boolean isOwnerPossessing(UUID ownerId) {
        return ownerId != null && OWNER_ACTIVE_SOUL.containsKey(ownerId);
    }

    public static Optional<SoulPlayer> getOwnerShell(UUID ownerId) {
        if (ownerId == null) return Optional.empty();
        return Optional.ofNullable(ACTIVE_SOUL_PLAYERS.get(ownerId));
    }

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

    /** Update the cached identity name for a soul; keeps same UUID but swaps display name. */
    public static void updateIdentityName(UUID soulId, String newName) {
        if (newName == null) newName = "";
        String name = newName.length() > 16 ? newName.substring(0, 16) : newName;
        SOUL_IDENTITIES.compute(soulId, (id, existing) -> {
            if (existing == null) return null; // will be created on next respawn using owner profile fallback
            GameProfile updated = new GameProfile(existing.getId(), name);
            copyProperties(existing, updated);
            return updated;
        });
    }

    /** Returns true if any cached identity already uses this name (case-insensitive). */
    public static boolean isIdentityNameInUse(String name) {
        if (name == null || name.isBlank()) return false;
        String needle = name.trim();
        for (GameProfile gp : SOUL_IDENTITIES.values()) {
            String n = gp.getName();
            if (n != null && n.equalsIgnoreCase(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOwner(UUID soulId, UUID ownerId) {
        SoulPlayer soul = ACTIVE_SOUL_PLAYERS.get(soulId);
        return soul != null && soul.getOwnerId().map(ownerId::equals).orElse(false);
    }

    public static Optional<SoulPlayer> respawnSoulFromProfile(ServerPlayer owner,
                                                            UUID profileId,
                                                            GameProfile sourceProfile,
                                                            boolean forceDerivedId,
                                                            String reason) {
        // Reject duplicate shell for the same profile to avoid illegal extra instances.
        SoulPlayer existing = ACTIVE_SOUL_PLAYERS.get(profileId);
        if (existing != null && !existing.isRemoved()) {
            SoulLog.error("[soul] invariant-violation duplicate-shell prevented soul={} existingEntity={} dim={} pos=({},{},{}) newReason={}",
                    new IllegalStateException("duplicate-shell-prevented"),
                    profileId, existing.getUUID(), existing.level().dimension().location(),
                    existing.getX(), existing.getY(), existing.getZ(), reason);
            return Optional.of(existing);
        }
        // Do not forcibly remove existing shells here; caller defines lifecycle.
        SoulContainer container = CCAttachments.getSoulContainer(owner);
        SoulProfile profile = container.getOrCreateProfile(profileId);
        GameProfile identity = ensureIdentity(profileId, sourceProfile, forceDerivedId);
        GameProfile clone = cloneProfile(identity);

        profile.position().ifPresent(snapshot -> SoulLog.info(
                "[soul] respawn-prepare reason={} soul={} snapshot=({}, {}, {}, {}) dim={}",
                reason, profileId,
                snapshot.x(), snapshot.y(), snapshot.z(), snapshot.yaw(),
                snapshot.dimension().location()
        ));

        // 1️⃣ 创建实体，但暂不设置位置（否则会被 spawn 逻辑覆盖）
        SoulPlayer soulPlayer = SoulPlayer.create(owner, profileId, clone);

        ServerLevel ownerLevel = owner.serverLevel();
        var server = ownerLevel.getServer();

        // 2️⃣ 先注册客户端显示
        server.getPlayerList().broadcastAll(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(soulPlayer)));

        // 3️⃣ 暂时放在 owner 当前维度中生成（spawn 点只是占位）
        if (!ownerLevel.tryAddFreshEntityWithPassengers(soulPlayer)) {
            server.getPlayerList().broadcastAll(new ClientboundPlayerInfoRemovePacket(List.of(soulPlayer.getUUID())));
            soulPlayer.discard();
            SoulLog.info("[soul] spawn-aborted reason={} soul={} owner={} cause=spawnFailed", reason, profileId, owner.getUUID());
            return Optional.empty();
        }

        // 4️⃣ 恢复数据 + 同步装备渲染
        profile.restoreBase(soulPlayer);
        net.tigereye.chestcavity.soul.util.SoulRenderSync.syncEquipmentForPlayer(soulPlayer);

        // 5️⃣ 再传送分魂到记录位置（注意：这里传送的是 soulPlayer 自身）
        profile.position().ifPresent(snapshot -> {
            ServerLevel targetLevel = server.getLevel(snapshot.dimension());
            if (targetLevel != null && targetLevel != soulPlayer.level()) {
                // ⛔ 不要用 owner 作为 teleport source！
                soulPlayer.teleportTo(targetLevel, snapshot.x(), snapshot.y(), snapshot.z(), snapshot.yaw(), snapshot.pitch());
            } else {
                soulPlayer.moveTo(snapshot.x(), snapshot.y(), snapshot.z(), snapshot.yaw(), snapshot.pitch());
            }
            soulPlayer.setYHeadRot(snapshot.headYaw());
            SoulLog.info("[soul] restore-position-complete reason={} soul={} dim={} pos=({},{},{})",
                    reason, profileId, snapshot.dimension().location(),
                    snapshot.x(), snapshot.y(), snapshot.z());
        });

        // 6️⃣ 登记映射
        ACTIVE_SOUL_PLAYERS.put(profileId, soulPlayer);
        ENTITY_TO_SOUL.put(soulPlayer.getUUID(), profileId);
        LEGIT_ENTITY_SPAWNS.add(soulPlayer.getUUID());
        ENTITY_SPAWN_REASON.put(soulPlayer.getUUID(), reason);

        SoulLog.info("[soul] spawn-complete reason={} soul={} owner={} dim={} pos=({},{},{})",
                reason, profileId, owner.getUUID(),
                soulPlayer.level().dimension().location(),
                soulPlayer.getX(), soulPlayer.getY(), soulPlayer.getZ());

        return Optional.of(soulPlayer);
    }



    /**
     * Public helper to respawn a soul avatar for its owner using a best-effort identity.
     */
    public static Optional<SoulPlayer> respawnForOwner(ServerPlayer owner, UUID soulId) {
        GameProfile identity = SOUL_IDENTITIES.getOrDefault(soulId, owner.getGameProfile());
        boolean forceDerived = !SOUL_IDENTITIES.containsKey(soulId);
        return respawnSoulFromProfile(owner, soulId, identity, forceDerived, "respawnForOwner");
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

        SoulPlayer soulPlayer = SoulPlayer.create(executor, soulId, spawnProfile);
        ensureIdentity(soulId, soulPlayer.getGameProfile(), false);

        SoulContainer container = CCAttachments.getSoulContainer(executor);
        var createdProfile = container.getOrCreateProfile(soulId);
        createdProfile.restoreBase(soulPlayer);
        createdProfile.position().ifPresent(snapshot -> {
            ServerLevel snapshotLevel = executor.server.getLevel(snapshot.dimension());
            if (snapshotLevel != null && snapshotLevel != soulPlayer.level()) {
                soulPlayer.teleportTo(snapshotLevel, snapshot.x(), snapshot.y(), snapshot.z(), snapshot.yaw(), snapshot.pitch());
            } else {
                soulPlayer.moveTo(snapshot.x(), snapshot.y(), snapshot.z(), snapshot.yaw(), snapshot.pitch());
            }
            soulPlayer.setYHeadRot(snapshot.headYaw());
        });

        var server = level.getServer();
        server.getPlayerList().broadcastAll(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(soulPlayer)));

        if (!level.tryAddFreshEntityWithPassengers(soulPlayer)) {
            server.getPlayerList().broadcastAll(new ClientboundPlayerInfoRemovePacket(List.of(soulPlayer.getUUID())));
            soulPlayer.discard();
            SOUL_IDENTITIES.remove(soulId);
            SoulLog.info("[soul] spawn-aborted reason=spawnTestFakePlayer soul={} owner={} cause=spawnFailed", soulId, executor.getUUID());
            return Optional.empty();
        }

        ACTIVE_SOUL_PLAYERS.put(soulId, soulPlayer);
        ENTITY_TO_SOUL.put(soulPlayer.getUUID(), soulId);
        soulPlayer.getOwnerId().ifPresent(owner -> OWNER_ACTIVE_SOUL.putIfAbsent(owner, soulId));
        SoulLog.info("[soul] spawn-complete reason=spawnTestFakePlayer soul={} owner={} dim={} pos=({},{},{})",
                soulId,
                executor.getUUID(),
                soulPlayer.level().dimension().location(),
                soulPlayer.getX(),
                soulPlayer.getY(),
                soulPlayer.getZ());

        // After entity is fully tracked, broadcast equipment so clients render armor/held items
        net.tigereye.chestcavity.soul.util.SoulRenderSync.syncEquipmentForPlayer(soulPlayer);

        return Optional.of(new SpawnResult(soulPlayer));
    }

    /**
     * 保存指定灵魂假人的当前状态到其 Owner 的 SoulProfile 中。
     */
    public static void saveSoulPlayerState(UUID soulId) {
        SoulPlayer player = ACTIVE_SOUL_PLAYERS.get(soulId);
        if (player == null) {
            // Fallback: if an entity UUID was accidentally passed, remap via ENTITY_TO_SOUL
            UUID mapped = ENTITY_TO_SOUL.get(soulId);
            if (mapped != null) {
                player = ACTIVE_SOUL_PLAYERS.get(mapped);
                if (player != null) {
                    SoulLog.warn("[soul] saveSoulPlayerState received entityUuid={}, remapped to profileId={}", soulId, mapped);
                    soulId = mapped;
                }
            }
            if (player == null) {
                return;
            }
        }
        // 为什么：确保有稳定的 GameProfile 身份（皮肤/属性集），以便序列化/回放时一致。
        ensureIdentity(soulId, player.getGameProfile(), false);
        UUID owner = player.getOwnerId().orElse(null);
        if (owner == null) {
            return;
        }
        var server = player.serverLevel().getServer();
        ServerPlayer ownerPlayer = server.getPlayerList().getPlayer(owner);
        if (ownerPlayer != null) {
            // 为什么（在线）：直接回写到 Owner 的容器，并标脏确保持久化。
            var containerOpt = CCAttachments.getExistingSoulContainer(ownerPlayer);
            if (containerOpt.isPresent()) {
                var container = containerOpt.get();
                container.getOrCreateProfile(soulId).updateFrom(player);
                SoulProfileOps.markContainerDirty(ownerPlayer, container, "saveSoulPlayerState-online");
            } else {
                SoulLog.warn("[soul] saveSoulPlayerState: missing SoulContainer for owner {}", owner);
            }
        } else {
            // 为什么（离线）：将快照写入 Overworld 的 SavedData，待下次登录时合并回容器。
            var provider = player.serverLevel().registryAccess();
            var snapshot = net.tigereye.chestcavity.soul.profile.SoulProfile.fromSnapshot(
                    soulId,
                    net.tigereye.chestcavity.soul.profile.InventorySnapshot.capture(player),
                    net.tigereye.chestcavity.soul.profile.PlayerStatsSnapshot.capture(player),
                    net.tigereye.chestcavity.soul.profile.PlayerEffectsSnapshot.capture(player),
                    net.tigereye.chestcavity.soul.profile.PlayerPositionSnapshot.capture(player),
                    CapabilityPipeline.captureFor(player));
            SoulOfflineStore.get(server).put(owner, soulId, snapshot.save(provider));
        }
    }

    /**
     * Periodically snapshot non-active soul players to reduce data loss risk.
     * Excludes the soul the owner is currently possessing to avoid double work in the hot path.
     */
    public static void runBackgroundSnapshots(MinecraftServer server) {
        if (ACTIVE_SOUL_PLAYERS.isEmpty()) {
            return;
        }

        Map<UUID, List<SoulPlayer>> onlineByOwner = new java.util.HashMap<>();
        java.util.LinkedHashSet<UUID> offlineSouls = new java.util.LinkedHashSet<>();

        for (Map.Entry<UUID, SoulPlayer> entry : ACTIVE_SOUL_PLAYERS.entrySet()) {
            UUID soulId = entry.getKey();
            SoulPlayer soulPlayer = entry.getValue();
            UUID ownerId = soulPlayer.getOwnerId().orElse(null);
            if (ownerId == null) {
                continue;
            }
            if (isOwnerPossessingSoul(ownerId, soulId)) {
                continue; // skip the soul currently possessed by the owner
            }
            ServerPlayer ownerPlayer = server.getPlayerList().getPlayer(ownerId);
            if (ownerPlayer != null) {
                if (net.tigereye.chestcavity.soul.registry.BackgroundSnapshotFilters.get().shouldSnapshot(ownerId, soulId, soulPlayer)) {
                    onlineByOwner.computeIfAbsent(ownerId, key -> new ArrayList<>()).add(soulPlayer);
                }
            } else {
                offlineSouls.add(soulId);
            }
        }

        int savedSouls = 0;
        int ownersTouched = 0;

        for (Map.Entry<UUID, List<SoulPlayer>> entry : onlineByOwner.entrySet()) {
            UUID ownerId = entry.getKey();
            ServerPlayer ownerPlayer = server.getPlayerList().getPlayer(ownerId);
            if (ownerPlayer == null) {
                entry.getValue().forEach(soul -> offlineSouls.add(soul.getUUID()));
                continue;
            }
            SoulContainer container = CCAttachments.getSoulContainer(ownerPlayer);
            boolean touched = false;
            for (SoulPlayer soulPlayer : entry.getValue()) {
                // Use the profile UUID, not the in-world entity UUID, when writing snapshots.
                // Using entity UUID here would create phantom profiles per-shell.
                UUID soulId = soulPlayer.getSoulId();
                ensureIdentity(soulId, soulPlayer.getGameProfile(), false);
                container.getOrCreateProfile(soulId).updateFrom(soulPlayer);
                touched = true;
            }
            if (touched) {
                ownersTouched++;
                savedSouls += entry.getValue().size();
                SoulProfileOps.markContainerDirty(ownerPlayer, container, "background-save");
                SoulPersistence.saveDirty(ownerPlayer);
            }
        }

        if (!offlineSouls.isEmpty()) {
            for (UUID soulId : offlineSouls) {
                saveSoulPlayerState(soulId);
            }
            savedSouls += offlineSouls.size();
        }

        if (savedSouls > 0) {
            SoulLog.info("[soul] background-snapshot savedSouls={} ownersTouched={} offlineSouls={}",
                    savedSouls,
                    ownersTouched,
                    offlineSouls.size());
        }
    }

    public static Optional<SoulPlayer> findSoulPlayer(UUID uuid) {
        return Optional.ofNullable(ACTIVE_SOUL_PLAYERS.get(uuid));
    }

    /**
     * Force container active profile to owner without externalizing/spawning any entities.
     * Used for safe logout flows to avoid spawning during shutdown.
     */
    public static void forceOwner(ServerPlayer player) {
        SoulContainer container = CCAttachments.getSoulContainer(player);
        UUID owner = player.getUUID();
        UUID prev = container.getActiveProfileId().orElse(owner);
        // 为什么：在切回本体前，先把当前“附身”状态（可能是分魂）保存到容器，避免丢失最新改动。
        container.updateActiveProfile();
        // 为什么：直接把激活指针指向本体，不外化/不生成实体，确保登出流程安全无副作用。
        container.setActiveProfile(owner);
        // 为什么：标记附件变更，触发后续持久化，防止崩服/停服导致的状态丢失。
        SoulProfileOps.markContainerDirty(player, container, "force-owner");
        SoulLog.info("[soul] force-owner applied owner={} prevActive={} nowActive={}", owner, prev, container.getActiveProfileId().orElse(owner));
    }

    /**
     * On logout, flush all in-world souls that belong to the owner into the owner's container,
     * mark the container dirty, and queue offline snapshots as a double safety net.
     */
    public static void refreshProfileSnapshot(ServerPlayer ownerPlayer, UUID soulId, SoulProfile profile) {
        UUID owner = ownerPlayer.getUUID();
        if (soulId.equals(owner)) {
            // Owner profile refresh: only from owner self when not possessing; otherwise from owner shell if present.
            if (!isOwnerPossessing(owner)) {
                if (net.tigereye.chestcavity.soul.registry.SoulWritePolicyRegistry.get().allowOwnerSelfWrite(owner)) {
                    profile.updateFrom(ownerPlayer);
                    SoulLog.info("[soul] refreshSnapshot owner=SELF ownerId={}", owner);
                } else {
                    SoulLog.info("[soul] refreshSnapshot owner=SKIP ownerId={} reason=policy-deny-self", owner);
                }
            } else {
                Optional<SoulPlayer> shell = getOwnerShell(owner);
                if (shell.isPresent()) {
                    if (net.tigereye.chestcavity.soul.registry.SoulWritePolicyRegistry.get().allowOwnerShellWrite(owner)) {
                        profile.updateFrom(shell.get());
                        SoulLog.info("[soul] refreshSnapshot owner=SHELL ownerId={}", owner);
                    } else {
                        SoulLog.info("[soul] refreshSnapshot owner=SKIP ownerId={} reason=policy-deny-shell", owner);
                    }
                } else {
                    SoulLog.info("[soul] refreshSnapshot owner=SKIP ownerId={} reason=noShellWhilePossessing", owner);
                }
            }
            return;
        }
        SoulPlayer soulPlayer = ACTIVE_SOUL_PLAYERS.get(soulId);
        if (soulPlayer != null && soulPlayer.getOwnerId().map(owner::equals).orElse(false)) {
            if (net.tigereye.chestcavity.soul.registry.SoulWritePolicyRegistry.get().allowSoulWrite(owner, soulId)) {
                profile.updateFrom(soulPlayer);
            } else {
                SoulLog.info("[soul] refreshSnapshot soul=SKIP owner={} soulId={} reason=policy-deny-soul", owner, soulId);
                return;
            }
            SoulLog.info("[soul] refreshSnapshot soul=OK owner={} soulId={}", owner, soulId);
        } else {
            SoulLog.info("[soul] refreshSnapshot soul=SKIP owner={} soulId={} reason=noActiveSoulOrWrongOwner", owner, soulId);
        }
    }

    public static Set<UUID> getOwnedSoulIds(UUID ownerId) {
        return ACTIVE_SOUL_PLAYERS.entrySet().stream()
                .filter(e -> e.getValue().getOwnerId().map(ownerId::equals).orElse(false))
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * 列出当前存活的灵魂假人基本信息，供调试/指令展示。
     */
    public static List<SoulPlayerInfo> listActive() {
        return ACTIVE_SOUL_PLAYERS.values().stream()
                .filter(sp -> {
                    UUID owner = sp.getOwnerId().orElse(null);
                    if (owner == null) return true;
                    UUID profileId = ENTITY_TO_SOUL.get(sp.getUUID());
                    return profileId == null || !profileId.equals(owner);
                })
                .map(sp -> new SoulPlayerInfo(
                        sp.getUUID(),
                        sp.getOwnerId().orElse(null),
                        sp.getOwnerId().map(owner -> isOwnerPossessingSoul(owner, sp.getUUID())).orElse(false)))
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

        // Resolve target
        UUID targetId = requestedId.equals(ownerUuid) ? ownerUuid : resolveSoulUuid(requestedId).orElse(null);
        if (targetId == null) {
            return false;
        }

        UUID currentId = container.getActiveProfileId().orElse(ownerUuid);
        // Pre-switch hooks
        net.tigereye.chestcavity.soul.registry.SoulSwitchHooks.preSwitch(executor, currentId, targetId);
        if (currentId.equals(targetId)) {
            return true; // no-op
        }

        // 1) Save current possession into its profile
        SoulSwitchGuard.begin(executor, "switchTo");
        try {
        if (currentId.equals(ownerUuid)) {
            container.updateActiveProfile();
        } else {
            container.getOrCreateProfile(currentId).updateFrom(executor);
        }

        // 2) Capture player's current coordinates/rotations to leave a shell AFTER possession
        final ServerLevel prevLevel = executor.serverLevel();
        final double prevX = executor.getX();
        final double prevY = executor.getY();
        final double prevZ = executor.getZ();
        final float prevYaw = executor.getYRot();
        final float prevPitch = executor.getXRot();
        final float prevHead = executor.getYHeadRot();

        // 3) Prepare target:
        // - If target is owner, do NOT spawn a temporary owner shell. Optionally teleport to owner's snapshot.
        // - If target is a soul, ensure shell exists, teleport to it, then consume it.
        SoulProfile targetProfile = container.getOrCreateProfile(targetId);
        if (targetId.equals(ownerUuid)) {
            // Optional: move executor to owner's last snapshot position for consistency
            targetProfile.position().ifPresent(snapshot -> {
                ServerLevel tl = executor.server.getLevel(snapshot.dimension());
                if (tl != null) {
                    double minY = tl.getMinBuildHeight() + 1;
                    double maxY = tl.getMaxBuildHeight() - 2;
                    double safeY = Math.max(minY, Math.min(maxY, snapshot.y()));
                    executor.teleportTo(tl, snapshot.x(), safeY, snapshot.z(), snapshot.yaw(), snapshot.pitch());
                    executor.setYHeadRot(snapshot.headYaw());
                    SoulLog.info("[soul] switch-teleport-to-owner-snapshot owner={} dim={} pos=({},{},{})",
                            ownerUuid, tl.dimension().location(), snapshot.x(), safeY, snapshot.z());
                }
            });
        } else {
            SoulPlayer targetShell = ACTIVE_SOUL_PLAYERS.get(targetId);
            if (targetShell == null) {
                var identity = SOUL_IDENTITIES.getOrDefault(targetId, executor.getGameProfile());
                boolean forceDerived = !SOUL_IDENTITIES.containsKey(targetId);
                targetShell = respawnSoulFromProfile(executor, targetId, identity, forceDerived, "switchTo:prepareTarget").orElse(null);
            }
            if (targetShell != null) {
                // Teleport player to target shell position for seamless swap
                ServerLevel tl = targetShell.serverLevel();
                double minY = tl.getMinBuildHeight() + 1;
                double maxY = tl.getMaxBuildHeight() - 2;
                double safeY = Math.max(minY, Math.min(maxY, targetShell.getY()));
                executor.teleportTo(tl, targetShell.getX(), safeY, targetShell.getZ(), targetShell.getYRot(), targetShell.getXRot());
                executor.setYHeadRot(targetShell.getYHeadRot());
                SoulLog.info("[soul] switch-teleport-to-target owner={} target={} dim={} pos=({},{},{})",
                        ownerUuid, targetId, tl.dimension().location(), targetShell.getX(), safeY, targetShell.getZ());
            }
        }

        // 应用目标档案（仅基础数据：物品/属性/效果+能力），不改位置
        applyProfileBaseOnly(targetProfile, executor, "switchTo:applyBaseOnly");

        // 若目标是分魂，则消费其外壳；目标为本体则仅在存在外壳时清理
        if (!targetId.equals(ownerUuid)) {
            handleRemoval(targetId, "switchTo:consumeTarget");
        } else if (ACTIVE_SOUL_PLAYERS.containsKey(ownerUuid)) {
            handleRemoval(ownerUuid, "switchTo:consumeOwnerIfPresent");
        }
        container.setActiveProfile(targetId);
        if (targetId.equals(ownerUuid)) {
            OWNER_ACTIVE_SOUL.remove(ownerUuid);
        } else {
            OWNER_ACTIVE_SOUL.put(ownerUuid, targetId);
        }

        // 4) Externalize the previous possessor NOW at the saved coordinates
        if (currentId.equals(ownerUuid)) {
            respawnSoulFromProfile(
                    executor,
                    ownerUuid,
                    executor.getGameProfile(),
                    true,
                    "switchTo:externalizeOwner"
            ).ifPresent(shell -> {
                if (shell.level() != prevLevel) {
                    double minY = prevLevel.getMinBuildHeight() + 1;
                    double maxY = prevLevel.getMaxBuildHeight() - 2;
                    double safeY = Math.max(minY, Math.min(maxY, prevY));
                    shell.teleportTo(prevLevel, prevX, safeY, prevZ, prevYaw, prevPitch);
                } else {
                    double minY = prevLevel.getMinBuildHeight() + 1;
                    double maxY = prevLevel.getMaxBuildHeight() - 2;
                    double safeY = Math.max(minY, Math.min(maxY, prevY));
                    shell.moveTo(prevX, safeY, prevZ, prevYaw, prevPitch);
                }
                shell.setYHeadRot(prevHead);
                SoulLog.info("[soul] externalize-override-position reason=switchTo:externalizeOwner soul={} dim={} pos=({},{},{})",
                        ownerUuid,
                        shell.level().dimension().location(),
                        shell.getX(), shell.getY(), shell.getZ());
            });
        } else {
            var identityPrev = SOUL_IDENTITIES.getOrDefault(currentId, executor.getGameProfile());
            boolean forceDerivedPrev = !SOUL_IDENTITIES.containsKey(currentId);
            respawnSoulFromProfile(
                    executor,
                    currentId,
                    identityPrev,
                    forceDerivedPrev,
                    "switchTo:externalizeActiveSoul"
            ).ifPresent(shell -> {
                if (shell.level() != prevLevel) {
                    double minY = prevLevel.getMinBuildHeight() + 1;
                    double maxY = prevLevel.getMaxBuildHeight() - 2;
                    double safeY = Math.max(minY, Math.min(maxY, prevY));
                    shell.teleportTo(prevLevel, prevX, safeY, prevZ, prevYaw, prevPitch);
                } else {
                    double minY = prevLevel.getMinBuildHeight() + 1;
                    double maxY = prevLevel.getMaxBuildHeight() - 2;
                    double safeY = Math.max(minY, Math.min(maxY, prevY));
                    shell.moveTo(prevX, safeY, prevZ, prevYaw, prevPitch);
                }
                shell.setYHeadRot(prevHead);
                SoulLog.info("[soul] externalize-override-position reason=switchTo:externalizeActiveSoul soul={} dim={} pos=({},{},{})",
                        currentId,
                        shell.level().dimension().location(),
                        shell.getX(), shell.getY(), shell.getZ());
            });
        }

        // Post-switch hooks
        net.tigereye.chestcavity.soul.registry.SoulSwitchHooks.postSwitch(executor, currentId, targetId, true);
        return true;
        } finally {
            SoulSwitchGuard.end(executor, "switchTo");
        }
    }

    // 仅应用基础数据（背包/属性/效果），不改变位置，用于“先TP至目标壳，再继承数据”的切换流程
    private static void applyProfileBaseOnly(SoulProfile profile, ServerPlayer player, String reason) {
        profile.restoreBase(player);
        net.tigereye.chestcavity.soul.util.SoulRenderSync.syncEquipmentForPlayer(player);
        SoulLog.info("[soul] apply-base-only reason={} soul={}", reason, profile.id());
    }

    /**
     * 解析输入 ID：若为灵魂 UUID 直接返回；若为可视化实体 UUID，转换为对应的灵魂 UUID。
     */
    public static Optional<UUID> resolveSoulUuid(UUID id) {
        if (ACTIVE_SOUL_PLAYERS.containsKey(id)) {
            return Optional.of(id);
        }
        UUID mapped = ENTITY_TO_SOUL.get(id);
        if (mapped != null) {
            return Optional.of(mapped);
        }
        return Optional.empty();
    }

    /**
     * Brigadier 补全：为指令提供灵魂/可视化实体 UUID 建议。
     */
    public static CompletableFuture<Suggestions> suggestSoulPlayerUuids(CommandSourceStack source, SuggestionsBuilder builder) {
        if (source.getEntity() instanceof ServerPlayer player) {
            UUID owner = player.getUUID();
            // Suggest profile UUIDs, entity UUIDs, and names (from attachment or identities)
            var container = CCAttachments.getSoulContainer(player);
            java.util.Set<UUID> ownedSouls = new java.util.HashSet<>(container.getKnownSoulIds());
            // Include any currently active shells (even if not in container yet)
            ACTIVE_SOUL_PLAYERS.values().stream()
                    .filter(sp -> sp.getOwnerId().map(owner::equals).orElse(false))
                    .forEach(sp -> ownedSouls.add(ENTITY_TO_SOUL.getOrDefault(sp.getUUID(), sp.getSoulId())));
            for (UUID sid : ownedSouls) {
                builder.suggest("profile-" + sid);
                builder.suggest(sid.toString()); // legacy plain UUID
                // active entity id (prefixed + plain)
                SoulPlayer sp = ACTIVE_SOUL_PLAYERS.get(sid);
                if (sp != null) {
                    builder.suggest("entity-" + sp.getUUID());
                    builder.suggest(sp.getUUID().toString());
                }
                // name suggestions
                String n = container.getName(sid);
                if (n != null && !n.isEmpty()) builder.suggest(n);
                GameProfile id = SOUL_IDENTITIES.get(sid);
                if (id != null && id.getName() != null && !id.getName().isEmpty()) builder.suggest(id.getName());
            }
            builder.suggest("owner");
            return builder.buildFuture();
        }
        ACTIVE_SOUL_PLAYERS.keySet().forEach(uuid -> builder.suggest(uuid.toString()));
        return builder.buildFuture();
    }

    /** Resolve by UUID (profile/entity) or by name (owner-scoped). */
    public static Optional<UUID> resolveSoulUuidFlexible(ServerPlayer owner, String token) {
        token = unquote(token);
        if (token == null || token.isBlank()) return Optional.empty();
        if ("owner".equalsIgnoreCase(token)) return Optional.of(owner.getUUID());
        // prefixed forms
        if (token.startsWith("profile-") || token.startsWith("data-")) {
            String raw = token.substring(token.indexOf('-') + 1);
            try { return Optional.of(UUID.fromString(raw)); } catch (IllegalArgumentException ignored) {}
        }
        if (token.startsWith("entity-")) {
            String raw = token.substring("entity-".length());
            try {
                UUID eid = UUID.fromString(raw);
                UUID pid = ENTITY_TO_SOUL.get(eid);
                if (pid != null) return Optional.of(pid);
            } catch (IllegalArgumentException ignored) {}
        }
        // try UUID directly (profile)
        try {
            UUID id = UUID.fromString(token);
            Optional<UUID> mapped = resolveSoulUuid(id);
            if (mapped.isPresent()) return mapped;
            // If this UUID is actually a soul profile present in container, accept
            SoulContainer c = CCAttachments.getSoulContainer(owner);
            if (c.hasProfile(id)) return Optional.of(id);
        } catch (IllegalArgumentException ignored) {}
        // By name: check container first, then identities
        SoulContainer c = CCAttachments.getSoulContainer(owner);
        for (var e : c.getAllNames().entrySet()) {
            if (token.equals(e.getValue())) return Optional.of(e.getKey());
        }
        for (var e : SOUL_IDENTITIES.entrySet()) {
            UUID soulId = e.getKey();
            SoulPlayer sp = ACTIVE_SOUL_PLAYERS.get(soulId);
            if (sp != null && sp.getOwnerId().map(owner.getUUID()::equals).orElse(false)) {
                if (token.equals(e.getValue().getName())) return Optional.of(soulId);
            }
        }
        return Optional.empty();
    }

    private static String unquote(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            return t.substring(1, t.length() - 1);
        }
        return t;
    }

    /** Rename and optionally apply immediately by respawning the shell. */
    public static boolean rename(ServerPlayer owner, UUID soulId, String newName, boolean applyNow) {
        SoulContainer c = CCAttachments.getSoulContainer(owner);
        c.setName(owner, soulId, newName, "name-rename");
        updateIdentityName(soulId, c.getName(soulId));
        if (applyNow && ACTIVE_SOUL_PLAYERS.containsKey(soulId)) {
            // Save, remove and respawn with new identity name
            saveSoulPlayerState(soulId);
            handleRemoval(soulId, "rename-apply");
            return respawnForOwner(owner, soulId).isPresent();
        }
        return true;
    }

    /**
     * Sets the soul's skin from an official Mojang player name by fetching its textures property
     * via the server's session service. Keeps the current identity UUID and display name.
     */
    public static boolean setSkinFromMojangName(ServerPlayer owner, UUID soulId, String mojangName, boolean applyNow) {
        if (mojangName == null || mojangName.isBlank()) return false;
        var server = owner.serverLevel().getServer();
        // Fetch textures property from Mojang session server
        java.util.Optional<com.mojang.authlib.properties.Property> texOpt = fetchMojangTexturesByName(mojangName);
        if (texOpt.isEmpty()) {
            SoulLog.warn("[soul] setSkinFromMojangName: no textures for name={} (rate limit/offline?)", mojangName);
            return false;
        }
        // Update identity: keep existing UUID and display name
        com.mojang.authlib.GameProfile existing = SOUL_IDENTITIES.get(soulId);
        if (existing == null) {
            // Create from owner's profile to keep base identity
            existing = ensureIdentity(soulId, owner.getGameProfile(), false);
        }
        com.mojang.authlib.GameProfile updated = new com.mojang.authlib.GameProfile(existing.getId(), existing.getName());
        // Replace with textures only
        updated.getProperties().clear();
        updated.getProperties().put("textures", texOpt.get());
        SOUL_IDENTITIES.put(soulId, updated);
        SoulLog.info("[soul] skin-set soul={} name={} (applyNow={})", soulId, mojangName, applyNow);
        if (applyNow && ACTIVE_SOUL_PLAYERS.containsKey(soulId)) {
            saveSoulPlayerState(soulId);
            handleRemoval(soulId, "skin-apply");
            return respawnForOwner(owner, soulId).isPresent();
        }
        return true;
    }

    private static java.util.Optional<com.mojang.authlib.properties.Property> fetchMojangTexturesByName(String mojangName) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(3))
                    .build();
            // 1) name -> uuid (no dashes)
            java.net.URI nameUri = java.net.URI.create("https://api.mojang.com/users/profiles/minecraft/" + java.net.URLEncoder.encode(mojangName, java.nio.charset.StandardCharsets.UTF_8));
            java.net.http.HttpRequest req1 = java.net.http.HttpRequest.newBuilder(nameUri).GET()
                    .timeout(java.time.Duration.ofSeconds(3)).build();
            java.net.http.HttpResponse<String> resp1 = client.send(req1, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp1.statusCode() != 200) return java.util.Optional.empty();
            String body1 = resp1.body();
            String id = extractJsonString(body1, "id");
            if (id == null || id.length() != 32) return java.util.Optional.empty();
            String dashed = id.substring(0,8)+"-"+id.substring(8,12)+"-"+id.substring(12,16)+"-"+id.substring(16,20)+"-"+id.substring(20);
            // 2) uuid -> profile with properties
            java.net.URI profUri = java.net.URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + dashed + "?unsigned=false");
            java.net.http.HttpRequest req2 = java.net.http.HttpRequest.newBuilder(profUri).GET()
                    .timeout(java.time.Duration.ofSeconds(3)).build();
            java.net.http.HttpResponse<String> resp2 = client.send(req2, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp2.statusCode() != 200) return java.util.Optional.empty();
            String body2 = resp2.body();
            // Find textures property value and signature (if present)
            String value = extractJsonStringInArrayObject(body2, "properties", "name", "textures", "value");
            String sig = extractJsonStringInArrayObject(body2, "properties", "name", "textures", "signature");
            if (value == null) return java.util.Optional.empty();
            com.mojang.authlib.properties.Property p = sig != null ?
                    new com.mojang.authlib.properties.Property("textures", value, sig) :
                    new com.mojang.authlib.properties.Property("textures", value);
            return java.util.Optional.of(p);
        } catch (Exception e) {
            SoulLog.warn("[soul] fetchMojangTexturesByName failed name={} cause={}", mojangName, e.toString());
            return java.util.Optional.empty();
        }
    }

    private static String extractJsonString(String json, String key) {
        // naive parser for simple objects: "key":"value"
        String pat = "\"" + key + "\"\s*:\s*\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pat).matcher(json);
        if (m.find()) {
            int start = m.end();
            int end = json.indexOf('"', start);
            if (end > start) return json.substring(start, end);
        }
        return null;
    }

    private static String extractJsonStringInArrayObject(String json, String arrayKey, String matchKey, String matchValue, String wantKey) {
        // very naive: find arrayKey, then locate object where "matchKey":"matchValue", then read wantKey value
        int arrIdx = json.indexOf("\""+arrayKey+"\"");
        if (arrIdx < 0) return null;
        int objIdx = json.indexOf("\""+matchKey+"\"\s*:\s*\""+matchValue+"\"", arrIdx);
        if (objIdx < 0) return null;
        int wantIdx = json.indexOf("\""+wantKey+"\"\s*:\s*\"", objIdx);
        if (wantIdx < 0) return null;
        int start = json.indexOf('"', wantIdx + ("\""+wantKey+"\"\s*:\s*\"").length());
        if (start < 0) return null;
        int end = json.indexOf('"', start + 1);
        if (end < 0) return null;
        return json.substring(start + 1, end);
    }

    public static boolean remove(UUID id, ServerPlayer executor) {
        Optional<UUID> resolved = resolveSoulUuid(id);
        if (resolved.isEmpty()) {
            return false;
        }
        UUID soulId = resolved.get();
        return despawn(executor, soulId);
    }

    public static boolean despawn(ServerPlayer owner, UUID soulId) {
        UUID ownerId = owner.getUUID();

        SoulPlayer soul = ACTIVE_SOUL_PLAYERS.remove(soulId);
        if (soul == null || soul.getOwnerId().map(ownerId::equals).orElse(false) == false) {
            SoulLog.warn("[soul] despawn requested for non-active soul={} owner={}", soulId, ownerId);
            return false;
        }

        ENTITY_TO_SOUL.remove(soul.getUUID());
        clearActivePossession(ownerId, soulId);

        SoulContainer container = CCAttachments.getSoulContainer(owner);
        SoulProfile profile = container.getOrCreateProfile(soulId);
        profile.updateFrom(soul);
        SoulProfileOps.markContainerDirty(owner, container, "despawn");
        SoulOfflineStore.get(owner.serverLevel().getServer())
                .put(ownerId, soulId, profile.save(owner.registryAccess()));
        profile.clearDirty();

        handleRemoval(soulId, "despawn");
        SoulLog.info("[soul] despawned soul={} owner={}", soulId, ownerId);
        return true;
    }

    public static void removeByOwner(UUID ownerId) {
        List<UUID> profileIds = ACTIVE_SOUL_PLAYERS.entrySet().stream()
                .filter(e -> e.getValue().getOwnerId().map(ownerId::equals).orElse(false))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        for (UUID profileId : profileIds) {
            SoulPlayer tracked = ACTIVE_SOUL_PLAYERS.get(profileId);
            if (tracked == null) {
                continue;
            }
            ServerPlayer ownerPlayer = tracked.serverLevel().getServer().getPlayerList().getPlayer(ownerId);
            if (ownerPlayer != null) {
                despawn(ownerPlayer, profileId);
            } else {
                var provider = tracked.serverLevel().registryAccess();
                SoulOfflineStore.get(tracked.serverLevel().getServer())
                        .put(ownerId, profileId, SoulProfile.capture(tracked, profileId).save(provider));
                handleRemoval(profileId, "removeByOwner");
            }
            SOUL_IDENTITIES.remove(profileId);
        }
    }

    public static void clearAll() {
        List<UUID> ids = new ArrayList<>(ACTIVE_SOUL_PLAYERS.keySet());
        ids.forEach(soulId -> {
            SoulPlayer soul = ACTIVE_SOUL_PLAYERS.remove(soulId);
            if (soul != null) {
                ENTITY_TO_SOUL.remove(soul.getUUID());
                soul.remove(Entity.RemovalReason.DISCARDED);
            }
        });
        OWNER_ACTIVE_SOUL.clear();
        SOUL_IDENTITIES.clear();
        ENTITY_TO_SOUL.clear();
    }

    public static int saveAll(ServerPlayer executor) {
        var server = executor.serverLevel().getServer();
        // 1) Flush all active SoulPlayers to owners' containers and mark attachments dirty
        int soulsSaved = 0;
        for (UUID soulId : ACTIVE_SOUL_PLAYERS.keySet()) {
            saveSoulPlayerState(soulId);
            soulsSaved++;
        }
        // 2) Update active profiles for all online players and mark attachments dirty
        int playersTouched = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            CCAttachments.getExistingSoulContainer(player).ifPresent(container -> {
                container.updateActiveProfile();
                player.setData(CCAttachments.SOUL_CONTAINER.get(), container);
            });
            playersTouched++;
        }
        // 3) Persist players and worlds
        server.getPlayerList().saveAll();
        server.getAllLevels().forEach(level -> level.save(null, false, false));
        SoulLog.info("[soul] saveAll: soulsSaved={} playersTouched={}", soulsSaved, playersTouched);
        return soulsSaved;
    }

    static void onSoulPlayerRemoved(SoulPlayer soulPlayer, String cause) {
        UUID entityUuid = soulPlayer.getUUID();
        if (!REMOVING_ENTITIES.add(entityUuid)) {
            return; // already processing removal for this entity
        }
        try {
            UUID profileId = soulPlayer.getSoulId();
            // stop navigation mirror for this soul
            net.tigereye.chestcavity.soul.navigation.SoulNavigationMirror.onSoulRemoved(profileId);
            if (!ACTIVE_SOUL_PLAYERS.containsKey(profileId)) {
                UUID mapped = ENTITY_TO_SOUL.get(entityUuid);
                if (mapped != null) {
                    SoulLog.warn("[soul] onSoulPlayerRemoved received entityUuid={}, remapped to profileId={}", entityUuid, mapped);
                    profileId = mapped;
                }
            }
            // Death semantics: honor keepInventory gamerule
            boolean isDeath = "die".equalsIgnoreCase(cause);
            if (isDeath) {
                boolean keepInv = soulPlayer.serverLevel().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY);
                UUID ownerId = soulPlayer.getOwnerId().orElse(null);
                var server = soulPlayer.serverLevel().getServer();
                ServerPlayer owner = ownerId != null ? server.getPlayerList().getPlayer(ownerId) : null;
                if (keepInv && owner != null) {
                    // Update snapshot from the dying shell so inventory/position are preserved, then remove and immediately respawn a new shell
                    var container = CCAttachments.getSoulContainer(owner);
                    container.getOrCreateProfile(profileId).updateFrom(soulPlayer);
                    // Remove old shell mappings and entity
                    handleRemoval(profileId, "onSoulPlayerRemoved:die-keepInventory");
                    // Respawn at snapshot position
                    final UUID fOwnerId = ownerId;
                    final UUID fProfileId = profileId;
                    respawnForOwner(owner, profileId).ifPresentOrElse(
                            spawned -> {
                                // Ensure fresh shell starts alive
                                spawned.setHealth(spawned.getMaxHealth());
                                SoulLog.info("[soul] auto-respawn reason=death-keepInventory owner={} soul={} dim={} pos=({},{},{})",
                                        fOwnerId, fProfileId, spawned.level().dimension().location(), spawned.getX(), spawned.getY(), spawned.getZ());
                            },
                            () -> SoulLog.warn("[soul] auto-respawn failed reason=death-keepInventory owner={} soul={}", fOwnerId, fProfileId)
                    );
                    return; // skip purge branch
                }
            }
            // Default: remove shell entity + mappings
            handleRemoval(profileId, cause != null ? "onSoulPlayerRemoved:" + cause : "onSoulPlayerRemoved");
            // On death without keepInventory: purge profile data and identity entries
            if (isDeath) {
                UUID ownerId = soulPlayer.getOwnerId().orElse(null);
                if (ownerId != null) {
                    var server = soulPlayer.serverLevel().getServer();
                    ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
                    if (owner != null) {
                        var container = CCAttachments.getSoulContainer(owner);
                        container.removeProfile(owner, profileId, "remove-on-death");
                        // purge identity caches
                        SOUL_IDENTITIES.remove(profileId);
                        SoulLog.info("[soul] purge-profile reason=death owner={} soul={}", ownerId, profileId);
                    } else {
                        SoulLog.warn("[soul] purge-profile skipped owner offline owner={} soul={}", ownerId, profileId);
                    }
                }
            }
        } finally {
            REMOVING_ENTITIES.remove(entityUuid);
        }
    }

    private static void handleRemoval(UUID soulUuid, String reason) {
        SoulPlayer removed = ACTIVE_SOUL_PLAYERS.remove(soulUuid);
        if (removed == null) {
            // Fallback: caller might pass entity UUID; try remap
            UUID mapped = ENTITY_TO_SOUL.remove(soulUuid);
            if (mapped != null) {
                SoulLog.warn("[soul] handleRemoval received entityUuid={}, remapped to profileId={} reason={} ", soulUuid, mapped, reason);
                soulUuid = mapped;
                removed = ACTIVE_SOUL_PLAYERS.remove(soulUuid);
            }
        }
        if (removed != null) {
            // clear per-soul AI orders and navigation mirror
            net.tigereye.chestcavity.soul.ai.SoulAIOrders.clear(soulUuid);
            net.tigereye.chestcavity.soul.navigation.SoulNavigationMirror.onSoulRemoved(soulUuid);
            // Ensure the in-world entity is properly removed to avoid stray visual models.
            if (!removed.isRemoved()) {
                removed.remove(Entity.RemovalReason.DISCARDED);
            }
            ENTITY_TO_SOUL.remove(removed.getUUID());
            LEGIT_ENTITY_SPAWNS.remove(removed.getUUID());
            ENTITY_SPAWN_REASON.remove(removed.getUUID());
            removed.serverLevel().getServer().getPlayerList()
                    .broadcastAll(new ClientboundPlayerInfoRemovePacket(List.of(removed.getUUID())));
            UUID owner = removed.getOwnerId().orElse(null);
            if (owner != null) {
                clearActivePossession(owner, soulUuid);
            }
            SoulLog.info("[soul] despawn reason={} soul={} owner={} dim={} pos=({},{},{})",
                    reason,
                    soulUuid,
                    removed.getOwnerId().orElse(null),
                    removed.level().dimension().location(),
                    removed.getX(),
                    removed.getY(),
                    removed.getZ());
        } else {
            SoulLog.warn("[soul] handleRemoval could not find tracked soul for id={} reason={}", soulUuid, reason);
        }
    }

    public record SpawnResult(SoulPlayer soulPlayer) {}

    public record SoulPlayerInfo(UUID soulUuid,
                                 @Nullable UUID ownerId,
                                 boolean active) {}
}
