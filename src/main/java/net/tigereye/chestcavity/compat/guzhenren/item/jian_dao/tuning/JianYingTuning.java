package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning;

import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;
import net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts.Tier;

/**
 * 剑影蛊（剑道）数值与参数集中定义。
 *
 * <p>注意：真元消耗统一走「设计转/阶段 + Tier」接口，避免直接传入 baseCost。
 */
public final class JianYingTuning {

  private JianYingTuning() {}

  // 设计基准：剑影蛊 3转 1阶段
  public static final int DESIGN_ZHUANSHU = 3;
  public static final int DESIGN_JIEDUAN = 1;

  // 真元消耗：
  // - 被动影袭：极轻（TINY），触发几率与旧版一致
  // - 主动分身：中等（MEDIUM），接近旧版约 5.2 单位的体感
  public static final Tier PASSIVE_ZHENYUAN_TIER = Tier.TINY;
  public static final Tier ACTIVE_ZHENYUAN_TIER = Tier.MEDIUM;

  // 精力消耗（基础值，可经配置覆盖）
  public static final double ACTIVE_JINGLI_COST =
      (double) BehaviorConfigAccess.getFloat(JianYingTuning.class, "ACTIVE_JINGLI_COST", 50.0f);

  // 被动影袭触发几率
  public static final double PASSIVE_TRIGGER_CHANCE =
      (double) BehaviorConfigAccess.getFloat(JianYingTuning.class, "PASSIVE_TRIGGER_CHANCE", 0.20f);

  // 伤害与效率
  public static final float BASE_DAMAGE =
      BehaviorConfigAccess.getFloat(JianYingTuning.class, "BASE_DAMAGE", 230.0f);

  // 被动影袭倍率衰减曲线
  public static final float PASSIVE_INITIAL_MULTIPLIER =
      BehaviorConfigAccess.getFloat(
          JianYingTuning.class, "PASSIVE_INITIAL_MULTIPLIER", 0.40f);
  public static final float PASSIVE_MIN_MULTIPLIER =
      BehaviorConfigAccess.getFloat(JianYingTuning.class, "PASSIVE_MIN_MULTIPLIER", 0.15f);
  public static final float PASSIVE_DECAY_STEP =
      BehaviorConfigAccess.getFloat(JianYingTuning.class, "PASSIVE_DECAY_STEP", 0.05f);
  public static final long PASSIVE_RESET_WINDOW_TICKS = 40L;

  // 分身伤害与冷却
  public static final float CLONE_DAMAGE_RATIO =
      BehaviorConfigAccess.getFloat(JianYingTuning.class, "CLONE_DAMAGE_RATIO", 0.30f);
  public static final int CLONE_DURATION_TICKS =
      BehaviorConfigAccess.getInt(JianYingTuning.class, "CLONE_DURATION_TICKS", 100);
  public static final int CLONE_COOLDOWN_TICKS =
      BehaviorConfigAccess.getInt(JianYingTuning.class, "CLONE_COOLDOWN_TICKS", 600);

  // 残影/剑痕
  public static final int AFTERIMAGE_DELAY_TICKS =
      BehaviorConfigAccess.getInt(JianYingTuning.class, "AFTERIMAGE_DELAY_TICKS", 20);
  public static final int AFTERIMAGE_DURATION_TICKS =
      BehaviorConfigAccess.getInt(JianYingTuning.class, "AFTERIMAGE_DURATION_TICKS", 20);
  public static final float AFTERIMAGE_DAMAGE_RATIO =
      BehaviorConfigAccess.getFloat(JianYingTuning.class, "AFTERIMAGE_DAMAGE_RATIO", 0.25f);
  public static final double AFTERIMAGE_CHANCE =
      (double) BehaviorConfigAccess.getFloat(JianYingTuning.class, "AFTERIMAGE_CHANCE", 0.20f);
  public static final double AFTERIMAGE_RADIUS =
      (double) BehaviorConfigAccess.getFloat(JianYingTuning.class, "AFTERIMAGE_RADIUS", 3.0f);
  public static final int SWORD_SCAR_DURATION_TICKS =
      BehaviorConfigAccess.getInt(JianYingTuning.class, "SWORD_SCAR_DURATION_TICKS", 120);

  // OnHit 冷却（秒伤限制）：默认 8 秒
  public static final int ON_HIT_COOLDOWN_TICKS =
      BehaviorConfigAccess.getInt(JianYingTuning.class, "ON_HIT_COOLDOWN_TICKS", 160);
}
