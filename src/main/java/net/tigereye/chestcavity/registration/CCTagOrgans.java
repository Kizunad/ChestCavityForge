package net.tigereye.chestcavity.registration;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class CCTagOrgans {

  public static Map<TagKey<Item>, Map<ResourceLocation, Float>> tagMap = new HashMap<>();

  public static void init() {
    Map<ResourceLocation, Float> ease_of_access = new HashMap<>();
    ease_of_access.put(CCOrganScores.EASE_OF_ACCESS, 1f * Items.OAK_DOOR.getDefaultMaxStackSize());
    tagMap.put(ItemTags.DOORS, ease_of_access);
    tagMap.put(ItemTags.TRAPDOORS, ease_of_access);
  }
}
