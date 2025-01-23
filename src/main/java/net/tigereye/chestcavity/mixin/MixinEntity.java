package net.tigereye.chestcavity.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.listeners.OrganOnHitCallback;
import net.tigereye.chestcavity.registration.CCOrganScores;
import net.tigereye.chestcavity.util.ChestCavityUtil;
import net.tigereye.chestcavity.util.NetworkUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Entity.class)
public class  MixinEntity {

    @ModifyVariable(at = @At("HEAD"), ordinal = 0, method = "checkFallDamage")
    public double chestCavityEntityFallMixin(double finalHeightDifference, double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition){
        if (heightDifference < 0.0D) {
            Optional<ChestCavityEntity> cce = ChestCavityEntity.of((Entity) (Object) this);
            if (cce.isPresent()) {
                finalHeightDifference = heightDifference * (1 - (cce.get().getChestCavityInstance().getOrganScore(CCOrganScores.BUOYANT)/3));
            }
        }
        return finalHeightDifference;
    }

    @Inject(at = @At("RETURN"), method = "interact", cancellable = true)
    public void chestCavityEntityInteractMixin(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> info){
        if(info.getReturnValue() == InteractionResult.PASS && ((Entity)(Object)this) instanceof EnderDragonPart){
            ChestCavity.LOGGER.info("Attempting to open dragon's " + ((EnderDragonPart)(Object)this).name);
            EnderDragon dragon = ((EnderDragonPart)(Object)this).parentMob;
            if(dragon != null){
                ChestCavity.LOGGER.info("Dragon was not null");
                info.setReturnValue(dragon.interact(player,hand));
            }
        }
    }
/*
    @Inject(at = @At("TAIL"), method = "dealDamage")
    public void chestCavityDealDamageMixin(LivingEntity attacker, Entity target, CallbackInfo info) {
        Optional<ChestCavityEntity> cce = ChestCavityEntity.of(attacker);
        if (cce.isPresent() && target instanceof LivingEntity) {
            OrganOnHitCallback.EVENT.invoker().onHit(attacker, (LivingEntity)target, cce.get().getChestCavityInstance());
        }
    }*/
    
}
