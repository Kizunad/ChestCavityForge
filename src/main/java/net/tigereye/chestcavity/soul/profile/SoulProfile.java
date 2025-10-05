package net.tigereye.chestcavity.soul.profile;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.UUID;

/**
 * 灵魂存档（单个槽位）
 *
 * 职责
 * - 表示一个完整的“灵魂”存档槽，记录玩家背包、原版属性与位置。
 * - 提供从玩家捕获快照、回写到玩家与 NBT 序列化/反序列化的能力。
 *
 * 约束
 * - 当前仅覆盖 vanilla 数据；后续可扩展：能力附件/ChestCavity/其他兼容。
 */
public final class SoulProfile {

    private final UUID profileId;
    private InventorySnapshot inventory;
    private PlayerStatsSnapshot stats;
    private PlayerEffectsSnapshot effects;
    private PlayerPositionSnapshot position;
    private boolean dirty;

    private SoulProfile(UUID profileId,
                        InventorySnapshot inventory,
                        PlayerStatsSnapshot stats,
                        PlayerEffectsSnapshot effects,
                        PlayerPositionSnapshot position,
                        boolean dirty) {
        this.profileId = profileId;
        this.inventory = inventory;
        this.stats = stats;
        this.effects = effects;
        this.position = position;
        this.dirty = dirty;
    }

    public static SoulProfile capture(ServerPlayer player, UUID id) {
        return new SoulProfile(id,
                InventorySnapshot.capture(player),
                PlayerStatsSnapshot.capture(player),
                PlayerEffectsSnapshot.capture(player),
                PlayerPositionSnapshot.capture(player),
                false);
    }

    public static SoulProfile fromSnapshot(UUID id,
                                           InventorySnapshot snapshot,
                                           PlayerStatsSnapshot stats,
                                           PlayerEffectsSnapshot effects,
                                           PlayerPositionSnapshot position) {
        return new SoulProfile(id, snapshot, stats, effects, position, false);
    }

    public static SoulProfile empty(UUID id) {
        return new SoulProfile(id,
                InventorySnapshot.empty(),
                PlayerStatsSnapshot.empty(),
                PlayerEffectsSnapshot.empty(),
                PlayerPositionSnapshot.empty(),
                false);
    }

    public UUID id() {
        return profileId;
    }

    public java.util.Optional<PlayerPositionSnapshot> position() {
        return java.util.Optional.ofNullable(position);
    }

    public int inventorySize() {
        return inventory.items().size();
    }

    public void restoreBase(ServerPlayer player) {
        // 恢复基础数据：背包、属性、效果
        inventory.restore(player);
        stats.restore(player, player.registryAccess());
        if (effects != null) {
            effects.restore(player);
        }
        // TODO: restore capabilities, chest cavity, guzhenren etc.
    }

    /**
     * 仅恢复记录的位置（同维度）。不会进行跨维传送。
     */
    public void restorePosition(ServerPlayer player) {
        if (position != null) {
            position.restoreSameDimension(player);
        }
    }

    public void updateFrom(ServerPlayer player) {
        // 从玩家当前状态刷新快照
        this.inventory = InventorySnapshot.capture(player);
        this.stats = PlayerStatsSnapshot.capture(player);
        this.effects = PlayerEffectsSnapshot.capture(player);
        this.position = PlayerPositionSnapshot.capture(player);
        this.dirty = true;
        // TODO: snapshot capabilities.
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", profileId);
        tag.put("inventory", inventory.save(provider));
        tag.put("stats", stats.save());
        if (effects != null) {
            tag.put("effects", effects.save());
        }
        if (position != null) {
            tag.put("position", position.save(provider));
        }
        return tag;
    }

    public static SoulProfile load(CompoundTag tag, HolderLookup.Provider provider) {
        // 容错：缺失 id 时生成随机 UUID，避免整体读取失败
        UUID id = tag.hasUUID("id") ? tag.getUUID("id") : UUID.randomUUID();
        InventorySnapshot inv = InventorySnapshot.load(tag.getCompound("inventory"), provider);
        PlayerStatsSnapshot stats = tag.contains("stats")
                ? PlayerStatsSnapshot.load(tag.getCompound("stats"), provider)
                : PlayerStatsSnapshot.empty();
        PlayerEffectsSnapshot effects = tag.contains("effects")
                ? PlayerEffectsSnapshot.load(tag.getCompound("effects"))
                : PlayerEffectsSnapshot.empty();
        PlayerPositionSnapshot position = tag.contains("position")
                ? PlayerPositionSnapshot.load(tag.getCompound("position"))
                : PlayerPositionSnapshot.empty();
        return new SoulProfile(id, inv, stats, effects, position, false);
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        this.dirty = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SoulProfile that)) return false;
        return profileId.equals(that.profileId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profileId);
    }
}
