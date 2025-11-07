package net.tigereye.chestcavity.engine.fx;

/**
 * FX Track 合并策略枚举。
 *
 * <p>当多个相同 mergeKey 的 Track 提交时，根据策略决定如何处理。
 */
public enum MergeStrategy {
  /** 延长现有 Track 的 TTL。 */
  EXTEND_TTL,

  /** 丢弃新的 Track 请求。 */
  DROP,

  /** 替换现有 Track。 */
  REPLACE
}
