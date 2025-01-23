package net.tigereye.chestcavity.chestcavities.instance;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import net.tigereye.chestcavity.chestcavities.ChestCavityType;
import net.tigereye.chestcavity.chestcavities.types.DefaultChestCavityType;
import net.tigereye.chestcavity.chestcavities.types.json.GeneratedChestCavityAssignmentManager;
import net.tigereye.chestcavity.chestcavities.types.json.GeneratedChestCavityTypeManager;

import java.util.HashMap;
import java.util.Map;

public class ChestCavityInstanceFactory {

    private static final Map<ResourceLocation, ChestCavityType> entityResourceLocationMap = new HashMap<>();
    private static final ChestCavityType DEFAULT_CHEST_CAVITY_TYPE = new DefaultChestCavityType();

    public static ChestCavityInstance newChestCavityInstance(EntityType<? extends LivingEntity> entityType, LivingEntity owner){
        ResourceLocation entityID = ForgeRegistries.ENTITIES.getKey(entityType);
        if(GeneratedChestCavityAssignmentManager.GeneratedChestCavityAssignments.containsKey(entityID)){
            ResourceLocation chestCavityTypeID = GeneratedChestCavityAssignmentManager.GeneratedChestCavityAssignments.get(entityID);
            if(GeneratedChestCavityTypeManager.GeneratedChestCavityTypes.containsKey(chestCavityTypeID)){
                return new ChestCavityInstance(GeneratedChestCavityTypeManager.GeneratedChestCavityTypes.get(chestCavityTypeID),owner);
            }
        }
        if(entityResourceLocationMap.containsKey(entityID)){
            return new ChestCavityInstance(entityResourceLocationMap.get(ForgeRegistries.ENTITIES.getKey(entityType)),owner);
        }
        return new ChestCavityInstance(DEFAULT_CHEST_CAVITY_TYPE,owner);
    }

    public static void register(EntityType<? extends LivingEntity> entityType,ChestCavityType chestCavityType){
        entityResourceLocationMap.put(ForgeRegistries.ENTITIES.getKey(entityType),chestCavityType);
    }
    public static void register(ResourceLocation entityResourceLocation, ChestCavityType chestCavityType){
        entityResourceLocationMap.put(entityResourceLocation,chestCavityType);
    }
}
