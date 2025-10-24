package net.tigereye.chestcavity.soul.entity.goal;

import net.minecraft.world.entity.ai.goal.Goal;
import net.tigereye.chestcavity.soul.entity.SoulClanEntity;

public class SplitOnLowHpGoal extends Goal {
  private final SoulClanEntity mob;
  private final float hpThreshold;
  private final int cdTicks;
  private final double areaRadius;
  private final int areaCap;

  public SplitOnLowHpGoal(
      SoulClanEntity mob, float hpThreshold, int cdTicks, double areaRadius, int areaCap) {
    this.mob = mob;
    this.hpThreshold = hpThreshold;
    this.cdTicks = cdTicks;
    this.areaRadius = areaRadius;
    this.areaCap = areaCap;
  }

  @Override
  public boolean canUse() {
    return mob.isAlive() && mob.getHealth() <= hpThreshold;
  }

  @Override
  public void start() {
    mob.trySplit();
  }
}
