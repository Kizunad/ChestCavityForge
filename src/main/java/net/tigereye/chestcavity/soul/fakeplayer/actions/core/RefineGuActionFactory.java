package net.tigereye.chestcavity.soul.fakeplayer.actions.core;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.soul.fakeplayer.actions.api.Action;
import net.tigereye.chestcavity.soul.fakeplayer.actions.registry.ActionFactory;

/**
 * Parses ids like chestcavity:action/refine_gu/<ns>/<path>
 * Example: chestcavity:action/refine_gu/guzhenren/dazhigugufang
 */
public final class RefineGuActionFactory implements ActionFactory {
    private static final String NS = "chestcavity";
    private static final String PREFIX = "action/refine_gu/";

    @Override
    public boolean supports(ResourceLocation id) {
        return NS.equals(id.getNamespace()) && id.getPath().startsWith(PREFIX);
    }

    @Override
    public Action create(ResourceLocation id) {
        String tail = id.getPath().substring(PREFIX.length());
        String[] seg = tail.split("/");
        if (seg.length < 2) return null;
        String gfNs = seg[0];
        String gfPath = seg[1];
        ResourceLocation guFangId;
        try { guFangId = ResourceLocation.fromNamespaceAndPath(gfNs, gfPath); } catch (IllegalArgumentException e) { return null; }
        return new RefineGuAction(id, guFangId);
    }
}

