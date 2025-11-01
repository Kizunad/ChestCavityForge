package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;

/**
 * 飞剑类型枚举
 *
 * <p>对应三种注册的实体类型，用于区分不同派系/特性的飞剑。
 */
public enum FlyingSwordType {
  /** 默认通用飞剑 */
  DEFAULT("flying_sword", "默认飞剑"),

  /** 正道飞剑 - 用于正道器官 */
  ZHENG_DAO("flying_sword_zheng_dao", "正道飞剑"),

  /** 人兽葬生飞剑 - 用于魔道器官 */
  REN_SHOU_ZANG_SHENG("flying_sword_ren_shou_zang_sheng", "人兽葬生");

  private final String registryName;
  private final String displayName;
  private final ResourceLocation id;

  FlyingSwordType(String registryName, String displayName) {
    this.registryName = registryName;
    this.displayName = displayName;
    this.id = ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, registryName);
  }

  public String getRegistryName() {
    return registryName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public ResourceLocation getId() {
    return id;
  }

  /**
   * 从实体类型ID识别飞剑类型
   *
   * @param entityTypeId 实体类型ID (如 "chestcavity:flying_sword_zheng_dao")
   * @return 对应的飞剑类型，未知则返回DEFAULT
   */
  public static FlyingSwordType fromEntityTypeId(ResourceLocation entityTypeId) {
    if (entityTypeId == null) {
      return DEFAULT;
    }

    String path = entityTypeId.getPath();
    for (FlyingSwordType type : values()) {
      if (type.registryName.equals(path)) {
        return type;
      }
    }

    return DEFAULT;
  }

  /**
   * 从注册名识别
   */
  public static FlyingSwordType fromRegistryName(String name) {
    if (name == null) {
      return DEFAULT;
    }

    for (FlyingSwordType type : values()) {
      if (type.registryName.equals(name)) {
        return type;
      }
    }

    return DEFAULT;
  }
}
