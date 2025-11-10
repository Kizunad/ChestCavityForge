package net.tigereye.chestcavity.compat.guzhenren.flyingsword.systems;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;

/**
 * RiderControlSystem
 *
 * <p>集中“主人骑乘控制飞剑”的逻辑： - 判定是否为主人驾驶 - 读取输入并计算目标速度 - 通过 MovementSystem.applySteeringVelocity
 * 应用（含加速度/限速/平滑）
 */
public final class RiderControlSystem {

  private RiderControlSystem() {}

  /** 是否由主人驾驶（且为玩家） */
  public static boolean hasOwnerController(FlyingSwordEntity sword) {
    LivingEntity controller = sword.getControllingPassenger();
    return controller instanceof Player && sword.isOwnedBy(controller);
  }

  /** 将玩家输入映射为目标速度，并交给 MovementSystem 处理 */
  public static void apply(FlyingSwordEntity sword, Player player) {
    // 对齐朝向（轻度跟随玩家视角）
    sword.setYRot(player.getYRot());
    sword.setXRot(player.getXRot());
    sword.yRotO = sword.getYRot();
    sword.xRotO = sword.getXRot();
    sword.yBodyRot = sword.getYRot();
    sword.yHeadRot = sword.getYRot();

    // 输入：前进/后退 与 左右横移
    float forward = player.zza;
    float strafe = player.xxa;

    // 方向：使用 Minecraft 提供的视线向量确保与视角一致
    Vec3 look = player.getLookAngle(); // 已归一化，包含俯仰

    // 右方向：Up × Look，避免 look×up 导致“左”向量
    Vec3 up = new Vec3(0.0, 1.0, 0.0);
    Vec3 right = up.cross(look);
    if (right.lengthSqr() < 1.0e-6) {
      // 当视线几乎垂直时，使用 yaw 水平向右作为退路
      double yawRad = Math.toRadians(player.getYRot());
      right = new Vec3(Math.cos(yawRad), 0.0, Math.sin(yawRad));
    } else {
      right = right.normalize();
    }

    // 合成目标方向：前进完全跟随视线含俯仰，横移仅沿右方向
    Vec3 desired = look.scale(forward).add(right.scale(strafe));
    double inputMag =
        Math.min(1.0, Math.sqrt((double) forward * forward + (double) strafe * strafe));

    // 死区处理：进入慢停
    if (inputMag
        < net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordControlTuning
            .RIDER_INPUT_DEADZONE) {
      desired = Vec3.ZERO;
      inputMag = 0.0;
    }

    // 计算增幅倍率：域缩放 × 道痕加成 × 基础倍率
    double domainScale =
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.domain.SwordSpeedModifiers
            .computeDomainSpeedScale(sword);
    double daoHenMult = computeDaohenSpeedMult(sword);
    double baseMult =
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordControlTuning
            .RIDER_BASE_SPEED_MULT;

    double effectiveMax = sword.getSwordAttributes().speedMax * domainScale * daoHenMult * baseMult;

    // 目标速度
    Vec3 targetVel =
        desired.lengthSqr() > 1.0e-6
            ? desired.normalize().scale(effectiveMax * inputMag)
            : Vec3.ZERO;

    // 平滑插值（手感）
    double a =
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordControlTuning
            .RIDER_VELOCITY_SMOOTHING;
    Vec3 cur = sword.getDeltaMovement();
    Vec3 newVel = cur.scale(1.0 - a).add(targetVel.scale(a));
    sword.setDeltaMovement(newVel);
  }

  /** 客户端侧本地控制（Boat式）： 仅当本地玩家为驾驶者时调用，由客户端预测速度并通过“MoveVehicle”包同步给服务端。 */
  public static void applyClient(FlyingSwordEntity sword, Player player) {
    // 直接复用服务端同样的输入到速度映射
    apply(sword, player);
  }

  private static double computeDaohenSpeedMult(FlyingSwordEntity sword) {
    try {
      net.minecraft.world.entity.LivingEntity owner = sword.getOwner();
      if (owner == null) return 1.0;
      java.util.Optional<
              net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle>
          handleOpt =
              net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps.openHandle(owner);
      double daohen = handleOpt.map(h -> h.read("daohen_jiandao").orElse(0.0)).orElse(0.0);
      if (!(daohen > 0.0)) return 1.0;
      double coef =
          net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordControlTuning
              .RIDER_DAOHEN_COEF;
      double maxExtra =
          net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordControlTuning
              .RIDER_DAOHEN_MAX_EXTRA;
      double extra = Math.min(maxExtra, coef * Math.sqrt(daohen));
      return 1.0 + Math.max(0.0, extra);
    } catch (Throwable ignored) {
      return 1.0;
    }
  }
}
