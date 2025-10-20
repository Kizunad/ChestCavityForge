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

    // 通用火系标签（采用统一命名）
    public static final ResourceLocation FLAME_MARK = tag("reaction/flame_mark");
    public static final ResourceLocation FIRE_MARK = FLAME_MARK; // 兼容引用
    public static final ResourceLocation OIL_COATING = tag("reaction/oil_coating");
    public static final ResourceLocation FIRE_COAT = tag("reaction/fire_coat");
    public static final ResourceLocation EMBER_RESIDUE = tag("reaction/ember_residue");
    public static final ResourceLocation FIRE_RESIDUE = EMBER_RESIDUE; // 兼容引用
    public static final ResourceLocation FIRE_EXPLOSION = tag("reaction/fire_explosion");
    public static final ResourceLocation FIRE_IMMUNE = tag("reaction/fire_immune");
    public static final ResourceLocation IGNITE_WINDOW = tag("reaction/ignite_window");
    public static final ResourceLocation IGNITE_AMP = tag("reaction/ignite_amp");
    public static final ResourceLocation CHAR_PRESSURE = tag("reaction/char_pressure");
    public static final ResourceLocation DRAGON_FLAME_MARK = tag("reaction/dragon_flame_mark");
    public static final ResourceLocation DRAGON_ASCENT = tag("reaction/dragon_ascent");
    public static final ResourceLocation DRAGON_DIVE = tag("reaction/dragon_dive");

    // 其它元素/学派标签（预留，按需在规则与行为中使用）
    public static final ResourceLocation FROST_MARK = tag("reaction/frost_mark");
    public static final ResourceLocation SOUL_MARK = tag("reaction/soul_mark");
    public static final ResourceLocation CORROSION_MARK = tag("reaction/corrosion_mark");
    public static final ResourceLocation FROST_IMMUNE = tag("reaction/frost_immune");
    public static final ResourceLocation SOUL_IMMUNE = tag("reaction/soul_immune");
    public static final ResourceLocation CORROSION_IMMUNE = tag("reaction/corrosion_immune");

    // 光道/魂道/剑道扩展标签
    public static final ResourceLocation LIGHT_DAZE = tag("reaction/light_daze");
    public static final ResourceLocation MIRROR_IMAGE = tag("reaction/mirror_image");
    public static final ResourceLocation ILLUSION_BURST = tag("reaction/illusion_burst");
    public static final ResourceLocation SOUL_SCAR = tag("reaction/soul_scar");
    public static final ResourceLocation SWORD_SCAR = tag("reaction/sword_scar");
    public static final ResourceLocation LIGHTNING_CHARGE = tag("reaction/lightning_charge");
    public static final ResourceLocation LIGHTNING_IMMUNE = tag("reaction/lightning_immune");
    public static final ResourceLocation WOOD_GROWTH = tag("reaction/wood_growth");
    public static final ResourceLocation HUMAN_AEGIS = tag("reaction/human_aegis");
    public static final ResourceLocation FOOD_MADNESS = tag("reaction/food_madness");
    public static final ResourceLocation WATER_VEIL = tag("reaction/water_veil");
    public static final ResourceLocation HEAVEN_GRACE = tag("reaction/heaven_grace");
    public static final ResourceLocation EARTH_GUARD = tag("reaction/earth_guard");
    public static final ResourceLocation STONE_SHELL = tag("reaction/stone_shell");
    public static final ResourceLocation STAR_GLINT = tag("reaction/star_glint");
    public static final ResourceLocation FLAME_TRAIL = tag("reaction/flame_trail");
    public static final ResourceLocation CLOUD_SHROUD = tag("reaction/cloud_shroud");

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

    // 智道通用标签（心智/专注/幻像）
    public static final ResourceLocation WISDOM_MARK = tag("reaction/wisdom_mark");
    public static final ResourceLocation CONFUSION = tag("reaction/confusion");
    public static final ResourceLocation ILLUSION = tag("reaction/illusion");
    public static final ResourceLocation FOCUS = tag("reaction/focus");
    public static final ResourceLocation MIND_IMMUNE = tag("reaction/mind_immune");
}
