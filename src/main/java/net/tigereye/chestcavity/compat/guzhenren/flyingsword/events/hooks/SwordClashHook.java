package net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.hooks;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.FlyingSwordEventHook;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context.HitEntityContext;

/**
 * 飞剑碰撞反推钩子（Sword Clash Hook）
 *
 * <p>实现真实剑斗效果：当飞剑命中另一个飞剑时，双方会被反向推开。
 *
 * <p>特性：
 * <ul>
 *   <li>检测飞剑与飞剑的碰撞</li>
 *   <li>计算反向推力（基于速度和质量）</li>
 *   <li>对双方施加反向加速度</li>
 *   <li>触发粒子和音效</li>
 * </ul>
 */
public class SwordClashHook implements FlyingSwordEventHook {

  /** 反推力倍数（控制推力强度） */
  private static final double REPEL_FORCE_MULT = 0.8;

  /** 最小推力（避免推力过小） */
  private static final double MIN_REPEL_FORCE = 0.3;

  /** 最大推力（避免推力过大） */
  private static final double MAX_REPEL_FORCE = 2.0;

  @Override
  public void onHitEntity(HitEntityContext ctx) {
    LivingEntity target = ctx.target;

    // 检查目标是否是飞剑
    if (!(target instanceof FlyingSwordEntity targetSword)) {
      return;
    }

    FlyingSwordEntity attackerSword = ctx.sword;

    // 检查是否是敌对飞剑（不同主人）
    if (attackerSword.getOwner() == null || targetSword.getOwner() == null) {
      return;
    }

    if (attackerSword.getOwner().getUUID().equals(targetSword.getOwner().getUUID())) {
      // 同一个主人的飞剑不互相反推
      return;
    }

    // 计算碰撞反推
    applyClashRepel(ctx, attackerSword, targetSword);

    // 调试日志
    ChestCavity.LOGGER.info(
        "[SwordClash] Sword clash detected! {} vs {}",
        attackerSword.getOwner().getName().getString(),
        targetSword.getOwner().getName().getString());
  }

  /**
   * 施加碰撞反推效果
   *
   * @param ctx 命中上下文
   * @param sword1 攻击方飞剑
   * @param sword2 被击方飞剑
   */
  private void applyClashRepel(
      HitEntityContext ctx, FlyingSwordEntity sword1, FlyingSwordEntity sword2) {
    Vec3 pos1 = sword1.position();
    Vec3 pos2 = sword2.position();
    Vec3 vel1 = sword1.getDeltaMovement();
    Vec3 vel2 = sword2.getDeltaMovement();

    // 计算碰撞方向（从 sword2 指向 sword1）
    Vec3 direction = pos1.subtract(pos2);
    double distance = direction.length();

    if (distance < 0.01) {
      // 距离太近，使用随机方向避免除零
      direction = new Vec3(
          (Math.random() - 0.5) * 2,
          (Math.random() - 0.5) * 2,
          (Math.random() - 0.5) * 2);
      distance = direction.length();
    }

    direction = direction.normalize();

    // 计算相对速度（用于判断是否正面碰撞）
    Vec3 relativeVel = vel1.subtract(vel2);
    double approachSpeed = -relativeVel.dot(direction);

    // 只在正面碰撞时施加反推（避免追尾时也推开）
    if (approachSpeed <= 0) {
      return;
    }

    // 计算推力大小（基于双方速度和接近速度）
    double speed1 = vel1.length();
    double speed2 = vel2.length();
    double avgSpeed = (speed1 + speed2) / 2.0;

    // 推力基于平均速度和接近速度
    double repelForce = (avgSpeed + approachSpeed) * REPEL_FORCE_MULT;
    repelForce = Math.max(MIN_REPEL_FORCE, Math.min(MAX_REPEL_FORCE, repelForce));

    // 计算质量比（等级高的飞剑"更重"，受推力影响更小）
    double mass1 = 1.0 + sword1.getSwordLevel() * 0.1; // 基础质量1，每级+0.1
    double mass2 = 1.0 + sword2.getSwordLevel() * 0.1;
    double totalMass = mass1 + mass2;

    // 根据质量分配推力（轻的剑被推得更远）
    double force1 = repelForce * (mass2 / totalMass);
    double force2 = repelForce * (mass1 / totalMass);

    // 施加推力（sword1 被推向正方向，sword2 被推向负方向）
    Vec3 repel1 = direction.scale(force1);
    Vec3 repel2 = direction.scale(-force2);

    // 直接设置速度（而不是累加），以确保反推效果明显
    sword1.setDeltaMovement(vel1.add(repel1));
    sword2.setDeltaMovement(vel2.add(repel2));

    // 触发粒子效果（碰撞火花）
    if (ctx.level != null) {
      Vec3 clashPoint = pos1.add(pos2).scale(0.5); // 碰撞中点
      net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.fx.FlyingSwordFX
          .spawnAttackImpact(ctx.level, sword1, clashPoint, repelForce * 5.0);
    }

    // 播放碰撞音效
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.ops.SoundOps
        .playHit(sword1);
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.ops.SoundOps
        .playHit(sword2);
  }
}
