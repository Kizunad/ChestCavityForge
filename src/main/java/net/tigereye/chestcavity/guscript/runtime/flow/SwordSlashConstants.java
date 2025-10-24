package net.tigereye.chestcavity.guscript.runtime.flow;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** 用于剑气相关行为的常量集合，集中维护可被剑气破坏的方块列表。 */
public final class SwordSlashConstants {

  private SwordSlashConstants() {}

  /** 剑 slash 行为允许破坏的方块资源 ID 集合。 */
  public static final String FLOW_PARAM_DAMAGE_AREA = "damage_area";

  public static final String FLOW_PARAM_DAMAGE_RAY = "damage_ray";

  public static final Set<ResourceLocation> SLASH_BREAKABLE_IDS =
      Set.of(
          ResourceLocation.withDefaultNamespace("grass_block"),
          ResourceLocation.withDefaultNamespace("dirt"),
          ResourceLocation.withDefaultNamespace("coarse_dirt"),
          ResourceLocation.withDefaultNamespace("podzol"),
          ResourceLocation.withDefaultNamespace("sand"),
          ResourceLocation.withDefaultNamespace("red_sand"),
          ResourceLocation.withDefaultNamespace("glass"),
          ResourceLocation.withDefaultNamespace("glass_pane"),
          ResourceLocation.withDefaultNamespace("white_stained_glass"),
          ResourceLocation.withDefaultNamespace("white_stained_glass_pane"),
          ResourceLocation.withDefaultNamespace("ice"),
          ResourceLocation.withDefaultNamespace("packed_ice"),
          ResourceLocation.withDefaultNamespace("snow_block"),
          ResourceLocation.withDefaultNamespace("melon"),
          ResourceLocation.withDefaultNamespace("pumpkin"),
          ResourceLocation.withDefaultNamespace("carved_pumpkin"),
          ResourceLocation.withDefaultNamespace("jack_o_lantern"),
          ResourceLocation.withDefaultNamespace("honey_block"),
          ResourceLocation.withDefaultNamespace("scaffolding"),
          ResourceLocation.withDefaultNamespace("bookshelf"),
          ResourceLocation.withDefaultNamespace("short_grass"),
          ResourceLocation.withDefaultNamespace("tall_grass"),
          ResourceLocation.withDefaultNamespace("fern"),
          ResourceLocation.withDefaultNamespace("large_fern"));
}
