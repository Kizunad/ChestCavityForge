package net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;

/**
 * 飞剑生成事件上下文
 */
public class SpawnContext {
  public final FlyingSwordEntity sword;
  public final ServerLevel level;
  public final LivingEntity owner;
  public final Vec3 spawnPos;
  @Nullable public final ItemStack sourceStack;

  /** 是否取消生成（设为true将移除实体） */
  public boolean cancelled = false;

  public SpawnContext(
      FlyingSwordEntity sword,
      ServerLevel level,
      LivingEntity owner,
      Vec3 spawnPos,
      @Nullable ItemStack sourceStack) {
    this.sword = sword;
    this.level = level;
    this.owner = owner;
    this.spawnPos = spawnPos;
    this.sourceStack = sourceStack;
  }
}
