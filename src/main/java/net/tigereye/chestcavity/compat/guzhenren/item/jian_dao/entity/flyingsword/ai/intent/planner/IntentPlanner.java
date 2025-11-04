package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.planner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.AIMode;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.Intent;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.IntentResult;

/**
 * Intent 规划器：收集候选 Intent，选择最高优先级。
 */
public final class IntentPlanner {

  private IntentPlanner() {}

  /**
   * 基于 AIMode 选择候选 Intent 列表。
   */
  public static List<Intent> intentsFor(AIMode mode) {
    List<Intent> list = new ArrayList<>();
    switch (mode) {
      case ORBIT -> {
        list.add(new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types.PatrolIntent());
        list.add(new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types.HoldIntent());
        list.add(new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types.SweepSearchIntent());
      }
      case GUARD -> {
        list.add(new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types.GuardIntent());
        list.add(new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types.InterceptIntent());
        list.add(new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types.DecoyIntent());
        list.add(new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types.KitingIntent());
      }
      case HUNT -> {
        list.add(new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types.FocusFireIntent());
        list.add(new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types.AssassinIntent());
        list.add(new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types.DuelIntent());
        list.add(new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types.BreakerIntent());
        list.add(new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types.SuppressIntent());
        list.add(new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types.ShepherdIntent());
        list.add(new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types.SweepIntent());
        list.add(new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types.KitingIntent());
        list.add(new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types.DecoyIntent());
        list.add(new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types.PivotIntent());
        list.add(new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types.SweepSearchIntent());
      }
      case HOVER -> {
        list.add(new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types.HoldIntent());
        list.add(new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types.PatrolIntent());
      }
      case RECALL -> {
        list.add(new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types.RecallIntent());
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
