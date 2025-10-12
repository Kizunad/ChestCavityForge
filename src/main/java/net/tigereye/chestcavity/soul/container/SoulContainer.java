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
    // Persistent Brain mode per soulId (forced mode; AUTO by default)
    private final Map<UUID, net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode> brainModes = new HashMap<>();
    // Persistent Brain intent per soulId (encoded as NBT; minimal: CombatIntent)
    private final Map<UUID, net.minecraft.nbt.CompoundTag> brainIntents = new HashMap<>();

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

    /**
     * 获取所有灵魂存档的快照并序列化为 NBT。
     *
     * @param ownerPlayer 持有该容器的玩家（仅服务端有效）。
     * @param clearDirty 是否在成功写出后清除脏标记。
     */
    public Map<UUID, CompoundTag> snapshotAll(ServerPlayer ownerPlayer, boolean clearDirty) {
        return snapshotProfiles(ownerPlayer, false, clearDirty);
    }

    /**
     * 获取仅被标记为“脏”的灵魂快照，减少增量保存成本。
     */
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

    /**
     * 将快照写回容器并应用到宿主玩家，同时尝试按需重生分魂实体。
     */
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
        // 同步持久化 Brain 状态到运行时控制器
        applyBrainRuntimeAfterRestore(ownerPlayer);

        SoulProfileOps.markContainerDirty(ownerPlayer, this, "login-restore-all");
    }

    /**
     * 序列化容器的完整状态，包括存档、名称、AI 设定与自动生成标记。
     */
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
        // brainModes
        CompoundTag modesTag = new CompoundTag();
        brainModes.forEach((uuid, mode) -> modesTag.putString(uuid.toString(), mode.name()));
        root.put("brainModes", modesTag);
        // brainIntents (per-soul encoded tag)
        CompoundTag intentsTag = new CompoundTag();
        brainIntents.forEach((uuid, itag) -> intentsTag.put(uuid.toString(), itag.copy()));
        root.put("brainIntents", intentsTag);
        return root;
    }

    /**
     * 反序列化容器状态。会清空现有缓存后重新写入。
     */
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
        brainModes.clear();
        CompoundTag modesTag = tag.getCompound("brainModes");
        for (String key : modesTag.getAllKeys()) {
            try {
                UUID id = UUID.fromString(key);
                String name = modesTag.getString(key);
                try {
                    var mode = net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode.valueOf(name);
                    brainModes.put(id, mode);
                } catch (IllegalArgumentException ignored2) {}
            } catch (IllegalArgumentException ignored) {}
        }
        brainIntents.clear();
        CompoundTag intentsTag = tag.getCompound("brainIntents");
        for (String key : intentsTag.getAllKeys()) {
            try {
                UUID id = UUID.fromString(key);
                CompoundTag itag = intentsTag.getCompound(key);
                brainIntents.put(id, itag.copy());
            } catch (IllegalArgumentException ignored) {}
        }
    }

    // -------- AI Orders (persistent) --------
    public net.tigereye.chestcavity.soul.ai.SoulAIOrders.Order getOrder(UUID soulId) {
        return orders.getOrDefault(soulId, net.tigereye.chestcavity.soul.ai.SoulAIOrders.Order.IDLE);
    }

    /**
     * 持久化灵魂当前的 AI 指令，并标记容器脏状态。
     */
    public void setOrder(ServerPlayer ownerPlayer, UUID soulId, net.tigereye.chestcavity.soul.ai.SoulAIOrders.Order order, String reason) {
        if (order == null) order = net.tigereye.chestcavity.soul.ai.SoulAIOrders.Order.IDLE;
        orders.put(soulId, order);
        SoulProfileOps.markContainerDirty(ownerPlayer, this, reason != null ? reason : "order-set");
    }

    // -------- Names (persistent) --------
    public String getName(UUID soulId) {
        return names.getOrDefault(soulId, "");
    }

    /**
     * 设置灵魂在 UI 与网络广播中使用的名称。
     */
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
    /**
     * 控制灵魂在宿主登录时是否自动生成实体。
     */
    public void setAutospawn(ServerPlayer ownerPlayer, UUID soulId, boolean value, String reason) {
        if (value) autospawnSouls.add(soulId); else autospawnSouls.remove(soulId);
        SoulProfileOps.markContainerDirty(ownerPlayer, this, reason != null ? reason : (value ? "autospawn-on" : "autospawn-off"));
    }

    // -------- Profile removal (safe)
    /**
     * 从容器中删除指定的灵魂存档及其衍生数据。
     */
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
        brainModes.remove(soulId);
        brainIntents.remove(soulId);
        if (activeProfileId != null && activeProfileId.equals(soulId)) {
            activeProfileId = ownerId; // fall back to owner base
        }
        SoulProfileOps.markContainerDirty(ownerPlayer, this, reason != null ? reason : "remove-profile");
    }

    // -------- Brain Mode (persistent) --------
    public net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode getBrainMode(UUID soulId) {
        return brainModes.getOrDefault(soulId, net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode.AUTO);
    }
    /**
     * 设置灵魂的长期 AI 模式，并同步至运行时控制器。
     */
    public void setBrainMode(ServerPlayer ownerPlayer, UUID soulId, net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode mode, String reason) {
        if (mode == null) mode = net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode.AUTO;
        brainModes.put(soulId, mode);
        // 同步到运行时控制器
        net.tigereye.chestcavity.soul.fakeplayer.brain.BrainController.get().setMode(soulId, mode);
        SoulProfileOps.markContainerDirty(ownerPlayer, this, reason != null ? reason : "brain-mode-set");
    }

    // -------- Brain Intent (persistent, minimal typed) --------
    /**
     * 记录灵魂的主动意图快照，同时推送至运行时控制器。
     */
    public void setBrainIntent(ServerPlayer ownerPlayer, UUID soulId, net.tigereye.chestcavity.soul.fakeplayer.brain.intent.BrainIntent intent, String reason) {
        if (intent == null) return;
        net.minecraft.nbt.CompoundTag itag = encodeIntent(intent);
        if (itag != null) {
            brainIntents.put(soulId, itag);
            // 同步到运行时控制器
            net.tigereye.chestcavity.soul.fakeplayer.brain.BrainController.get().pushIntent(soulId, intent);
            SoulProfileOps.markContainerDirty(ownerPlayer, this, reason != null ? reason : "brain-intent-set");
        }
    }

    /**
     * 清除灵魂的主动意图（容器 + 运行时）。
     */
    public void clearBrainIntent(ServerPlayer ownerPlayer, UUID soulId, String reason) {
        brainIntents.remove(soulId);
        net.tigereye.chestcavity.soul.fakeplayer.brain.BrainController.get().clearIntents(soulId);
        SoulProfileOps.markContainerDirty(ownerPlayer, this, reason != null ? reason : "brain-intent-clear");
    }

    private static net.minecraft.nbt.CompoundTag encodeIntent(net.tigereye.chestcavity.soul.fakeplayer.brain.intent.BrainIntent intent) {
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        if (intent instanceof net.tigereye.chestcavity.soul.fakeplayer.brain.intent.CombatIntent ci) {
            t.putString("type", "combat");
            t.putString("style", ci.style().name());
            if (ci.focusTarget() != null) t.putUUID("focus", ci.focusTarget());
            t.putInt("ttl", Math.max(0, ci.ttlTicks()));
            return t;
        }
        // 其他类型暂未实现，返回 null 不保存
        return null;
    }

    private static net.tigereye.chestcavity.soul.fakeplayer.brain.intent.BrainIntent decodeIntent(net.minecraft.nbt.CompoundTag t) {
        if (t == null || !t.contains("type")) return null;
        String type = t.getString("type");
        if ("combat".equalsIgnoreCase(type)) {
            net.tigereye.chestcavity.soul.fakeplayer.brain.intent.CombatStyle style;
            try {
                style = net.tigereye.chestcavity.soul.fakeplayer.brain.intent.CombatStyle.valueOf(t.getString("style"));
            } catch (IllegalArgumentException ex) {
                style = net.tigereye.chestcavity.soul.fakeplayer.brain.intent.CombatStyle.FORCE_FIGHT;
            }
            java.util.UUID focus = t.hasUUID("focus") ? t.getUUID("focus") : null;
            int ttl = t.getInt("ttl");
            return new net.tigereye.chestcavity.soul.fakeplayer.brain.intent.CombatIntent(style, focus, ttl);
        }
        return null;
    }

    /** 在登录恢复后，同步持久化的 BrainMode/Intent 到运行时控制器。 */
    public void applyBrainRuntimeAfterRestore(ServerPlayer ownerPlayer) {
        for (Map.Entry<UUID, net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode> e : brainModes.entrySet()) {
            net.tigereye.chestcavity.soul.fakeplayer.brain.BrainController.get().setMode(e.getKey(), e.getValue());
        }
        for (Map.Entry<UUID, net.minecraft.nbt.CompoundTag> e : brainIntents.entrySet()) {
            var intent = decodeIntent(e.getValue());
            if (intent != null) {
                net.tigereye.chestcavity.soul.fakeplayer.brain.BrainController.get().pushIntent(e.getKey(), intent);
            }
        }
        SoulProfileOps.markContainerDirty(ownerPlayer, this, "brain-apply-runtime");
    }
}
