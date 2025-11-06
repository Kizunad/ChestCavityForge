package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ;

/**
 * 持久化键名统一管理，避免散落硬编码。
 */
public final class JianLiaoGuState {

  private JianLiaoGuState() {}

  public static final String ROOT = "JianLiaoGu";

  public static final String KEY_NEXT_HEARTBEAT_TICK = "NextHeartbeatTick";
  public static final String KEY_NEXT_SWORD_REPAIR_TICK = "NextSwordRepairTick";
  public static final String KEY_READY_TICK = "ActiveReadyTick";
}

