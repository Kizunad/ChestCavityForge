package net.tigereye.chestcavity.compat.guzhenren.flyingsword.systems;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordController;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.AIMode;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.behavior.GuardBehavior;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.behavior.HoverBehavior;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.behavior.HuntBehavior;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.behavior.OrbitBehavior;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.behavior.RecallBehavior;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.behavior.TargetFinder;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.command.SwordCommandCenter;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.Intent;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.planner.IntentPlanner;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types.RecallIntent;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.Trajectories;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.KinematicsSnapshot;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.SteeringOps;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.SteeringTemplate;

/**
 * Phase 2: 运动系统 (Movement System)
 *
 * <p>职责:
 *
 * <ul>
 *   <li>应用 AI 意图与轨迹模板计算速度
 *   <li>调用 setDeltaMovement 更新实体运动
 *   <li>集中管理各种 AI 模式的运动逻辑
 * </ul>
 *
 * <p>设计原则:
 *
 * <ul>
 *   <li>无状态: 所有方法为静态方法，不持有实例变量
 *   <li>事件驱动: 关键操作触发事件钩子
 *   <li>可测试: 输入输出明确，便于单元测试
 * </ul>
 */
public final class MovementSystem {

  private MovementSystem() {
    // 禁止实例化
  }

  /**
   * Phase 2: 运动系统 Tick 入口
   *
   * <p>根据 AI 模式与意图结果，计算并应用新速度
   *
   * @param sword 飞剑实体
   * @param owner 飞剑主人
   * @param mode 当前 AI 模式
   */
  public static void tick(FlyingSwordEntity sword, LivingEntity owner, AIMode mode) {
    if (sword == null || owner == null || sword.level() == null) {
      return;
    }

    // Phase 2: 检查是否为服务端
    if (!(sword.level() instanceof ServerLevel server)) {
      return;
    }

    // Phase 2: 尝试 TUI 命令系统 (优先级最高)
    boolean commandHandled = false;
    if (owner instanceof ServerPlayer player) {
      AIContext commandCtx = new AIContext(sword, owner, server, server.getGameTime());
      Optional<IntentResult> directive = SwordCommandCenter.buildIntent(commandCtx);

      if (directive.isPresent()) {
        IntentResult res = directive.get();
        res.getTargetEntity().ifPresent(sword::setTargetEntity);
        applyIntentResult(sword, commandCtx, res);
        commandHandled = true;
      }
    }

    // Phase 2: 根据 AI 模式执行对应行为
    if (!commandHandled) {
      executeAIBehavior(sword, owner, mode, server);
    }

    // Phase 2: 更新当前速度同步器
    sword.setCurrentSpeed((float) sword.getDeltaMovement().length());
  }

  /** Phase 2: 执行 AI 模式对应的行为逻辑 */
  private static void executeAIBehavior(
      FlyingSwordEntity sword, LivingEntity owner, AIMode mode, ServerLevel server) {

    switch (mode) {
      case ORBIT:
        // Phase 2: 环绕模式 - 使用旧实现 (保持稳定)
        OrbitBehavior.tick(sword, owner);
        break;

      case GUARD:
        // Phase 2: 防守模式 - 新 Intent 系统优先，回退到旧实现
        handleGuardMode(sword, owner, server);
        break;

      case HUNT:
        // Phase 2: 出击模式 - 使用旧实现
        LivingEntity huntTarget =
            TargetFinder.findNearestHostile(sword, sword.position(), HuntBehavior.getSearchRange());
        HuntBehavior.tick(sword, owner, huntTarget);
        break;

      case HOVER:
        // Phase 2: 悬停模式 - 使用旧实现
        HoverBehavior.tick(sword, owner);
        break;

      case RECALL:
        // Phase 2: 召回模式 - 新 Intent 系统优先，回退到旧实现
        handleRecallMode(sword, owner, server);
        break;

      case SWARM:
        // Phase 2: 集群模式 - 由集群管理器统一调度，飞剑不自行决策
        // 速度已在集群管理器的 tick 中通过 applySteeringVelocity 设置
        break;

      default:
        break;
    }
  }

  /** Phase 2: 处理 GUARD 模式 (新 Intent 系统) */
  private static void handleGuardMode(
      FlyingSwordEntity sword, LivingEntity owner, ServerLevel server) {

    AIContext ctx = new AIContext(sword, owner, server, server.getGameTime());
    java.util.List<Intent> intents = IntentPlanner.intentsFor(AIMode.GUARD);
    Optional<IntentResult> best = IntentPlanner.pickBest(ctx, intents);

    if (best.isPresent()) {
      IntentResult res = best.get();
      res.getTargetEntity().ifPresent(sword::setTargetEntity);
      applyIntentResult(sword, ctx, res);
    } else {
      // Phase 2: 回退到旧实现
      LivingEntity nearest =
          TargetFinder.findNearestHostileForGuard(
              sword, owner.position(), GuardBehavior.getSearchRange());
      GuardBehavior.tick(sword, owner, nearest);
    }
  }

  /** Phase 2: 处理 RECALL 模式 (新 Intent 系统) */
  private static void handleRecallMode(
      FlyingSwordEntity sword, LivingEntity owner, ServerLevel server) {

    AIContext ctx = new AIContext(sword, owner, server, server.getGameTime());
    java.util.List<Intent> intents = java.util.List.of(new RecallIntent());
    Optional<IntentResult> best = IntentPlanner.pickBest(ctx, intents);

    if (best.isPresent()) {
      IntentResult res = best.get();
      applyIntentResult(sword, ctx, res);

      // Phase 2: 抵达后执行实际召回
      if (sword.distanceTo(owner) < 1.0) {
        FlyingSwordController.recall(sword);
      }
    } else {
      // Phase 2: 回退到旧召回实现
      RecallBehavior.tick(sword, owner);
    }
  }

  /**
   * Phase 2: 应用意图结果，计算并设置速度
   *
   * <p>核心流程:
   *
   * <ol>
   *   <li>获取轨迹模板 (SteeringTemplate)
   *   <li>捕获运动学快照 (KinematicsSnapshot)
   *   <li>计算转向指令 (SteeringCommand)
   *   <li>计算新速度 (SteeringOps.computeNewVelocity)
   *   <li>应用速度 (setDeltaMovement)
   * </ol>
   *
   * @param sword 飞剑实体
   * @param ctx AI 上下文
   * @param intent 意图结果
   */
  private static void applyIntentResult(
      FlyingSwordEntity sword, AIContext ctx, IntentResult intent) {

    try {
      // Phase 2: 获取轨迹模板
      SteeringTemplate template = Trajectories.template(intent.getTrajectoryType());
      if (template == null) {
        return; // 无效轨迹，保持当前速度
      }

      // Phase 2: 捕获运动学快照
      KinematicsSnapshot snapshot = KinematicsSnapshot.capture(sword);

      // Phase 2: 计算转向指令
      var command = template.compute(ctx, intent, snapshot);

      // Phase 2: 计算新速度
      Vec3 newVelocity = SteeringOps.computeNewVelocity(sword, command, snapshot);

      // Phase 2: 应用速度
      sword.setDeltaMovement(newVelocity);

    } catch (Exception e) {
      // Phase 2: 防御性错误处理，避免单个飞剑崩溃影响全局
      // 在生产环境中可替换为日志系统
      // LOGGER.warn("Failed to apply intent result for sword {}: {}", sword.getId(),
      // e.getMessage());
    }
  }

  /**
   * Phase 2: 应用转向速度 (旧 API 兼容接口)
   *
   * <p>保留此方法以兼容外部模块 (如 Swarm 集群管理器)
   *
   * @param sword 飞剑实体
   * @param desiredVelocity 期望速度向量
   */
  public static void applySteeringVelocity(FlyingSwordEntity sword, Vec3 desiredVelocity) {
    if (sword == null || desiredVelocity == null) {
      return;
    }

    try {
      var snapshot = KinematicsSnapshot.capture(sword);
      var command =
          net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.LegacySteeringAdapter
              .fromDesiredVelocity(desiredVelocity, snapshot);
      Vec3 newVelocity = SteeringOps.computeNewVelocity(sword, command, snapshot);
      sword.setDeltaMovement(newVelocity);
    } catch (Exception e) {
      // Phase 2: 防御性错误处理
    }
  }
}
