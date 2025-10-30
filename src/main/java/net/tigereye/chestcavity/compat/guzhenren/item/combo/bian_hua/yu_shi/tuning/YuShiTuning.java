package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_shi.tuning;

import java.util.List;

/** 饵祭召鲨组合技的调参工具类，用于定义常量和计算冷却时间。 */
public final class YuShiTuning {
  private YuShiTuning() {}

  public static final int MAX_SUMMONS = 5;
  public static final int BASE_COOLDOWN_TICKS = 20 * 15;

  public static final List<CooldownTier> FLOW_EXPERIENCE_COOLDOWN =
      List.of(new CooldownTier(10_001.0D, 20 * 5));

  /**
   * 根据总经验值计算冷却时间的 ticks。
   * <p>
   * 冷却时间根据流派经验递减，最低为 1 秒（20 ticks）。
   * </p>
   *
   * @param totalExperience 总经验值
   * @return 计算后的冷却时间 ticks
   */
  public static int computeCooldownTicks(double totalExperience) {
    int result = BASE_COOLDOWN_TICKS;
    double sanitized = Math.max(0.0D, totalExperience);
    for (CooldownTier tier : FLOW_EXPERIENCE_COOLDOWN) {
      double threshold = Math.max(1.0D, tier.threshold());
      double ratio = Math.min(1.0D, sanitized / threshold);
      int candidate =
          (int)
              Math.round(
                  BASE_COOLDOWN_TICKS
                      - (BASE_COOLDOWN_TICKS - tier.minCooldownTicks()) * ratio);
      candidate = Math.max(tier.minCooldownTicks(), candidate);
      result = Math.min(result, candidate);
    }
    return Math.max(20, result); // 至少 1 秒，防止为 0
  }

  /**
   * 记录冷却时间阶层的记录类。
   *
   * @param threshold 经验门槛值
   * @param minCooldownTicks 最小冷却时间 ticks
   */
  public record CooldownTier(double threshold, int minCooldownTicks) {}
}
