package net.tigereye.chestcavity.mixin;


import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.util.ChestCavityUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Mixin(value = ThrownPotion.class, priority = 1100)
public class MixinPotionEntity extends ThrowableItemProjectile {

    public MixinPotionEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level world) {
        super(entityType, world);
    }
    @Shadow
    protected Item getDefaultItem() {
        return null;
    }

    @Inject(at = @At("TAIL"), method = "applyWater")
    private void ChestCavityPotionEntityDamageEntitiesHurtByWaterMixin(CallbackInfo info) {
        ChestCavityUtil.splashHydrophobicWithWater((ThrownPotion) (Object) this);
    }

}
