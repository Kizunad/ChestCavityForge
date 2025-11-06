package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx;

/**
 * 剑疗蛊特效与音效调参配置。
 *
 * <p>统一管理所有特效和音效的开关、音量、音调等参数。
 */
public final class JianLiaoGuFxTuning {

  private JianLiaoGuFxTuning() {}

  // ==================== 总开关 ====================

  /** 是否启用特效（粒子效果）。 */
  public static final boolean FX_ENABLED = true;

  /** 是否启用音效。 */
  public static final boolean SOUND_ENABLED = true;

  // ==================== 主动技能：剑血互济 ====================

  /** 主动技能激活音效音量（生命消耗）。 */
  public static final float SOUND_VOLUME_ACTIVE_CONSUME = 0.5f;

  /** 主动技能激活音效音调（生命消耗）。 */
  public static final float SOUND_PITCH_ACTIVE_CONSUME = 0.9f;

  /** 主动技能完成音效音量（修复完成）。 */
  public static final float SOUND_VOLUME_ACTIVE_COMPLETE = 0.7f;

  /** 主动技能完成音效音调（修复完成）。 */
  public static final float SOUND_PITCH_ACTIVE_COMPLETE = 1.2f;

  // ==================== 被动技能1：心跳治疗 ====================

  /** 心跳音效音量。 */
  public static final float SOUND_VOLUME_HEARTBEAT = 0.25f;

  /** 心跳音效音调。 */
  public static final float SOUND_PITCH_HEARTBEAT = 1.8f;

  /**
   * 心跳音效触发概率（0.0-1.0）。
   *
   * <p>由于心跳每2秒触发一次，为避免音效过于频繁，设置较低的触发概率。
   */
  public static final double SOUND_CHANCE_HEARTBEAT = 0.3;

  // ==================== 被动技能2：飞剑互补修复 ====================

  /** 飞剑互补修复音效音量。 */
  public static final float SOUND_VOLUME_SWORD_REPAIR = 0.5f;

  /** 飞剑互补修复音效音调。 */
  public static final float SOUND_PITCH_SWORD_REPAIR = 1.4f;
}
