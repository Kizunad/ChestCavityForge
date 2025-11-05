package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory;

import java.util.EnumMap;
import java.util.Map;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.SteeringTemplate;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.SteeringTemplate.SpeedUnit;

/**
 * 轨迹注册与解析器。
 */
public final class Trajectories {
  private static final Map<TrajectoryType, Trajectory> REGISTRY = new EnumMap<>(TrajectoryType.class);
  private static final Map<TrajectoryType, TrajectoryMeta> META =
      new EnumMap<>(TrajectoryType.class);
  private static final Map<TrajectoryType, SteeringTemplate> TEMPLATE =
      new EnumMap<>(TrajectoryType.class);
  private static final TrajectoryMeta DEFAULT_META = TrajectoryMeta.builder().build();

  static {
    // === Phase 1: 核心轨迹（始终启用） ===
    register(
        TrajectoryType.Orbit,
        new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl.OrbitTrajectory(),
        TrajectoryMeta.builder().speedUnit(SpeedUnit.BASE).build(),
        new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.templates.OrbitTemplate());
    register(
        TrajectoryType.PredictiveLine,
        new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl.PredictiveLineTrajectory(),
        TrajectoryMeta.builder().speedUnit(SpeedUnit.MAX).build(),
        new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.templates.PredictiveLineTemplate());
    register(
        TrajectoryType.CurvedIntercept,
        new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl.CurvedInterceptTrajectory(),
        TrajectoryMeta.builder().speedUnit(SpeedUnit.MAX).build(),
        new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.templates.CurvedInterceptTemplate());

    // === Phase 1: 高级轨迹（功能开关控制） ===
    if (net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning.ENABLE_ADVANCED_TRAJECTORIES) {
      register(
          TrajectoryType.Boomerang,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl.BoomerangTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.MAX).build(),
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.templates.BoomerangTemplate());
      register(
          TrajectoryType.Corkscrew,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl.CorkscrewTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.MAX).build(),
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.templates.CorkscrewTemplate());
      register(
          TrajectoryType.BezierS,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl.BezierSTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.MAX).build());
      register(
          TrajectoryType.Serpentine,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl.SerpentineTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.BASE).build());
      register(
          TrajectoryType.VortexOrbit,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl.VortexOrbitTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.BASE).build());
      register(
          TrajectoryType.Sawtooth,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl.SawtoothTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.MAX).build());
      register(
          TrajectoryType.PetalScan,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl.PetalScanTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.BASE).build());
      register(
          TrajectoryType.WallGlide,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl.WallGlideTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.BASE).build());
      register(
          TrajectoryType.ShadowStep,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl.ShadowStepTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.MAX).separation(false).build());
      register(
          TrajectoryType.DomainEdgePatrol,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl.DomainEdgePatrolTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.BASE).build());
      register(
          TrajectoryType.Ricochet,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl.RicochetTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.BASE).build());
      register(
          TrajectoryType.HelixPair,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl.HelixPairTrajectory(),
          TrajectoryMeta.builder().speedUnit(SpeedUnit.MAX).build());
      register(
          TrajectoryType.PierceGate,
          new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl.PierceGateTrajectory(),
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
