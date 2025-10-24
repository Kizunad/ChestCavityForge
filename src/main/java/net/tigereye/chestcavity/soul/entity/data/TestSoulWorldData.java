package net.tigereye.chestcavity.soul.entity.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** 持久化 Test 生物的世界级配置（当前仅包含生成上限）。 */
public final class TestSoulWorldData extends SavedData {

  private static final String DATA_NAME = "chestcavity_test_soul";
  private static final String TAG_MAX = "MaxCount";

  private int maxCount = 1;

  public TestSoulWorldData() {}

  public static TestSoulWorldData get(ServerLevel level) {
    return level
        .getDataStorage()
        .computeIfAbsent(
            new SavedData.Factory<>(TestSoulWorldData::new, TestSoulWorldData::load), DATA_NAME);
  }

  private static TestSoulWorldData load(CompoundTag tag, HolderLookup.Provider provider) {
    TestSoulWorldData data = new TestSoulWorldData();
    if (tag != null && tag.contains(TAG_MAX)) {
      data.maxCount = Math.max(1, tag.getInt(TAG_MAX));
    }
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
    tag.putInt(TAG_MAX, maxCount);
    return tag;
  }

  public int getMaxCount() {
    return maxCount;
  }

  public void setMaxCount(int value) {
    int clamped = Math.max(1, value);
    if (clamped != this.maxCount) {
      this.maxCount = clamped;
      setDirty();
    }
  }
}
