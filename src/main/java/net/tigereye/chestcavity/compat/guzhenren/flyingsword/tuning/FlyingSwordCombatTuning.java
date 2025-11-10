package net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning;

/** 战斗参数（攻击范围与冷却） */
public final class FlyingSwordCombatTuning {
  private FlyingSwordCombatTuning() {}

  /** 碰撞攻击范围（方块） */
  public static final double ATTACK_RANGE = 3.0;

  /** 攻击冷却（ticks） */
  public static final int ATTACK_COOLDOWN_TICKS = 10;

  /** 调试日志：碰撞检测心跳日志（默认关闭） */
  public static final boolean COMBAT_DEBUG_LOGS = false;
}
