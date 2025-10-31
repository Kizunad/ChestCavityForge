package net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.tuning;

import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;

/** 风道·清风轮 脱耦调参。所有行为数值从这里读取，便于服主覆写和单测稳定。 */
public final class FengTuning {
  private FengTuning() {}

  // 叠层/移动与闪避
  public static final double MOVE_EPSILON =
      BehaviorConfigAccess.getFloat(FengTuning.class, "MOVE_EPSILON", 1.0e-3f);
  public static final int WIND_STACK_MAX =
      BehaviorConfigAccess.getInt(FengTuning.class, "WIND_STACK_MAX", 10);
  public static final double WIND_STACK_SPEED_BONUS_PER_STACK =
      BehaviorConfigAccess.getFloat(
          FengTuning.class, "WIND_STACK_SPEED_BONUS_PER_STACK", 0.02f);
  public static final double WIND_STACK_DODGE_PER_STACK =
      BehaviorConfigAccess.getFloat(FengTuning.class, "WIND_STACK_DODGE_PER_STACK", 0.01f);
  public static final double BASE_DODGE_STAGE3 =
      BehaviorConfigAccess.getFloat(FengTuning.class, "BASE_DODGE_STAGE3", 0.10f);
  public static final int WIND_STACK_IDLE_RESET_TICKS =
      BehaviorConfigAccess.getInt(FengTuning.class, "WIND_STACK_IDLE_RESET_TICKS", 40);

  // 冷却/窗口（仅部分，逐步迁移）
  public static final int WIND_RING_COOLDOWN_TICKS =
      BehaviorConfigAccess.getInt(FengTuning.class, "WIND_RING_COOLDOWN_TICKS", 8 * 20);

  // 冲刺里程累计
  public static final double RUN_SAMPLE_THRESHOLD =
      BehaviorConfigAccess.getFloat(FengTuning.class, "RUN_SAMPLE_THRESHOLD", 0.2f);
  public static final long RUN_FX_INTERVAL_UNITS =
      BehaviorConfigAccess.getInt(FengTuning.class, "RUN_FX_INTERVAL_UNITS", 10_000);

  // 滑翔/下落
  public static final double GLIDE_MAX_FALL_SPEED =
      BehaviorConfigAccess.getFloat(FengTuning.class, "GLIDE_MAX_FALL_SPEED", 0.25f);

  // 被动维护
  public static final int PASSIVE_INTERVAL_TICKS =
      BehaviorConfigAccess.getInt(FengTuning.class, "PASSIVE_INTERVAL_TICKS", 4 * 20);

  // 主动技与表现参数
  public static final double DASH_DISTANCE =
      BehaviorConfigAccess.getFloat(FengTuning.class, "DASH_DISTANCE", 6.0f);
  public static final int DASH_COOLDOWN_TICKS =
      BehaviorConfigAccess.getInt(FengTuning.class, "DASH_COOLDOWN_TICKS", 6 * 20);
  public static final int DASH_WINDOW_TICKS =
      BehaviorConfigAccess.getInt(FengTuning.class, "DASH_WINDOW_TICKS", 5);
  public static final int WIND_SLASH_RANGE =
      BehaviorConfigAccess.getInt(FengTuning.class, "WIND_SLASH_RANGE", 5);
  public static final double WIND_SLASH_DAMAGE =
      BehaviorConfigAccess.getFloat(FengTuning.class, "WIND_SLASH_DAMAGE", 6.0f);
  public static final double DOMAIN_PARTICLE_RADIUS =
      BehaviorConfigAccess.getFloat(FengTuning.class, "DOMAIN_PARTICLE_RADIUS", 4.0f);
  public static final int DOMAIN_DURATION_TICKS =
      BehaviorConfigAccess.getInt(FengTuning.class, "DOMAIN_DURATION_TICKS", 10 * 20);
  public static final int DOMAIN_COOLDOWN_TICKS =
      BehaviorConfigAccess.getInt(FengTuning.class, "DOMAIN_COOLDOWN_TICKS", 45 * 20);
  public static final double WIND_RING_PUSH =
      BehaviorConfigAccess.getFloat(FengTuning.class, "WIND_RING_PUSH", 0.45f);

  // 叠层移速/闪避系数（表现数值）
  public static final double WIND_STACK_SPEED_BONUS_PER_STACK_CFG =
      BehaviorConfigAccess.getFloat(
          FengTuning.class, "WIND_STACK_SPEED_BONUS_PER_STACK", 0.02f);

  // FX/提示开关
  public static final boolean FX_ENABLED =
      BehaviorConfigAccess.getBoolean(FengTuning.class, "FX_ENABLED", true);
  public static final boolean SOUND_ENABLED =
      BehaviorConfigAccess.getBoolean(FengTuning.class, "SOUND_ENABLED", true);
  public static final boolean PARTICLE_ENABLED =
      BehaviorConfigAccess.getBoolean(FengTuning.class, "PARTICLE_ENABLED", true);
  public static final boolean MESSAGES_ENABLED =
      BehaviorConfigAccess.getBoolean(FengTuning.class, "MESSAGES_ENABLED", true);
  public static final boolean TOAST_ENABLED =
      BehaviorConfigAccess.getBoolean(FengTuning.class, "TOAST_ENABLED", true);

  // 真元设计基准（默认：3转1阶段）
  public static final int DESIGN_ZHUANSHU =
      BehaviorConfigAccess.getInt(FengTuning.class, "DESIGN_ZHUANSHU", 3);
  public static final int DESIGN_JIEDUAN =
      BehaviorConfigAccess.getInt(FengTuning.class, "DESIGN_JIEDUAN", 1);

  // 主动技消耗分级
  public static final net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts.Tier DASH_TIER =
      net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts.Tier.SMALL;
  public static final net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts.Tier WIND_SLASH_TIER =
      net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts.Tier.HEAVY;
  public static final net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts.Tier DOMAIN_START_TIER =
      net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts.Tier.HEAVY;
  public static final net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts.Tier DOMAIN_MAINTAIN_TIER =
      net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts.Tier.SMALL;
  public static final net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts.Tier PASSIVE_TIER =
      net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts.Tier.TINY;
}
