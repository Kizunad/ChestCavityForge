package net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.AIMode;

/**
 * AI模式切换事件上下文
 *
 * <p>Phase 3: 当飞剑的AI模式发生变化时触发。
 *
 * <p>用途：
 *
 * <ul>
 *   <li>记录模式切换历史（统计、调试）
 *   <li>触发模式特定的初始化逻辑（粒子效果、音效）
 *   <li>阻止非法模式切换（如在某些状态下禁止进入HUNT模式）
 *   <li>同步客户端状态（自定义渲染、UI更新）
 * </ul>
 */
public class ModeChangeContext {
  public final FlyingSwordEntity sword;
  public final AIMode oldMode;
  public final AIMode newMode;

  /** 触发模式切换的实体（如目标、玩家），可能为null（自动切换） */
  @Nullable public final LivingEntity trigger;

  /** 是否取消本次模式切换（恢复为oldMode） */
  public boolean cancelled = false;

  public ModeChangeContext(
      FlyingSwordEntity sword, AIMode oldMode, AIMode newMode, @Nullable LivingEntity trigger) {
    this.sword = sword;
    this.oldMode = oldMode;
    this.newMode = newMode;
    this.trigger = trigger;
  }
}
