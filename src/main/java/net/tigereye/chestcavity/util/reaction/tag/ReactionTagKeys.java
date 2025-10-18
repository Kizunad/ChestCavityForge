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

    // 其它元素/学派标签（预留，按需在规则与行为中使用）
    public static final ResourceLocation FROST_MARK = tag("reaction/frost_mark");
    public static final ResourceLocation SOUL_MARK = tag("reaction/soul_mark");
    public static final ResourceLocation CORROSION_MARK = tag("reaction/corrosion_mark");
    public static final ResourceLocation FROST_IMMUNE = tag("reaction/frost_immune");
    public static final ResourceLocation SOUL_IMMUNE = tag("reaction/soul_immune");
    public static final ResourceLocation CORROSION_IMMUNE = tag("reaction/corrosion_immune");

    // 血道通用标签
    public static final ResourceLocation BLOOD_MARK = tag("reaction/blood_mark");
    public static final ResourceLocation HEMORRHAGE = tag("reaction/hemorrhage");
    public static final ResourceLocation BLOOD_TRAIL = tag("reaction/blood_trail");
    public static final ResourceLocation BLOOD_RESIDUE = tag("reaction/blood_residue");
    public static final ResourceLocation BLOOD_RAGE = tag("reaction/blood_rage");
    public static final ResourceLocation BLOOD_OATH = tag("reaction/blood_oath");
    public static final ResourceLocation BLOOD_RITUAL = tag("reaction/blood_ritual");
    public static final ResourceLocation BLOOD_FLOW = tag("reaction/blood_flow");

    // 毒道通用标签
    public static final ResourceLocation TOXIC_MARK = tag("reaction/toxic_mark");
    public static final ResourceLocation STENCH_CLOUD = tag("reaction/stench_cloud");
    public static final ResourceLocation PLAGUE_MARK = tag("reaction/plague_mark");
    public static final ResourceLocation TOXIC_IMMUNE = tag("reaction/toxic_immune");

    // 骨道通用标签
    public static final ResourceLocation BONE_MARK = tag("reaction/bone_mark");
    public static final ResourceLocation SHARD_FIELD = tag("reaction/shard_field");
    public static final ResourceLocation BONE_IMMUNE = tag("reaction/bone_immune");
}
