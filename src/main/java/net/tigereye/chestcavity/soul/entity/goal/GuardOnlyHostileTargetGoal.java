package net.tigereye.chestcavity.soul.entity.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.tigereye.chestcavity.soul.entity.SoulClanEntity;

public class GuardOnlyHostileTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {
  private final SoulClanEntity mob;

  public GuardOnlyHostileTargetGoal(SoulClanEntity mob) {
    super(mob, LivingEntity.class, 10, true, false, SoulClanEntity::isHostileToClan);
    this.mob = mob;
  }

  @Override
  public boolean canUse() {
    return mob.getVariant() == SoulClanEntity.Variant.GUARD && super.canUse();
  }

  @Override
  public boolean canContinueToUse() {
    return mob.getVariant() == SoulClanEntity.Variant.GUARD && super.canContinueToUse();
  }
}
