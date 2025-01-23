package net.tigereye.chestcavity.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.listeners.OrganOnHitCallback;
import net.tigereye.chestcavity.listeners.OrganOnHitContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(EnchantmentHelper.class)
public class MixinEnchantmentHelper {

    @Inject(at = @At("TAIL"), method = "doPostDamageEffects")
    private static void chestCavityDealDamageMixin(LivingEntity user, Entity target, CallbackInfo info) {
        Optional<ChestCavityEntity> cce = ChestCavityEntity.of(user);
        if (cce.isPresent() && target instanceof LivingEntity) {
            OrganOnHitCallback.organOnHit(user, (LivingEntity) target, cce.get().getChestCavityInstance());
        }
    }
    
}
