package net.tigereye.chestcavity.compat.guzhenren.flyingsword.systems;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;

/**
 * RiderControlSystem
 *
 * 集中“主人骑乘控制飞剑”的逻辑：
 * - 判定是否为主人驾驶
 * - 读取输入并计算目标速度
 * - 通过 MovementSystem.applySteeringVelocity 应用（含加速度/限速/平滑）
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

    // 方向：前进跟随视线（含俯仰），横移在水平面
    double yawRad = Math.toRadians(player.getYRot());
    double pitchRad = Math.toRadians(player.getXRot());
    double sinY = Math.sin(yawRad);
    double cosY = Math.cos(yawRad);
    double cosP = Math.cos(pitchRad);
    double sinP = Math.sin(pitchRad);

    Vec3 fwd3d = new Vec3(-sinY * cosP, -sinP, cosY * cosP);
    Vec3 rightH = new Vec3(cosY, 0.0, sinY);

    Vec3 desired = fwd3d.scale(forward).add(rightH.scale(strafe));
    if (desired.lengthSqr() > 1.0e-6) {
      desired = desired.normalize().scale(sword.getSwordAttributes().speedMax);
    } else {
      desired = Vec3.ZERO;
    }

    MovementSystem.applySteeringVelocity(sword, desired);
  }
}

