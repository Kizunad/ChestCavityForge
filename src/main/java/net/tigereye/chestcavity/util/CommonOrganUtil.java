package net.tigereye.chestcavity.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.interfaces.CCStatusEffectInstance;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.registration.CCOrganScores;
import net.tigereye.chestcavity.registration.CCStatusEffects;

import java.util.*;

public class CommonOrganUtil {
    public static void explode(LivingEntity entity, float explosionYield) {
        if (!entity.level().isClientSide) {
            Level.ExplosionInteraction destructionType = entity.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                    ? Level.ExplosionInteraction.MOB
                    : Level.ExplosionInteraction.NONE;
            entity.level().explode(null, entity.getX(), entity.getY(), entity.getZ(), (float)Math.sqrt(explosionYield), destructionType);
            spawnEffectsCloud(entity);
        }
    }

    public static List<MobEffectInstance> getStatusEffects(ItemStack organ){
        PotionContents contents = organ.get(DataComponents.POTION_CONTENTS);
        if (contents != null) {
            List<MobEffectInstance> list = new ArrayList<>();
            contents.forEachEffect(effect -> list.add(new MobEffectInstance(effect)));
            if (!list.isEmpty()) {
                return list;
            }
        }
        CompoundTag tag = organ.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.isEmpty() || !tag.contains("CustomPotionEffects", 9)) {
            return new ArrayList<>();
        }
        ListTag nbtList = tag.getList("CustomPotionEffects", 10);
        List<MobEffectInstance> list = new ArrayList<>();
        for(int i = 0; i < nbtList.size(); ++i) {
            CompoundTag compoundTag = nbtList.getCompound(i);
            MobEffectInstance instance = MobEffectInstance.load(compoundTag);
            if (instance != null) {
                list.add(instance);
            }
        }
        return list;
    }

    public static void milkSilk(LivingEntity entity){
        if(!entity.hasEffect(CCStatusEffects.SILK_COOLDOWN)){
            ChestCavityEntity.of(entity).ifPresent(cce -> {
                if(cce.getChestCavityInstance().opened){
                    ChestCavityInstance cc = cce.getChestCavityInstance();
                    float silk = cc.getOrganScore(CCOrganScores.SILK);
                    if(silk > 0){
                        if(spinWeb(entity,cc,silk)) {
                            entity.addEffect(new MobEffectInstance(CCStatusEffects.SILK_COOLDOWN, ChestCavity.config.SILK_COOLDOWN,0,false,false,true));
                        }
                    }
                }
            });
        }
    }

    public static void queueDragonBombs(LivingEntity entity, ChestCavityInstance cc, int bombs){
        if(entity instanceof Player){
            ((Player)entity).causeFoodExhaustion(bombs*.6f);
        }
        for(int i = 0; i < bombs;i++){
            cc.projectileQueue.add(CommonOrganUtil::spawnDragonBomb);
        }
        entity.addEffect(new MobEffectInstance(CCStatusEffects.DRAGON_BOMB_COOLDOWN, ChestCavity.config.DRAGON_BOMB_COOLDOWN, 0, false, false, true));
    }

    public static void queueForcefulSpit(LivingEntity entity, ChestCavityInstance cc, int projectiles){
        if(entity instanceof Player){
            ((Player)entity).causeFoodExhaustion(projectiles*.1f);
        }
        for(int i = 0; i < projectiles;i++){
            cc.projectileQueue.add(CommonOrganUtil::spawnSpit);
        }
        entity.addEffect(new MobEffectInstance(CCStatusEffects.FORCEFUL_SPIT_COOLDOWN, ChestCavity.config.FORCEFUL_SPIT_COOLDOWN, 0, false, false, true));
    }

    public static void queueGhastlyFireballs(LivingEntity entity, ChestCavityInstance cc, int ghastly){
        if(entity instanceof Player){
            ((Player)entity).causeFoodExhaustion(ghastly*.3f);
        }
        for(int i = 0; i < ghastly;i++){
            cc.projectileQueue.add(CommonOrganUtil::spawnGhastlyFireball);
        }
        entity.addEffect(new MobEffectInstance(CCStatusEffects.GHASTLY_COOLDOWN, ChestCavity.config.GHASTLY_COOLDOWN, 0, false, false, true));
    }

    public static void queuePyromancyFireballs(LivingEntity entity, ChestCavityInstance cc, int pyromancy){
        if(entity instanceof Player){
            ((Player)entity).causeFoodExhaustion(pyromancy*.1f);
        }
        for(int i = 0; i < pyromancy;i++){
            cc.projectileQueue.add(CommonOrganUtil::spawnPyromancyFireball);
        }
        entity.addEffect(new MobEffectInstance(CCStatusEffects.PYROMANCY_COOLDOWN, ChestCavity.config.PYROMANCY_COOLDOWN, 0, false, false, true));
    }

    public static void queueShulkerBullets(LivingEntity entity, ChestCavityInstance cc, int shulkerBullets){
        if(entity instanceof Player){
            ((Player)entity).causeFoodExhaustion(shulkerBullets*.3f);
        }
        for(int i = 0; i < shulkerBullets;i++){
            cc.projectileQueue.add(CommonOrganUtil::spawnShulkerBullet);
        }
        entity.addEffect(new MobEffectInstance(CCStatusEffects.SHULKER_BULLET_COOLDOWN, ChestCavity.config.SHULKER_BULLET_COOLDOWN, 0, false, false, true));
    }

    public static void setStatusEffects(ItemStack organ, ItemStack potion){
        List<MobEffectInstance> list = new ArrayList<>();
        PotionContents contents = potion.get(DataComponents.POTION_CONTENTS);
        if (contents != null) {
            contents.forEachEffect(effect -> {
                MobEffectInstance copy = new MobEffectInstance(effect);
                ((CCStatusEffectInstance) copy).CC_setDuration(Math.max(1, copy.getDuration() / 4));
                list.add(copy);
            });
        }
        setStatusEffects(organ,list);
    }

    public static void setStatusEffects(ItemStack organ, List<MobEffectInstance> list){
        List<MobEffectInstance> copies = new ArrayList<>();
        for (MobEffectInstance effect : list) {
            if (effect != null) {
                copies.add(new MobEffectInstance(effect));
            }
        }
        if (copies.isEmpty()) {
            organ.remove(DataComponents.POTION_CONTENTS);
            return;
        }
        organ.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.empty(), copies));
    }

    public static void shearSilk(LivingEntity entity){
        ChestCavityEntity.of(entity).ifPresent(cce -> {
            if(cce.getChestCavityInstance().opened){
                float silk = cce.getChestCavityInstance().getOrganScore(CCOrganScores.SILK);

                if(silk > 0){
                    if(silk >= 2){
                        ItemStack stack = new ItemStack(Items.COBWEB,((int)silk)/2);
                        ItemEntity itemEntity = new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), stack);
                        entity.level().addFreshEntity(itemEntity);
                    }
                    if(silk % 2 >= 1){
                        ItemStack stack = new ItemStack(Items.STRING);
                        ItemEntity itemEntity = new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), stack);
                        entity.level().addFreshEntity(itemEntity);
                    }
                }
            }
        });
    }

    public static void spawnEffectsCloud(LivingEntity entity) {
        Collection<MobEffectInstance> collection = entity.getActiveEffects();
        if (!collection.isEmpty()) {
            AreaEffectCloud areaEffectCloudEntity = new AreaEffectCloud(entity.level(), entity.getX(), entity.getY(), entity.getZ());
            areaEffectCloudEntity.setRadius(2.5F);
            areaEffectCloudEntity.setRadiusOnUse(-0.5F);
            areaEffectCloudEntity.setWaitTime(10);
            areaEffectCloudEntity.setDuration(areaEffectCloudEntity.getDuration() / 2);
            areaEffectCloudEntity.setRadiusPerTick(-areaEffectCloudEntity.getRadius() / (float)areaEffectCloudEntity.getDuration());
            Iterator<MobEffectInstance> var3 = collection.iterator();

            while(var3.hasNext()) {
                MobEffectInstance statusEffectInstance = var3.next();
                areaEffectCloudEntity.addEffect(new MobEffectInstance(statusEffectInstance));
            }

            entity.level().addFreshEntity(areaEffectCloudEntity);
        }

    }

    public static void spawnSilk(LivingEntity entity){
        entity.spawnAtLocation(Items.STRING);
    }

    public static void spawnSpit(LivingEntity entity){
        Vec3 entityFacing = entity.getLookAngle().normalize();

        Llama fakeLlama = new Llama(EntityType.LLAMA,entity.level());
        fakeLlama.setPos(entity.getX(),entity.getY(),entity.getZ());
        fakeLlama.setXRot(entity.getXRot());
        fakeLlama.setYRot(entity.getYRot());
        fakeLlama.yBodyRot = entity.yBodyRot;
        LlamaSpit llamaSpitEntity = new LlamaSpit(entity.level(), fakeLlama);
        llamaSpitEntity.setOwner(entity);
        llamaSpitEntity.setDeltaMovement(entityFacing.x*2,entityFacing.y*2,entityFacing.z*2);
        entity.level().addFreshEntity(llamaSpitEntity);
        entityFacing = entityFacing.scale(-.1D);
        entity.push(entityFacing.x,entityFacing.y,entityFacing.z);
    }

    public static void spawnDragonBomb(LivingEntity entity){
        Vec3 entityFacing = entity.getLookAngle().normalize();
        DragonFireball fireballEntity = new DragonFireball(entity.level(), entity, entityFacing);
        fireballEntity.absMoveTo(fireballEntity.getX(), entity.getX(0.5D) + 0.3D, fireballEntity.getZ());
        entity.level().addFreshEntity(fireballEntity);
        entityFacing = entityFacing.scale(-0.2D);
        entity.push(entityFacing.x,entityFacing.y,entityFacing.z);
    }

    public static void spawnDragonBreath(LivingEntity entity){
        Optional<ChestCavityEntity> optional = ChestCavityEntity.of(entity);
        if(optional.isEmpty()){
            return;
        }
        ChestCavityEntity cce = optional.get();
        ChestCavityInstance cc= cce.getChestCavityInstance();
        float breath = cc.getOrganScore(CCOrganScores.DRAGON_BREATH);
        double range = Math.sqrt(breath/2)*5;
        HitResult result = entity.pick(range, 0, false);
        Vec3 pos = result.getLocation();
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(x,y,z);
        while(entity.level().isEmptyBlock(mutable)) {
            --y;
            if (y < 0.0D) {
                return;
            }

            mutable.set(x,y,z);
        }
        y = (Mth.floor(y) + 1);
        AreaEffectCloud breathEntity = new AreaEffectCloud(entity.level(), x, y, z);
        breathEntity.setOwner(entity);
        breathEntity.setRadius((float)Math.max(range/2,Math.min(range, MathUtil.horizontalDistanceTo(breathEntity,entity))));
        breathEntity.setDuration(200);
        breathEntity.setParticle(ParticleTypes.DRAGON_BREATH);
        breathEntity.addEffect(new MobEffectInstance(MobEffects.HARM));
        entity.level().addFreshEntity(breathEntity);

    }

    public static void spawnGhastlyFireball(LivingEntity entity){
        Vec3 entityFacing = entity.getLookAngle().normalize();
        LargeFireball fireballEntity = new LargeFireball(entity.level(), entity, entityFacing, 1);
        fireballEntity.absMoveTo(fireballEntity.getX(), entity.getY(0.5D) + 0.3D, fireballEntity.getZ());
        entity.level().addFreshEntity(fireballEntity);
        entityFacing = entityFacing.scale(-.8D);
        entity.push(entityFacing.x,entityFacing.y,entityFacing.z);
    }

    public static void spawnPyromancyFireball(LivingEntity entity){
        Vec3 entityFacing = entity.getLookAngle().normalize();
        Vec3 velocity = entityFacing.add(
                entity.getRandom().nextGaussian() * 0.1,
                0,
                entity.getRandom().nextGaussian() * 0.1
        );
        SmallFireball smallFireballEntity = new SmallFireball(entity.level(), entity, velocity);
        smallFireballEntity.absMoveTo(smallFireballEntity.getX(), entity.getY(0.5D) + 0.3D, smallFireballEntity.getZ());
        entity.level().addFreshEntity(smallFireballEntity);
        entityFacing = entityFacing.scale(-.2D);
        entity.push(entityFacing.x,entityFacing.y,entityFacing.z);
    }

    public static void spawnShulkerBullet(LivingEntity entity){
        //Vec3d entityFacing = entity.getRotationVector().normalize();
        TargetingConditions targetPredicate = TargetingConditions.forCombat();
        targetPredicate.range(ChestCavity.config.SHULKER_BULLET_TARGETING_RANGE*2);
        LivingEntity target = entity.level().getNearestEntity(LivingEntity.class,
                targetPredicate, entity, entity.getX(), entity.getY(),entity.getZ(),
                new AABB(entity.getX()-ChestCavity.config.SHULKER_BULLET_TARGETING_RANGE,entity.getY()-ChestCavity.config.SHULKER_BULLET_TARGETING_RANGE,entity.getZ()-ChestCavity.config.SHULKER_BULLET_TARGETING_RANGE,
                        entity.getX()+ChestCavity.config.SHULKER_BULLET_TARGETING_RANGE,entity.getY()+ChestCavity.config.SHULKER_BULLET_TARGETING_RANGE,entity.getZ()+ChestCavity.config.SHULKER_BULLET_TARGETING_RANGE));
        if(target == null){
            return;
        }
        ShulkerBullet shulkerBulletEntity = new ShulkerBullet(entity.level(),entity,target, Direction.Axis.Y);
        shulkerBulletEntity.absMoveTo(shulkerBulletEntity.getX(), entity.getY(0.5D) + 0.3D, shulkerBulletEntity.getZ());
        entity.level().addFreshEntity(shulkerBulletEntity);
        //entityFacing = entityFacing.multiply(-.4D);
        //entity.addVelocity(entityFacing.x,entityFacing.y,entityFacing.z);
    }

    public static boolean spinWeb(LivingEntity entity, ChestCavityInstance cc, float silkScore){
        int hungerCost = 0;
        Player player = null;
        if(entity instanceof Player){
            player = (Player)entity;
            if(player.getFoodData().getFoodLevel() < 6){
                return false;
            }
        }

        if(silkScore >= 2) {
            BlockPos pos = entity.getOnPos().relative(entity.getDirection().getOpposite());
            if(entity.level().getBlockState(pos).isAir()){
                if(silkScore >= 3) {
                    hungerCost = 16;
                    silkScore -= 3;
                    entity.level().setBlock(pos, Blocks.WHITE_WOOL.defaultBlockState(), 2);
                }
                else{
                    hungerCost = 8;
                    silkScore -= 2;
                    entity.level().setBlock(pos, Blocks.COBWEB.defaultBlockState(), 2);
                }
            }
        }
        while(silkScore >= 1) {
            silkScore--;
            hungerCost += 4;
            cc.projectileQueue.add(CommonOrganUtil::spawnSilk);
        }
        if(player != null){
            player.getFoodData().addExhaustion(hungerCost);
        }
        return hungerCost > 0;
    }

    public static boolean teleportRandomly(LivingEntity entity, float range) {
        if (!entity.level().isClientSide() && entity.isAlive()) {
            for(int i = 0; i < ChestCavity.config.MAX_TELEPORT_ATTEMPTS; i++) {
                double d = entity.getX() + ((entity.getRandom().nextDouble() - 0.5D) * range);
                double e = Math.max(1, entity.getY() + ((entity.getRandom().nextDouble() - 0.5D) * range));
                double f = entity.getZ() + ((entity.getRandom().nextDouble() - 0.5D) * range);
                if(teleportTo(entity, d, e, f)){
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean teleportTo(LivingEntity entity, double x, double y, double z) {
        if(entity.isPassenger()){
            entity.stopRiding();
        }
        BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos(x, y, z);
        BlockState blockState = entity.level().getBlockState(targetPos);
        while (targetPos.getY() > 0 && !(blockState.blocksMotion() || !blockState.getFluidState().isEmpty()))
        {
            targetPos.move(Direction.DOWN);
            blockState = entity.level().getBlockState(targetPos);
        }
        if(targetPos.getY() <= 0){
            return false;
        }

        targetPos.move(Direction.UP);
        blockState = entity.level().getBlockState(targetPos);
        BlockState blockState2 = entity.level().getBlockState(targetPos.above());
        while (blockState.blocksMotion()
                || !blockState.getFluidState().isEmpty()
                || blockState2.blocksMotion()
                || !blockState2.getFluidState().isEmpty()) {
            targetPos.move(Direction.UP);
            blockState = entity.level().getBlockState(targetPos);
            blockState2 = entity.level().getBlockState(targetPos.above());
        }

        if(entity.level().dimensionType().hasCeiling() && targetPos.getY() >= entity.level().getHeight()){
            return false;
        }
        entity.teleportTo(x, targetPos.getY()+.1, z);
        if (!entity.isSilent()) {
            entity.level().playSound(null, entity.xOld, entity.yOld, entity.zOld, SoundEvents.ENDERMAN_TELEPORT, entity.getSoundSource(), 1.0F, 1.0F);
            entity.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        }

        return true;
    }
}
