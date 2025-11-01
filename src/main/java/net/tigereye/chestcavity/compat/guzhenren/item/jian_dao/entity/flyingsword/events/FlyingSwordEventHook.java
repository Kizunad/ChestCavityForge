package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events;

import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events.context.*;

/**
 * 飞剑事件钩子接口
 *
 * <p>允许外部模块（器官、道痕、技能等）监听并修改飞剑行为。
 *
 * <p>所有方法都有默认实现（空操作），实现者只需覆盖感兴趣的事件。
 *
 * <p>钩子执行顺序：按注册顺序依次执行，早注册的先执行。
 *
 * <p>钩子可以：
 * <ul>
 *   <li>读取上下文信息</li>
 *   <li>修改上下文中的可变字段（如伤害、速度、是否取消）</li>
 *   <li>调用实体方法（需谨慎，避免递归）</li>
 *   <li>生成粒子、音效、消息等副作用</li>
 * </ul>
 */
public interface FlyingSwordEventHook {

  /**
   * 飞剑生成时触发
   *
   * <p>在实体添加到世界之后、应用初始化定制之后立即调用。
   *
   * @param ctx 生成上下文
   */
  default void onSpawn(SpawnContext ctx) {}

  /**
   * 每tick触发（仅服务端）
   *
   * <p>在维持消耗检查、AI逻辑、破块逻辑之前调用。
   *
   * @param ctx tick上下文
   */
  default void onTick(TickContext ctx) {}

  /**
   * 命中实体时触发（攻击成功）
   *
   * <p>在伤害计算之后、实际造成伤害之前调用。
   * <p>可修改最终伤害、添加额外效果。
   *
   * @param ctx 命中实体上下文
   */
  default void onHitEntity(HitEntityContext ctx) {}

  /**
   * 破坏方块时触发
   *
   * <p>在每个方块成功破坏后调用。
   *
   * @param ctx 破块上下文
   */
  default void onBlockBreak(BlockBreakContext ctx) {}

  /**
   * 飞剑本体受击时触发
   *
   * <p>在伤害处理之前调用，可以取消伤害、修改伤害、触发特殊效果。
   *
   * @param ctx 受击上下文
   */
  default void onHurt(HurtContext ctx) {}

  /**
   * 玩家交互时触发（右键实体）
   *
   * <p>在召回逻辑之前调用，可以拦截或修改行为。
   *
   * @param ctx 交互上下文
   */
  default void onInteract(InteractContext ctx) {}

  /**
   * 飞剑消散或召回时触发
   *
   * <p>在实体discard()之前调用，用于清理、保存状态、回写物品NBT等。
   *
   * @param ctx 消散上下文
   */
  default void onDespawnOrRecall(DespawnContext ctx) {}
}
