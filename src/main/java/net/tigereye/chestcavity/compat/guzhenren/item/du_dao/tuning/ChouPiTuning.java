package net.tigereye.chestcavity.compat.guzhenren.item.du_dao.tuning;

import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;

/**
 * 臭屁蛊（Du 道）参数集。
 *
 * - 数值全部集中于此，支持 BehaviorConfigAccess 覆盖；
 * - 行为类仅引用本处常量，便于调参与测试。
 */
public final class ChouPiTuning {

  private ChouPiTuning() {}

  // 触发概率
  public static final float FOOD_TRIGGER_BASE_CHANCE =
      BehaviorConfigAccess.getFloat(ChouPiTuning.class, "FOOD_TRIGGER_BASE_CHANCE", 0.30f);
  public static final float ROTTEN_FOOD_MULTIPLIER =
      BehaviorConfigAccess.getFloat(ChouPiTuning.class, "ROTTEN_FOOD_MULTIPLIER", 2.0f);
  public static final float DAMAGE_TRIGGER_BASE_CHANCE =
      BehaviorConfigAccess.getFloat(ChouPiTuning.class, "DAMAGE_TRIGGER_BASE_CHANCE", 0.20f);
  public static final float SELF_DEBUFF_CHANCE =
      BehaviorConfigAccess.getFloat(ChouPiTuning.class, "SELF_DEBUFF_CHANCE", 0.10f);
  public static final float ATTRACT_CHANCE =
      BehaviorConfigAccess.getFloat(ChouPiTuning.class, "ATTRACT_CHANCE", 0.01f);

  // 节奏与间隔
  public static final int RANDOM_INTERVAL_MIN_TICKS =
      BehaviorConfigAccess.getInt(ChouPiTuning.class, "RANDOM_INTERVAL_MIN_TICKS", 100);
  public static final int RANDOM_INTERVAL_MAX_TICKS =
      BehaviorConfigAccess.getInt(ChouPiTuning.class, "RANDOM_INTERVAL_MAX_TICKS", 400);
  public static final int SLOW_TICK_STEP =
      BehaviorConfigAccess.getInt(ChouPiTuning.class, "SLOW_TICK_STEP", 20);

  // 效果范围/行为
  public static final float EFFECT_RADIUS =
      BehaviorConfigAccess.getFloat(ChouPiTuning.class, "EFFECT_RADIUS", 3.0f);
  public static final float PANIC_DISTANCE =
      BehaviorConfigAccess.getFloat(ChouPiTuning.class, "PANIC_DISTANCE", 6.0f);

  // 粒子表现
  public static final float PARTICLE_BACK_OFFSET =
      BehaviorConfigAccess.getFloat(ChouPiTuning.class, "PARTICLE_BACK_OFFSET", 0.8f);
  public static final float PARTICLE_VERTICAL_OFFSET =
      BehaviorConfigAccess.getFloat(ChouPiTuning.class, "PARTICLE_VERTICAL_OFFSET", 0.1f);
  public static final int PARTICLE_SMOKE_COUNT =
    BehaviorConfigAccess.getInt(ChouPiTuning.class, "PARTICLE_SMOKE_COUNT", 18);
  public static final int PARTICLE_SNEEZE_COUNT =
    BehaviorConfigAccess.getInt(ChouPiTuning.class, "PARTICLE_SNEEZE_COUNT", 10);

  // FX/提示开关
  public static final boolean FX_SOUND_ENABLED =
      BehaviorConfigAccess.getBoolean(ChouPiTuning.class, "FX_SOUND_ENABLED", true);
  public static final boolean FX_PARTICLE_ENABLED =
      BehaviorConfigAccess.getBoolean(ChouPiTuning.class, "FX_PARTICLE_ENABLED", true);
  public static final boolean MESSAGE_SELF_ENABLED =
      BehaviorConfigAccess.getBoolean(ChouPiTuning.class, "MESSAGE_SELF_ENABLED", true);
  public static final boolean MESSAGE_BROADCAST_ENABLED =
      BehaviorConfigAccess.getBoolean(ChouPiTuning.class, "MESSAGE_BROADCAST_ENABLED", true);

  // 残留域（复用腐蚀残留）
  public static final float RESIDUE_RADIUS_MIN =
      BehaviorConfigAccess.getFloat(ChouPiTuning.class, "RESIDUE_RADIUS_MIN", 1.2f);
  public static final float RESIDUE_RADIUS_SCALE =
      BehaviorConfigAccess.getFloat(ChouPiTuning.class, "RESIDUE_RADIUS_SCALE", 0.6f);
  public static final int RESIDUE_MIN_DURATION_TICKS =
      BehaviorConfigAccess.getInt(ChouPiTuning.class, "RESIDUE_MIN_DURATION_TICKS", 40);

  // 伤害/药水持续：按堆叠数算
  public static final int DURATION_PER_STACK_TICKS =
      BehaviorConfigAccess.getInt(ChouPiTuning.class, "DURATION_PER_STACK_TICKS", 40);
  public static final int MIN_EFFECT_DURATION_TICKS =
      BehaviorConfigAccess.getInt(ChouPiTuning.class, "MIN_EFFECT_DURATION_TICKS", 20);
}
