package net.tigereye.chestcavity.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.common.util.ITeleporter;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstanceFactory;
import net.tigereye.chestcavity.listeners.LootRegister;
import net.tigereye.chestcavity.listeners.OrganFoodEffectCallback;
import net.tigereye.chestcavity.registration.CCItems;
import net.tigereye.chestcavity.items.ChestOpener;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.registration.CCOrganScores;
import net.tigereye.chestcavity.util.ChestCavityUtil;
import net.tigereye.chestcavity.util.CommonOrganUtil;
import net.tigereye.chestcavity.util.NetworkUtil;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Mixin(value = LivingEntity.class,priority = 900)
public class MixinLivingEntity extends Entity implements ChestCavityEntity {
    private ChestCavityInstance chestCavityInstance;

    protected MixinLivingEntity(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(at = @At("TAIL"), method = "<init>")
    public void chestCavityLivingEntityConstructorMixin(EntityType<? extends LivingEntity> entityType, Level world, CallbackInfo info){
        chestCavityInstance = ChestCavityInstanceFactory.newChestCavityInstance(entityType,(LivingEntity)(Object)this);
        //chestCavityInstance.init();
    }

    @Inject(at = @At("HEAD"), method = "baseTick")
    public void chestCavityLivingEntityBaseTickMixin(CallbackInfo info){
        ChestCavityUtil.onTick(chestCavityInstance);
    }

    @Inject(at = @At("TAIL"), method = "baseTick")
    protected void chestCavityLivingEntityBaseTickBreathAirMixin(CallbackInfo info) {
        if(!this.isEyeInFluid(FluidTags.WATER) || this.level.getBlockState(new BlockPos(this.getX(), this.getEyeY(), this.getZ())).is(Blocks.BUBBLE_COLUMN)) {
            this.setAirSupply(ChestCavityUtil.applyBreathOnLand(chestCavityInstance,this.getAirSupply(), this.increaseAirSupply(0)));
        }
    }

    @ModifyVariable(at = @At(value = "CONSTANT", args = "floatValue=0.0F", ordinal = 0), ordinal = 0, method = "actuallyHurt")
    public float chestCavityLivingEntityOnHitMixin(float amount, DamageSource source){
        if(source.getEntity() instanceof LivingEntity){
            Optional<ChestCavityEntity> cce = ChestCavityEntity.of(source.getEntity());
            if(cce.isPresent()){
                    amount = ChestCavityUtil.onHit(cce.get().getChestCavityInstance(), source, (LivingEntity)(Object)this,amount);
            }
        }
        return amount;
    }

    @Inject(at = @At("RETURN"), method = "createLootContext")
    protected void addCustomLootFromEntity(boolean p_21105_, DamageSource p_21106_, CallbackInfoReturnable<LootContext.Builder> cir) {
        LootContext lootContext = cir.getReturnValue().create(LootContextParamSets.ENTITY);
        List<ItemStack> list = LootRegister.addLoot(lootContext);
        list = LootRegister.modifyLoot(list, lootContext);
        list.forEach(this::spawnAtLocation);
    }

    @Inject(at = @At("RETURN"), method = "decreaseAirSupply", cancellable = true)
    protected void chestCavityLivingEntityGetNextAirUnderwaterMixin(int air, CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(ChestCavityUtil.applyBreathInWater(chestCavityInstance,air,info.getReturnValueI()));
    }

    @Inject(at = @At("RETURN"), method = "getDamageAfterArmorAbsorb",cancellable = true)
    public void chestCavityLivingEntityDamageMixin(DamageSource source, float amount, CallbackInfoReturnable<Float> info){
        info.setReturnValue(ChestCavityUtil.applyDefenses(chestCavityInstance, source, info.getReturnValueF()));
    }

    @Inject(at = @At("HEAD"), method = "dropEquipment")
    public void chestCavityLivingEntityDropInventoryMixin(CallbackInfo info){
        //chestCavityInstance.getChestCavityType().onDeath(chestCavityInstance);
        ChestCavityUtil.onDeath(this);
    }

    @ModifyVariable(at = @At("HEAD"), method = "addEffect", ordinal = 0)
    public MobEffectInstance chestCavityLivingEntityAddStatusEffectMixin(MobEffectInstance effect){
        return ChestCavityUtil.onAddStatusEffect(chestCavityInstance,effect);
    }


    //booneldan - why was this gone?? deleted not just commented
    @ModifyVariable(//at = @At(value = "INVOKE", target = "Lnet/minecraft/item/Item;isFood()Z"),
            at = @At("STORE"),
            method = "addEatEffect", ordinal = 0)
    public List<Pair<MobEffectInstance, Float>> chestCavityLivingEntityApplyFoodEffectsMixin(List<Pair<MobEffectInstance, Float>> list, ItemStack stack, Level world, LivingEntity targetEntity){
        Optional<ChestCavityEntity> option = ChestCavityEntity.of(targetEntity);
        if (option.isPresent()) {
            list = new LinkedList<>(list); //this divorces the list I shall modify from the item's long term list
            //OrganFoodEffectCallback.EVENT.invoker().onApplyFoodEffects(list, stack, world, targetEntity, option.get().getChestCavityInstance());
            OrganFoodEffectCallback.addFoodEffects(list, stack, world, targetEntity, option.get().getChestCavityInstance());
        }
        return list;
    }




    /*
    Lnet/minecraft/entity/Entity; (maybe LivingEntity)
    updateVelocity(
            F
            Lnet/minecraft/util/math/Vec3d;
    )V
     */
    //Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/entity/effect/StatusEffect;)Z
    //@ModifyVariable(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;updateVelocity(FLnet/minecraft/util/math/Vec3d;)V"), method = "travel",ordinal = 1)
    //@Group(name = "WaterTravelMixins", max = 2, min = 1)
    @ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V"), method = "travel",index = 0, require = 0)
    protected float chestCavityLivingEntityWaterTravelMixin(float g) {
        return g*ChestCavityUtil.applySwimSpeedInWater(chestCavityInstance);
    }
    //Origins combatibility variant. Why must I do this?!?
    //@Group(name = "WaterTravelMixins")
    //@ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;updateVelocity(Lnet/minecraft/entity/LivingEntity;FLnet/minecraft/util/math/Vec3d;)V"), method = "travel",index = 0)
    //protected double chestCavityLivingEntityWaterTravelMixinAlt(double g) {
    //    return g*ChestCavityUtil.applySwimSpeedInWater(chestCavityInstance);
    //}

    @Inject(at = @At("RETURN"), method = "getJumpPower",cancellable = true)
    public void chestCavityLivingEntityJumpVelocityMixin(CallbackInfoReturnable<Float> info){
        info.setReturnValue(ChestCavityUtil.applyLeaping(chestCavityInstance, info.getReturnValueF()));
    }

    public ChestCavityInstance getChestCavityInstance() {
        return chestCavityInstance;
    }

    public void setChestCavityInstance(ChestCavityInstance chestCavityInstance) {
        this.chestCavityInstance = chestCavityInstance;
    }


    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readCustomDataFromNbt(CompoundTag tag, CallbackInfo callbackInfo) {
        chestCavityInstance.fromTag(tag,(LivingEntity)(Object)this);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void writeCustomDataToNbt(CompoundTag tag, CallbackInfo callbackInfo) {
        chestCavityInstance.toTag(tag);
    }

    @Mixin(Mob.class)
    private static abstract class MixinMobEntity extends LivingEntity{
        protected MixinMobEntity(EntityType<? extends LivingEntity> entityType, Level world) {super(entityType, world);}

        @Inject(at = @At("HEAD"), method = "checkAndHandleImportantInteractions"/*"method_29506"*/, cancellable = true) //if this breaks, its likely because yarn changed the name to interactWithItem
        protected void chestCavityLivingEntityInteractMobMixin(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> info) {
            if(player.getItemInHand(hand).getItem() == CCItems.CHEST_OPENER.get() && (!(((LivingEntity)(Object)this) instanceof Player))){
                ((ChestOpener)player.getItemInHand(hand).getItem()).openChestCavity(player,(LivingEntity)(Object)this);
                ChestCavity.printOnDebug("Opened Cavity from checkAndHandleImportantInteractions. this: " + ((LivingEntity)(Object)this).toString());
                info.setReturnValue(InteractionResult.SUCCESS);
            }
        }
    }
    
    @Mixin(Player.class)
    public static abstract class MixinPlayerEntity extends LivingEntity{
        protected MixinPlayerEntity(EntityType<? extends LivingEntity> entityType, Level world) {
            super(entityType, world);
        }

        @ModifyVariable(at = @At(value = "CONSTANT", args = "floatValue=0.0F", ordinal = 0), ordinal = 0, method = "actuallyHurt")
        public float chestCavityPlayerEntityOnHitMixin(float amount, DamageSource source){
            if(source.getEntity() instanceof LivingEntity){
                Optional<ChestCavityEntity> cce = ChestCavityEntity.of(source.getEntity());
                if(cce.isPresent()){
                    amount = ChestCavityUtil.onHit(cce.get().getChestCavityInstance(), source, (LivingEntity)(Object)this,amount);
                }
            }
            return amount;
        }

        @Inject(at = @At("HEAD"), method = "interactOn", cancellable = true)
        void chestCavityPlayerEntityInteractPlayerMixin(Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> info) {
            ChestCavity.printOnDebug("InteractOn Mixin Called! entity instanceof LivingEntity: " + (entity instanceof LivingEntity) + ". Entity.toString(): " + entity.toString());
            if (entity instanceof LivingEntity && ChestCavity.config.CAN_OPEN_OTHER_PLAYERS) {
                Player player = ((Player) (Object) this);
                ItemStack stack = player.getItemInHand(hand);
    
                if (stack.getItem() == CCItems.CHEST_OPENER.get()) {
                    ((ChestOpener) stack.getItem()).openChestCavity(player, (LivingEntity) entity);
                    ChestCavity.printOnDebug("Opened Cavity On InteractOn Method!");
                    info.setReturnValue(InteractionResult.SUCCESS);
                    info.cancel();
                }
            }
        }
        @Inject(at = @At("RETURN"), method = "getDigSpeed", cancellable = true, remap = false)
        void chestCavityPlayerEntityGetBlockBreakingSpeedMixin(BlockState block, @Nullable BlockPos pos, CallbackInfoReturnable<Float> cir) {
            cir.setReturnValue(ChestCavityUtil.applyNervesToMining(((ChestCavityEntity)this).getChestCavityInstance(),cir.getReturnValue()));
        }
    }

    @Mixin(Cow.class)
    private static abstract class MixinCowEntity extends Animal {

        protected MixinCowEntity(EntityType<? extends Animal> entityType, Level world) {super(entityType, world);}

        @Inject(method = "mobInteract",
                /*at = @At(value = "INVOKE",
                        target = "Lnet/minecraft/entity/LivingEntity;setStackInHand(" +
                                    "Lnet/minecraft/util/Hand;" +
                                    "Lnet/minecraft/item/ItemStack;" +
                                    ")V",
                        shift = At.Shift.AFTER)*/
                at = @At(value = "RETURN", ordinal = 0)
                )
        protected void interactMob(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> info) {
            CommonOrganUtil.milkSilk(this);
        }
    }

    @Mixin(Creeper.class)
    private static abstract class MixinCreeperEntity extends Monster {
        @Shadow
        private int swell;

        protected MixinCreeperEntity(EntityType<? extends Monster> entityType, Level world) {super(entityType, world);}

        @Inject(at = @At("HEAD"), method = "tick")
        protected void chestCavityCreeperTickMixin(CallbackInfo info) {
            if(this.isAlive()
                    && swell > 1){
                ChestCavityEntity.of(this).ifPresent(cce -> {
                    if(cce.getChestCavityInstance().opened
                            && cce.getChestCavityInstance().getOrganScore(CCOrganScores.CREEPY) <= 0){
                        swell = 1;
                    }
                });
            }
        }
        /*value = "FIELD",
                                target = "Lnet/minecraft/entity/CreeperEntity;dead:Z"*/
        //"Lnet/minecraft/world/explosion/Explosion;createExplosion("+
        //  "Lnet/minecraft/entity/Entity;"+
        //  "DDDF"+
        //  "Lnet/minecraft/world/explosion/Explosion/DestructionType;"+
        //  ")V"
        /*
        @ModifyVariable(at = @At(value = "INVOKE",
                target = "Lnet/minecraft/world/explosion/Explosion;createExplosion("+
                "Lnet/minecraft/entity/Entity;"+
                "DDDF"+
                "Lnet/minecraft/world/explosion/Explosion/DestructionType;"+
                ")V"), ordinal = 0, method = "explode")
        public float chestCavityCreeperExplodeMixin(float f){
            MutableFloat boom = new MutableFloat(f);
            ChestCavityEntity.of(this).ifPresent(cce -> {
                ChestCavityInstance cc = cce.getChestCavityInstance();
                if(cc.opened){
                    boom.setValue(f*Math.sqrt(cc.getOrganScore(CCOrganScores.EXPLOSIVE))
                            /Math.min(1,Math.sqrt(cc.getChestCavityType().getDefaultOrganScore(CCOrganScores.EXPLOSIVE))));
                }
            });
            return f;
        }*/
    }

    @Mixin(ServerPlayer.class)
    private static abstract class MixinServerPlayerEntity extends Player {
        public MixinServerPlayerEntity(Level world, BlockPos pos, float yaw, GameProfile profile) {
            super(world, pos, yaw, profile);
        }

        @Inject(method = "restoreFrom", at = @At("TAIL"))
        public void copyFrom(ServerPlayer oldPlayer, boolean alive, CallbackInfo callbackInfo) {
            ChestCavity.printOnDebug("Attempting to load ChestCavityInstance");
            ChestCavityEntity.of(this).ifPresent(chestCavityEntity -> ChestCavityEntity.of(oldPlayer).ifPresent(oldCCPlayerEntityInterface -> {
                ChestCavity.printOnDebug("Copying ChestCavityInstance");
                chestCavityEntity.getChestCavityInstance().clone(oldCCPlayerEntityInterface.getChestCavityInstance());
            }));
        }

        @Inject(at = @At("RETURN"), method = "changeDimension", cancellable = true, remap = false)
        public void chestCavityEntityMoveToWorldMixin(ServerLevel level, ITeleporter teleporter, CallbackInfoReturnable<Entity> info){
            Entity entity = info.getReturnValue();
            if(entity instanceof ChestCavityEntity && !entity.level.isClientSide){
                NetworkUtil.SendS2CChestCavityUpdatePacket(((ChestCavityEntity)entity).getChestCavityInstance());
            }
        }
    }

    @Mixin(Sheep.class)
    private static abstract class MixinSheepEntity extends Animal {

        protected MixinSheepEntity(EntityType<? extends Animal> entityType, Level world) {super(entityType, world);}

        @Inject(method = "shear",
                at = @At(value = "HEAD")
        )
        protected void chestCavitySheared(SoundSource shearedSoundCategory, CallbackInfo info) {
            CommonOrganUtil.shearSilk(this);
        }
    }

    @Mixin(WitherBoss.class)
    private static abstract class MixinWitherEntity extends Monster {


        protected MixinWitherEntity(EntityType<? extends Monster> entityType, Level world) {
            super(entityType, world);
        }

        //Lnet/minecraft/entity/boss/WitherEntity;dropItem(      //note that this might just be Entity instead.
        //  Lnet/minecraft/item/ItemConvertible;
        //)Lnet/minecraft/entity/ItemEntity;
        @Inject(method = "dropCustomDeathLoot",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;spawnAtLocation(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"),
                cancellable = true
        )
        protected void chestCavityPreventNetherStarDrop(DamageSource source, int lootingMultiplier, boolean allowDrops, CallbackInfo info) {
            Optional<ChestCavityEntity> chestCavityEntity = ChestCavityEntity.of(this);
            if(chestCavityEntity.isPresent()){
                ChestCavityInstance cc = chestCavityEntity.get().getChestCavityInstance();

                //if the nether star was taken from the wither's chest, refuse to drop
                if(cc.opened && cc.inventory.countItem(Items.NETHER_STAR) == 0){
                    info.cancel();
                }
            }
        }
    }


    @Shadow
    protected void defineSynchedData() {}

    @Shadow
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Shadow
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Shadow
    public Packet<?> getAddEntityPacket() {return null;}

    @Shadow
    protected int increaseAirSupply(int air) {
        return 0;
    }
}
