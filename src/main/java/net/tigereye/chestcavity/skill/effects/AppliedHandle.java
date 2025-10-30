package net.tigereye.chestcavity.skill.effects;

/**
 * 已应用效果的句柄。由 pre 阶段返回，用于 post 阶段按结果清理或安排过期。
 */
public interface AppliedHandle {

  /**
   * 效果应持续的时长（tick）。返回 0 表示只在本次触发过程内生效，post 阶段立即清理。
   */
  default int ttlTicks() {
    return 0;
  }

  /** 便于日志追踪的名称。 */
  default String debugName() {
    return getClass().getSimpleName();
  }

  /** 撤销该效果（用于失败回滚或到期清理）。需保证可幂等。 */
  void revert();
}

