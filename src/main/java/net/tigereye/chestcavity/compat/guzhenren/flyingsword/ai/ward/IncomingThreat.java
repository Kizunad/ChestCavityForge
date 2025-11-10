package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.ward;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 完整的威胁描述（近战或投射）。
 *
 * <p>在 D 阶段之后，威胁对象携带明确的类型、位置与速度信息，供拦截、反弹与反击逻辑共用。
 *
 * @param attacker 攻击发起者（可为空，比如环境投射物）
 * @param target 预期目标（通常为玩家）
 * @param position 威胁当前位置
 * @param velocity 威胁速度向量（m/s）
 * @param speed 威胁速度标量（m/s）
 * @param type 威胁类型（投射物/近战）
 * @param worldTime 事件发生的世界时刻（tick）
 */
public record IncomingThreat(
    @Nullable Entity attacker,
    @Nullable Entity target,
    Vec3 position,
    Vec3 velocity,
    double speed,
    Type type,
    long worldTime) {

  /** 威胁类型 */
  public enum Type {
    PROJECTILE,
    MELEE
  }

  /** 是否为投射物 */
  public boolean isProjectile() {
    return type == Type.PROJECTILE;
  }

  /** 是否为近战 */
  public boolean isMelee() {
    return type == Type.MELEE;
  }

  /** 获取威胁描述（用于日志） */
  public String describe() {
    String attackerName = attacker != null ? attacker.getName().getString() : "environment";
    String targetName = target != null ? target.getName().getString() : "unknown";
    if (isProjectile()) {
      return String.format("Projectile[%s -> %s @ %.1fm/s]", attackerName, targetName, speed);
    }
    return String.format("Melee[%s -> %s]", attackerName, targetName);
  }

  /** 构建用于测试的投射物威胁 */
  public static IncomingThreat forTest(Vec3 projPos, Vec3 projVel, @Nullable Entity target) {
    return new IncomingThreat(
        null, target, projPos, projVel, projVel.length(), Type.PROJECTILE, 0L);
  }

  /** 构建用于测试的投射物威胁（无目标实体） */
  public static IncomingThreat forTest(Vec3 projPos, Vec3 projVel) {
    return forTest(projPos, projVel, null);
  }

  /** 构建用于测试的近战威胁（不依赖实体，仅几何数据） */
  public static IncomingThreat forTestMelee(Vec3 attackerEye, Vec3 attackerVel) {
    return new IncomingThreat(
        null, null, attackerEye, attackerVel, attackerVel.length(), Type.MELEE, 0L);
  }
}
