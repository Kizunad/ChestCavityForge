package net.tigereye.chestcavity.util;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class OrganDataPacketHelper {
    private ResourceLocation resourceLocation;
    private Boolean pseudoOrgan;
    private int abilityCount;
    private Map<ResourceLocation, Float> map;

    public OrganDataPacketHelper(ResourceLocation resourceLocation, boolean pseudoOrgan, int abilityCount, Map<ResourceLocation, Float> map) {
        this.resourceLocation = resourceLocation;
        this.pseudoOrgan = pseudoOrgan;
        this.abilityCount = abilityCount;
        this.map = map;
    }

    public ResourceLocation getResourceLocation() {
        return resourceLocation;
    }

    public Boolean getPseudoOrgan() {
        return pseudoOrgan;
    }

    public int getAbilityCount() {
        return abilityCount;
    }

    public Map<ResourceLocation, Float> getMap() {
        return map;
    }
}
