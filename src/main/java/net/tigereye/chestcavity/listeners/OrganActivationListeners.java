package net.tigereye.chestcavity.listeners;

import it.unimi.dsi.fastutil.ints.IntComparators;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ForgeHooks;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.registration.CCOrganScores;
import net.tigereye.chestcavity.registration.CCStatusEffects;
import net.tigereye.chestcavity.registration.CCTags;
import net.tigereye.chestcavity.util.ChestCavityUtil;
import net.tigereye.chestcavity.util.CommonOrganUtil;

import java.util.*;
import java.util.function.BiConsumer;

public class OrganActivationListeners {

    private static Map<ResourceLocation, BiConsumer<LivingEntity,ChestCavityInstance>> abilityIDMap = new HashMap<>();
    public static void register(){
        register(CCOrganScores.CREEPY, OrganActivationListeners::ActivateCreepy);
        register(CCOrganScores.DRAGON_BREATH, OrganActivationListeners::ActivateDragonBreath);
        register(CCOrganScores.DRAGON_BOMBS, OrganActivationListeners::ActivateDragonBombs);
        register(CCOrganScores.FORCEFUL_SPIT, OrganActivationListeners::ActivateForcefulSpit);
        register(CCOrganScores.FURNACE_POWERED, OrganActivationListeners::ActivateFurnacePowered);
        register(CCOrganScores.IRON_REPAIR, OrganActivationListeners::ActivateIronRepair);
        register(CCOrganScores.PYROMANCY, OrganActivationListeners::ActivatePyromancy);
        register(CCOrganScores.GHASTLY, OrganActivationListeners::ActivateGhastly);
        register(CCOrganScores.GRAZING, OrganActivationListeners::ActivateGrazing);
        register(CCOrganScores.SHULKER_BULLETS, OrganActivationListeners::ActivateShulkerBullets);
        register(CCOrganScores.SILK, OrganActivationListeners::ActivateSilk);
    }
    public static void register(ResourceLocation id,BiConsumer<LivingEntity,ChestCavityInstance> ability){
        abilityIDMap.put(id,ability);
    }
    public static boolean activate(ResourceLocation id, ChestCavityInstance cc){
        if(abilityIDMap.containsKey(id)) {
            abilityIDMap.get(id).accept(cc.owner,cc);
            return true;
        }
        else{
            return false;
        }
    }

    public static void ActivateCreepy(LivingEntity entity, ChestCavityInstance cc){
        if(cc.getOrganScore(CCOrganScores.CREEPY) < 1){
            return;
        }
        if(entity.hasEffect(CCStatusEffects.EXPLOSION_COOLDOWN.get())){
            return;
        }
        float explosion_yield = cc.getOrganScore(CCOrganScores.EXPLOSIVE);
        ChestCavityUtil.destroyOrgansWithKey(cc,CCOrganScores.EXPLOSIVE);
        CommonOrganUtil.explode(entity, explosion_yield);
        if(entity.isAlive()) {
            entity.addEffect(new MobEffectInstance(CCStatusEffects.EXPLOSION_COOLDOWN.get(), ChestCavity.config.EXPLOSION_COOLDOWN, 0, false, false, true));
        }
    }

    public static void ActivateDragonBreath(LivingEntity entity, ChestCavityInstance cc){
        //if(entity.world.isClient){
        //    return; //this is spawning entities, this is no place for a client
        //}
        float breath = cc.getOrganScore(CCOrganScores.DRAGON_BREATH);
        if(entity instanceof Player){
            ((Player)entity).causeFoodExhaustion(breath*.6f);
        }
        if(breath <= 0){
            return;
        }

        if(!entity.hasEffect(CCStatusEffects.DRAGON_BREATH_COOLDOWN.get())){
            entity.addEffect(new MobEffectInstance(CCStatusEffects.DRAGON_BREATH_COOLDOWN.get(), ChestCavity.config.DRAGON_BREATH_COOLDOWN, 0, false, false, true));
            cc.projectileQueue.add(CommonOrganUtil::spawnDragonBreath);
        }
    }

    public static void ActivateDragonBombs(LivingEntity entity, ChestCavityInstance cc){
        //if(entity.world.isClient){
        //    return; //this is spawning entities, this is no place for a client
        //}
        float projectiles = cc.getOrganScore(CCOrganScores.DRAGON_BOMBS);
        if(projectiles < 1){
            return;
        }
        if(!entity.hasEffect(CCStatusEffects.DRAGON_BOMB_COOLDOWN.get())){
            CommonOrganUtil.queueDragonBombs(entity,cc,(int)projectiles);
        }
    }

    public static void ActivateForcefulSpit(LivingEntity entity, ChestCavityInstance cc){
        //if(entity.world.isClient){
        //    return; //we are spawning entities, this is no place for a client
        //}
        float projectiles = cc.getOrganScore(CCOrganScores.FORCEFUL_SPIT);
        if(projectiles < 1){
            return;
        }
        if(!entity.hasEffect(CCStatusEffects.FORCEFUL_SPIT_COOLDOWN.get())){
            CommonOrganUtil.queueForcefulSpit(entity,cc,(int)projectiles);
        }
    }

    public static void ActivateFurnacePowered(LivingEntity entity, ChestCavityInstance cc){
        int furnacePowered = Math.round(cc.getOrganScore(CCOrganScores.FURNACE_POWERED));
        if(furnacePowered < 1){
            return;
        }

        //test for fuelA
        int fuelValue = 0;
        ItemStack itemStack = cc.owner.getItemBySlot(EquipmentSlot.MAINHAND);
        if(itemStack != null && itemStack != ItemStack.EMPTY) {
            try {

                fuelValue = ForgeHooks.getBurnTime(itemStack, null); //10; //FuelRegistry.INSTANCE.get(itemStack.getItem());
            }
            catch (Exception e){
                fuelValue = 0;
            }
        }
        if(fuelValue == 0){
            itemStack = cc.owner.getItemBySlot(EquipmentSlot.OFFHAND);
            if(itemStack != null && itemStack != ItemStack.EMPTY) {
                try{
                fuelValue = ForgeHooks.getBurnTime(itemStack, null);//10; //FuelRegistry.INSTANCE.get(itemStack.getItem());
                }
                catch (Exception e){
                    fuelValue = 0;
                }
            }
        }
        if(fuelValue == 0){
            return;
        }

        //setup the new status effect
        MobEffectInstance newSEI = null;
        if(cc.owner.hasEffect(CCStatusEffects.FURNACE_POWER.get())) {
            MobEffectInstance oldPower = cc.owner.getEffect(CCStatusEffects.FURNACE_POWER.get());
            if(oldPower.getAmplifier() >= furnacePowered-1){
                return; //you can only fuel up if you have room
            }
            CompoundTag oldTag = new CompoundTag();
            List<Integer> durations = new ArrayList<>();
            durations.add(fuelValue);

            oldPower.save(oldTag);
            while(true) {
                durations.add(oldTag.getInt("Duration"));
                if (oldTag.contains("HiddenEffect")) {
                    oldTag = oldTag.getCompound("HiddenEffect");
                } else {
                    break;
                }
            }

            durations.sort(IntComparators.OPPOSITE_COMPARATOR);
            int amplifier = 0;
            for (Integer duration:
                 durations) {
                newSEI = new MobEffectInstance(CCStatusEffects.FURNACE_POWER.get(), duration, amplifier, false, false, true,newSEI);
                amplifier++;
            }
        }
        else{
            newSEI = new MobEffectInstance(CCStatusEffects.FURNACE_POWER.get(), fuelValue, 0, false, false, true);
        }
        entity.removeEffect(CCStatusEffects.FURNACE_POWER.get());
        entity.addEffect(newSEI);
        itemStack.shrink(1); //TODO: if player "eats" lava bucket it does not leave the bucket behind
    }

    public static void ActivateIronRepair(LivingEntity entity, ChestCavityInstance cc){
        float ironRepair = cc.getOrganScore(CCOrganScores.IRON_REPAIR) - cc.getChestCavityType().getDefaultOrganScore(CCOrganScores.IRON_REPAIR);
        //test for ability
        if(ironRepair <= 0){
            return;
        }
        //test for cooldown
        if(cc.owner.hasEffect(CCStatusEffects.IRON_REPAIR_COOLDOWN.get())){
            return;
        }
        //test for missing health
        if(cc.owner.getHealth() >= cc.owner.getMaxHealth()){
            return;
        }
        //test for iron
        ItemStack itemStack = cc.owner.getItemBySlot(EquipmentSlot.MAINHAND);
        if(itemStack == null || !itemStack.is(CCTags.IRON_REPAIR_MATERIAL)) {
            itemStack = cc.owner.getItemBySlot(EquipmentSlot.OFFHAND);
            if(itemStack == null || !itemStack.is(CCTags.IRON_REPAIR_MATERIAL)) {
                return;
            }
        }

        //success! heal target
        cc.owner.heal(cc.owner.getMaxHealth()*ChestCavity.config.IRON_REPAIR_PERCENT);
        entity.playSound(SoundEvents.IRON_GOLEM_REPAIR, .75f, 1);
        cc.owner.addEffect(new MobEffectInstance(CCStatusEffects.IRON_REPAIR_COOLDOWN.get(), (int)(ChestCavity.config.IRON_REPAIR_COOLDOWN/ironRepair), 0, false, false, true));
        itemStack.shrink(1);

    }

    public static void ActivateGhastly(LivingEntity entity, ChestCavityInstance cc){
        //if(entity.world.isClient){
        //    return; //this is spawning entities, this is no place for a client
        //}
        float ghastly = cc.getOrganScore(CCOrganScores.GHASTLY);
        if(ghastly < 1){
            return;
        }
        if(!entity.hasEffect(CCStatusEffects.GHASTLY_COOLDOWN.get())){
            CommonOrganUtil.queueGhastlyFireballs(entity,cc,(int)ghastly);
        }
    }

    private static void ActivateGrazing(LivingEntity entity, ChestCavityInstance cc) {
        float grazing = cc.getOrganScore(CCOrganScores.GRAZING);
        if(grazing <= 0){
            return;
        }
        BlockPos blockPos = entity.getOnPos();//.below();
        boolean ateGrass = false;
        if (entity.level.getBlockState(blockPos).is(Blocks.GRASS_BLOCK)
        || entity.level.getBlockState(blockPos).is(Blocks.MYCELIUM)){
            //entity.world.syncWorldEvent(2001, blockPos, Block.getRawIdFromState(Blocks.GRASS_BLOCK.getDefaultState()));
            entity.level.setBlock(blockPos, Blocks.DIRT.defaultBlockState(), 2);
            ateGrass = true;
        }
        else if(entity.level.getBlockState(blockPos).is(Blocks.CRIMSON_NYLIUM)
        || entity.level.getBlockState(blockPos).is(Blocks.WARPED_NYLIUM)){
            entity.level.setBlock(blockPos, Blocks.NETHERRACK.defaultBlockState(), 2);
            ateGrass = true;
        }
        if(ateGrass){
            int duration;
            if(entity.hasEffect(CCStatusEffects.RUMINATING.get())){
                MobEffectInstance ruminating = entity.getEffect(CCStatusEffects.RUMINATING.get());
                duration = (int)Math.min(ChestCavity.config.RUMINATION_TIME*ChestCavity.config.RUMINATION_GRASS_PER_SQUARE*ChestCavity.config.RUMINATION_SQUARES_PER_STOMACH*grazing,
                        ruminating.getDuration()+(ChestCavity.config.RUMINATION_TIME*ChestCavity.config.RUMINATION_GRASS_PER_SQUARE));
            }
            else{
                duration = ChestCavity.config.RUMINATION_TIME*ChestCavity.config.RUMINATION_GRASS_PER_SQUARE;
            }
            entity.addEffect(new MobEffectInstance(CCStatusEffects.RUMINATING.get(), duration, 0, false, false, true));
        }
    }

    public static void ActivatePyromancy(LivingEntity entity, ChestCavityInstance cc){
        //if(entity.world.isClient){
        //    return; //we are spawning entities, this is no place for a client
        //}
        float pyromancy = cc.getOrganScore(CCOrganScores.PYROMANCY);
        if(pyromancy < 1){
            return;
        }
        if(!entity.hasEffect(CCStatusEffects.PYROMANCY_COOLDOWN.get())){
            CommonOrganUtil.queuePyromancyFireballs(entity,cc,(int)pyromancy);
        }
    }

    public static void ActivateShulkerBullets(LivingEntity entity, ChestCavityInstance cc){
        //if(entity.world.isClient){
        //    return; //we are spawning entities, this is no place for a client
        //}
        float projectiles = cc.getOrganScore(CCOrganScores.SHULKER_BULLETS);
        if(projectiles < 1){
            return;
        }
        if(!entity.hasEffect(CCStatusEffects.SHULKER_BULLET_COOLDOWN.get())){
            CommonOrganUtil.queueShulkerBullets(entity,cc,(int)projectiles);
        }
    }

    public static void ActivateSilk(LivingEntity entity, ChestCavityInstance cc){
        if(cc.getOrganScore(CCOrganScores.SILK) == 0){
            return;
        }
        if(entity.hasEffect(CCStatusEffects.SILK_COOLDOWN.get())){
            return;
        }
        if(CommonOrganUtil.spinWeb(entity,cc, cc.getOrganScore(CCOrganScores.SILK))) {
            entity.addEffect(new MobEffectInstance(CCStatusEffects.SILK_COOLDOWN.get(),ChestCavity.config.SILK_COOLDOWN,0,false,false,true));
        }
    }
}