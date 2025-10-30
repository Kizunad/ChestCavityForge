package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_shi.calculator;

import java.util.List;
import java.util.Locale;

/** 纯逻辑工具类：统计流派、计算召唤强化参数，便于测试。 */
public final class YuShiSummonComboLogic {

  private static final int MAX_FLOW_COUNT = 10;

  private YuShiSummonComboLogic() {}

  /**
   * 根据玩家的流派信息计算水道和奴道派别的统计数据。
   *
   * @param flows 玩家流派信息的列表，每个元素是一个字符串列表表示一组流派
   * @return 包含水道和奴道计数结果的 FlowStats 对象
   */
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
        if (!countedWater && containsAny(lowered, "shui", "水", "aqua")) {
          water++;
          countedWater = true;
        }
        if (!countedSlave && containsAny(lowered, "nu", "奴", "servant")) {
          slave++;
          countedSlave = true;
        }
      }
    }
    return new FlowStats(Math.min(MAX_FLOW_COUNT, water), Math.min(MAX_FLOW_COUNT, slave));
  }

  /**
   * 根据流派统计数据计算召唤鲨鱼的强化修改器。
   *
   * @param stats 流派统计数据，包含水道和奴道计数
   * @return 包含各种强化参数的 SummonModifiers 对象
   */
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
    return lowered.replace('_', ' ');
  }

  private static boolean containsAny(String value, String... keywords) {
    if (value == null || value.isBlank() || keywords == null || keywords.length == 0) {
      return false;
    }
    for (String keyword : keywords) {
      if (keyword == null || keyword.isBlank()) {
        continue;
      }
      if (value.contains(keyword)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 记录流派统计数据的记录类。
   * <p>
   * 包含水道和奴道的数量计数，用于计算召唤修改器。
   * </p>
   *
   * @param waterCount 水道流派的数量
   * @param slaveCount 奴道流派的数量
   */
  public record FlowStats(int waterCount, int slaveCount) {}

  /**
   * 记录召唤鲨鱼强化修改器的记录类。
   * <p>
   * 包含生命值倍率、速度倍率、再生效果、伤害抗性以及生存时间加成信息。
   * </p>
   *
   * @param healthMultiplier 生命值倍率
   * @param speedMultiplier 速度倍率
   * @param regenDurationTicks 再生效果持续时间（ticks）
   * @param regenAmplifier 再生效果强度
   * @param resistanceDurationTicks 伤害抗性持续时间（ticks）
   * @param resistanceAmplifier 伤害抗强度
   * @param ttlBonusTicks 生存时间加成（ticks）
   */
  public record SummonModifiers(
      double healthMultiplier,
      double speedMultiplier,
      int regenDurationTicks,
      int regenAmplifier,
      int resistanceDurationTicks,
      int resistanceAmplifier,
      int ttlBonusTicks) {}
}
