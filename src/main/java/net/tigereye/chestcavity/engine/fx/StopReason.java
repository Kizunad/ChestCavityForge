package net.tigereye.chestcavity.engine.fx;

/**
 * FX Track 停止原因枚举。
 */
public enum StopReason {
  /** TTL 到期。 */
  TTL_EXPIRED,

  /** 手动取消。 */
  CANCELLED,

  /** 异常导致停止。 */
  EXCEPTION,

  /** 引擎关闭。 */
  ENGINE_SHUTDOWN,

  /** 门控条件不满足（Owner 死亡/区块未加载等）。 */
  GATING_FAILED
}
