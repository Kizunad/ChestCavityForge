package net.tigereye.chestcavity.compat.guzhenren.flyingsword;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * 飞剑存储（Flying Sword Storage）
 *
 * <p>存储玩家召回的飞剑数据，用于持久化和恢复。 每个玩家可以存储多个召回的飞剑状态。
 */
public class FlyingSwordStorage implements INBTSerializable<CompoundTag> {

  /** 召回的飞剑列表 */
  private final List<RecalledSword> recalledSwords = new ArrayList<>();

  /** 默认最大存储数量（无额外加成时）。 */
  private static final int DEFAULT_MAX_RECALLED = 10;

  /**
   * 召回飞剑并保存状态
   *
   * @param sword 要召回的飞剑
   * @return 是否成功召回
   */
  public boolean recallSword(FlyingSwordEntity sword) {
    return recallSword(sword, DEFAULT_MAX_RECALLED);
  }

  /**
   * 召回飞剑并保存状态
   *
   * @param sword 要召回的飞剑
   * @param maxCapacity 本次允许的最大容量（<=0 时退回默认值）
   * @return 是否成功召回
   */
  public boolean recallSword(FlyingSwordEntity sword, int maxCapacity) {
    int limit = maxCapacity <= 0 ? DEFAULT_MAX_RECALLED : maxCapacity;
    if (recalledSwords.size() >= limit) {
      return false; // 存储已满
    }

    RecalledSword recalled = RecalledSword.fromEntity(sword);
    recalledSwords.add(recalled);
    return true;
  }

  /** 获取所有召回的飞剑 */
  public List<RecalledSword> getRecalledSwords() {
    return new ArrayList<>(recalledSwords);
  }

  /** 移除指定索引的飞剑 */
  public void remove(int index) {
    if (index >= 0 && index < recalledSwords.size()) {
      recalledSwords.remove(index);
    }
  }

  /** 清空所有召回的飞剑 */
  public void clear() {
    recalledSwords.clear();
  }

  /** 获取召回的飞剑数量 */
  public int getCount() {
    return recalledSwords.size();
  }

  @Override
  public CompoundTag serializeNBT(HolderLookup.Provider provider) {
    CompoundTag tag = new CompoundTag();

    ListTag swordList = new ListTag();
    for (RecalledSword sword : recalledSwords) {
      swordList.add(sword.serializeNBT());
    }

    tag.put("RecalledSwords", swordList);
    return tag;
  }

  @Override
  public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
    recalledSwords.clear();

    if (tag.contains("RecalledSwords", Tag.TAG_LIST)) {
      ListTag swordList = tag.getList("RecalledSwords", Tag.TAG_COMPOUND);
      for (int i = 0; i < swordList.size(); i++) {
        CompoundTag swordTag = swordList.getCompound(i);
        RecalledSword recalled = RecalledSword.fromNBT(swordTag);
        recalledSwords.add(recalled);
      }
    }
  }

  /** 召回的飞剑数据 */
  public static class RecalledSword {
    public FlyingSwordAttributes attributes;
    public int level;
    public int experience;
    public float durability;
    public net.minecraft.nbt.CompoundTag chestCavity; // 保存胸腔内容
    // 额外：渲染与类型信息（用于恢复后保持外观与分型）
    // 优先持久化完整的 ItemStack NBT，以保留附魔发光/自定义组件；旧版兼容保留 itemId。
    public @org.jetbrains.annotations.Nullable net.minecraft.nbt.CompoundTag
        displayItem; // 完整 ItemStack NBT
    public @org.jetbrains.annotations.Nullable net.minecraft.resources.ResourceLocation
        displayItemId; // 兼容字段
    public @org.jetbrains.annotations.Nullable String modelKey;
    public @org.jetbrains.annotations.Nullable String soundProfile;
    public @org.jetbrains.annotations.Nullable String swordType; // FlyingSwordType 的注册名
    public @org.jetbrains.annotations.Nullable String displayItemUUID; // 稳定物品UUID字符串
    public boolean itemWithdrawn; // 是否已从存储中“拿出”物品（true 时禁止恢复/召唤）

    public RecalledSword(
        FlyingSwordAttributes attributes,
        int level,
        int experience,
        float durability,
        net.minecraft.nbt.CompoundTag chestCavity,
        @org.jetbrains.annotations.Nullable net.minecraft.nbt.CompoundTag displayItem,
        @org.jetbrains.annotations.Nullable net.minecraft.resources.ResourceLocation displayItemId,
        @org.jetbrains.annotations.Nullable String modelKey,
        @org.jetbrains.annotations.Nullable String soundProfile,
        @org.jetbrains.annotations.Nullable String swordType,
        @org.jetbrains.annotations.Nullable String displayItemUUID) {
      this.attributes = attributes;
      this.level = level;
      this.experience = experience;
      this.durability = durability;
      this.chestCavity = chestCavity;
      this.displayItem = displayItem;
      this.displayItemId = displayItemId;
      this.modelKey = modelKey;
      this.soundProfile = soundProfile;
      this.swordType = swordType;
      this.displayItemUUID = displayItemUUID;
      this.itemWithdrawn = false;
    }

    /** 从实体创建召回数据 */
    public static RecalledSword fromEntity(FlyingSwordEntity entity) {
      net.minecraft.nbt.CompoundTag ccTag = new net.minecraft.nbt.CompoundTag();
      try {
        var provider = entity.registryAccess();
        var cc = net.tigereye.chestcavity.registration.CCAttachments.getChestCavity(entity);
        net.minecraft.nbt.CompoundTag wrapper = new net.minecraft.nbt.CompoundTag();
        cc.toTag(wrapper, provider);
        ccTag = wrapper.getCompound("ChestCavity");
      } catch (Throwable t) {
        net.tigereye.chestcavity.ChestCavity.LOGGER.warn(
            "[FlyingSword] Failed to capture chest cavity on recall; continuing without it", t);
      }
      // 显示模型（完整 ItemStack + UUID + itemId 回退）
      net.minecraft.world.item.ItemStack display = entity.getDisplayItemStack();
      net.minecraft.nbt.CompoundTag displayTag = null;
      net.minecraft.resources.ResourceLocation displayId = null;
      java.util.Optional<java.util.UUID> displayUuid = java.util.Optional.empty();
      if (display != null && !display.isEmpty()) {
        // 先按飞剑耐久比例“映射”为物品耐久（仅对可损耗物生效）
        try {
          double max = Math.max(1.0, entity.getSwordAttributes().maxDurability);
          double percent = entity.getDurability() / max; // 1=满耐久，0=无耐久
          net.tigereye.chestcavity.compat.guzhenren.flyingsword.util.ItemDurabilityUtil
              .applyPercentToStack(display, percent);
        } catch (Throwable ignored) {
        }

        // 保存完整 ItemStack NBT（含附魔/组件），并保留 itemId 作为回退
        try {
          net.minecraft.nbt.Tag raw = display.save(entity.registryAccess());
          if (raw instanceof net.minecraft.nbt.CompoundTag ct) {
            displayTag = ct.copy();
          }
        } catch (Throwable ignored) {
        }
        displayId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(display.getItem());
        displayUuid =
            net.tigereye.chestcavity.compat.guzhenren.flyingsword.util.ItemIdentityUtil.getItemUUID(
                display);
      }

      // 模型键与音效档
      String modelKey = entity.getModelKey();
      String soundProfile = entity.getSoundProfile();

      // 飞剑类型注册名
      String swordType = entity.getSwordType().getRegistryName();

      return new RecalledSword(
          entity.getSwordAttributes(),
          entity.getSwordLevel(),
          entity.getExperience(),
          entity.getDurability(),
          ccTag,
          displayTag,
          displayId,
          modelKey == null || modelKey.isEmpty() ? null : modelKey,
          soundProfile == null || soundProfile.isEmpty() ? null : soundProfile,
          swordType,
          displayUuid.map(java.util.UUID::toString).orElse(null));
    }

    /** 序列化为NBT */
    public CompoundTag serializeNBT() {
      CompoundTag tag = new CompoundTag();
      CompoundTag attrTag = new CompoundTag();
      attributes.saveToNBT(attrTag);
      tag.put("Attributes", attrTag);
      tag.putInt("Level", level);
      tag.putInt("Experience", experience);
      tag.putFloat("Durability", durability);
      if (chestCavity != null && !chestCavity.isEmpty()) {
        tag.put("ChestCavity", chestCavity.copy());
      }
      if (displayItem != null && !displayItem.isEmpty()) {
        tag.put("DisplayItem", displayItem.copy());
      }
      if (displayItemId != null) {
        tag.putString("DisplayItemId", displayItemId.toString());
      }
      if (displayItemUUID != null && !displayItemUUID.isEmpty()) {
        tag.putString("DisplayItemUUID", displayItemUUID);
      }
      tag.putBoolean("ItemWithdrawn", itemWithdrawn);
      if (modelKey != null && !modelKey.isEmpty()) {
        tag.putString("ModelKey", modelKey);
      }
      if (soundProfile != null && !soundProfile.isEmpty()) {
        tag.putString("SoundProfile", soundProfile);
      }
      if (swordType != null && !swordType.isEmpty()) {
        tag.putString("SwordType", swordType);
      }
      return tag;
    }

    /** 从NBT反序列化 */
    public static RecalledSword fromNBT(CompoundTag tag) {
      FlyingSwordAttributes attributes;
      if (tag.contains("Attributes")) {
        attributes = FlyingSwordAttributes.loadFromNBT(tag.getCompound("Attributes"));
      } else {
        attributes = FlyingSwordAttributes.createDefault();
      }

      int level = tag.getInt("Level");
      int experience = tag.getInt("Experience");
      float durability = tag.getFloat("Durability");
      net.minecraft.nbt.CompoundTag ccTag =
          tag.contains("ChestCavity") ? tag.getCompound("ChestCavity").copy() : new CompoundTag();

      net.minecraft.nbt.CompoundTag displayItem = null;
      if (tag.contains("DisplayItem")) {
        displayItem = tag.getCompound("DisplayItem").copy();
      }
      net.minecraft.resources.ResourceLocation displayItemId = null;
      if (tag.contains("DisplayItemId")) {
        try {
          displayItemId =
              net.minecraft.resources.ResourceLocation.parse(tag.getString("DisplayItemId"));
        } catch (Exception ignored) {
          displayItemId = null;
        }
      }
      String modelKey = tag.contains("ModelKey") ? tag.getString("ModelKey") : null;
      String soundProfile = tag.contains("SoundProfile") ? tag.getString("SoundProfile") : null;
      String swordType = tag.contains("SwordType") ? tag.getString("SwordType") : null;
      String displayItemUUID =
          tag.contains("DisplayItemUUID") ? tag.getString("DisplayItemUUID") : null;
      boolean withdrawn = tag.contains("ItemWithdrawn") && tag.getBoolean("ItemWithdrawn");

      return new RecalledSword(
          attributes,
          level,
          experience,
          durability,
          ccTag,
          displayItem,
          displayItemId,
          modelKey,
          soundProfile,
          swordType,
          displayItemUUID) {
        {
          this.itemWithdrawn = withdrawn;
        }
      };
    }
  }
}
