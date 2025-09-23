package net.tigereye.chestcavity.compat.guzhenren.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Lightweight accessors for Guzhenren mod items referenced by the compat layer.
 */
public final class GuzhenrenItems {

    private static final String MOD_ID = "guzhenren";

    public static final Item JIANJITENG = resolve("jianjiteng");
    public static final Item JIANMAI_GU_GUFANG = resolve("jianmaigugufang");

    private GuzhenrenItems() {
    }

    private static Item resolve(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MOD_ID, path));
    }
}
