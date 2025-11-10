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
 *
 * <p><b>Phase 7: 软删除标记（Soft Deletion Marks）</b>
 *
 * <p>本类使用功能开关实现"软删除"机制，将意图分为两类：
 *
 * <ul>
 *   <li><b>核心意图</b>（始终启用，每个模式 ≤2 个）：
 *       <ul>
 *         <li>ORBIT 模式：{@code HoldIntent}、{@code PatrolIntent}
 *         <li>GUARD 模式：{@code GuardIntent}、{@code InterceptIntent}
 *         <li>HUNT 模式：{@code AssassinIntent}、{@code DuelIntent}
 *         <li>HOVER 模式：{@code HoldIntent}、{@code PatrolIntent}
 *         <li>RECALL 模式：{@code RecallIntent}
 *       </ul>
 *   <li><b>扩展意图</b>（仅当 {@code ENABLE_EXTRA_INTENTS=true} 时启用）：
 *       <ul>
 *         <li>ORBIT 模式：{@code SweepSearchIntent}
 *         <li>GUARD 模式：{@code DecoyIntent}、{@code KitingIntent}
 *         <li>HUNT 模式：{@code FocusFireIntent}、{@code BreakerIntent}、{@code SuppressIntent}、 {@code
 *             ShepherdIntent}、{@code SweepIntent}、{@code KitingIntent}、{@code DecoyIntent}、 {@code
 *             PivotIntent}、{@code SweepSearchIntent}
 *       </ul>
 * </ul>
 *
 * <p><b>软删除策略：</b>
 *
 * <ul>
 *   <li>默认配置（{@code ENABLE_EXTRA_INTENTS=false}）下，扩展意图不会被实例化， 降低 AI 决策复杂度
 *   <li>扩展意图实现类保留在代码库中，不硬删除，保持可选功能的完整性
 *   <li>用户可通过修改 {@link
 *       net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning#ENABLE_EXTRA_INTENTS}
 *       开关启用扩展意图
 *   <li>详见：{@code docs/stages/PHASE_7.md} §7.3.2
 * </ul>
 *
 * @see
 *     net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning#ENABLE_EXTRA_INTENTS
 * @see AIMode
 */
public final class IntentPlanner {

  private IntentPlanner() {}

  /**
   * 基于 AIMode 选择候选 Intent 列表。
   *
   * <p>Phase 1: 精简为核心意图（≤2/模式），额外意图受功能开关控制。
   *
   * <p>Phase 7: 核心意图保留最小集合，扩展意图通过 {@code ENABLE_EXTRA_INTENTS} 开关控制。
   *
   * @param mode AI 模式
   * @return 候选意图列表（按优先级评估）
   */
  public static List<Intent> intentsFor(AIMode mode) {
    List<Intent> list = new ArrayList<>();
    switch (mode) {
      case ORBIT -> {
        // Phase 1: 核心意图（2个）
        list.add(
            new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.HoldIntent());
        list.add(
            new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types
                .PatrolIntent());
        // Phase 1: 额外意图（功能开关控制）
        if (net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning
            .ENABLE_EXTRA_INTENTS) {
          list.add(
              new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types
                  .SweepSearchIntent());
        }
      }
      case GUARD -> {
        // Phase 1: 核心意图（2个）
        list.add(
            new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types
                .GuardIntent());
        list.add(
            new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types
                .InterceptIntent());
        // Phase 1: 额外意图（功能开关控制）
        if (net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning
            .ENABLE_EXTRA_INTENTS) {
          list.add(
              new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types
                  .DecoyIntent());
          list.add(
              new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types
                  .KitingIntent());
        }
      }
      case HUNT -> {
        // Phase 1: 核心意图（2个）
        list.add(
            new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types
                .AssassinIntent());
        list.add(
            new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.DuelIntent());
        // Phase 1: 额外意图（功能开关控制）
        if (net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning
            .ENABLE_EXTRA_INTENTS) {
          list.add(
              new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types
                  .FocusFireIntent());
          list.add(
              new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types
                  .BreakerIntent());
          list.add(
              new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types
                  .SuppressIntent());
          list.add(
              new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types
                  .ShepherdIntent());
          list.add(
              new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types
                  .SweepIntent());
          list.add(
              new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types
                  .KitingIntent());
          list.add(
              new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types
                  .DecoyIntent());
          list.add(
              new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types
                  .PivotIntent());
          list.add(
              new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types
                  .SweepSearchIntent());
        }
      }
      case HOVER -> {
        list.add(
            new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.HoldIntent());
        list.add(
            new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types
                .PatrolIntent());
      }
      case RECALL -> {
        list.add(
            new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types
                .RecallIntent());
      }
      case SWARM -> {
        // 交由集群系统调度：此处不返回任何 Intent
      }
    }
    return list;
  }

  /** 评估并选择最高优先级的 IntentResult。 */
  public static Optional<IntentResult> pickBest(AIContext ctx, List<Intent> intents) {
    return intents.stream()
        .map(i -> i.evaluate(ctx))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .max(Comparator.comparingDouble(IntentResult::getPriority));
  }
}
