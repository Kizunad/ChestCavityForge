package net.tigereye.chestcavity.soul.entity.goal;

import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.tigereye.chestcavity.soul.entity.SoulClanEntity;

public class FollowElderGoal extends Goal {
  private final SoulClanEntity mob;
  private final Supplier<Optional<SoulClanEntity>> elderSupplier;
  private final double followDist;

  public FollowElderGoal(
      SoulClanEntity mob, Supplier<Optional<SoulClanEntity>> elderSupplier, double followDist) {
    this.mob = mob;
    this.elderSupplier = elderSupplier;
    this.followDist = followDist;
    this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
  }

  @Override
  public boolean canUse() {
    return mob.isAlive()
        && mob.getVariant() != SoulClanEntity.Variant.ELDER
        && elderSupplier.get().isPresent();
  }

  @Override
  public void tick() {
    elderSupplier
        .get()
        .ifPresent(
            leader -> {
              double d2 = mob.distanceToSqr(leader);
              if (d2 > (followDist * followDist) / 4.0) {
                mob.getNavigation().moveTo(leader, 1.15);
              }
              mob.getLookControl().setLookAt(leader, 20.0f, 20.0f);
            });
  }
}
