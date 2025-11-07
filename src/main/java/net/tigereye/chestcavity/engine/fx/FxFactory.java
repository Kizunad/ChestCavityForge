package net.tigereye.chestcavity.engine.fx;

import java.util.function.Function;

/**
 * FX 工厂接口：根据上下文创建 FxTrackSpec。
 *
 * <p>用于 FxRegistry 的 factory 模式，实现"先注册、后处处可调用"的便捷模式。
 *
 * <p>Stage 3 核心接口。
 *
 * @see FxRegistry
 * @see FxContext
 */
@FunctionalInterface
public interface FxFactory extends Function<FxContext, FxTrackSpec> {

  /**
   * 根据上下文创建 FxTrackSpec。
   *
   * @param context FX 上下文
   * @return FxTrackSpec 实例
   */
  @Override
  FxTrackSpec apply(FxContext context);

  /**
   * 创建一个简单的工厂（Lambda 辅助）。
   *
   * @param factory 工厂函数
   * @return FxFactory 实例
   */
  static FxFactory of(Function<FxContext, FxTrackSpec> factory) {
    return factory::apply;
  }
}
