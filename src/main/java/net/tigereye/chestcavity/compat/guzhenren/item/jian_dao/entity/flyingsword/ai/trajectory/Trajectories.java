package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory;

import java.util.EnumMap;
import java.util.Map;

/**
 * 轨迹注册与解析器。
 */
public final class Trajectories {
  private static final Map<TrajectoryType, Trajectory> REGISTRY = new EnumMap<>(TrajectoryType.class);

  static {
    // 基础可用实现
    REGISTRY.put(TrajectoryType.Orbit, new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.impl.OrbitTrajectory());
    REGISTRY.put(TrajectoryType.PredictiveLine, new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.impl.PredictiveLineTrajectory());
    REGISTRY.put(TrajectoryType.Boomerang, new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.impl.BoomerangTrajectory());
    REGISTRY.put(TrajectoryType.Corkscrew, new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.impl.CorkscrewTrajectory());
    REGISTRY.put(TrajectoryType.BezierS, new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.impl.BezierSTrajectory());
    REGISTRY.put(TrajectoryType.Serpentine, new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.impl.SerpentineTrajectory());
    REGISTRY.put(TrajectoryType.CurvedIntercept, new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.impl.CurvedInterceptTrajectory());
    REGISTRY.put(TrajectoryType.VortexOrbit, new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.impl.VortexOrbitTrajectory());
    REGISTRY.put(TrajectoryType.Sawtooth, new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.impl.SawtoothTrajectory());
    REGISTRY.put(TrajectoryType.PetalScan, new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.impl.PetalScanTrajectory());
    REGISTRY.put(TrajectoryType.WallGlide, new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.impl.WallGlideTrajectory());
    REGISTRY.put(TrajectoryType.ShadowStep, new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.impl.ShadowStepTrajectory());
    REGISTRY.put(TrajectoryType.DomainEdgePatrol, new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.impl.DomainEdgePatrolTrajectory());
    REGISTRY.put(TrajectoryType.Ricochet, new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.impl.RicochetTrajectory());
    REGISTRY.put(TrajectoryType.HelixPair, new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.impl.HelixPairTrajectory());
    REGISTRY.put(TrajectoryType.PierceGate, new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.impl.PierceGateTrajectory());
  }

  private Trajectories() {}

  public static Trajectory resolver(TrajectoryType type) {
    return REGISTRY.getOrDefault(type, Trajectories::zeroVelocity);
  }

  private static net.minecraft.world.phys.Vec3 zeroVelocity(
      net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.AIContext ctx,
      net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.IntentResult intent) {
    return net.minecraft.world.phys.Vec3.ZERO;
  }
}
