package net.tigereye.chestcavity.compat.guzhenren.flyingsword.events;

import net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context.*;

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
 *
 * <ul>
 *   <li>读取上下文信息
 *   <li>修改上下文中的可变字段（如伤害、速度、是否取消）
 *   <li>调用实体方法（需谨慎，避免递归）
 *   <li>生成粒子、音效、消息等副作用
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
   *
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

  // ========== Phase 3: 事件模型扩展 ==========

  /**
   * AI模式切换时触发
   *
   * <p>Phase 3: 在飞剑的AI模式发生变化时调用。
   *
   * <p>可用于触发模式特定的初始化、粒子效果、或阻止非法模式切换。
   *
   * @param ctx 模式切换上下文
   */
  default void onModeChange(ModeChangeContext ctx) {}

  /**
   * 目标锁定时触发
   *
   * <p>Phase 3: 当飞剑获取新目标时调用（GUARD自动搜索、HUNT指定目标等）。
   *
   * <p>可用于记录目标历史、触发锁定音效、或阻止锁定特定目标。
   *
   * @param ctx 目标锁定上下文
   */
  default void onTargetAcquired(TargetAcquiredContext ctx) {}

  /**
   * 目标丢失时触发
   *
   * <p>Phase 3: 当飞剑失去当前目标时调用（目标死亡、超出范围、模式切换等）。
   *
   * <p>可用于触发搜索新目标、清理状态、或自动切换AI模式。
   *
   * @param ctx 目标丢失上下文
   */
  default void onTargetLost(TargetLostContext ctx) {}

  /**
   * 维持消耗检查时触发
   *
   * <p>Phase 3: 在每次维持消耗检查时调用（间隔由FlyingSwordTuning配置）。
   *
   * <p>可用于修改消耗量、记录消耗历史、或跳过本次消耗。
   *
   * @param ctx 维持消耗检查上下文
   */
  default void onUpkeepCheck(UpkeepCheckContext ctx) {}

  /**
   * 命中后触发（伤害已造成）
   *
   * <p>Phase 3: 在飞剑成功造成伤害后调用（在HitEntity之后）。
   *
   * <p>只读上下文，用于触发附加效果（吸血、经验获取、连击效果等）。
   *
   * @param ctx 命中后上下文
   */
  default void onPostHit(PostHitContext ctx) {}

  /**
   * 破块尝试时触发（破块之前）
   *
   * <p>Phase 3: 在飞剑尝试破坏方块之前调用。
   *
   * <p>可用于权限检查、特殊方块逻辑、或修改破块行为。
   *
   * @param ctx 破块尝试上下文
   */
  default void onBlockBreakAttempt(BlockBreakAttemptContext ctx) {}

  /**
   * 经验获取时触发
   *
   * <p>Phase 3: 当飞剑获得经验时调用（击杀、破块、手动注入等）。
   *
   * <p>可用于修改经验获取量、记录经验来源、或触发特效。
   *
   * @param ctx 经验获取上下文
   */
  default void onExperienceGain(ExperienceGainContext ctx) {}

  /**
   * 等级提升时触发
   *
   * <p>Phase 3: 当飞剑等级提升时调用（经验值突破阈值）。
   *
   * <p>可用于触发升级效果、解锁新技能、或向玩家发送通知。
   *
   * @param ctx 等级提升上下文
   */
  default void onLevelUp(LevelUpContext ctx) {}
}
