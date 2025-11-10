package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.behavior.TargetFinder;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.Intent;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.TrajectoryType;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordAITuning;

/** 护主（Guard）： - 目标：最近威胁（优先拦截敌方飞剑，其次普通敌对） - 轨迹：环身警戒/拦截线（Orbit / PredictiveLine） */
public final class GuardIntent implements Intent {
  @Override
  public Optional<IntentResult> evaluate(AIContext ctx) {
    var sword = ctx.sword();
    var owner = ctx.owner();
    LivingEntity target =
        TargetFinder.findNearestHostileForGuard(
            sword, owner.position(), FlyingSwordAITuning.GUARD_SEARCH_RANGE);

    if (target != null) {
      double dist = Math.max(0.5, sword.distanceTo(target));
      // 简易优先级：越近越高，投射物等高级权重后续补充
      double priority = 10.0 / dist;
      return Optional.of(
          IntentResult.builder()
              .target(target)
              .trajectory(TrajectoryType.PredictiveLine)
              .priority(priority)
              // 拦截窗口（秒）作为提示参数，供轨迹/战斗层使用
              .param("intercept_window_min_s", 0.6)
              .param("intercept_window_max_s", 1.2)
              .build());
    }

    // 无威胁时：在主人附近环绕警戒
    Vec3 anchor = owner.getEyePosition();
    return Optional.of(
        IntentResult.builder()
            .target(anchor)
            .trajectory(TrajectoryType.Orbit)
            .priority(0.2) // 低优先级占位
            .build());
  }

  @Override
  public String name() {
    return "Guard";
  }
}
