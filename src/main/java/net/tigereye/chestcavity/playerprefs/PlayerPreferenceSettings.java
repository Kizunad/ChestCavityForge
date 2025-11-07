package net.tigereye.chestcavity.playerprefs;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * 通用玩家偏好设置容器，允许按键值存储布尔开关，供客户端 UI 与服务端行为共享。
 */
public class PlayerPreferenceSettings implements INBTSerializable<CompoundTag> {

  private static final String ROOT = "preferences";

  private final Map<ResourceLocation, Boolean> toggles = new HashMap<>();

  public boolean resolveBoolean(ResourceLocation key, BooleanSupplier defaultSupplier) {
    Boolean existing = toggles.get(key);
    if (existing != null) {
      return existing;
    }
    boolean def = defaultSupplier == null ? false : defaultSupplier.getAsBoolean();
    toggles.put(key, def);
    return def;
  }

  public boolean getBoolean(ResourceLocation key, boolean fallback) {
    return toggles.getOrDefault(key, fallback);
  }

  public void setBoolean(ResourceLocation key, boolean value) {
    toggles.put(key, value);
  }

  public Map<ResourceLocation, Boolean> export() {
    return new HashMap<>(toggles);
  }

  @Override
  public CompoundTag serializeNBT(HolderLookup.Provider provider) {
    CompoundTag tag = new CompoundTag();
    CompoundTag data = new CompoundTag();
    for (var entry : toggles.entrySet()) {
      data.putBoolean(entry.getKey().toString(), entry.getValue());
    }
    tag.put(ROOT, data);
    return tag;
  }

  @Override
  public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
    toggles.clear();
    if (tag == null || tag.isEmpty()) {
      return;
    }
    CompoundTag data = tag.getCompound(ROOT);
    for (String key : data.getAllKeys()) {
      try {
        ResourceLocation id = ResourceLocation.parse(key);
        toggles.put(id, data.getBoolean(key));
      } catch (IllegalArgumentException ignored) {
      }
    }
  }

  public static class Serializer
      implements IAttachmentSerializer<CompoundTag, PlayerPreferenceSettings> {
    @Override
    public PlayerPreferenceSettings read(
        IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
      PlayerPreferenceSettings settings = new PlayerPreferenceSettings();
      settings.deserializeNBT(provider, tag);
      return settings;
    }

    @Override
    public CompoundTag write(
        PlayerPreferenceSettings attachment, HolderLookup.Provider provider) {
      return attachment.serializeNBT(provider);
    }
  }
}
