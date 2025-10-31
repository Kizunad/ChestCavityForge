package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.passive;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.common.passive.PassiveHook;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.runtime.yu.YuLinGuRuntime;

public class YuLinGuPassive implements PassiveHook {

  @Override
  public void onTick(LivingEntity owner, ChestCavityInstance cc, long now) {
    if (owner instanceof ServerPlayer player) {
      YuLinGuRuntime.onTick(player, cc, now);
    }
  }

  @Override
  public void onHurt(
      LivingEntity self,
      net.minecraft.world.damagesource.DamageSource source,
      float amount,
      ChestCavityInstance cc,
      long now) {
    if (self instanceof Player player) {
      YuLinGuRuntime.onHurt(player, cc, source, amount);
    }
  }

  @Override
  public void onHitMelee(
      LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, long now) {
    if (attacker instanceof Player player) {
      YuLinGuRuntime.onMeleeHit(player, target, cc);
    }
  }
}
