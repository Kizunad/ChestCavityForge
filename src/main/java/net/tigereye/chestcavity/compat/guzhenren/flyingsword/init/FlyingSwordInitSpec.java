package net.tigereye.chestcavity.compat.guzhenren.flyingsword.init;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 飞剑初始化定制规格（InitSpec）。
 *
 * <p>用于在生成飞剑时按需定制：
 * - 属性覆盖（部分字段可选）
 * - 显示模型（物品ID），以及预留 Geckolib 模型键
 * - 音效档标识（预留）
 */
public class FlyingSwordInitSpec {
  @Nullable public CompoundTag attributesOverride; // 可选：覆盖 FlyingSwordAttributes 的字段
  @Nullable public ResourceLocation displayItemId; // 渲染使用的物品模型ID
  @Nullable public String geckoModelKey; // 预留：Geckolib 模型键
  @Nullable public String soundProfile; // 预留：音效档ID

  public static FlyingSwordInitSpec empty() {
    return new FlyingSwordInitSpec();
  }

  public boolean hasAny() {
    return (attributesOverride != null && !attributesOverride.isEmpty())
        || displayItemId != null
        || (geckoModelKey != null && !geckoModelKey.isEmpty())
        || (soundProfile != null && !soundProfile.isEmpty());
  }
}

