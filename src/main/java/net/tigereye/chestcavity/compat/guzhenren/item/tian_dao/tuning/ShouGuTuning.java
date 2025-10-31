package net.tigereye.chestcavity.compat.guzhenren.item.tian_dao.tuning;

import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;
import net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts;

/**
 * 天道·寿蛊（ShouGu）参数集中定义，仅提取与资源相关的关键常量，保持其余行为不变。
 */
public final class ShouGuTuning {

  private ShouGuTuning() {}

  // 设计基准：3转 1阶段（与剑道保持一致，便于统一标定）
  public static final int DESIGN_ZHUANSHU = 3;
  public static final int DESIGN_JIEDUAN = 1;

  // 主动技资源消耗
  // 旧版为 baseCost=150（传入 consumeScaledZhenyuan 的 base）；
  // 以 3转1阶段折算为单位用量约 150 / 768 ≈ 0.195。
  public static final float ABILITY_ZHENYUAN_UNITS =
      BehaviorConfigAccess.getFloat(ShouGuTuning.class, "ABILITY_ZHENYUAN_UNITS", 0.20f);

  // 便于退款：预先计算 baseCost（供 tryReplenishScaledZhenyuan 使用）
  public static final double ABILITY_ZHENYUAN_BASE_COST =
      ZhenyuanBaseCosts.baseForUnits(DESIGN_ZHUANSHU, DESIGN_JIEDUAN, ABILITY_ZHENYUAN_UNITS);

  // 精力消耗（保持旧版默认 20，可被配置覆盖）
  public static final double ABILITY_JINGLI_COST =
      (double) BehaviorConfigAccess.getFloat(ShouGuTuning.class, "ABILITY_JINGLI_COST", 20.0f);

  // 计息/阈值/治疗等关键参数（可由配置覆盖）
  public static final double BASE_INTEREST_RATE =
      BehaviorConfigAccess.getFloat(ShouGuTuning.class, "BASE_INTEREST_RATE", 0.05f);
  public static final double INTEREST_REDUCTION_PER_MARK =
      BehaviorConfigAccess.getFloat(ShouGuTuning.class, "INTEREST_REDUCTION_PER_MARK", 0.02f);
  public static final double ENVIRONMENT_REDUCTION_PER_MARK =
      BehaviorConfigAccess.getFloat(ShouGuTuning.class, "ENVIRONMENT_REDUCTION_PER_MARK", 0.02f);

  public static final double MARK_SELF_HEAL_PER_SECOND =
      BehaviorConfigAccess.getFloat(ShouGuTuning.class, "MARK_SELF_HEAL_PER_SECOND", 0.5f);
  public static final double MARK_CONSUME_DEBT_REDUCTION =
      BehaviorConfigAccess.getFloat(ShouGuTuning.class, "MARK_CONSUME_DEBT_REDUCTION", 40.0f);
  public static final double MARK_CONSUME_HEAL =
      BehaviorConfigAccess.getFloat(ShouGuTuning.class, "MARK_CONSUME_HEAL", 8.0f);

  public static final double ACTIVE_BASE_HEAL =
      BehaviorConfigAccess.getFloat(ShouGuTuning.class, "ACTIVE_BASE_HEAL", 4.0f);
  public static final double ACTIVE_HEAL_PER_CONSUMED =
      BehaviorConfigAccess.getFloat(ShouGuTuning.class, "ACTIVE_HEAL_PER_CONSUMED", 4.0f);
  public static final int ACTIVE_DURATION_TICKS =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "ACTIVE_DURATION_TICKS", 6 * 20);
  public static final int ACTIVE_HEAL_INTERVAL_TICKS =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "ACTIVE_HEAL_INTERVAL_TICKS", 20);

  public static final long COMBAT_WINDOW_TICKS =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "COMBAT_WINDOW_TICKS", 8 * 20);
  public static final long MARK_INTERVAL_COMBAT_TICKS =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "MARK_INTERVAL_COMBAT_TICKS", 5 * 20);
  public static final long MARK_INTERVAL_OUT_OF_COMBAT_TICKS =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "MARK_INTERVAL_OUT_OF_COMBAT_TICKS", 10 * 20);
  public static final long INTEREST_INTERVAL_TICKS =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "INTEREST_INTERVAL_TICKS", 3 * 20);

  // 偿债速率（每个慢速 tick 结算一次）
  public static final double REPAY_BASE =
      BehaviorConfigAccess.getFloat(ShouGuTuning.class, "REPAY_BASE", 12.0f);
  public static final double REPAY_PER_MARK =
      BehaviorConfigAccess.getFloat(ShouGuTuning.class, "REPAY_PER_MARK", 3.0f);
  public static final double REPAY_JINLIAO_MULTIPLIER =
      BehaviorConfigAccess.getFloat(ShouGuTuning.class, "REPAY_JINLIAO_MULTIPLIER", 0.5f);

  public static final double DEBT_OVERFLOW_WITHER_SECONDS =
      BehaviorConfigAccess.getFloat(ShouGuTuning.class, "DEBT_OVERFLOW_WITHER_SECONDS", 8.0f);
  public static final double REMOVAL_WITHER_SECONDS =
      BehaviorConfigAccess.getFloat(ShouGuTuning.class, "REMOVAL_WITHER_SECONDS", 10.0f);
  public static final double REMOVAL_OVERFLOW_FACTOR =
      BehaviorConfigAccess.getFloat(ShouGuTuning.class, "REMOVAL_OVERFLOW_FACTOR", 1.5f);
  public static final double REMOVAL_DAMAGE_MULTIPLIER =
      BehaviorConfigAccess.getFloat(ShouGuTuning.class, "REMOVAL_DAMAGE_MULTIPLIER", 2.0f);
  public static final int MAX_MARKS_SPENT_PER_CAST =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "MAX_MARKS_SPENT_PER_CAST", 3);

  // 分阶段参数（S1/S2/S3），用于生成 TierParameters。
  public static final int S1_MAX_MARKS =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "S1_MAX_MARKS", 4);
  public static final int S1_BASE_DEBT_THRESHOLD =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "S1_BASE_DEBT_THRESHOLD", 100);
  public static final int S1_DEBT_THRESHOLD_PER_MARK =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "S1_DEBT_THRESHOLD_PER_MARK", 15);
  public static final int S1_GRACE_SHIFT_COOLDOWN_TICKS =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "S1_GRACE_SHIFT_COOLDOWN_TICKS", 55 * 20);
  public static final int S1_ACTIVE_ABILITY_COOLDOWN_TICKS =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "S1_ACTIVE_ABILITY_COOLDOWN_TICKS", 26 * 20);
  public static final double S1_ABILITY_DEFERRED_RATIO =
      BehaviorConfigAccess.getFloat(ShouGuTuning.class, "S1_ABILITY_DEFERRED_RATIO", 0.50f);

  public static final int S2_MAX_MARKS =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "S2_MAX_MARKS", 6);
  public static final int S2_BASE_DEBT_THRESHOLD =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "S2_BASE_DEBT_THRESHOLD", 300);
  public static final int S2_DEBT_THRESHOLD_PER_MARK =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "S2_DEBT_THRESHOLD_PER_MARK", 20);
  public static final int S2_GRACE_SHIFT_COOLDOWN_TICKS =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "S2_GRACE_SHIFT_COOLDOWN_TICKS", 45 * 20);
  public static final int S2_ACTIVE_ABILITY_COOLDOWN_TICKS =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "S2_ACTIVE_ABILITY_COOLDOWN_TICKS", 22 * 20);
  public static final double S2_ABILITY_DEFERRED_RATIO =
      BehaviorConfigAccess.getFloat(ShouGuTuning.class, "S2_ABILITY_DEFERRED_RATIO", 0.60f);

  public static final int S3_MAX_MARKS =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "S3_MAX_MARKS", 7);
  public static final int S3_BASE_DEBT_THRESHOLD =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "S3_BASE_DEBT_THRESHOLD", 500);
  public static final int S3_DEBT_THRESHOLD_PER_MARK =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "S3_DEBT_THRESHOLD_PER_MARK", 20);
  public static final int S3_GRACE_SHIFT_COOLDOWN_TICKS =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "S3_GRACE_SHIFT_COOLDOWN_TICKS", 40 * 20);
  public static final int S3_ACTIVE_ABILITY_COOLDOWN_TICKS =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "S3_ACTIVE_ABILITY_COOLDOWN_TICKS", 20 * 20);
  public static final double S3_ABILITY_DEFERRED_RATIO =
      BehaviorConfigAccess.getFloat(ShouGuTuning.class, "S3_ABILITY_DEFERRED_RATIO", 0.65f);

  // 行为性阈值/倍率
  public static final double FINISH_EXTRA_DEFERRED_RATIO =
      BehaviorConfigAccess.getFloat(ShouGuTuning.class, "FINISH_EXTRA_DEFERRED_RATIO", 0.30f);
  public static final int GRACE_COOLDOWN_REDUCTION_TICKS =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "GRACE_COOLDOWN_REDUCTION_TICKS", 20 * 20);

  // GraceShift 恢复/状态参数
  public static final float GRACE_HEALTH_BASE =
      BehaviorConfigAccess.getFloat(ShouGuTuning.class, "GRACE_HEALTH_BASE", 8.0f);
  public static final float GRACE_HEALTH_ABSORB_SCALE =
      BehaviorConfigAccess.getFloat(ShouGuTuning.class, "GRACE_HEALTH_ABSORB_SCALE", 0.3f);
  public static final int GRACE_INVULN_TICKS =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "GRACE_INVULN_TICKS", 40);

  public static final int GRACE_RESIST_TICKS =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "GRACE_RESIST_TICKS", 40);
  public static final int GRACE_RESIST_AMPLIFIER =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "GRACE_RESIST_AMPLIFIER", 4);
  public static final int GRACE_WEAKNESS_TICKS =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "GRACE_WEAKNESS_TICKS", 60);
  public static final int GRACE_WEAKNESS_AMPLIFIER =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "GRACE_WEAKNESS_AMPLIFIER", 2);
  public static final int GRACE_SLOW_TICKS =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "GRACE_SLOW_TICKS", 60);
  public static final int GRACE_SLOW_AMPLIFIER =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "GRACE_SLOW_AMPLIFIER", 1);
  public static final int GRACE_TAG_DURATION_TICKS =
      BehaviorConfigAccess.getInt(ShouGuTuning.class, "GRACE_TAG_DURATION_TICKS", 60);

  public static final float GRACE_SOUND_VOLUME =
      BehaviorConfigAccess.getFloat(ShouGuTuning.class, "GRACE_SOUND_VOLUME", 0.7f);
  public static final float GRACE_SOUND_PITCH =
      BehaviorConfigAccess.getFloat(ShouGuTuning.class, "GRACE_SOUND_PITCH", 1.2f);
}
