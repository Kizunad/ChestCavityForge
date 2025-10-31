package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning;

import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;

/**
 * 飞剑基础参数配置
 * 所有数值都通过BehaviorConfigAccess读取，支持服主覆写
 */
public final class FlyingSwordTuning {
  private FlyingSwordTuning() {}

  // ========== 基础属性 ==========
  /** 基础速度（方块/tick） */
  public static final double SPEED_BASE =
      config("SPEED_BASE", 0.18);

  /** 最大速度（方块/tick） */
  public static final double SPEED_MAX =
      config("SPEED_MAX", 0.42);

  /** 加速度 */
  public static final double ACCEL =
      config("ACCEL", 0.015);

  /** 转向速率 */
  public static final double TURN_RATE =
      config("TURN_RATE", 0.14);

  // ========== 伤害属性 ==========
  /** 基础伤害 */
  public static final double DAMAGE_BASE =
      config("DAMAGE_BASE", 4.0);

  /** 速度²伤害系数 */
  public static final double VEL_DMG_COEF =
      config("VEL_DMG_COEF", 1.0);

  /** 参考速度（用于速度²计算） */
  public static final double V_REF =
      config("V_REF", 0.35);

  // ========== 耐久属性 ==========
  /** 最大耐久 */
  public static final double MAX_DURABILITY =
      config("MAX_DURABILITY", 1000.0);

  /** 耐久损耗比例 */
  public static final double DURA_LOSS_RATIO =
      config("DURA_LOSS_RATIO", 0.1);

  /** 破块时耐久损耗倍率 */
  public static final double DURA_BREAK_MULT =
      config("DURA_BREAK_MULT", 2.0);

  // ========== 维持消耗 ==========
  /** 基础维持消耗率 */
  public static final double UPKEEP_BASE_RATE =
      config("UPKEEP_BASE_RATE", 1.0);

  /** 环绕模式消耗倍率 */
  public static final double UPKEEP_ORBIT_MULT =
      config("UPKEEP_ORBIT_MULT", 0.6);

  /** 防守模式消耗倍率 */
  public static final double UPKEEP_GUARD_MULT =
      config("UPKEEP_GUARD_MULT", 1.0);

  /** 出击模式消耗倍率 */
  public static final double UPKEEP_HUNT_MULT =
      config("UPKEEP_HUNT_MULT", 1.4);

  /** 冲刺时消耗倍率 */
  public static final double UPKEEP_SPRINT_MULT =
      config("UPKEEP_SPRINT_MULT", 1.5);

  /** 破块时消耗倍率 */
  public static final double UPKEEP_BREAK_MULT =
      config("UPKEEP_BREAK_MULT", 2.0);

  /** 速度影响维持的缩放系数 */
  public static final double UPKEEP_SPEED_SCALE =
      config("UPKEEP_SPEED_SCALE", 0.5);

  /** 维持检查间隔（tick） */
  public static final int UPKEEP_CHECK_INTERVAL =
      configInt("UPKEEP_CHECK_INTERVAL", 20);

  // ========== 经验成长 ==========
  /** 每点伤害获得的经验 */
  public static final double EXP_PER_DAMAGE =
      config("EXP_PER_DAMAGE", 2.0);

  /** 击杀经验倍率 */
  public static final int EXP_KILL_MULT =
      configInt("EXP_KILL_MULT", 5);

  /** 精英经验倍率 */
  public static final int EXP_ELITE_MULT =
      configInt("EXP_ELITE_MULT", 2);

  /** 经验曲线基数 */
  public static final double EXP_BASE =
      config("EXP_BASE", 40.0);

  /** 经验曲线指数 */
  public static final double EXP_ALPHA =
      config("EXP_ALPHA", 1.5);

  /** 最大等级 */
  public static final int MAX_LEVEL =
      configInt("MAX_LEVEL", 30);

  /** 每级伤害成长 */
  public static final double DAMAGE_PER_LEVEL =
      config("DAMAGE_PER_LEVEL", 0.6);

  // ========== 释放继承 ==========
  /** 继承伤害下限 */
  public static final double INHERIT_DMG_MIN =
      config("INHERIT_DMG_MIN", -2.0);

  /** 继承伤害上限 */
  public static final double INHERIT_DMG_MAX =
      config("INHERIT_DMG_MAX", 10.0);

  /** 继承速度下限 */
  public static final double INHERIT_SPEED_MIN =
      config("INHERIT_SPEED_MIN", -0.1);

  /** 继承速度上限 */
  public static final double INHERIT_SPEED_MAX =
      config("INHERIT_SPEED_MAX", 0.3);

  /** 锋利附魔每级增加的伤害 */
  public static final double INHERIT_SHARPNESS_DMG =
      config("INHERIT_SHARPNESS_DMG", 0.5);

  /** 锋利附魔每级增加的速度²系数 */
  public static final double INHERIT_SHARPNESS_VEL =
      config("INHERIT_SHARPNESS_VEL", 0.03);

  /** 攻击伤害转换系数 */
  public static final double INHERIT_ATTACK_DAMAGE_COEF =
      config("INHERIT_ATTACK_DAMAGE_COEF", 0.5);

  /** 攻击速度转换系数 */
  public static final double INHERIT_ATTACK_SPEED_COEF =
      config("INHERIT_ATTACK_SPEED_COEF", 0.05);

  // ========== 破块系统 ==========
  /** 是否启用破块 */
  public static final boolean ENABLE_BLOCK_BREAK =
      configBool("ENABLE_BLOCK_BREAK", true);

  /** 破块效率基数 */
  public static final double BLOCK_BREAK_EFF_BASE =
      config("BLOCK_BREAK_EFF_BASE", 0.75);

  // ========== 辅助方法 ==========
  private static double config(String key, double defaultValue) {
    return BehaviorConfigAccess.getFloat(FlyingSwordTuning.class, key, (float) defaultValue);
  }

  private static int configInt(String key, int defaultValue) {
    return BehaviorConfigAccess.getInt(FlyingSwordTuning.class, key, defaultValue);
  }

  private static boolean configBool(String key, boolean defaultValue) {
    return BehaviorConfigAccess.getBoolean(FlyingSwordTuning.class, key, defaultValue);
  }
}
