package net.tigereye.chestcavity.mob_effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.registration.CCDamageSources;

public class OrganRejection extends CCStatusEffect {

  public OrganRejection() {
    super(MobEffectCategory.NEUTRAL, 0xC8FF00);
  }

  @Override
  public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
    return duration <= 1;
  }

  @Override
  public boolean applyEffectTick(LivingEntity entity, int amplifier) {
    if (!entity.level().isClientSide()) {
      entity.hurt(
          CCDamageSources.organRejection(entity), ChestCavity.config.ORGAN_REJECTION_DAMAGE);
    }
    return true;
  }
}
