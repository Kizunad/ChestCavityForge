package net.tigereye.chestcavity.soul.profile;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.soul.profile.capability.CapabilityPipeline;
import net.tigereye.chestcavity.soul.profile.capability.CapabilitySnapshot;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
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
    private final Map<ResourceLocation, CapabilitySnapshot> capabilities;
    private boolean dirty;

    private SoulProfile(UUID profileId,
                        InventorySnapshot inventory,
                        PlayerStatsSnapshot stats,
                        PlayerEffectsSnapshot effects,
                        PlayerPositionSnapshot position,
                        Map<ResourceLocation, CapabilitySnapshot> capabilities,
                        boolean dirty) {
        this.profileId = profileId;
        this.inventory = inventory;
        this.stats = stats;
        this.effects = effects;
        this.position = position;
        this.capabilities = new LinkedHashMap<>(capabilities);
        this.dirty = dirty;
    }

    public static SoulProfile capture(ServerPlayer player, UUID id) {
        Map<ResourceLocation, CapabilitySnapshot> capabilitySnapshots = CapabilityPipeline.captureFor(player);
        CapabilityPipeline.clearDirty(capabilitySnapshots);
        return new SoulProfile(id,
                InventorySnapshot.capture(player),
                PlayerStatsSnapshot.capture(player),
                PlayerEffectsSnapshot.capture(player),
                PlayerPositionSnapshot.capture(player),
                capabilitySnapshots,
                false);
    }

    public static SoulProfile fromSnapshot(UUID id,
                                           InventorySnapshot snapshot,
                                           PlayerStatsSnapshot stats,
                                           PlayerEffectsSnapshot effects,
                                           PlayerPositionSnapshot position) {
        return fromSnapshot(id, snapshot, stats, effects, position, CapabilityPipeline.createDefaultSnapshots());
    }

    public static SoulProfile fromSnapshot(UUID id,
                                           InventorySnapshot snapshot,
                                           PlayerStatsSnapshot stats,
                                           PlayerEffectsSnapshot effects,
                                           PlayerPositionSnapshot position,
                                           Map<ResourceLocation, CapabilitySnapshot> capabilities) {
        CapabilityPipeline.clearDirty(capabilities);
        return new SoulProfile(id, snapshot, stats, effects, position, capabilities, false);
    }

    public static SoulProfile empty(UUID id) {
        return new SoulProfile(id,
                InventorySnapshot.empty(),
                PlayerStatsSnapshot.empty(),
                PlayerEffectsSnapshot.empty(),
                PlayerPositionSnapshot.empty(),
                CapabilityPipeline.createDefaultSnapshots(),
                false);
    }

    public UUID id() {
        return profileId;
    }

    public java.util.Optional<PlayerPositionSnapshot> position() {
        return java.util.Optional.ofNullable(position);
    }

    public Map<ResourceLocation, CapabilitySnapshot> capabilities() {
        return Collections.unmodifiableMap(capabilities);
    }

    public <T extends CapabilitySnapshot> java.util.Optional<T> capability(ResourceLocation id, Class<T> type) {
        CapabilitySnapshot snapshot = capabilities.get(id);
        if (snapshot == null || !type.isInstance(snapshot)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(type.cast(snapshot));
    }

    public int inventorySize() {
        return inventory.items().size();
    }

    public void restoreBase(ServerPlayer player) {
        // 恢复基础数据：背包、能力/附件（可能调整属性上限）、再回写原版状态，最后效果
        inventory.restore(player);
        // 确保主手选中了一个可见物品（以便后续装备修饰符应用到 MAINHAND）
        net.tigereye.chestcavity.soul.util.SoulRenderSync.ensureMainHandSelected(player);
        // 应用装备修饰符：直接通知 LivingEntity 装备变更，确保武器/护甲的属性修饰符生效
        reapplyEquipmentAttributeModifiers(player);
        // 先应用能力（ChestCavity/Guzhenren 等），以便 MAX_HEALTH 等上限先就位
        CapabilityPipeline.applyAll(capabilities, player);
        // 再回写原版统计（生命值会按当前 max health 裁剪，避免 700→20 的误裁）
        stats.restore(player, player.registryAccess());
        if (effects != null) {
            effects.restore(player);
        }
    }

    /**
     * After inventory restore, re-issue equip change notifications so item attribute modifiers
     * (weapon attack damage/speed, armor bonuses, etc.) are properly applied to the player.
     */
    private static void reapplyEquipmentAttributeModifiers(ServerPlayer player) {
        // Armor slots
        for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[]{
                net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET
        }) {
            net.minecraft.world.item.ItemStack current = player.getItemBySlot(slot);
            player.onEquipItem(slot, net.minecraft.world.item.ItemStack.EMPTY, current);
        }
        // Hands
        net.minecraft.world.item.ItemStack main = player.getMainHandItem();
        net.minecraft.world.item.ItemStack off = player.getOffhandItem();
        player.onEquipItem(net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY, main);
        player.onEquipItem(net.minecraft.world.entity.EquipmentSlot.OFFHAND, net.minecraft.world.item.ItemStack.EMPTY, off);
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
        CapabilityPipeline.captureAll(capabilities, player);
        this.dirty = true;
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
        CompoundTag capabilityTag = CapabilityPipeline.save(capabilities, provider);
        if (!capabilityTag.isEmpty()) {
            tag.put("capabilities", capabilityTag);
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
        Map<ResourceLocation, CapabilitySnapshot> capabilities = CapabilityPipeline.load(tag.getCompound("capabilities"), provider);
        return new SoulProfile(id, inv, stats, effects, position, capabilities, false);
    }

    public boolean isDirty() {
        return dirty || CapabilityPipeline.isDirty(capabilities);
    }

    public void clearDirty() {
        CapabilityPipeline.clearDirty(capabilities);
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
