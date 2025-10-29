package net.tigereye.chestcavity.soul.entity.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class SoulClanWorldData extends SavedData {
  private static final String DATA_NAME = "chestcavity_soul_clan";
  private static final String TAG_ELDER = "Elder";
  private static final String TAG_CAP = "AreaCap";

  private java.util.UUID elderId;
  private int areaCap = 12;

  public static SoulClanWorldData get(ServerLevel level) {
    return level
        .getDataStorage()
        .computeIfAbsent(
            new SavedData.Factory<>(SoulClanWorldData::new, SoulClanWorldData::load), DATA_NAME);
  }

  public static SoulClanWorldData load(CompoundTag tag, HolderLookup.Provider registries) {
    SoulClanWorldData d = new SoulClanWorldData();
    if (tag.hasUUID(TAG_ELDER)) {
      d.elderId = tag.getUUID(TAG_ELDER);
    }
    int cap = tag.getInt(TAG_CAP);
    d.areaCap = cap > 0 ? cap : 12;
    return d;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    if (elderId != null) {
      tag.putUUID(TAG_ELDER, elderId);
    }
    tag.putInt(TAG_CAP, areaCap);
    return tag;
  }

  public java.util.UUID getElderId() {
    return elderId;
  }

  public void setElderId(java.util.UUID id) {
    elderId = id;
    setDirty();
  }

  public int getAreaCapOr(int fallback) {
    return areaCap > 0 ? areaCap : fallback;
  }

  public void setAreaCap(int value) {
    areaCap = Math.max(1, value);
    setDirty();
  }
}
