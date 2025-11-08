package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ;

/**
 * 剑梭蛊器官状态键定义。
 *
 * <p>所有状态键均以 {@link #ROOT} 为根路径，通过 {@link net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown}
 * 和 {@link net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps} 访问。
 */
public final class JianSuoGuState {

  private JianSuoGuState() {}

  /** 状态根路径（NBT 根键）。*/
  public static final String ROOT = "JianSuoGu";

  /** 主动技能冷却（ready tick）。*/
  public static final String KEY_READY_TICK = "active_ready_tick";

  /** 被动躲避冷却（ready tick）。*/
  public static final String KEY_EVADE_READY_TICK = "evade_ready_tick";
}
