package net.tigereye.chestcavity.registration;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.tigereye.chestcavity.ChestCavity;

public class CCTags {
    public static final TagKey<Item> BUTCHERING_TOOL = TagKey.create(Registries.ITEM, ChestCavity.id("butchering_tool"));
    public static final TagKey<Item> ROTTEN_FOOD = TagKey.create(Registries.ITEM, ChestCavity.id("rotten_food"));
    public static final TagKey<Item> CARNIVORE_FOOD = TagKey.create(Registries.ITEM, ChestCavity.id("carnivore_food"));
    public static final TagKey<Item> SALVAGEABLE = TagKey.create(Registries.ITEM, ChestCavity.id("salvageable"));
    public static final TagKey<Item> IRON_REPAIR_MATERIAL = TagKey.create(Registries.ITEM, ChestCavity.id("iron_repair_material"));
    public static final TagKey<Block> SWORD_SLASH_BREAKABLE = TagKey.create(Registries.BLOCK, ChestCavity.id("breakable_by_sword_slash"));

    //public static final Tag<Item> BUTCHERING_TOOL = TagRegistry.item(new ResourceLocation(ChestCavity.MODID,"butchering_tool"));
    //public static final Tag<Item> ROTTEN_FOOD = TagRegistry.item(new ResourceLocation(ChestCavity.MODID,"rotten_food"));
    //public static final Tag<Item> CARNIVORE_FOOD = TagRegistry.item(new ResourceLocation(ChestCavity.MODID,"carnivore_food"));
    //public static final Tag<Item> SALVAGEABLE = TagRegistry.item(new ResourceLocation(ChestCavity.MODID,"salvageable"));
    //public static final Tag<Item> IRON_REPAIR_MATERIAL = TagRegistry.item(new ResourceLocation(ChestCavity.MODID,"iron_repair_material"));
}
