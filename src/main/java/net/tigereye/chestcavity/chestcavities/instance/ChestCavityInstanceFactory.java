package net.tigereye.chestcavity.chestcavities.instance;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.chestcavities.ChestCavityType;
import net.tigereye.chestcavity.chestcavities.types.DefaultChestCavityType;
import net.tigereye.chestcavity.chestcavities.types.json.GeneratedChestCavityAssignmentManager;
import net.tigereye.chestcavity.chestcavities.types.json.GeneratedChestCavityTypeManager;

public class ChestCavityInstanceFactory {

  private static final Map<ResourceLocation, ChestCavityType> entityResourceLocationMap =
      new HashMap<>();
  private static final ChestCavityType DEFAULT_CHEST_CAVITY_TYPE = new DefaultChestCavityType();

  public static ChestCavityInstance newChestCavityInstance(LivingEntity owner) {
    return new ChestCavityInstance(resolveChestCavityType(owner), owner);
  }

  public static ChestCavityInstance newChestCavityInstance(
      EntityType<? extends LivingEntity> entityType, LivingEntity owner) {
    return new ChestCavityInstance(resolveChestCavityType(entityType), owner);
  }

  public static ChestCavityType resolveChestCavityType(LivingEntity owner) {
    @SuppressWarnings("unchecked")
    EntityType<? extends LivingEntity> type = (EntityType<? extends LivingEntity>) owner.getType();
    return resolveChestCavityType(type);
  }

  public static ChestCavityType resolveChestCavityType(
      EntityType<? extends LivingEntity> entityType) {
    ResourceLocation entityID = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
    if (GeneratedChestCavityAssignmentManager.GeneratedChestCavityAssignments.containsKey(
        entityID)) {
      ResourceLocation chestCavityTypeID =
          GeneratedChestCavityAssignmentManager.GeneratedChestCavityAssignments.get(entityID);
      if (GeneratedChestCavityTypeManager.GeneratedChestCavityTypes.containsKey(
          chestCavityTypeID)) {
        return GeneratedChestCavityTypeManager.GeneratedChestCavityTypes.get(chestCavityTypeID);
      }
    }
    if (entityResourceLocationMap.containsKey(entityID)) {
      return entityResourceLocationMap.get(entityID);
    }
    return DEFAULT_CHEST_CAVITY_TYPE;
  }

  public static void register(
      EntityType<? extends LivingEntity> entityType, ChestCavityType chestCavityType) {
    entityResourceLocationMap.put(
        BuiltInRegistries.ENTITY_TYPE.getKey(entityType), chestCavityType);
  }

  public static void register(
      ResourceLocation entityResourceLocation, ChestCavityType chestCavityType) {
    entityResourceLocationMap.put(entityResourceLocation, chestCavityType);
  }
}
