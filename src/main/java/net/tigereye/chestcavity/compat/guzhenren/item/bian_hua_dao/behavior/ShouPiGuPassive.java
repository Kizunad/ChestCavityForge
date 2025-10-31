package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.common.passive.PassiveHook;

/**
 * 兽皮蛊被动（占位实现）：
 * - 现阶段组合技与被动逻辑尚未完全迁移，先提供空实现以确保编译通过。
 */
public class ShouPiGuPassive implements PassiveHook {

  @Override
  public void onTick(LivingEntity owner, ChestCavityInstance cc, long now) {}

  @Override
  public void onHurt(
      LivingEntity self, DamageSource source, float amount, ChestCavityInstance cc, long now) {}

  @Override
  public void onHitMelee(
      LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, long now) {}
}
