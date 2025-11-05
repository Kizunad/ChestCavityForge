package net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context;

import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.AIMode;

/**
 * 目标锁定事件上下文
 *
 * <p>Phase 3: 当飞剑获取新目标时触发。
 *
 * <p>触发时机：
 * <ul>
 *   <li>GUARD模式自动搜索到敌对目标</li>
 *   <li>HUNT模式被指定新追击目标</li>
 *   <li>玩家通过命令标记目标</li>
 * </ul>
 *
 * <p>用途：
 * <ul>
 *   <li>记录目标切换历史（分析AI决策）</li>
 *   <li>触发锁定音效、粒子效果</li>
 *   <li>阻止锁定特定目标（友军保护、特权实体）</li>
 *   <li>修改目标优先级（如优先攻击BOSS）</li>
 * </ul>
 */
public class TargetAcquiredContext {
  public final FlyingSwordEntity sword;
  public final LivingEntity target;
  public final AIMode mode;

  /** 是否取消本次目标锁定（清除目标） */
  public boolean cancelled = false;

  public TargetAcquiredContext(FlyingSwordEntity sword, LivingEntity target, AIMode mode) {
    this.sword = sword;
    this.target = target;
    this.mode = mode;
  }
}
