package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.swarm;

/**
 * 剑群集群行为模式
 *
 * <p>定义青莲剑群的不同战术模式
 */
public enum SwarmBehaviorMode {
  /**
   * 莲花护卫阵
   *
   * <p>所有剑形成莲花瓣状环绕玩家，3层立体结构，缓慢旋转
   */
  LOTUS_GUARD,

  /**
   * 螺旋攻击
   *
   * <p>剑依次螺旋飞向目标，形成连续打击波次
   */
  SPIRAL_ATTACK,

  /**
   * 轮流防御
   *
   * <p>50%剑护卫主人，50%剑攻击敌人，定期轮换角色
   */
  ROTATING_DEFENSE,

  /**
   * 合击
   *
   * <p>所有剑同时从不同方向攻击同一目标，形成球形包围
   */
  CONVERGING_STRIKE
}
