package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events.context;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;

/**
 * 飞剑本体受击事件上下文
 */
public class HurtContext {
  public final FlyingSwordEntity sword;
  public final ServerLevel level;
  public final Player owner;
  public final DamageSource source;

  /** 原始伤害（可修改） */
  public float damage;

  /** 是否取消本次伤害 */
  public boolean cancelled = false;

  /** 受击后是否触发折返（反弹回主人） */
  public boolean triggerRetreat = false;

  /** 受击后是否进入虚弱状态（降速/禁破块） */
  public boolean triggerWeakened = false;

  /** 虚弱状态持续时间（ticks） */
  public int weakenedDuration = 60;

  public HurtContext(
      FlyingSwordEntity sword, ServerLevel level, Player owner, DamageSource source, float damage) {
    this.sword = sword;
    this.level = level;
    this.owner = owner;
    this.source = source;
    this.damage = damage;
  }
}
