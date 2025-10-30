package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.common.passive.PassiveHook;

public class YinYangZhuanShenGuPassive implements PassiveHook {

  @Override
  public void onTick(LivingEntity owner, ChestCavityInstance cc, long now) {
    YinYangZhuanShenGuBehavior.INSTANCE.onSlowTick(owner, cc, YinYangZhuanShenGuBehavior.findOrgan(cc));
  }

  @Override
  public void onHurt(
      LivingEntity self, DamageSource source, float amount, ChestCavityInstance cc, long now) {
    YinYangZhuanShenGuBehavior.INSTANCE.onIncomingDamage(
        source, self, cc, YinYangZhuanShenGuBehavior.findOrgan(cc), amount);
  }

  @Override
  public void onHitMelee(
      LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, long now) {
    YinYangZhuanShenGuBehavior.INSTANCE.onHit(
        null, attacker, target, cc, YinYangZhuanShenGuBehavior.findOrgan(cc), 0);
  }
}
