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
        // 兼容旧的 "status/oil_coating" -> 统一到 "reaction/oil_coating"
        if (ChestCavity.id("status/oil_coating").equals(id)) {
            return ReactionTagKeys.OIL_COATING;
        }
        return id;
    }
}

