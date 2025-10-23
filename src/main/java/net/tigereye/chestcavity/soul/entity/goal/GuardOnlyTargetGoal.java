package net.tigereye.chestcavity.soul.entity.goal;

import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.tigereye.chestcavity.soul.entity.SoulClanEntity;

/** 仅在个体为保安形态时启用的目标选择器，确保交易体和长老不会被迫参与战斗。 */
public class GuardOnlyTargetGoal extends NearestAttackableTargetGoal<Monster> {
  private final SoulClanEntity mob;

  public GuardOnlyTargetGoal(SoulClanEntity mob) {
    super(mob, Monster.class, true);
    this.mob = mob;
  }

  @Override
  public boolean canUse() {
    // 仅当个体当前为保安形态时允许寻找战斗目标。
    return mob.getVariant() == SoulClanEntity.Variant.GUARD && super.canUse();
  }

  @Override
  public boolean canContinueToUse() {
    // 在目标追击过程中持续校验形态，避免中途变体后仍锁定敌对单位。
    return mob.getVariant() == SoulClanEntity.Variant.GUARD && super.canContinueToUse();
  }
}
