package net.tigereye.chestcavity.soul.entity.goal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * 一个可在单个 goal slot 内调度多组子 Goal 的调度器。
 *
 * <p>兼容原生 goalSelector：调度器本身作为一个 Goal 注册， 内部根据配置顺序选择首个满足条件且可执行的子 goal， 并在更高优先级的分支满足时主动让位，避免重复添加多个互斥
 * goal。
 */
public final class DynamicGoalDispatcher<M extends PathfinderMob> extends Goal {

  private final M mob;
  private final List<Branch<M>> branches;
  private Branch<M> runningBranch;
  private Goal runningGoal;
  private Branch<M> pendingBranch;
  private Goal pendingGoal;

  private DynamicGoalDispatcher(M mob, List<Branch<M>> branches) {
    this.mob = Objects.requireNonNull(mob, "mob");
    if (branches.isEmpty()) {
      throw new IllegalArgumentException("DynamicGoalDispatcher requires at least one branch");
    }
    this.branches = Collections.unmodifiableList(new ArrayList<>(branches));
    EnumSet<Goal.Flag> aggregated = EnumSet.noneOf(Goal.Flag.class);
    for (Branch<M> branch : this.branches) {
      aggregated.addAll(branch.goal().getFlags());
    }
    this.setFlags(aggregated);
  }

  public static <M extends PathfinderMob> Builder<M> builder(M mob) {
    return new Builder<>(mob);
  }

  @Override
  public boolean canUse() {
    selectCandidate();
    return pendingGoal != null;
  }

  @Override
  public void start() {
    if (pendingBranch == null || pendingGoal == null) {
      selectCandidate();
    }
    runningBranch = pendingBranch;
    runningGoal = pendingGoal;
    pendingBranch = null;
    pendingGoal = null;
    if (runningGoal != null) {
      runningGoal.start();
    }
  }

  @Override
  public boolean canContinueToUse() {
    if (runningGoal == null || runningBranch == null) {
      return false;
    }
    for (Branch<M> branch : branches) {
      if (!branch.condition().test(mob)) {
        continue;
      }
      Goal candidate = branch.goal();
      if (branch == runningBranch) {
        return candidate.canContinueToUse();
      }
      if (candidate.canUse()) {
        pendingBranch = branch;
        pendingGoal = candidate;
        return false;
      }
    }
    return false;
  }

  @Override
  public void tick() {
    if (runningGoal != null) {
      runningGoal.tick();
    }
  }

  @Override
  public void stop() {
    if (runningGoal != null) {
      runningGoal.stop();
    }
    runningBranch = null;
    runningGoal = null;
  }

  @Override
  public boolean isInterruptable() {
    return runningGoal == null || runningGoal.isInterruptable();
  }

  @Override
  public boolean requiresUpdateEveryTick() {
    return runningGoal != null && runningGoal.requiresUpdateEveryTick();
  }

  private void selectCandidate() {
    pendingBranch = null;
    pendingGoal = null;
    for (Branch<M> branch : branches) {
      if (!branch.condition().test(mob)) {
        continue;
      }
      Goal candidate = branch.goal();
      if (candidate.canUse()) {
        pendingBranch = branch;
        pendingGoal = candidate;
        return;
      }
    }
  }

  private record Branch<M extends PathfinderMob>(String name, Predicate<M> condition, Goal goal) {
    Branch {
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(condition, "condition");
      Objects.requireNonNull(goal, "goal");
    }
  }

  public static final class Builder<M extends PathfinderMob> {

    private final M mob;
    private final List<Branch<M>> branches = new ArrayList<>();

    private Builder(M mob) {
      this.mob = Objects.requireNonNull(mob, "mob");
    }

    public Builder<M> add(String name, Goal goal) {
      return add(name, unused -> true, goal);
    }

    public Builder<M> add(String name, Predicate<M> condition, Goal goal) {
      branches.add(new Branch<>(name, condition, goal));
      return this;
    }

    public DynamicGoalDispatcher<M> build() {
      return new DynamicGoalDispatcher<>(mob, branches);
    }
  }
}
