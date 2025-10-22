package net.tigereye.chestcavity.soul.util;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.util.ChestCavityUtil;

/** 通用胸腔插槽工具，负责安全地将物品放入胸腔。 */
public final class ChestCavityInsertOps {

  private ChestCavityInsertOps() {}

  /** 尝试将物品插入实体胸腔，若无法安放则返回剩余物品。 */
  public static ItemStack tryInsert(LivingEntity entity, ItemStack stack) {
    if (entity == null || stack == null || stack.isEmpty()) {
      return stack == null ? ItemStack.EMPTY : stack.copy();
    }
    Optional<ChestCavityEntity> optional = ChestCavityEntity.of(entity);
    if (optional.isEmpty()) {
      return stack.copy();
    }
    ChestCavityInstance cc = optional.get().getChestCavityInstance();
    if (cc == null || cc.inventory == null) {
      return stack.copy();
    }
    if (ChestCavityUtil.getCompatibilityLevel(cc, stack) <= 0) {
      return stack.copy();
    }
    ItemStack remaining = stack.copy();
    for (int slot = 0; slot < cc.inventory.getContainerSize(); slot++) {
      if (!cc.inventory.getItem(slot).isEmpty()) {
        continue;
      }
      cc.inventory.setItem(slot, remaining.copy());
      cc.containerChanged(cc.inventory);
      return ItemStack.EMPTY;
    }
    return remaining;
  }
}
