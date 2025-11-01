package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.state;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * 记录玩家“已指定”的飞剑（单个）。
 */
public class FlyingSwordSelection implements INBTSerializable<CompoundTag> {

  private UUID selectedSword; // 实体UUID

  public Optional<UUID> getSelectedSword() {
    return Optional.ofNullable(selectedSword);
  }

  public void setSelectedSword(UUID id) {
    this.selectedSword = id;
  }

  public void clear() {
    this.selectedSword = null;
  }

  @Override
  public CompoundTag serializeNBT(net.minecraft.core.HolderLookup.Provider provider) {
    CompoundTag tag = new CompoundTag();
    if (selectedSword != null) {
      tag.putUUID("Selected", selectedSword);
    }
    return tag;
  }

  @Override
  public void deserializeNBT(
      net.minecraft.core.HolderLookup.Provider provider, CompoundTag nbt) {
    if (nbt.hasUUID("Selected")) {
      selectedSword = nbt.getUUID("Selected");
    } else {
      selectedSword = null;
    }
  }
}

