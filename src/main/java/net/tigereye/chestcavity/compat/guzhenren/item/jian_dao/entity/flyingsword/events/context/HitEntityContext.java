package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events.context;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;

/**
 * 飞剑命中实体事件上下文
 */
public class HitEntityContext {
  public final FlyingSwordEntity sword;
  public final ServerLevel level;
  public final Player owner;
  public final LivingEntity target;
  public final DamageSource damageSource;

  /** 当前速度 */
  public final double speed;

  /** 计算出的伤害（可修改） */
  public double damage;

  /** 是否取消本次攻击 */
  public boolean cancelled = false;

  /** 攻击成功后是否跳过经验获取 */
  public boolean skipExp = false;

  /** 攻击成功后是否跳过耐久损耗 */
  public boolean skipDurability = false;

  public HitEntityContext(
      FlyingSwordEntity sword,
      ServerLevel level,
      Player owner,
      LivingEntity target,
      DamageSource damageSource,
      double speed,
      double damage) {
    this.sword = sword;
    this.level = level;
    this.owner = owner;
    this.target = target;
    this.damageSource = damageSource;
    this.speed = speed;
    this.damage = damage;
  }
}
