package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.behavior;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.SpellcasterIllager;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.command.SwordCommandCenter;

/**
 * 目标搜索工具
 *
 * <p>负责在范围内搜索最近的敌对实体
 */
public final class TargetFinder {

  private static final long MARK_EXPIRY_TICKS = 80L;

  private TargetFinder() {}

  /**
   * Guard模式专用：优先搜索敌方飞剑，其次才是普通敌对生物
   *
   * @param sword 飞剑实体
   * @param center 搜索中心点
   * @param range 搜索范围
   * @return 最近的目标（优先飞剑），如果没有则返回null
   */
  @Nullable
  public static LivingEntity findNearestHostileForGuard(
      FlyingSwordEntity sword, Vec3 center, double range) {
    if (!(sword.level() instanceof ServerLevel server)) {
      return null;
    }

    LivingEntity owner = sword.getOwner();
    if (owner == null) {
      return null;
    }

    AABB searchBox = new AABB(center, center).inflate(range);

    // 第一优先级：查找敌方飞剑
    FlyingSwordEntity nearestEnemySword = null;
    double minSwordDistSq = range * range;

    for (Entity entity : server.getEntities(null, searchBox)) {
      if (!(entity instanceof FlyingSwordEntity enemySword)) {
        continue;
      }

      // 排除自己
      if (enemySword == sword) {
        continue;
      }

      // 检查是否是敌方飞剑（不同主人）
      LivingEntity enemyOwner = enemySword.getOwner();
      if (enemyOwner == null || enemyOwner.getUUID().equals(owner.getUUID())) {
        continue;
      }

      // 只拦截存活的飞剑
      if (enemySword.isRemoved()) {
        continue;
      }

      double distSq = enemySword.distanceToSqr(center);
      if (distSq < minSwordDistSq) {
        minSwordDistSq = distSq;
        nearestEnemySword = enemySword;
      }
    }

    // 如果找到敌方飞剑，优先返回
    if (nearestEnemySword != null) {
      return nearestEnemySword;
    }

    // 如果没有敌方飞剑，查找普通敌对生物
    return findNearestHostile(sword, center, range);
  }

  /**
   * 搜索最近的敌对实体
   *
   * @param sword 飞剑实体
   * @param center 搜索中心点
   * @param range 搜索范围
   * @return 最近的敌对实体，如果没有则返回null
   */
  @Nullable
  public static LivingEntity findNearestHostile(
      FlyingSwordEntity sword, Vec3 center, double range) {
    if (!(sword.level() instanceof ServerLevel server)) {
      return null;
    }

    LivingEntity owner = sword.getOwner();
    if (owner == null) {
      return null;
    }

    AABB searchBox = new AABB(center, center).inflate(range);

    LivingEntity nearest = null;
    double minDistSq = range * range;

    for (Entity entity : server.getEntities(null, searchBox)) {
      if (!(entity instanceof LivingEntity living)) {
        continue;
      }
      if (!isEligibleHostileCandidate(living, sword, owner)) {
        continue;
      }

      if (isHostileForOwner(living, owner)) {
        double distSq = living.distanceToSqr(center);
        if (distSq < minDistSq) {
          minDistSq = distSq;
          nearest = living;
        }
      }
    }

    return nearest;
  }

  /**
   * 搜索低血量敌对实体（Assassin 使用）。
   */
  @Nullable
  public static LivingEntity findLowestHealthHostile(
      FlyingSwordEntity sword, Vec3 center, double range) {
    if (!(sword.level() instanceof ServerLevel server)) {
      return null;
    }

    LivingEntity owner = sword.getOwner();
    if (owner == null) {
      return null;
    }

    AABB searchBox = new AABB(center, center).inflate(range);

    LivingEntity best = null;
    double bestScore = Double.NEGATIVE_INFINITY;

    for (Entity entity : server.getEntities(null, searchBox)) {
      if (!(entity instanceof LivingEntity living)) {
        continue;
      }
      if (!isEligibleHostileCandidate(living, sword, owner)) {
        continue;
      }
      if (!isHostileForOwner(living, owner)) {
        continue;
      }

      double maxHealth = Math.max(1.0, living.getMaxHealth());
      double healthRatio = Math.min(1.0, living.getHealth() / maxHealth);
      double missingRatio = 1.0 - healthRatio;

      double distance = Math.sqrt(living.distanceToSqr(center));
      double proximityScore = 2.0 / Math.max(1.0, distance);

      // 低血量优先，其次兼顾高价值目标（低总生命、正在专注主人）
      double baseScore = missingRatio * 8.0 + proximityScore;

      if (living instanceof Mob mob && mob.getTarget() == owner) {
        baseScore += 2.0; // 正在威胁主人，额外加权
      }

      if (baseScore > bestScore) {
        bestScore = baseScore;
        best = living;
      }
    }

    return best;
  }

  /**
   * 查找“标记目标”：优先当前锁定，其次是主人最近攻击/被攻击的敌人。
   */
  @Nullable
  public static LivingEntity findMarkedTarget(
      FlyingSwordEntity sword, Vec3 center, double range) {
    LivingEntity owner = sword.getOwner();
    if (owner == null) {
      return null;
    }

    LivingEntity current = sword.getTargetEntity();
    if (isMarkedCandidate(current, sword, owner, center, range)) {
      return current;
    }

    if (!(owner.level() instanceof ServerLevel)) {
      return null;
    }
    int ownerTicks = owner.tickCount;

    LivingEntity lastAttackTarget = owner.getLastHurtMob();
    if (isRecentCombatTarget(lastAttackTarget, owner.getLastHurtMobTimestamp(), ownerTicks)
        && isMarkedCandidate(lastAttackTarget, sword, owner, center, range)) {
      return lastAttackTarget;
    }

    LivingEntity lastAttacker = owner.getLastHurtByMob();
    if (isRecentCombatTarget(lastAttacker, owner.getLastHurtByMobTimestamp(), ownerTicks)
        && isMarkedCandidate(lastAttacker, sword, owner, center, range)) {
      return lastAttacker;
    }

    return null;
  }

  /**
   * 查找“断阵”目标：优先召唤物 / 防御装置 / 低移动护盾实体。
   */
  @Nullable
  public static LivingEntity findBreakerTarget(
      FlyingSwordEntity sword, Vec3 center, double range) {
    List<LivingEntity> hostiles = collectHostiles(sword, center, range);
    LivingEntity best = null;
    double bestScore = Double.NEGATIVE_INFINITY;

    for (LivingEntity living : hostiles) {
      double distance = Math.sqrt(living.distanceToSqr(center));
      double distanceScore = 4.0 / Math.max(2.0, distance);

      double summonScore = 0.0;
      if (living instanceof OwnableEntity ownable) {
        java.util.UUID ownerId = ownable.getOwnerUUID();
        if (ownerId != null && !ownerId.equals(sword.getOwnerUUID())) {
          summonScore += 12.0;
        }
      }

      if (living instanceof SpellcasterIllager || living instanceof Shulker || living instanceof Ghast) {
        summonScore += 6.0;
      }

      if (living.getType().getCategory() == MobCategory.MISC) {
        summonScore += 8.0;
      }

      double mobilityPenalty = 0.0;
      if (living.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
        double speed = living.getAttribute(Attributes.MOVEMENT_SPEED).getValue();
        mobilityPenalty = Math.max(0.0, 1.0 - speed * 3.0);
      }

      double armorScore = living.getArmorValue();

      double score = summonScore + mobilityPenalty * 4.0 + armorScore + distanceScore;

      if (score > bestScore) {
        bestScore = score;
        best = living;
      }
    }

    return best;
  }

  /**
   * 查找“高威胁近战”目标（放风筝用）。
   */
  @Nullable
  public static LivingEntity findHighThreatMelee(
      FlyingSwordEntity sword, Vec3 center, double range) {
    List<LivingEntity> hostiles = collectHostiles(sword, center, range);
    LivingEntity best = null;
    double bestScore = Double.NEGATIVE_INFINITY;

    for (LivingEntity living : hostiles) {
      if (!(living instanceof Mob mob)) {
        continue;
      }

      double distance = Math.sqrt(living.distanceToSqr(center));
      double distanceScore = 6.0 / Math.max(1.5, distance);

      double attackBase = 0.0;
      if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
        attackBase = mob.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
      }

      double healthScore = mob.getMaxHealth() * 0.1;

      double speedScore = 0.0;
      if (mob.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
        speedScore = mob.getAttribute(Attributes.MOVEMENT_SPEED).getValue() * 4.0;
      }

      double score = attackBase * 2.0 + speedScore + healthScore + distanceScore;

      if (score > bestScore) {
        bestScore = score;
        best = mob;
      }
    }

    return best;
  }

  /**
   * 计算敌群中心（牧群/扫荡用）。
   */
  @Nullable
  public static Vec3 estimateHostileClusterCenter(
      FlyingSwordEntity sword, Vec3 center, double range) {
    List<LivingEntity> hostiles = collectHostiles(sword, center, range);
    if (hostiles.isEmpty()) {
      return null;
    }

    Vec3 bestCenter = null;
    double bestScore = 0.0;

    for (LivingEntity pivot : hostiles) {
      Vec3 pivotPos = pivot.position();
      Vec3 accumulator = Vec3.ZERO;
      int count = 0;
      for (LivingEntity other : hostiles) {
        if (other == pivot) continue;
        if (other.position().distanceTo(pivotPos) <= 6.0) {
          accumulator = accumulator.add(other.position());
          count++;
        }
      }
      if (count >= 2) {
        Vec3 avg = accumulator.add(pivotPos).scale(1.0 / (count + 1));
        double score = count;
        if (score > bestScore) {
          bestScore = score;
          bestCenter = avg;
        }
      }
    }

    return bestCenter;
  }

  /**
   * 查找位于外域的威胁，用于穿域 Pivot。
   */
  @Nullable
  public static LivingEntity findOuterThreat(
      FlyingSwordEntity sword, Vec3 center, double innerRadius, double maxRange) {
    List<LivingEntity> hostiles = collectHostiles(sword, center, maxRange);
    LivingEntity best = null;
    double bestScore = Double.NEGATIVE_INFINITY;

    for (LivingEntity living : hostiles) {
      double distance = Math.sqrt(living.distanceToSqr(center));
      if (distance <= innerRadius) {
        continue;
      }
      double score = distance * 0.6 + living.getMaxHealth() * 0.05;
      if (score > bestScore) {
        bestScore = score;
        best = living;
      }
    }

    return best;
  }

  /**
   * 查找远程/施法目标用于压制。
   */
  @Nullable
  public static LivingEntity findCasterOrChanneler(
      FlyingSwordEntity sword, Vec3 center, double range) {
    List<LivingEntity> hostiles = collectHostiles(sword, center, range);
    LivingEntity best = null;
    double bestScore = Double.NEGATIVE_INFINITY;

    for (LivingEntity living : hostiles) {
      double distance = Math.sqrt(living.distanceToSqr(center));
      double distanceScore = 5.0 / Math.max(1.0, distance);

      double casterScore = 0.0;
      if (living instanceof SpellcasterIllager || living instanceof Ghast || living instanceof Blaze) {
        casterScore = 10.0;
      }
      if (living instanceof Warden) {
        casterScore = 8.0;
      }

      if (living instanceof Mob mob) {
        if (mob.getAttribute(Attributes.FOLLOW_RANGE) != null) {
          casterScore += mob.getAttribute(Attributes.FOLLOW_RANGE).getValue() * 0.4;
        }
      }

      double score = casterScore + distanceScore;
      if (score > bestScore) {
        bestScore = score;
        best = living;
      }
    }

    return best;
  }

  /**
   * 搜索高速飞行或冲锋实体，用于拦截。
   */
  @Nullable
  public static InterceptCandidate findInterceptCandidate(
      FlyingSwordEntity sword, Vec3 center, double range) {
    if (!(sword.level() instanceof ServerLevel server)) {
      return null;
    }

    LivingEntity owner = sword.getOwner();
    if (owner == null) {
      return null;
    }

    AABB searchBox = new AABB(center, center).inflate(range);
    InterceptCandidate best = null;
    double bestScore = Double.NEGATIVE_INFINITY;

    for (Entity entity : server.getEntities(null, searchBox)) {
      if (entity instanceof Projectile projectile) {
        if (projectile.getOwner() == owner) continue;
        Vec3 velocity = projectile.getDeltaMovement();
        double speed = velocity.length();
        if (speed < 0.35) continue;
        double distance = projectile.position().distanceTo(center);
        double eta = distance / Math.max(0.001, speed);
        double score = speed * 6.0 + 8.0 / Math.max(1.0, distance) + Math.max(0.0, 1.2 - eta) * 4.0;
        if (projectile instanceof Fireball || projectile instanceof SmallFireball || projectile instanceof ThrownTrident) {
          score += 8.0;
        }
        Vec3 interceptPoint = projectile.position().add(velocity.scale(Math.min(eta, 1.5)));
        if (score > bestScore) {
          bestScore = score;
          best = new InterceptCandidate(null, interceptPoint, speed, eta);
        }
      } else if (entity instanceof LivingEntity living) {
        if (!isHostileForOwner(living, owner)) continue;
        Vec3 velocity = living.getDeltaMovement();
        double speed = velocity.length();
        if (speed < 0.32) continue;
        double distance = living.position().distanceTo(center);
        double eta = distance / Math.max(0.001, speed);
        double score = speed * 5.0 + 6.0 / Math.max(1.0, distance) + Math.max(0.0, 1.0 - eta) * 3.0;
        if (score > bestScore) {
          bestScore = score;
          Vec3 interceptPoint = living.position().add(velocity.scale(Math.min(eta, 1.2)));
          best = new InterceptCandidate(living, interceptPoint, speed, eta);
        }
      }
    }

    return best;
  }

  /**
   * 收集范围内的敌对实体列表。
   */
  private static List<LivingEntity> collectHostiles(
      FlyingSwordEntity sword, Vec3 center, double range) {
    List<LivingEntity> result = new ArrayList<>();
    LivingEntity owner = sword.getOwner();
    if (owner == null) {
      return result;
    }
    if (!(sword.level() instanceof ServerLevel server)) {
      return result;
    }

    AABB searchBox = new AABB(center, center).inflate(range);
    for (Entity entity : server.getEntities(null, searchBox)) {
      if (!(entity instanceof LivingEntity living)) continue;
      if (!isEligibleHostileCandidate(living, sword, owner)) continue;
      if (!isHostileForOwner(living, owner)) continue;
      result.add(living);
    }
    return result;
  }

  private static boolean isEligibleHostileCandidate(
      LivingEntity living, FlyingSwordEntity sword, LivingEntity owner) {
    if (!living.isAlive()) {
      return false;
    }
    if (living == owner || living == sword) {
      return false;
    }
    if (living instanceof Player) {
      return false;
    }
    if (living instanceof FlyingSwordEntity otherSword) {
      LivingEntity otherOwner = otherSword.getOwner();
      if (otherOwner != null && otherOwner.getUUID().equals(owner.getUUID())) {
        return false;
      }
    }
    if (living instanceof OwnableEntity ownable) {
      java.util.UUID ownerId = ownable.getOwnerUUID();
      if (ownerId != null && ownerId.equals(owner.getUUID())) {
        return false;
      }
    }
    return true;
  }

  private static boolean isHostileForOwner(LivingEntity living, LivingEntity owner) {
    if (SwordCommandCenter.hasCommandOverride(living)) {
      return true;
    }
    if (living instanceof Mob mob) {
      if (mob.getTarget() == owner) {
        return true;
      }
      return mob.getType().getCategory() == MobCategory.MONSTER;
    }
    return living.getType().getCategory() == MobCategory.MONSTER;
  }

  private static boolean isMarkedCandidate(
      @Nullable LivingEntity living,
      FlyingSwordEntity sword,
      LivingEntity owner,
      Vec3 center,
      double range) {
    if (living == null) {
      return false;
    }
    if (!isEligibleHostileCandidate(living, sword, owner)) {
      return false;
    }
    if (!isHostileForOwner(living, owner)) {
      return false;
    }
    double rangeSq = range * range;
    return living.distanceToSqr(center) <= rangeSq;
  }

  private static boolean isRecentCombatTarget(
      @Nullable LivingEntity target, int timestamp, int nowTick) {
    if (target == null) {
      return false;
    }
    return nowTick - timestamp <= MARK_EXPIRY_TICKS;
  }

  public record InterceptCandidate(
      @Nullable LivingEntity livingTarget, Vec3 interceptPoint, double speed, double eta) {}
}
