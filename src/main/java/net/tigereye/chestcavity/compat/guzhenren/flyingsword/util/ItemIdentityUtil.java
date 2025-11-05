package net.tigereye.chestcavity.compat.guzhenren.flyingsword.util;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 物品“身份”工具：为 ItemStack 赋予稳定 UUID，用于跨附魔/修复后的同物识别。
 *
 * <p>实现基于 1.21 的 DataComponents.CUSTOM_DATA，键名使用命名空间前缀避免冲突。
 */
public final class ItemIdentityUtil {
  private ItemIdentityUtil() {}

  public static final String KEY_ITEM_UUID = "guzhenren:item_uuid";

  /** 读取物品上的稳定 UUID。 */
  public static Optional<UUID> getItemUUID(ItemStack stack) {
    if (stack == null || stack.isEmpty()) return Optional.empty();
    CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
    var tag = data.copyTag();
    if (tag == null || !tag.contains(KEY_ITEM_UUID)) return Optional.empty();
    try {
      return Optional.of(UUID.fromString(tag.getString(KEY_ITEM_UUID)));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * 确保物品拥有稳定 UUID；若无则生成并写入，返回当前 UUID。
   * 会原地修改传入的 ItemStack。
   */
  public static UUID ensureItemUUID(ItemStack stack) {
    var existing = getItemUUID(stack);
    if (existing.isPresent()) return existing.get();

    UUID uuid = UUID.randomUUID();
    CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
    var tag = data.copyTag();
    if (tag == null) tag = new net.minecraft.nbt.CompoundTag();
    tag.putString(KEY_ITEM_UUID, uuid.toString());
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    return uuid;
  }

  /** 比较两个物品是否为“同一个物理物品”。 */
  public static boolean isSamePhysicalItem(ItemStack a, ItemStack b) {
    var ua = getItemUUID(a);
    var ub = getItemUUID(b);
    return ua.isPresent() && ub.isPresent() && ua.get().equals(ub.get());
  }
}

