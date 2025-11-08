package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.runtime.jian_suo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.util.CombatEntityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 剑梭蛊运行时逻辑工具类。
 *
 * <p>提供：
 * <ul>
 *   <li>突进与路径伤害（逐帧碰撞预测、胶囊采样、去重）</li>
 *   <li>安全后退（碰撞检测）</li>
 *   <li>反向向量计算</li>
 * </ul>
 */
public final class JianSuoRuntime {

  private static final Logger LOGGER = LoggerFactory.getLogger(JianSuoRuntime.class);

  /** 命中去重记录：entityId -> lastHitTick。*/
  private static final ConcurrentHashMap<Integer, Long> LAST_HIT_TICK = new ConcurrentHashMap<>();

  /** 后退安全检测步数。*/
  private static final int BACKSTEP_SAFE_STEPS = 8;

  private JianSuoRuntime() {}

  /**
   * 执行突进并沿路径造成伤害。
   *
   * <p>特性：
   * <ul>
   *   <li>逐帧推进，每步检查碰撞，遇到障碍提前终止</li>
   *   <li>胶囊体采样命中，过滤友方与自身</li>
   *   <li>命中去重（基于时间窗口）</li>
   *   <li>应用轻微击退</li>
   * </ul>
   *
   * @param self 突进者（玩家或飞剑）
   * @param dir 突进方向（已归一化）
   * @param totalDist 总距离（格）
   * @param damage 路径伤害
   * @param halfWidth 胶囊体半宽
   * @param maxSteps 最大步数
   * @param hitDedupTicks 命中去重窗口（ticks）
   * @return 实际移动距离
   */
  public static double tryDashAndDamage(
      Entity self,
      Vec3 dir,
      double totalDist,
      double damage,
      double halfWidth,
      int maxSteps,
      int hitDedupTicks) {
    // 如果 self 是 LivingEntity，则用自己作为攻击者
    LivingEntity attacker = self instanceof LivingEntity living ? living : null;
    return tryDashAndDamage(self, attacker, dir, totalDist, damage, halfWidth, maxSteps, hitDedupTicks);
  }

  /**
   * 执行突进并沿路径造成伤害（带显式攻击者参数）。
   *
   * <p>特性：
   * <ul>
   *   <li>逐帧推进，每步检查碰撞，遇到障碍提前终止</li>
   *   <li>胶囊体采样命中，过滤友方与自身</li>
   *   <li>命中去重（基于时间窗口）</li>
   *   <li>应用轻微击退</li>
   * </ul>
   *
   * @param self 突进者（玩家或飞剑）
   * @param attacker 攻击者（用于敌我判断和伤害源），可为 null
   * @param dir 突进方向（已归一化）
   * @param totalDist 总距离（格）
   * @param damage 路径伤害
   * @param halfWidth 胶囊体半宽
   * @param maxSteps 最大步数
   * @param hitDedupTicks 命中去重窗口（ticks）
   * @return 实际移动距离
   */
  public static double tryDashAndDamage(
      Entity self,
      @Nullable LivingEntity attacker,
      Vec3 dir,
      double totalDist,
      double damage,
      double halfWidth,
      int maxSteps,
      int hitDedupTicks) {

    Level level = self.level();
    if (level.isClientSide()) {
      return 0.0;
    }

    ServerLevel serverLevel = (ServerLevel) level;
    long now = serverLevel.getGameTime();

    Vec3 step = dir.normalize().scale(totalDist / maxSteps);
    Vec3 startPos = self.position();
    Vec3 currentPos = startPos;

    Set<Integer> sessionHitEntities = new HashSet<>();
    double actualDist = 0.0;

    for (int i = 0; i < maxSteps; i++) {
      Vec3 nextPos = currentPos.add(step);

      // 碰撞检测：计算移动后的 AABB
      AABB movedBB = self.getBoundingBox().move(step);
      if (!level.noCollision(self, movedBB)) {
        // 遇到障碍，提前终止
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(
              "[JianSuoRuntime] Dash blocked at step {}/{}, pos={}", i, maxSteps, nextPos);
        }
        break;
      }

      // 安全移动
      self.setPos(nextPos);
      actualDist += step.length();

      // 胶囊采样：从上一帧到当前帧的路径
      AABB sweepBox =
          new AABB(currentPos, nextPos)
              .inflate(halfWidth, self.getBbHeight() * 0.5, halfWidth);

      List<LivingEntity> candidates =
          level.getEntitiesOfClass(LivingEntity.class, sweepBox, entity -> entity != self);

      for (LivingEntity target : candidates) {
        // 友方过滤：使用 attacker（如果提供）进行敌我判断
        if (attacker != null) {
          if (!CombatEntityUtil.areEnemies(attacker, target)) {
            continue;
          }
        }
        // 如果没有 attacker 但 self 是 LivingEntity，也尝试判断
        else if (self instanceof LivingEntity living) {
          if (!CombatEntityUtil.areEnemies(living, target)) {
            continue;
          }
        }
        // 否则跳过友方过滤（无法判断，默认允许命中）

        int targetId = target.getId();

        // 会话内去重（单次突进只命中一次）
        if (sessionHitEntities.contains(targetId)) {
          continue;
        }

        // 全局时间窗口去重
        long lastHit = LAST_HIT_TICK.getOrDefault(targetId, 0L);
        if (now - lastHit < hitDedupTicks) {
          continue;
        }

        // 造成伤害：优先使用 attacker 作为伤害源
        DamageSource damageSource;
        if (attacker != null) {
          damageSource = attacker.damageSources().mobAttack(attacker);
        } else if (self instanceof LivingEntity living) {
          damageSource = living.damageSources().mobAttack(living);
        } else {
          damageSource = target.damageSources().generic();
        }

        boolean hurt = target.hurt(damageSource, (float) damage);
        if (hurt) {
          // 轻微击退
          Vec3 knockback = dir.normalize().scale(0.15);
          target.push(knockback.x, 0.05, knockback.z);

          // 记录命中
          sessionHitEntities.add(targetId);
          LAST_HIT_TICK.put(targetId, now);

          // 特效
          spawnHitEffect(serverLevel, target.position());

          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                "[JianSuoRuntime] Dash hit entity {} at pos={}, damage={}",
                target.getName().getString(),
                target.position(),
                damage);
          }
        }
      }

      currentPos = nextPos;
    }

    // 起手特效
    spawnDashTrail(serverLevel, startPos, currentPos);

    return actualDist;
  }

  /**
   * 计算反向向量（用于后退）。
   *
   * <p>优先级：
   * <ol>
   *   <li>若伤害源有位置，则从来源指向受击者的反方向</li>
   *   <li>否则，取受击者朝向的反方向</li>
   * </ol>
   *
   * @param victim 受击者
   * @param source 伤害源
   * @return 反向向量（未归一化）
   */
  public static Vec3 backstepVector(LivingEntity victim, @Nullable DamageSource source) {
    if (source != null && source.getSourcePosition() != null) {
      Vec3 srcPos = source.getSourcePosition();
      Vec3 victimPos = victim.position();
      return victimPos.subtract(srcPos);
    }

    // 退化：朝向反方向
    return victim.getLookAngle().scale(-1.0);
  }

  /**
   * 安全后退（尝试分步检测碰撞，失败则退化为速度设定）。
   *
   * @param self 实体
   * @param delta 后退向量（已包含距离）
   */
  public static void safeSlide(LivingEntity self, Vec3 delta) {
    Level level = self.level();
    Vec3 stepDelta = delta.scale(1.0 / BACKSTEP_SAFE_STEPS);

    Vec3 currentPos = self.position();
    int successfulSteps = 0;

    for (int i = 0; i < BACKSTEP_SAFE_STEPS; i++) {
      Vec3 nextPos = currentPos.add(stepDelta);
      AABB movedBB = self.getBoundingBox().move(stepDelta);

      if (level.noCollision(self, movedBB)) {
        self.setPos(nextPos);
        currentPos = nextPos;
        successfulSteps++;
      } else {
        // 遇到障碍，终止
        break;
      }
    }

    // 若完全无法移动，退化为设定速度
    if (successfulSteps == 0) {
      self.setDeltaMovement(delta.scale(0.35));
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[JianSuoRuntime] safeSlide: {}/{} steps, final pos={}",
          successfulSteps,
          BACKSTEP_SAFE_STEPS,
          self.position());
    }
  }

  /**
   * 播放突进轨迹特效（轻量）。
   */
  private static void spawnDashTrail(ServerLevel level, Vec3 start, Vec3 end) {
    Vec3 delta = end.subtract(start);
    int particleCount = Math.max(3, (int) (delta.length() * 2));

    for (int i = 0; i < particleCount; i++) {
      double t = i / (double) particleCount;
      Vec3 pos = start.add(delta.scale(t));
      level.sendParticles(
          ParticleTypes.SWEEP_ATTACK, pos.x, pos.y + 0.5, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    level.playSound(
        null, start.x, start.y, start.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.3f, 1.2f);
  }

  /**
   * 播放命中特效。
   */
  private static void spawnHitEffect(ServerLevel level, Vec3 pos) {
    level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y + 1.0, pos.z, 5, 0.2, 0.5, 0.2, 0.05);
    level.playSound(
        null, pos.x, pos.y, pos.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.2f, 1.5f);
  }

  /**
   * 播放躲避特效。
   */
  public static void spawnEvadeEffect(ServerLevel level, Vec3 pos) {
    level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.5, pos.z, 8, 0.3, 0.3, 0.3, 0.05);
    level.playSound(
        null, pos.x, pos.y, pos.z, SoundEvents.WIND_CHARGE_SHOOT, SoundSource.PLAYERS, 0.1f, 1.0f);
  }
}
