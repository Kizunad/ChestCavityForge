package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.state.DomainConfigOps;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning.JianXinDomainMath;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning.JianXinDomainTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianYuGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

/**
 * 剑域蛊 纯计算：将行为中的所有计算逻辑集中到此，便于测试与复用。
 */
public final class JianYuGuCalc {
  private JianYuGuCalc() {}

  /** 当前半径：BASE_RADIUS * radiusScale。*/
  public static double currentRadius(ServerPlayer player) {
    double scale = DomainConfigOps.radiusScale(player);
    return JianXinDomainTuning.BASE_RADIUS * Math.max(1.0e-6, scale);
  }

  /** Rmax = 5 + floor(sqrt(道痕/200)) + floor(经验/3) （上限11）。*/
  public static int rmax(ServerPlayer player) {
    Optional<GuzhenrenResourceBridge.ResourceHandle> h = ResourceOps.openHandle(player);
    int d = (int) Math.round(h.map(x -> x.read("daohen_jiandao").orElse(0.0)).orElse(0.0));
    int e = (int) Math.round(h.map(x -> x.read("liupai_jiandao").orElse(0.0)).orElse(0.0));
    return JianXinDomainMath.computeRmax(d, e);
  }

  /** 归一化收缩度 s。*/
  public static double sFor(ServerPlayer player) {
    double R = currentRadius(player);
    int rmax = rmax(player);
    return JianXinDomainMath.computeS(R, rmax);
  }

  /** 被动扣费（每2秒一次）。*/
  public static double passiveDrainPer2s(ServerPlayer player) {
    double R = currentRadius(player);
    double s = sFor(player);
    return JianXinDomainMath.computePassiveDrainPer2s(R, s);
  }

  /** 主动期间每秒扣费。*/
  public static double activeDrainPerSec(ServerPlayer player) {
    double R = currentRadius(player);
    double s = sFor(player);
    return JianXinDomainMath.computeActiveDrainPerSec(R, s);
  }

  /** 是否在正面锥内（余弦阈值）。*/
  public static boolean isInFrontCone(LivingEntity owner, LivingEntity attacker) {
    if (owner == null || attacker == null) return false;
    Vec3 look = owner.getLookAngle().normalize();
    Vec3 toAtt = attacker.position().subtract(owner.position()).normalize();
    double dot = look.dot(toAtt);
    return dot >= JianYuGuTuning.FRONT_CONE_HALF_ANGLE_COS;
  }

  /** 返回额外减伤后的伤害倍率（不小于0）。*/
  public static double frontConeDamageMultiplier() {
    return Math.max(0.0, 1.0 - JianYuGuTuning.FRONT_CONE_EXTRA_REDUCTION);
  }
}
