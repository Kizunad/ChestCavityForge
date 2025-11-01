package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword;

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
 * <p>存储玩家召回的飞剑数据，用于持久化和恢复。
 * 每个玩家可以存储多个召回的飞剑状态。
 */
public class FlyingSwordStorage implements INBTSerializable<CompoundTag> {

  /** 召回的飞剑列表 */
  private final List<RecalledSword> recalledSwords = new ArrayList<>();

  /** 最大存储数量 */
  private static final int MAX_RECALLED = 10;

  /**
   * 召回飞剑并保存状态
   *
   * @param sword 要召回的飞剑
   * @return 是否成功召回
   */
  public boolean recallSword(FlyingSwordEntity sword) {
    if (recalledSwords.size() >= MAX_RECALLED) {
      return false; // 存储已满
    }

    RecalledSword recalled = RecalledSword.fromEntity(sword);
    recalledSwords.add(recalled);
    return true;
  }

  /**
   * 获取所有召回的飞剑
   */
  public List<RecalledSword> getRecalledSwords() {
    return new ArrayList<>(recalledSwords);
  }

  /**
   * 移除指定索引的飞剑
   */
  public void remove(int index) {
    if (index >= 0 && index < recalledSwords.size()) {
      recalledSwords.remove(index);
    }
  }

  /**
   * 清空所有召回的飞剑
   */
  public void clear() {
    recalledSwords.clear();
  }

  /**
   * 获取召回的飞剑数量
   */
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

  /**
   * 召回的飞剑数据
   */
  public static class RecalledSword {
    public FlyingSwordAttributes attributes;
    public int level;
    public int experience;
    public float durability;

    public RecalledSword(
        FlyingSwordAttributes attributes, int level, int experience, float durability) {
      this.attributes = attributes;
      this.level = level;
      this.experience = experience;
      this.durability = durability;
    }

    /**
     * 从实体创建召回数据
     */
    public static RecalledSword fromEntity(FlyingSwordEntity entity) {
      return new RecalledSword(
          entity.getSwordAttributes(),
          entity.getSwordLevel(),
          entity.getExperience(),
          entity.getDurability());
    }

    /**
     * 序列化为NBT
     */
    public CompoundTag serializeNBT() {
      CompoundTag tag = new CompoundTag();
      CompoundTag attrTag = new CompoundTag();
      attributes.saveToNBT(attrTag);
      tag.put("Attributes", attrTag);
      tag.putInt("Level", level);
      tag.putInt("Experience", experience);
      tag.putFloat("Durability", durability);
      return tag;
    }

    /**
     * 从NBT反序列化
     */
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

      return new RecalledSword(attributes, level, experience, durability);
    }
  }
}
