package net.tigereye.chestcavity.engine.fx;

/**
 * FxEngine 配置：控制引擎行为、预算控制、合并策略等。
 *
 * <p>Stage 2 核心配置类。默认所有开关关闭，必要时启用。
 */
public final class FxEngineConfig {

  /** FxEngine 总开关（默认关闭）。 */
  public boolean enabled = false;

  /** 预算控制开关（默认关闭）。 */
  public boolean budgetEnabled = false;

  /** 每 Level 活跃 FX 上限（默认 256）。 */
  public int perLevelCap = 256;

  /** 每 Owner 活跃 FX 上限（默认 16）。 */
  public int perOwnerCap = 16;

  /** 默认合并策略（默认 EXTEND_TTL）。 */
  public MergeStrategy defaultMergeStrategy = MergeStrategy.EXTEND_TTL;

  /** 默认 Tick 间隔（默认 1）。 */
  public int defaultTickInterval = 1;

  /** 创建默认配置（所有开关关闭）。 */
  public static FxEngineConfig createDefault() {
    return new FxEngineConfig();
  }

  /** 验证配置的有效性。 */
  public void validate() {
    if (perLevelCap <= 0) {
      perLevelCap = 256;
    }
    if (perOwnerCap <= 0) {
      perOwnerCap = 16;
    }
    if (defaultTickInterval <= 0) {
      defaultTickInterval = 1;
    }
    if (defaultMergeStrategy == null) {
      defaultMergeStrategy = MergeStrategy.EXTEND_TTL;
    }
  }

  @Override
  public String toString() {
    return "FxEngineConfig{"
        + "enabled="
        + enabled
        + ", budgetEnabled="
        + budgetEnabled
        + ", perLevelCap="
        + perLevelCap
        + ", perOwnerCap="
        + perOwnerCap
        + ", defaultMergeStrategy="
        + defaultMergeStrategy
        + ", defaultTickInterval="
        + defaultTickInterval
        + '}';
  }
}
