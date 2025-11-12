package net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.tuning;

import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;

/**
 * 水道调参配置类。
 *
 * <p>集中管理水道相关的所有配置参数,便于服主覆写和单元测试。
 */
public final class ShuiTuning {
  private ShuiTuning() {}

  // ==================== 灵涎蛊 (LingXiangu) ====================

  /** 玩家灵涎蛊触发间隔(秒)。 */
  public static final int LING_XIAN_PLAYER_INTERVAL_SECONDS =
      BehaviorConfigAccess.getInt(ShuiTuning.class, "LING_XIAN_PLAYER_INTERVAL_SECONDS", 30);

  /** 非玩家灵涎蛊触发间隔(秒)。 */
  public static final int LING_XIAN_NON_PLAYER_INTERVAL_SECONDS =
      BehaviorConfigAccess.getInt(
          ShuiTuning.class,
          "LING_XIAN_NON_PLAYER_INTERVAL_SECONDS",
          LING_XIAN_PLAYER_INTERVAL_SECONDS * 2);

  /** 灵涎蛊基础普通真元消耗。 */
  public static final double LING_XIAN_BASE_NORMAL_ZHENYUAN_COST = 30.0;

  /** 灵涎蛊基础应激真元消耗。 */
  public static final double LING_XIAN_BASE_STRESS_ZHENYUAN_COST = 60.0;

  /** 灵涎蛊基础普通治疗量。 */
  public static final float LING_XIAN_BASE_NORMAL_HEAL =
      BehaviorConfigAccess.getFloat(ShuiTuning.class, "LING_XIAN_BASE_NORMAL_HEAL", 10.0f);

  /** 灵涎蛊基础应激治疗量。 */
  public static final float LING_XIAN_BASE_STRESS_HEAL =
      BehaviorConfigAccess.getFloat(ShuiTuning.class, "LING_XIAN_BASE_STRESS_HEAL", 20.0f);

  /** 灵涎蛊应激触发血量阈值比例。 */
  public static final float LING_XIAN_STRESS_THRESHOLD_RATIO =
      BehaviorConfigAccess.getFloat(ShuiTuning.class, "LING_XIAN_STRESS_THRESHOLD_RATIO", 0.30f);

  /** 灵涎蛊虚弱效果持续时间(ticks)。 */
  public static final int LING_XIAN_WEAKNESS_DURATION_TICKS =
      BehaviorConfigAccess.getInt(ShuiTuning.class, "LING_XIAN_WEAKNESS_DURATION_TICKS", 5 * 20);

  /** 灵涎蛊玩家应激虚弱等级。 */
  public static final int LING_XIAN_PLAYER_STRESS_AMPLIFIER =
      BehaviorConfigAccess.getInt(ShuiTuning.class, "LING_XIAN_PLAYER_STRESS_AMPLIFIER", 0);

  /** 灵涎蛊非玩家应激虚弱等级。 */
  public static final int LING_XIAN_NON_PLAYER_STRESS_AMPLIFIER =
      BehaviorConfigAccess.getInt(ShuiTuning.class, "LING_XIAN_NON_PLAYER_STRESS_AMPLIFIER", 2);

  // ==================== 水神蛊 (Shuishengu) ====================

  /** 水神蛊基础最大充能。 */
  public static final int SHUI_SHEN_BASE_MAX_CHARGE = 20;

  /** 水神蛊伤害减免。 */
  public static final float SHUI_SHEN_DAMAGE_REDUCTION = 40.0f;

  /** 水神蛊护盾曲线系数。 */
  public static final double SHUI_SHEN_SHIELD_ALPHA = 3.0;

  // ==================== 水体蛊 (ShuiTiGu) ====================

  /** 水体蛊水下呼吸检查间隔(ticks)。 */
  public static final int SHUI_TI_CHECK_INTERVAL_TICKS =
      BehaviorConfigAccess.getInt(ShuiTuning.class, "SHUI_TI_CHECK_INTERVAL_TICKS", 10);

  /** 水体蛊每tick提供的空气值。 */
  public static final int SHUI_TI_AIR_PER_TICK =
      BehaviorConfigAccess.getInt(ShuiTuning.class, "SHUI_TI_AIR_PER_TICK", 4);

  // ==================== 节泽蛊 (Jiezegu) ====================

  /** 节泽蛊检查间隔(ticks)。 */
  public static final int JIEZE_CHECK_INTERVAL_TICKS =
      BehaviorConfigAccess.getInt(ShuiTuning.class, "JIEZE_CHECK_INTERVAL_TICKS", 20);

  /** 节泽蛊水中移速加成(每个器官)。 */
  public static final float JIEZE_WATER_SPEED_BONUS =
      BehaviorConfigAccess.getFloat(ShuiTuning.class, "JIEZE_WATER_SPEED_BONUS", 0.15f);

  /** 节泽蛊反击伤害倍率。 */
  public static final float JIEZE_COUNTER_DAMAGE_MULTIPLIER =
      BehaviorConfigAccess.getFloat(ShuiTuning.class, "JIEZE_COUNTER_DAMAGE_MULTIPLIER", 0.5f);

  // ==================== 泉涌冥蛊 (QuanYongMingGu) ====================

  /** 泉涌冥蛊冷却时间(ticks)。 */
  public static final int QUAN_YONG_COOLDOWN_TICKS =
      BehaviorConfigAccess.getInt(ShuiTuning.class, "QUAN_YONG_COOLDOWN_TICKS", 20 * 20);

  /** 泉涌冥蛊每层治疗量。 */
  public static final float QUAN_YONG_HEAL_PER_STACK =
      BehaviorConfigAccess.getFloat(ShuiTuning.class, "QUAN_YONG_HEAL_PER_STACK", 2.0f);

  // ==================== FX/提示开关 ====================

  /** 特效总开关。 */
  public static final boolean FX_ENABLED =
      BehaviorConfigAccess.getBoolean(ShuiTuning.class, "FX_ENABLED", true);

  /** 音效开关。 */
  public static final boolean SOUND_ENABLED =
      BehaviorConfigAccess.getBoolean(ShuiTuning.class, "SOUND_ENABLED", true);

  /** 粒子特效开关。 */
  public static final boolean PARTICLE_ENABLED =
      BehaviorConfigAccess.getBoolean(ShuiTuning.class, "PARTICLE_ENABLED", true);

  /** 消息提示开关。 */
  public static final boolean MESSAGES_ENABLED =
      BehaviorConfigAccess.getBoolean(ShuiTuning.class, "MESSAGES_ENABLED", true);

  /** Toast提示开关。 */
  public static final boolean TOAST_ENABLED =
      BehaviorConfigAccess.getBoolean(ShuiTuning.class, "TOAST_ENABLED", true);
}
