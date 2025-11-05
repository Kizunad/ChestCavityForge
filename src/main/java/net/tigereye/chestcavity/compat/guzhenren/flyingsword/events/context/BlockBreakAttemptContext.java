package net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;

/**
 * 破块尝试事件上下文
 *
 * <p>Phase 3: 在飞剑尝试破坏方块之前触发（在BlockBreak之前）。
 *
 * <p>区别于BlockBreak事件：
 * <ul>
 *   <li>BlockBreakAttempt: 破块之前，可以阻止破块</li>
 *   <li>BlockBreak: 破块之后，只读上下文</li>
 * </ul>
 *
 * <p>用途：
 * <ul>
 *   <li>权限检查（如保护区、领地系统）</li>
 *   <li>特殊方块逻辑（如黑名单、白名单）</li>
 *   <li>修改破块速度、概率</li>
 *   <li>触发破块前效果（如粒子预告、警告音效）</li>
 * </ul>
 */
public class BlockBreakAttemptContext {
  public final FlyingSwordEntity sword;
  public final BlockPos pos;
  public final BlockState state;

  /** 是否可以破坏该方块（基于硬度、权限等） */
  public boolean canBreak;

  /** 是否取消本次破块尝试 */
  public boolean cancelled = false;

  public BlockBreakAttemptContext(
      FlyingSwordEntity sword, BlockPos pos, BlockState state, boolean canBreak) {
    this.sword = sword;
    this.pos = pos;
    this.state = state;
    this.canBreak = canBreak;
  }
}
