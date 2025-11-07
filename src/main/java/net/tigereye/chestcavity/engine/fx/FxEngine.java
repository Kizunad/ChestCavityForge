package net.tigereye.chestcavity.engine.fx;

import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.engine.TickEngineHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FxEngine 模块入口：负责初始化 FX 时间线引擎并注册到 {@link TickEngineHub}。
 *
 * <p>调用 {@link #bootstrap()} 来完成引擎注册与初始化。
 *
 * <p>优先级设置为 DOT+15（215），确保在 ShockfieldManager（DOT+10）之后运行，便于采样波场状态。
 */
public final class FxEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(FxEngine.class);

  /** FX 引擎优先级：DOT+15，在 ShockfieldManager 之后运行。 */
  public static final int PRIORITY_FX = TickEngineHub.PRIORITY_DOT + 15;

  private static boolean bootstrapped = false;

  // 运行时配置（全局单例）。
  // 说明：测试会通过 FxEngine.getConfig() 拿到此对象并修改字段，
  // 引擎内部每次读取配置都会返回同一实例，确保测试对配置的修改能生效。
  private static volatile FxEngineConfig RUNTIME_CONFIG;

  private FxEngine() {}

  /**
   * 初始化 FX 引擎并注册到 TickEngineHub。
   *
   * <p>该方法应在模组初始化阶段调用一次。
   */
  public static void bootstrap() {
    if (bootstrapped) {
      LOGGER.warn("[FxEngine] Already bootstrapped, skipping");
      return;
    }

    LOGGER.info("[FxEngine] Bootstrapping FX Timeline Engine...");

    // 注册 FxTimelineEngine 到 TickEngineHub
    TickEngineHub.register(PRIORITY_FX, FxTimelineEngine.getInstance());

    bootstrapped = true;
    LOGGER.info("[FxEngine] FX Timeline Engine registered with priority {}", PRIORITY_FX);
  }

  /**
   * 获取 FxScheduler 单例（门面入口）。
   *
   * @return FxScheduler 实例
   */
  public static FxScheduler scheduler() {
    return FxScheduler.getInstance();
  }

  /**
   * 获取 FxTimelineEngine 单例（引擎核心）。
   *
   * @return FxTimelineEngine 实例
   */
  public static FxTimelineEngine engine() {
    return FxTimelineEngine.getInstance();
  }

  /**
   * 获取 FxRegistry 单例（注册中心）。
   *
   * @return FxRegistry 实例
   */
  public static FxRegistry registry() {
    return FxRegistry.getInstance();
  }

  /**
   * 获取当前 FxEngine 配置（从 CCConfig 读取）。
   *
   * @return FxEngineConfig 实例
   */
  public static FxEngineConfig getConfig() {
    FxEngineConfig cfg = RUNTIME_CONFIG;
    if (cfg != null) {
      return cfg;
    }

    // 首次访问：从 CCConfig 加载到全局单例
    RUNTIME_CONFIG = loadFromCCConfig();
    return RUNTIME_CONFIG;
  }

  /** 重新从 CCConfig 加载配置（运行期或测试可调用）。 */
  public static void reloadConfigFromCC() {
    RUNTIME_CONFIG = loadFromCCConfig();
  }

  private static FxEngineConfig loadFromCCConfig() {
    FxEngineConfig config = new FxEngineConfig();
    try {
      var ccConfig = ChestCavity.config.FX_ENGINE;
      config.enabled = ccConfig.enabled;
      config.budgetEnabled = ccConfig.budgetEnabled;
      config.perLevelCap = ccConfig.perLevelCap;
      config.perOwnerCap = ccConfig.perOwnerCap;
      config.defaultTickInterval = ccConfig.defaultTickInterval;

      try {
        config.defaultMergeStrategy = MergeStrategy.valueOf(ccConfig.defaultMergeStrategy);
      } catch (Exception e) {
        LOGGER.warn(
            "[FxEngine] Invalid merge strategy '{}', using default EXTEND_TTL",
            ccConfig.defaultMergeStrategy);
        config.defaultMergeStrategy = MergeStrategy.EXTEND_TTL;
      }

      config.validate();
    } catch (Exception e) {
      LOGGER.error("[FxEngine] Failed to load config, using defaults", e);
    }
    return config;
  }
}
