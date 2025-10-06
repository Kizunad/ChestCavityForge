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

    private static boolean isOwner(UUID soulId, UUID ownerId) {
        SoulPlayer soul = ACTIVE_SOUL_PLAYERS.get(soulId);
        return soul != null && soul.getOwnerId().map(ownerId::equals).orElse(false);
    }

    public static Optional<SoulPlayer> respawnSoulFromProfile(ServerPlayer owner,
                                                            UUID profileId,
                                                            GameProfile sourceProfile,
                                                            boolean forceDerivedId,
                                                            String reason) {
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

        // 4️⃣ 恢复数据
        profile.restoreBase(soulPlayer);

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
                    net.tigereye.chestcavity.soul.profile.PlayerPositionSnapshot.capture(player));
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
                onlineByOwner.computeIfAbsent(ownerId, key -> new ArrayList<>()).add(soulPlayer);
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
                UUID soulId = soulPlayer.getUUID();
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
                profile.updateFrom(ownerPlayer);
                SoulLog.info("[soul] refreshSnapshot owner=SELF ownerId={}", owner);
            } else {
                Optional<SoulPlayer> shell = getOwnerShell(owner);
                if (shell.isPresent()) {
                    profile.updateFrom(shell.get());
                    SoulLog.info("[soul] refreshSnapshot owner=SHELL ownerId={}", owner);
                } else {
                    SoulLog.info("[soul] refreshSnapshot owner=SKIP ownerId={} reason=noShellWhilePossessing", owner);
                }
            }
            return;
        }
        SoulPlayer soulPlayer = ACTIVE_SOUL_PLAYERS.get(soulId);
        if (soulPlayer != null && soulPlayer.getOwnerId().map(owner::equals).orElse(false)) {
            profile.updateFrom(soulPlayer);
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
        if (currentId.equals(targetId)) {
            return true; // no-op
        }

        // 1) Save current possession into its profile
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

        // 3) Ensure target shell exists; teleport player to target shell; apply base-only; remove target shell
        SoulPlayer targetShell = ACTIVE_SOUL_PLAYERS.get(targetId);
        boolean spawnedTempTarget = false;
        if (targetShell == null) {
            var identity = SOUL_IDENTITIES.getOrDefault(targetId, executor.getGameProfile());
            boolean forceDerived = !SOUL_IDENTITIES.containsKey(targetId);
            targetShell = respawnSoulFromProfile(executor, targetId, identity, forceDerived, "switchTo:prepareTarget").orElse(null);
            spawnedTempTarget = targetShell != null;
        }

        if (targetShell != null) {
            // 先把玩家传送到目标壳所在位置（始终使用 teleportTo，确保客户端同步）
            ServerLevel tl = targetShell.serverLevel();
            executor.teleportTo(tl, targetShell.getX(), targetShell.getY(), targetShell.getZ(), targetShell.getYRot(), targetShell.getXRot());
            executor.setYHeadRot(targetShell.getYHeadRot());
            SoulLog.info("[soul] switch-teleport-to-target owner={} target={} dim={} pos=({},{},{})",
                    ownerUuid, targetId, tl.dimension().location(), targetShell.getX(), targetShell.getY(), targetShell.getZ());
        }

        // 应用目标档案（仅基础数据：物品/属性/效果），不改位置
        SoulProfile targetProfile = container.getOrCreateProfile(targetId);
        applyProfileBaseOnly(targetProfile, executor, "switchTo:applyBaseOnly");

        // 移除（消费）目标分魂壳并设为激活
        saveSoulPlayerState(targetId);
        handleRemoval(targetId, "switchTo:consumeTarget");
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
                    shell.teleportTo(prevLevel, prevX, prevY, prevZ, prevYaw, prevPitch);
                } else {
                    shell.moveTo(prevX, prevY, prevZ, prevYaw, prevPitch);
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
                    shell.teleportTo(prevLevel, prevX, prevY, prevZ, prevYaw, prevPitch);
                } else {
                    shell.moveTo(prevX, prevY, prevZ, prevYaw, prevPitch);
                }
                shell.setYHeadRot(prevHead);
                SoulLog.info("[soul] externalize-override-position reason=switchTo:externalizeActiveSoul soul={} dim={} pos=({},{},{})",
                        currentId,
                        shell.level().dimension().location(),
                        shell.getX(), shell.getY(), shell.getZ());
            });
        }

        return true;
    }

    // 仅应用基础数据（背包/属性/效果），不改变位置，用于“先TP至目标壳，再继承数据”的切换流程
    private static void applyProfileBaseOnly(SoulProfile profile, ServerPlayer player, String reason) {
        profile.restoreBase(player);
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
            ACTIVE_SOUL_PLAYERS.values().stream()
                    .filter(sp -> sp.getOwnerId().map(owner::equals).orElse(false))
                    .filter(sp -> {
                        UUID profileId = ENTITY_TO_SOUL.get(sp.getUUID());
                        return profileId == null || !profileId.equals(owner);
                    })
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

    static void onSoulPlayerRemoved(SoulPlayer soulPlayer) {
        UUID entityUuid = soulPlayer.getUUID();
        UUID profileId = soulPlayer.getSoulId();
        if (!ACTIVE_SOUL_PLAYERS.containsKey(profileId)) {
            UUID mapped = ENTITY_TO_SOUL.get(entityUuid);
            if (mapped != null) {
                SoulLog.warn("[soul] onSoulPlayerRemoved received entityUuid={}, remapped to profileId={}", entityUuid, mapped);
                profileId = mapped;
            }
        }
        saveSoulPlayerState(profileId);
        handleRemoval(profileId, "onSoulPlayerRemoved");
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
            // Ensure the in-world entity is properly removed to avoid stray visual models.
            if (!removed.isRemoved()) {
                removed.remove(Entity.RemovalReason.DISCARDED);
            }
            ENTITY_TO_SOUL.remove(removed.getUUID());
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
