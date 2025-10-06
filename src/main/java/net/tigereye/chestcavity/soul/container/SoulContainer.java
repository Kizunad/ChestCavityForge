package net.tigereye.chestcavity.soul.container;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner;
import net.tigereye.chestcavity.soul.profile.InventorySnapshot;
import net.tigereye.chestcavity.soul.profile.PlayerPositionSnapshot;
import net.tigereye.chestcavity.soul.profile.PlayerStatsSnapshot;
import net.tigereye.chestcavity.soul.profile.SoulProfile;
import net.tigereye.chestcavity.soul.util.SoulLog;
import net.tigereye.chestcavity.soul.util.SoulProfileOps;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 灵魂容器（玩家附件/能力的后备存储）
 *
 * 用途
 * - 维护玩家身上的多个“灵魂存档槽”（{@link net.tigereye.chestcavity.soul.profile.SoulProfile}）。
 * - 提供激活存档的选择/更新，以及所有存档的 NBT 持久化。
 *
 * 设计说明
 * - 每个存档用 UUID 唯一标识；activeProfileId 指向当前激活的存档。
 * - 仅在服务端（ServerPlayer）执行快照捕获与回写，客户端路径做最小可用的回退以避免 NPE。
 * - 本类不包含游戏性逻辑，专注数据维护与序列化。
 */
public final class SoulContainer {

    public static final String CAPABILITY_ID = "chestcavity:soul_container";

    private final Player owner;
    private UUID activeProfileId;
    private final Map<UUID, SoulProfile> profiles = new HashMap<>();
    // Persistent display names per soul (Unicode allowed, suggest ≤16 chars for tab list)
    private final Map<UUID, String> names = new HashMap<>();
    // Persistent AI orders per soulId
    private final Map<UUID, net.tigereye.chestcavity.soul.ai.SoulAIOrders.Order> orders = new HashMap<>();
    // Souls to auto-respawn as shells when owner logs in
    private final Set<UUID> autospawnSouls = new HashSet<>();

    public SoulContainer(Player owner) {
        this.owner = owner;
    }

    public Optional<SoulProfile> getActiveProfile() {
        if (activeProfileId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(profiles.get(activeProfileId));
    }

    public boolean hasProfile(UUID id) {
        return profiles.containsKey(id);
    }

    public void putProfile(UUID id, SoulProfile profile) {
        profiles.put(id, profile);
    }

    public SoulProfile getOrCreateProfile(UUID id) {
        // 若不存在则创建新存档：服务端尽量捕获完整快照；
        // 客户端/非服务端上下文退化为以 Player 可读状态构造。
        return profiles.computeIfAbsent(id, key -> {
            if (owner instanceof ServerPlayer serverPlayer && serverPlayer.getUUID().equals(key)) {
                return SoulProfile.capture(serverPlayer, key);
            }
            return SoulProfile.empty(key);
        });
    }

    public void setActiveProfile(UUID id) {
        this.activeProfileId = id;
    }

    public Optional<UUID> getActiveProfileId() {
        return Optional.ofNullable(activeProfileId);
    }

    public void updateActiveProfile() {
        // 将玩家当前状态写回至“激活的”灵魂存档（仅限服务端），并避免越界源写入
        if (activeProfileId == null || !(owner instanceof ServerPlayer serverPlayer)) {
            return;
        }
        UUID ownerId = serverPlayer.getUUID();
        SoulProfile profile = getOrCreateProfile(activeProfileId);
        if (activeProfileId.equals(ownerId)) {
            // 目标是 Owner 档案：若玩家正附身，则改为从 Owner 外化壳刷新；若无外化壳，跳过并记录
            if (SoulFakePlayerSpawner.isOwnerPossessing(ownerId)) {
                SoulFakePlayerSpawner.getOwnerShell(ownerId).ifPresentOrElse(shell -> {
                    profile.updateFrom(shell);
                    SoulLog.info("[soul] updateActiveProfile source=SHELL owner={}", ownerId);
                }, () -> SoulLog.info("[soul] updateActiveProfile source=SKIP owner={} reason=noShellWhilePossessing", ownerId));
            } else {
                profile.updateFrom(serverPlayer);
                SoulLog.info("[soul] updateActiveProfile source=SELF owner={}", ownerId);
            }
        } else {
            // 目标是某分魂：此时 serverPlayer 即为当前操控体，允许直接刷新当前激活分魂
            profile.updateFrom(serverPlayer);
            SoulLog.info("[soul] updateActiveProfile source=POSSESSED owner={} soulId={}", ownerId, activeProfileId);
        }
    }

    public Set<UUID> getKnownSoulIds() {
        return Collections.unmodifiableSet(profiles.keySet());
    }

    public SoulProfile getProfile(UUID id) {
        return profiles.get(id);
    }

    public Map<UUID, CompoundTag> snapshotAll(ServerPlayer ownerPlayer, boolean clearDirty) {
        return snapshotProfiles(ownerPlayer, false, clearDirty);
    }

    public Map<UUID, CompoundTag> snapshotDirty(ServerPlayer ownerPlayer, boolean clearDirty) {
        return snapshotProfiles(ownerPlayer, true, clearDirty);
    }

    private Map<UUID, CompoundTag> snapshotProfiles(ServerPlayer ownerPlayer, boolean onlyDirty, boolean clearDirty) {
        UUID ownerId = ownerPlayer.getUUID();
        var provider = ownerPlayer.registryAccess();
        Map<UUID, CompoundTag> snapshots = new HashMap<>();

        Set<UUID> soulIds = new HashSet<>(getKnownSoulIds());
        soulIds.add(ownerId);
        soulIds.addAll(SoulFakePlayerSpawner.getOwnedSoulIds(ownerId));

        for (UUID soulId : soulIds) {
            SoulProfile profile = getOrCreateProfile(soulId);
            if (onlyDirty && !profile.isDirty()) {
                continue;
            }
            SoulFakePlayerSpawner.refreshProfileSnapshot(ownerPlayer, soulId, profile);
            snapshots.put(soulId, profile.save(provider));
            if (clearDirty) {
                profile.clearDirty();
            }
        }

        return snapshots;
    }

    public void restoreAll(ServerPlayer ownerPlayer, Map<UUID, CompoundTag> snapshots) {
        if (snapshots.isEmpty()) {
            return;
        }
        UUID ownerId = ownerPlayer.getUUID();
        var provider = ownerPlayer.registryAccess();

        // Load snapshots into container
        snapshots.forEach((soulId, tag) -> profiles.put(soulId, SoulProfile.load(tag, provider)));

        // Restore owner state immediately
        SoulProfile ownerProfile = getOrCreateProfile(ownerId);
        setActiveProfile(ownerId);
        SoulProfileOps.applyProfileToPlayer(ownerProfile, ownerPlayer, "login-restore-owner");
        ownerProfile.clearDirty();

        autospawnSouls.stream()
                .filter(snapshots::containsKey)
                .filter(soulId -> !soulId.equals(ownerId))
                .forEach(soulId -> SoulFakePlayerSpawner.respawnForOwner(ownerPlayer, soulId)
                        .ifPresentOrElse(spawned -> SoulLog.info("[soul] login-restore owner={} soul={} action=spawn-shell", ownerId, soulId),
                                () -> SoulLog.warn("[soul] login-restore owner={} soul={} action=spawn-shell-failed", ownerId, soulId)));

        // Verify cardinality post autospawn to catch accidental duplicate shells
        SoulFakePlayerSpawner.verifyOwnerShellCardinality(ownerPlayer, "login-restore");

        snapshots.keySet().stream()
                .map(this::getProfile)
                .filter(java.util.Objects::nonNull)
                .forEach(SoulProfile::clearDirty);

        SoulProfileOps.markContainerDirty(ownerPlayer, this, "login-restore-all");
    }

    public CompoundTag saveNBT(HolderLookup.Provider provider) {
        // 序列化：写出激活 ID 与所有存档的内容
        CompoundTag root = new CompoundTag();
        if (activeProfileId != null) {
            root.putUUID("active", activeProfileId);
        }
        CompoundTag listTag = new CompoundTag();
        profiles.forEach((uuid, profile) -> listTag.put(uuid.toString(), profile.save(provider)));
        root.put("profiles", listTag);
        // names
        CompoundTag nameTag = new CompoundTag();
        names.forEach((uuid, name) -> nameTag.putString(uuid.toString(), name));
        root.put("names", nameTag);
        // autospawn list
        net.minecraft.nbt.ListTag autoList = new net.minecraft.nbt.ListTag();
        for (UUID id : autospawnSouls) {
            autoList.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
        }
        root.put("autospawn", autoList);
        // orders
        CompoundTag orderTag = new CompoundTag();
        orders.forEach((uuid, order) -> orderTag.putString(uuid.toString(), order.name()));
        root.put("orders", orderTag);
        return root;
    }

    public void loadNBT(CompoundTag tag, HolderLookup.Provider provider) {
        // 反序列化：恢复激活 ID 与存档列表；非法 UUID 键将被忽略
        profiles.clear();
        if (tag.hasUUID("active")) {
            activeProfileId = tag.getUUID("active");
        }
        CompoundTag listTag = tag.getCompound("profiles");
        for (String key : listTag.getAllKeys()) {
            try {
                UUID id = UUID.fromString(key);
                profiles.put(id, SoulProfile.load(listTag.getCompound(key), provider));
            } catch (IllegalArgumentException ignored) {
            }
        }
        names.clear();
        CompoundTag nameTag = tag.getCompound("names");
        for (String key : nameTag.getAllKeys()) {
            try {
                UUID id = UUID.fromString(key);
                String n = nameTag.getString(key);
                if (n != null && !n.isEmpty()) names.put(id, n);
            } catch (IllegalArgumentException ignored) {}
        }
        autospawnSouls.clear();
        net.minecraft.nbt.ListTag autoList = tag.getList("autospawn", net.minecraft.nbt.Tag.TAG_STRING);
        for (int i = 0; i < autoList.size(); i++) {
            String raw = autoList.getString(i);
            try { autospawnSouls.add(UUID.fromString(raw)); } catch (IllegalArgumentException ignored) {}
        }
        orders.clear();
        CompoundTag orderTag = tag.getCompound("orders");
        for (String key : orderTag.getAllKeys()) {
            try {
                UUID id = UUID.fromString(key);
                String name = orderTag.getString(key);
                try {
                    var ord = net.tigereye.chestcavity.soul.ai.SoulAIOrders.Order.valueOf(name);
                    orders.put(id, ord);
                } catch (IllegalArgumentException ignored2) {
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    // -------- AI Orders (persistent) --------
    public net.tigereye.chestcavity.soul.ai.SoulAIOrders.Order getOrder(UUID soulId) {
        return orders.getOrDefault(soulId, net.tigereye.chestcavity.soul.ai.SoulAIOrders.Order.IDLE);
    }

    public void setOrder(ServerPlayer ownerPlayer, UUID soulId, net.tigereye.chestcavity.soul.ai.SoulAIOrders.Order order, String reason) {
        if (order == null) order = net.tigereye.chestcavity.soul.ai.SoulAIOrders.Order.IDLE;
        orders.put(soulId, order);
        SoulProfileOps.markContainerDirty(ownerPlayer, this, reason != null ? reason : "order-set");
    }

    // -------- Names (persistent) --------
    public String getName(UUID soulId) {
        return names.getOrDefault(soulId, "");
    }

    public void setName(ServerPlayer ownerPlayer, UUID soulId, String displayName, String reason) {
        if (displayName == null) displayName = "";
        // Trim to 16 chars for vanilla tab-list compatibility
        if (displayName.length() > 16) {
            displayName = displayName.substring(0, 16);
        }
        names.put(soulId, displayName);
        SoulProfileOps.markContainerDirty(ownerPlayer, this, reason != null ? reason : "name-set");
    }

    public Map<UUID, String> getAllNames() {
        return java.util.Collections.unmodifiableMap(names);
    }

    // -------- Autospawn flag (persistent)
    public boolean isAutospawn(UUID soulId) { return autospawnSouls.contains(soulId); }
    public void setAutospawn(ServerPlayer ownerPlayer, UUID soulId, boolean value, String reason) {
        if (value) autospawnSouls.add(soulId); else autospawnSouls.remove(soulId);
        SoulProfileOps.markContainerDirty(ownerPlayer, this, reason != null ? reason : (value ? "autospawn-on" : "autospawn-off"));
    }

    // -------- Profile removal (safe)
    public void removeProfile(ServerPlayer ownerPlayer, UUID soulId, String reason) {
        if (soulId == null) return;
        UUID ownerId = ownerPlayer.getUUID();
        // Never remove the owner's base profile via this path
        if (soulId.equals(ownerId)) {
            SoulLog.warn("[soul] removeProfile skipped base owner profile owner={}", ownerId);
            return;
        }
        profiles.remove(soulId);
        names.remove(soulId);
        orders.remove(soulId);
        autospawnSouls.remove(soulId);
        if (activeProfileId != null && activeProfileId.equals(soulId)) {
            activeProfileId = ownerId; // fall back to owner base
        }
        SoulProfileOps.markContainerDirty(ownerPlayer, this, reason != null ? reason : "remove-profile");
    }
}
