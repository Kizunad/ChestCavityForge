package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_shi.calculator;

import java.util.List;
import java.util.Locale;

/** 纯逻辑工具类：统计流派、计算召唤强化参数，便于测试。 */
public final class YuShiSummonComboLogic {

  private static final int MAX_FLOW_COUNT = 10;

  private YuShiSummonComboLogic() {}

  public static FlowStats computeFlowStats(List<List<String>> flows) {
    if (flows == null || flows.isEmpty()) {
      return new FlowStats(0, 0);
    }
    int water = 0;
    int slave = 0;
    for (List<String> entry : flows) {
      if (entry == null || entry.isEmpty()) {
        continue;
      }
      boolean countedWater = false;
      boolean countedSlave = false;
      for (String raw : entry) {
        if (raw == null || raw.isBlank()) {
          continue;
        }
        String lowered = normalizeFlow(raw);
        if (!countedWater && (lowered.contains("水") || lowered.contains("aqua"))) {
          water++;
          countedWater = true;
        }
        if (!countedSlave && (lowered.contains("奴") || lowered.contains("servant"))) {
          slave++;
          countedSlave = true;
        }
      }
    }
    return new FlowStats(Math.min(MAX_FLOW_COUNT, water), Math.min(MAX_FLOW_COUNT, slave));
  }

  public static SummonModifiers computeModifiers(FlowStats stats) {
    int water = Math.max(0, stats.waterCount());
    int slave = Math.max(0, stats.slaveCount());

    double healthMultiplier = 1.0 + Math.min(0.30, water * 0.05); // 最多 +30%
    double speedMultiplier = 1.0 + Math.min(0.20, water * 0.03);
    int regenDuration = water > 0 ? 40 + water * 10 : 0; // 2s 基础 + 每层0.5s
    int regenAmplifier = water >= 6 ? 1 : 0;

    int resistanceDuration = slave > 0 ? 60 + slave * 10 : 0;
    int resistanceAmplifier = slave >= 5 ? 1 : 0;
    int ttlBonus = slave * 20; // 每层延长1秒

    return new SummonModifiers(
        healthMultiplier,
        speedMultiplier,
        regenDuration,
        regenAmplifier,
        resistanceDuration,
        resistanceAmplifier,
        ttlBonus);
  }

  private static String normalizeFlow(String raw) {
    String lowered = raw.toLowerCase(Locale.ROOT).trim();
    if (lowered.endsWith("道")) {
      lowered = lowered.substring(0, lowered.length() - 1);
    }
    return lowered;
  }

  public record FlowStats(int waterCount, int slaveCount) {}

  public record SummonModifiers(
      double healthMultiplier,
      double speedMultiplier,
      int regenDurationTicks,
      int regenAmplifier,
      int resistanceDurationTicks,
      int resistanceAmplifier,
      int ttlBonusTicks) {}
}
