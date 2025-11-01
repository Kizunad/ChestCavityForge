package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events.context;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;

/**
 * 飞剑破坏方块事件上下文
 */
public class BlockBreakContext {
  public final FlyingSwordEntity sword;
  public final ServerLevel level;
  public final Player owner;
  public final BlockPos pos;
  public final BlockState state;
  public final float hardness;

  /** 当前速度 */
  public final double speed;

  /** 有效镐等级 */
  public final int effectiveToolTier;

  /** 计算出的耐久损耗（可修改） */
  public double durabilityLoss;

  /** 是否跳过耐久损耗 */
  public boolean skipDurability = false;

  /** 破块后的减速量（可修改，0-1之间） */
  public double deceleration;

  public BlockBreakContext(
      FlyingSwordEntity sword,
      ServerLevel level,
      Player owner,
      BlockPos pos,
      BlockState state,
      float hardness,
      double speed,
      int effectiveToolTier,
      double durabilityLoss,
      double deceleration) {
    this.sword = sword;
    this.level = level;
    this.owner = owner;
    this.pos = pos;
    this.state = state;
    this.hardness = hardness;
    this.speed = speed;
    this.effectiveToolTier = effectiveToolTier;
    this.durabilityLoss = durabilityLoss;
    this.deceleration = deceleration;
  }
}
