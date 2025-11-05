package net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context;

import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;

/**
 * 命中后事件上下文
 *
 * <p>Phase 3: 在飞剑成功造成伤害后触发（在HitEntity事件之后）。
 *
 * <p>区别于HitEntity事件：
 * <ul>
 *   <li>HitEntity: 伤害计算之后、实际造成伤害之前，可修改伤害</li>
 *   <li>PostHit: 伤害已造成，只读上下文，用于后续效果</li>
 * </ul>
 *
 * <p>用途：
 * <ul>
 *   <li>触发附加效果（如吸血、经验获取）</li>
 *   <li>记录战斗统计（伤害总量、击杀数）</li>
 *   <li>触发连击效果、斩杀奖励</li>
 *   <li>更新UI显示（伤害数字、连击计数器）</li>
 * </ul>
 */
public class PostHitContext {
  public final FlyingSwordEntity sword;
  public final LivingEntity target;
  public final float damageDealt;
  public final boolean wasKilled;

  public PostHitContext(
      FlyingSwordEntity sword, LivingEntity target, float damageDealt, boolean wasKilled) {
    this.sword = sword;
    this.target = target;
    this.damageDealt = damageDealt;
    this.wasKilled = wasKilled;
  }
}
