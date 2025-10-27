package net.tigereye.chestcavity.compat.guzhenren.item.combo.framework;

import net.minecraft.resources.ResourceLocation;

/**
 * 组合杀招 ID 构建工具。
 *
 * <p>统一构造 ResourceLocation，保障后续拆分时不再散落硬编码。
 */
public final class ComboSkillIds {

  private static final String MOD_ID = "guzhenren";

  private ComboSkillIds() {}

  public static ResourceLocation skill(String path) {
    return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
  }

  public static ResourceLocation config(String path) {
    return ResourceLocation.fromNamespaceAndPath(MOD_ID, path + "_config");
  }
}

