package net.tigereye.chestcavity.compat.guzhenren.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

/**
 * 通用道痕计算基类。
 *
 * <p>提供可扩展的道痕计算框架,允许注册多个道痕提供器(provider),最终汇总计算总道痕值。
 *
 * <p>每个道应该创建自己的子类,在静态初始化块或构造函数中注册相关器官的道痕提供器。
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * public final class FengDaoDaohenOps extends DaohenCalculator {
 *   private static final FengDaoDaohenOps INSTANCE = new FengDaoDaohenOps();
 *
 *   private FengDaoDaohenOps() {
 *     // 注册来自风系器官的道痕
 *     registerProvider(cc -> calculateDaohen(
 *         cc.getOrganScore(CCOrganScores.FENG_ORGAN_COUNT),
 *         1.5  // 每个器官提供1.5道痕
 *     ));
 *
 *     // 可以注册多个提供器
 *     registerProvider(cc -> calculateDaohen(
 *         cc.getOrganScore(CCOrganScores.FENG_ELITE_ORGAN_COUNT),
 *         3.0  // 精英器官提供更多道痕
 *     ));
 *   }
 *
 *   public static double compute(ChestCavityInstance cc) {
 *     return INSTANCE.compute(cc);
 *   }
 * }
 * }</pre>
 *
 * <p>道痕值将用于技能效果增幅、伤害加成等计算。
 */
public abstract class DaohenCalculator {

  private final List<Function<ChestCavityInstance, Double>> providers = new ArrayList<>();

  /**
   * 注册道痕提供器。
   *
   * <p>提供器是一个函数,接收胸腔实例,返回该来源的道痕值。
   * 可以多次调用此方法注册多个提供器,最终道痕值为所有提供器返回值之和。
   *
   * @param provider 从胸腔实例计算道痕的函数,如果为null则忽略
   */
  protected void registerProvider(Function<ChestCavityInstance, Double> provider) {
    if (provider != null) {
      providers.add(provider);
    }
  }

  /**
   * 计算总道痕值。
   *
   * <p>遍历所有已注册的提供器,汇总计算总道痕值。
   * 如果某个提供器抛出异常,该提供器的贡献值视为0.0,不影响其他提供器。
   *
   * @param cc 胸腔实例,如果为null则返回0.0
   * @return 道痕总值,所有提供器返回值之和
   */
  public double compute(ChestCavityInstance cc) {
    if (cc == null) {
      return 0.0;
    }

    return providers.stream()
        .mapToDouble(
            provider -> {
              try {
                Double result = provider.apply(cc);
                return result != null ? result : 0.0;
              } catch (Exception e) {
                // 静默处理异常,避免单个提供器错误影响整体
                return 0.0;
              }
            })
        .sum();
  }

  /**
   * 根据器官数量和加成倍率计算道痕。
   *
   * <p>这是一个便捷方法,用于简化道痕计算。
   *
   * @param organCount 器官数量
   * @param multiplier 道痕倍率(每个器官提供的道痕值)
   * @return 道痕值 = organCount * multiplier
   */
  protected static double calculateDaohen(int organCount, double multiplier) {
    return organCount * multiplier;
  }

  /**
   * 根据器官分数和加成倍率计算道痕。
   *
   * <p>与 {@link #calculateDaohen(int, double)} 类似,但接受浮点数的器官分数。
   *
   * @param organScore 器官分数(可以是小数)
   * @param multiplier 道痕倍率
   * @return 道痕值 = organScore * multiplier
   */
  protected static double calculateDaohen(float organScore, double multiplier) {
    return organScore * multiplier;
  }

  /**
   * 获取已注册的提供器数量。
   *
   * @return 提供器数量,主要用于调试和测试
   */
  protected int getProviderCount() {
    return providers.size();
  }
}

