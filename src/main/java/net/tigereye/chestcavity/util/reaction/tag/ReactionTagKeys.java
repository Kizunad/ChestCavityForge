package net.tigereye.chestcavity.util.reaction.tag;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;

/**
 * 反应系统通用 Tag 常量集合（不依赖 Minecraft 的 Registry/TagKey 机制）。
 *
 * 设计目标：
 * - 用 ResourceLocation 统一表示“状态/标记/元素反应”的语义标签；
 * - 通过 {@link ReactionTagOps} 附着到实体并带有时效；
 * - 便于 ReactionRegistry/器官行为按标签判断与编排规则，避免硬编码唯一名称。
 */
public final class ReactionTagKeys {
    private ReactionTagKeys() {}

    private static ResourceLocation tag(String path) {
        return ChestCavity.id(path);
    }

    // 通用火系标签
    public static final ResourceLocation FIRE_MARK = tag("reaction/fire_mark");
    public static final ResourceLocation OIL_COATING = tag("reaction/oil_coating");
    public static final ResourceLocation FIRE_COAT = tag("reaction/fire_coat");
    public static final ResourceLocation FIRE_RESIDUE = tag("reaction/fire_residue");
    public static final ResourceLocation FIRE_EXPLOSION = tag("reaction/fire_explosion");
    public static final ResourceLocation FIRE_IMMUNE = tag("reaction/fire_immune");
}

