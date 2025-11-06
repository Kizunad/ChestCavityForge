package net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning;

import net.minecraft.resources.ResourceLocation;
/**
 * 飞剑模型参数（默认显示物品、Geckolib 开关预留）。
 */
public final class FlyingSwordModelTuning {
  private FlyingSwordModelTuning() {}

  /** 默认用于渲染的物品ID（可由服主配置） */
  public static final String DEFAULT_ITEM_ID = "minecraft:iron_sword";

  /** 是否启用 Geckolib 渲染管线（占位） */
  // 默认关闭 Geckolib 覆盖渲染（功能保留，可在行为配置中开启）
  public static final boolean ENABLE_GECKOLIB = false;

  /** 是否在实体上显示绑定物品的名称（悬浮名称牌）。 */
  public static final boolean SHOW_ITEM_NAME = true;

  /**
   * 模型固有"刀面倾斜"纠正角（单位：度）。
   *
   * 对许多物品模型（如原版铁剑），其本体沿 X 轴为"前向"，但刀面会相对该轴有一个固定的倾斜。
   * 将该值作为绕本地 X 轴的预旋转，可把刀面调正，使"剑头"沿路径且不再呈现"/"的倾斜。
   */
  public static final float BLADE_ROLL_DEGREES = -45.0f;

  /**
   * 是否使用基于正交基/四元数的姿态计算（Phase 8）。
   *
   * <p>true（默认）：使用 OrientationOps 的 BASIS 模式，根治"半圆抬头"问题；
   * <p>false：沿用 LEGACY_EULER 欧拉角顺序，保持旧资源兼容性。
   */
  public static final boolean USE_BASIS_ORIENTATION = true;

  public static ResourceLocation defaultItemId() {
    try {
      return ResourceLocation.parse(DEFAULT_ITEM_ID);
    } catch (Exception e) {
      return ResourceLocation.withDefaultNamespace("iron_sword");
    }
  }
}
