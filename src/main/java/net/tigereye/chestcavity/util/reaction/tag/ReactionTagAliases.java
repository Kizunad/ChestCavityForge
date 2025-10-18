package net.tigereye.chestcavity.util.reaction.tag;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;

/**
 * 将历史的 statusId/特例 ID 映射到新的通用 reaction/* tag 上，保证平滑过渡。
 */
public final class ReactionTagAliases {
    private ReactionTagAliases() {}

    public static ResourceLocation resolve(ResourceLocation id) {
        if (id == null) return null;
        // 兼容旧的 status/* 命名，统一到 reaction/*
        if (ChestCavity.id("status/oil_coating").equals(id)) return ReactionTagKeys.OIL_COATING;
        if (ChestCavity.id("status/fire_mark").equals(id))    return ReactionTagKeys.FIRE_MARK;
        if (ChestCavity.id("status/charcoal_mark").equals(id))return ReactionTagKeys.FIRE_MARK; // 别名
        if (ChestCavity.id("status/fire_coat").equals(id))    return ReactionTagKeys.FIRE_COAT;
        if (ChestCavity.id("status/fire_residue").equals(id)) return ReactionTagKeys.FIRE_RESIDUE;
        if (ChestCavity.id("status/fire_explosion").equals(id)) return ReactionTagKeys.FIRE_EXPLOSION;
        if (ChestCavity.id("status/fire_immune").equals(id))  return ReactionTagKeys.FIRE_IMMUNE;
        // 其它元素/学派常见名称（预留兼容）
        if (ChestCavity.id("status/frost_mark").equals(id))   return ReactionTagKeys.FROST_MARK;
        if (ChestCavity.id("status/soul_mark").equals(id))    return ReactionTagKeys.SOUL_MARK;
        if (ChestCavity.id("status/corrosion_mark").equals(id)) return ReactionTagKeys.CORROSION_MARK;
        if (ChestCavity.id("status/frost_immune").equals(id)) return ReactionTagKeys.FROST_IMMUNE;
        if (ChestCavity.id("status/soul_immune").equals(id))  return ReactionTagKeys.SOUL_IMMUNE;
        if (ChestCavity.id("status/corrosion_immune").equals(id)) return ReactionTagKeys.CORROSION_IMMUNE;
        return id;
    }
}
