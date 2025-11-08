package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ;

/**
 * 碎刃蛊持久化状态键名。
 *
 * <p>所有运行时状态存储在器官 NBT 的 "SuiRenGu" 根键下。
 */
public final class SuiRenGuState {

  private SuiRenGuState() {}

  /** OrganState 根键。*/
  public static final String ROOT = "SuiRenGu";

  /** 下次可用时间（long tick）。*/
  public static final String KEY_READY_TICK = "ReadyTick";

  /** Buff 结束时间（long tick）。*/
  public static final String KEY_BUFF_END_AT_TICK = "BuffEndAtTick";

  /** 当前已应用的道痕增幅值（int），用于 buff 结束时回滚。*/
  public static final String KEY_BUFF_APPLIED_DELTA = "BuffAppliedDelta";
}
