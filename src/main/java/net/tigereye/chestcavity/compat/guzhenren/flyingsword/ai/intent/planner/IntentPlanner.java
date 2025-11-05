package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.planner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.AIMode;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.Intent;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;

/**
 * Intent 规划器：收集候选 Intent，选择最高优先级。
 */
public final class IntentPlanner {

  private IntentPlanner() {}

  /**
   * 基于 AIMode 选择候选 Intent 列表。
   * Phase 1: 精简为核心意图（≤2/模式），额外意图受功能开关控制。
   */
  public static List<Intent> intentsFor(AIMode mode) {
    List<Intent> list = new ArrayList<>();
    switch (mode) {
      case ORBIT -> {
        // Phase 1: 核心意图（2个）
        list.add(new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.HoldIntent());
        list.add(new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.PatrolIntent());
        // Phase 1: 额外意图（功能开关控制）
        if (net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning.ENABLE_EXTRA_INTENTS) {
          list.add(new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.SweepSearchIntent());
        }
      }
      case GUARD -> {
        // Phase 1: 核心意图（2个）
        list.add(new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.GuardIntent());
        list.add(new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.InterceptIntent());
        // Phase 1: 额外意图（功能开关控制）
        if (net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning.ENABLE_EXTRA_INTENTS) {
          list.add(new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.DecoyIntent());
          list.add(new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.KitingIntent());
        }
      }
      case HUNT -> {
        // Phase 1: 核心意图（2个）
        list.add(new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.AssassinIntent());
        list.add(new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.DuelIntent());
        // Phase 1: 额外意图（功能开关控制）
        if (net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning.ENABLE_EXTRA_INTENTS) {
          list.add(new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.FocusFireIntent());
          list.add(new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.BreakerIntent());
          list.add(new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.SuppressIntent());
          list.add(new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.ShepherdIntent());
          list.add(new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.SweepIntent());
          list.add(new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.KitingIntent());
          list.add(new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.DecoyIntent());
          list.add(new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.PivotIntent());
          list.add(new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.SweepSearchIntent());
        }
      }
      case HOVER -> {
        list.add(new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.HoldIntent());
        list.add(new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.PatrolIntent());
      }
      case RECALL -> {
        list.add(new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.RecallIntent());
      }
      case SWARM -> {
        // 交由集群系统调度：此处不返回任何 Intent
      }
    }
    return list;
  }

  /**
   * 评估并选择最高优先级的 IntentResult。
   */
  public static Optional<IntentResult> pickBest(AIContext ctx, List<Intent> intents) {
    return intents.stream()
        .map(i -> i.evaluate(ctx))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .max(Comparator.comparingDouble(IntentResult::getPriority));
  }
}
