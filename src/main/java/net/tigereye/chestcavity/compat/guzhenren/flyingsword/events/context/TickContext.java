package net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.AIMode;

/** 飞剑Tick事件上下文 */
public class TickContext {
  public final FlyingSwordEntity sword;
  public final ServerLevel level;
  public final LivingEntity owner;
  public final AIMode currentMode;
  public final int tickCount;

  /** 是否跳过本次tick的AI逻辑 */
  public boolean skipAI = false;

  /** 是否跳过本次tick的破块逻辑 */
  public boolean skipBlockBreak = false;

  /** 是否跳过本次tick的维持消耗检查 */
  public boolean skipUpkeep = false;

  public TickContext(
      FlyingSwordEntity sword,
      ServerLevel level,
      LivingEntity owner,
      AIMode currentMode,
      int tickCount) {
    this.sword = sword;
    this.level = level;
    this.owner = owner;
    this.currentMode = currentMode;
    this.tickCount = tickCount;
  }
}
