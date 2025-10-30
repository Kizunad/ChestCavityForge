package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.common.passive.PassiveHook;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.YuLinGuBehavior;

public class YuLinGuPassive implements PassiveHook {

  @Override
  public void onTick(LivingEntity owner, ChestCavityInstance cc, long now) {
    YuLinGuBehavior.INSTANCE.onSlowTick(owner, cc, YuLinGuBehavior.findOrgan(cc));
  }

  @Override
  public void onHurt(
      LivingEntity self, DamageSource source, float amount, ChestCavityInstance cc, long now) {
    YuLinGuBehavior.INSTANCE.onIncomingDamage(
        source, self, cc, YuLinGuBehavior.findOrgan(cc), amount);
  }

  @Override
  public void onHitMelee(
      LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, long now) {
    YuLinGuBehavior.INSTANCE.onHit(
        null, attacker, target, cc, YuLinGuBehavior.findOrgan(cc), 0);
  }
}
