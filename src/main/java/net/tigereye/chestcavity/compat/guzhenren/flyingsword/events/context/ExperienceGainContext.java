package net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context;

import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;

/**
 * 经验获取事件上下文
 *
 * <p>Phase 3: 当飞剑获得经验时触发。
 *
 * <p>用途：
 *
 * <ul>
 *   <li>修改经验获取量（如道痕加成、buff效果）
 *   <li>记录经验来源（统计、分析）
 *   <li>触发经验获取特效（粒子、音效）
 *   <li>阻止特定来源的经验（如刷怪塔限制）
 * </ul>
 */
public class ExperienceGainContext {
  public final FlyingSwordEntity sword;

  /** 原始经验值 */
  public final int originalExpAmount;

  /** 经验来源 */
  public final GainSource source;

  /** 最终经验值（可修改） */
  public int finalExpAmount;

  /** 是否取消本次经验获取 */
  public boolean cancelled = false;

  public ExperienceGainContext(FlyingSwordEntity sword, int expAmount, GainSource source) {
    this.sword = sword;
    this.originalExpAmount = expAmount;
    this.source = source;
    this.finalExpAmount = expAmount;
  }

  /** 经验来源枚举 */
  public enum GainSource {
    /** 击杀怪物 */
    KILL_MOB,
    /** 击杀玩家 */
    KILL_PLAYER,
    /** 破坏方块 */
    BREAK_BLOCK,
    /** 玩家手动注入 */
    MANUAL_INJECT,
    /** 道痕被动效果 */
    PASSIVE_EFFECT,
    /** 其他来源 */
    OTHER
  }
}
