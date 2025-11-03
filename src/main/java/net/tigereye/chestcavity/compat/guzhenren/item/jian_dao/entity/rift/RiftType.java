package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.rift;

import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.RiftTuning;

/**
 * 裂隙类型
 */
public enum RiftType {
  /** 主裂隙 - 通过主动技能"裂刃空隙"放置 */
  MAJOR(
      RiftTuning.MAJOR_DISPLAY_WIDTH,
      RiftTuning.MAJOR_DISPLAY_HEIGHT,
      RiftTuning.MAJOR_DAMAGE_RADIUS,
      RiftTuning.MAJOR_BASE_DURATION_TICKS,
      RiftTuning.MAJOR_DECAY_INTERVAL_TICKS,
      RiftTuning.MAJOR_INITIAL_DAMAGE_MULTIPLIER,
      RiftTuning.MAJOR_DECAY_STEP,
      RiftTuning.MAJOR_MIN_DAMAGE_MULTIPLIER,
      RiftTuning.MAJOR_CAN_ABSORB
      ),

  /** 微型裂隙 - 通过被动效果或飞剑放置 */
  MINOR(
      RiftTuning.MINOR_DISPLAY_WIDTH,
      RiftTuning.MINOR_DISPLAY_HEIGHT,
      RiftTuning.MINOR_DAMAGE_RADIUS,
      RiftTuning.MINOR_BASE_DURATION_TICKS,
      0,
      RiftTuning.MINOR_INITIAL_DAMAGE_MULTIPLIER,
      RiftTuning.MINOR_DECAY_STEP,
      RiftTuning.MINOR_MIN_DAMAGE_MULTIPLIER,
      RiftTuning.MINOR_CAN_ABSORB
      );

  /** 显示宽度（格） */
  public final float displayWidth;
  /** 显示高度（格） */
  public final float displayHeight;
  /** 伤害范围（格） */
  public final double damageRadius;
  /** 基础持续时间（tick） */
  public final int baseDuration;
  /** 衰减间隔（tick，0表示不衰减） */
  public final int decayInterval;
  /** 初始伤害倍率（开始时的倍率） */
  public final double initialDamageMultiplier;
  /** 每次衰减的步进（从当前倍率减去该值） */
  public final double decayStep;
  /** 最低伤害倍率 */
  public final double minDamageMultiplier;
  /** 是否可以吸纳其他裂隙 */
  public final boolean canAbsorb;

  RiftType(
      float displayWidth,
      float displayHeight,
      double damageRadius,
      int baseDuration,
      int decayInterval,
      double initialDamageMultiplier,
      double decayStep,
      double minDamageMultiplier,
      boolean canAbsorb) {
    this.displayWidth = displayWidth;
    this.displayHeight = displayHeight;
    this.damageRadius = damageRadius;
    this.baseDuration = baseDuration;
    this.decayInterval = decayInterval;
    this.initialDamageMultiplier = initialDamageMultiplier;
    this.decayStep = decayStep;
    this.minDamageMultiplier = minDamageMultiplier;
    this.canAbsorb = canAbsorb;
  }
}
