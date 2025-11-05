package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.goal.ForceHuntTargetGoal;

/**
 * 飞剑 Goal 管理工具：按需挂载/复用强制追击目标的 Goal，并提供便捷方法。
 */
public final class SwordGoalOps {

  private SwordGoalOps() {}

  private static final Map<FlyingSwordEntity, ForceHuntTargetGoal> FORCE_HUNT = new WeakHashMap<>();

  /**
   * 确保飞剑上存在一个可复用的 ForceHuntTargetGoal，并按指定优先级挂到 goalSelector。
   */
  public static ForceHuntTargetGoal ensureForceHuntGoal(
      FlyingSwordEntity sword, int priority) {
    Objects.requireNonNull(sword, "sword");
    ForceHuntTargetGoal goal = FORCE_HUNT.get(sword);
    if (goal == null) {
      goal = new ForceHuntTargetGoal(sword);
      FORCE_HUNT.put(sword, goal);
      sword.goalSelector.addGoal(Math.max(0, priority), goal);
    }
    return goal;
  }

  /**
   * 指示飞剑在一段时间内（durationTicks）追击指定目标。若 durationTicks<=0 则持续直到目标失效。
   */
  public static void huntTarget(
      FlyingSwordEntity sword, LivingEntity target, int durationTicks) {
    Objects.requireNonNull(sword, "sword");
    Objects.requireNonNull(target, "target");
    ForceHuntTargetGoal goal = ensureForceHuntGoal(sword, /*priority*/ 1);
    goal.setTarget(target, durationTicks);

    if (durationTicks > 0 && sword.level() instanceof ServerLevel level) {
      TickOps.schedule(
          level,
          () -> {
            // 到期后若仍是该目标，可清空以恢复默认 AI
            if (sword.getTargetEntity() == target) {
              sword.setTargetEntity(null);
            }
          },
          durationTicks);
    }
  }

  /** 清除强制追击（若有）。 */
  public static void clearForcedHunt(FlyingSwordEntity sword) {
    Objects.requireNonNull(sword, "sword");
    ForceHuntTargetGoal goal = FORCE_HUNT.get(sword);
    if (goal != null) {
      goal.setTarget(null, 0);
    }
  }
}

