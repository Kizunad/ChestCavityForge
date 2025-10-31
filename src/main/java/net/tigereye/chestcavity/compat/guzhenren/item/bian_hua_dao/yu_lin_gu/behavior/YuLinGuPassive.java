package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.yu_lin_gu.behavior;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.common.passive.PassiveHook;

public class YuLinGuPassive implements PassiveHook {

    @Override
    public void onTick(LivingEntity owner, ChestCavityInstance cc, long now) {
        // TODO: Move logic from old passive class here
    }

    @Override
    public float onHurt(LivingEntity self, ChestCavityInstance cc, DamageSource source, float amount, long now) {
        // TODO: Move logic from old passive class here
        return amount;
    }

    @Override
    public float onHitMelee(LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, float damage, long now) {
        // TODO: Move logic from old passive class here
        return damage;
    }
}
