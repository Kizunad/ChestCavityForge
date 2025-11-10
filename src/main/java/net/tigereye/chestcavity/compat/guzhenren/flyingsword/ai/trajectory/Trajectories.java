package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory;

import java.util.EnumMap;
import java.util.Map;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.SteeringTemplate;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.SteeringTemplate.SpeedUnit;

/**
 * 轨迹注册与解析器。
 *
 * <p><b>Phase 7: 软删除标记（Soft Deletion Marks）</b>
 *
 * <p>本类使用功能开关实现"软删除"机制，将轨迹分为两类：
 *
 * <ul>
 *   <li><b>核心轨迹</b>（始终注册）：
 *       <ul>
 *         <li>{@link TrajectoryType#Orbit} - 环绕轨迹
 *         <li>{@link TrajectoryType#PredictiveLine} - 预测直线
 *         <li>{@link TrajectoryType#CurvedIntercept} - 曲线拦截
 *       </ul>
 *   <li><b>高级轨迹</b>（仅当 {@code ENABLE_ADVANCED_TRAJECTORIES=true} 时注册）：
 *       <ul>
 *         <li>{@link TrajectoryType#Boomerang} - 回旋镖
 *         <li>{@link TrajectoryType#Corkscrew} - 螺旋钻
 *         <li>{@link TrajectoryType#BezierS} - 贝塞尔S曲线
 *         <li>{@link TrajectoryType#Serpentine} - 蛇形
 *         <li>{@link TrajectoryType#VortexOrbit} - 旋涡环绕
 *         <li>{@link TrajectoryType#Sawtooth} - 锯齿
 *         <li>{@link TrajectoryType#PetalScan} - 花瓣扫描
 *         <li>{@link TrajectoryType#WallGlide} - 贴墙滑行
 *         <li>{@link TrajectoryType#ShadowStep} - 影步
 *         <li>{@link TrajectoryType#DomainEdgePatrol} - 领域边缘巡逻
 *         <li>{@link TrajectoryType#Ricochet} - 弹射
 *         <li>{@link TrajectoryType#HelixPair} - 双螺旋
 *         <li>{@link TrajectoryType#PierceGate} - 穿刺门
 *       </ul>
 * </ul>
 *
 * <p><b>软删除策略：</b>
 *
 * <ul>
 *   <li>默认配置（{@code ENABLE_ADVANCED_TRAJECTORIES=false}）下，高级轨迹不会被注册， 实现零性能开销
 *   <li>高级轨迹实现类保留在代码库中，不硬删除，保持可选功能的完整性
 *   <li>用户可通过修改 {@link
 *       net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning#ENABLE_ADVANCED_TRAJECTORIES}
 *       开关启用高级轨迹
 *   <li>详见：{@code docs/stages/PHASE_7.md} §7.3.1
 * </ul>
 *
 * @see
 *     net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning#ENABLE_ADVANCED_TRAJECTORIES
 * @see TrajectoryType
 */
public final class Trajectories {
  private static final Map<TrajectoryType, Trajectory> REGISTRY =
      new EnumMap<>(TrajectoryType.class);
  private static final Map<TrajectoryType, TrajectoryMeta> META =
      new EnumMap<>(TrajectoryType.class);
  private static final Map<TrajectoryType, SteeringTemplate> TEMPLATE =
      new EnumMap<>(TrajectoryType.class);
  private static final TrajectoryMeta DEFAULT_META = TrajectoryMeta.builder().build();

  static {
    // === Phase 1: 核心轨迹（始终启用） ===
    register(
        TrajectoryType.Orbit,
        new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl
            .OrbitTrajectory(),
        TrajectoryMeta.builder().speedUnit(SpeedUnit.BASE).build(),
        new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.templates
            .OrbitTemplate());
    register(
        TrajectoryType.PredictiveLine,
        new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl
            .PredictiveLineTrajectory(),
        TrajectoryMeta.builder().speedUnit(SpeedUnit.MAX).build(),
        new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.templates
            .PredictiveLineTemplate());
    register(
        TrajectoryType.CurvedIntercept,
        new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl
            .CurvedInterceptTrajectory(),
        TrajectoryMeta.builder().speedUnit(SpeedUnit.MAX).build(),
        new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.templates
            .CurvedInterceptTemplate());

    // === Phase 1: 高级轨迹（功能开关控制） ===
    if (net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning
        .ENABLE_ADVANCED_TRAJECTORIES) {
      register(
          TrajectoryType.Boomerang,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl
              .BoomerangTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.MAX).build(),
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.templates
              .BoomerangTemplate());
      register(
          TrajectoryType.Corkscrew,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl
              .CorkscrewTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.MAX).build(),
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.templates
              .CorkscrewTemplate());
      register(
          TrajectoryType.BezierS,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl
              .BezierSTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.MAX).build());
      register(
          TrajectoryType.Serpentine,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl
              .SerpentineTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.BASE).build());
      register(
          TrajectoryType.VortexOrbit,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl
              .VortexOrbitTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.BASE).build());
      register(
          TrajectoryType.Sawtooth,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl
              .SawtoothTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.MAX).build());
      register(
          TrajectoryType.PetalScan,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl
              .PetalScanTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.BASE).build());
      register(
          TrajectoryType.WallGlide,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl
              .WallGlideTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.BASE).build());
      register(
          TrajectoryType.ShadowStep,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl
              .ShadowStepTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.MAX).separation(false).build());
      register(
          TrajectoryType.DomainEdgePatrol,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl
              .DomainEdgePatrolTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.BASE).build());
      register(
          TrajectoryType.Ricochet,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl
              .RicochetTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.BASE).build());
      register(
          TrajectoryType.HelixPair,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl
              .HelixPairTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.MAX).build());
      register(
          TrajectoryType.PierceGate,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl
              .PierceGateTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.MAX).build());
    }
  }

  private Trajectories() {}

  public static Trajectory resolver(TrajectoryType type) {
    return REGISTRY.getOrDefault(type, Trajectories::zeroVelocity);
  }

  public static SteeringTemplate template(TrajectoryType type) {
    SteeringTemplate direct = TEMPLATE.get(type);
    if (direct != null) {
      return direct;
    }
    Trajectory trajectory = resolver(type);
    TrajectoryMeta meta = META.getOrDefault(type, DEFAULT_META);
    return new TrajectorySteeringAdapter(trajectory, meta);
  }

  private static void register(
      TrajectoryType type, Trajectory trajectory, TrajectoryMeta meta, SteeringTemplate template) {
    REGISTRY.put(type, trajectory);
    META.put(type, meta);
    if (template != null) {
      TEMPLATE.put(type, template);
    }
  }

  private static void register(TrajectoryType type, Trajectory trajectory, TrajectoryMeta meta) {
    register(type, trajectory, meta, null);
  }

  private static net.minecraft.world.phys.Vec3 zeroVelocity(
      net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext ctx,
      net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult intent) {
    return net.minecraft.world.phys.Vec3.ZERO;
  }
}
