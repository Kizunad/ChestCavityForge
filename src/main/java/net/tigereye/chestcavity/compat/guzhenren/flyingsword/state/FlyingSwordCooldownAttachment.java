package net.tigereye.chestcavity.compat.guzhenren.flyingsword.state;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * 飞剑冷却附件（存储于 owner 实体上）。
 *
 * <p>结构：使用字符串 Key → int ticks 存储冷却倒计时。
 * Key 格式建议："cc:flying_sword/<sword_uuid>/<domain>"
 */
public class FlyingSwordCooldownAttachment implements INBTSerializable<CompoundTag> {

  private static final String ROOT = "cooldowns";

  private final Map<String, Integer> cooldowns = new HashMap<>();

  /** 获取倒计时（剩余 tick）。不存在时返回 0。 */
  public int get(String key) {
    if (key == null) return 0;
    return Math.max(0, cooldowns.getOrDefault(key, 0));
  }

  /** 设置倒计时（剩余 tick）。负数将被钳制为 0。 */
  public void set(String key, int ticks) {
    if (key == null) return;
    int v = Math.max(0, ticks);
    if (v == 0) {
      cooldowns.remove(key);
    } else {
      cooldowns.put(key, v);
    }
  }

  /** 递减一次（若 >0），返回新的剩余值。 */
  public int tickDown(String key) {
    if (key == null) return 0;
    Integer v = cooldowns.get(key);
    if (v == null || v <= 0) {
      return 0;
    }
    int nv = v - 1;
    if (nv <= 0) {
      cooldowns.remove(key);
      return 0;
    }
    cooldowns.put(key, nv);
    return nv;
  }

  @Override
  public CompoundTag serializeNBT(HolderLookup.Provider provider) {
    CompoundTag tag = new CompoundTag();
    CompoundTag map = new CompoundTag();
    for (var e : cooldowns.entrySet()) {
      String k = Objects.requireNonNull(e.getKey());
      int v = Math.max(0, e.getValue() == null ? 0 : e.getValue());
      if (v > 0) {
        map.putInt(k, v);
      }
    }
    tag.put(ROOT, map);
    return tag;
  }

  @Override
  public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
    cooldowns.clear();
    if (tag == null) return;
    CompoundTag map = tag.getCompound(ROOT);
    for (String key : map.getAllKeys()) {
      int v = Math.max(0, map.getInt(key));
      if (v > 0) {
        cooldowns.put(key, v);
      }
    }
  }
}

