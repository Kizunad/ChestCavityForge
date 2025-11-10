package net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context;

import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;

/**
 * 等级提升事件上下文
 *
 * <p>Phase 3: 当飞剑等级提升时触发（经验值突破阈值）。
 *
 * <p>用途：
 *
 * <ul>
 *   <li>触发升级效果（粒子爆发、音效、全体回血）
 *   <li>记录升级历史（统计、成就）
 *   <li>解锁新技能或属性（如新轨迹、新意图）
 *   <li>向玩家发送升级通知消息
 * </ul>
 */
public class LevelUpContext {
  public final FlyingSwordEntity sword;
  public final int oldLevel;
  public final int newLevel;

  /** 是否跳过升级（如封印等级） */
  public boolean cancelled = false;

  public LevelUpContext(FlyingSwordEntity sword, int oldLevel, int newLevel) {
    this.sword = sword;
    this.oldLevel = oldLevel;
    this.newLevel = newLevel;
  }
}
