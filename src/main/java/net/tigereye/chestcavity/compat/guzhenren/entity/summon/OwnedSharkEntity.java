package net.tigereye.chestcavity.compat.guzhenren.entity.summon;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 轻量级的召唤物追踪器。用于记录鱼鳞蛊召唤的鲨鱼实体并负责生命周期管控。
 *
 * <p>该类不会注册新的实体类型，而是包装已有的鲨鱼实体，通过 UUID 追踪、TTL 控制和 所有者标记来实现“召唤物”效果。真正的行为由鲨鱼实体自身 AI 处理。
 */
public record OwnedSharkEntity(
    UUID entityId, UUID ownerId, int tier, long createdAt, long expiresAt) {

  /**
   * 玩家在最近一次战斗内的目标记忆时间（单位：tick）。
   *
   * <p>值为 100 tick ≈ 5 秒，用于避免鲨鱼在战斗结束后长时间纠缠旧目标，也能减少频繁的 target 刷新导致的 AI 抖动。
   */
  private static final int COMBAT_MEMORY_TICKS = 100;

  /** 每次 tick 返回 true 表示召唤物仍有效。 */
  public boolean tick(ServerLevel level, Player owner, long gameTime) {
    Entity entity = level.getEntity(entityId);
    if (entity == null) {
      return false;
    }
    if (gameTime >= expiresAt) {
      entity.discard();
      return false;
    }
    if (entity instanceof TamableAnimal tamable && owner != null) {
      if (!tamable.isTame()) {
        tamable.tame(owner);
      }
    }
    if (owner != null && entity instanceof PathfinderMob mob) {
      double distanceSq = entity.distanceToSqr(owner);
      if (distanceSq > 64.0 * 64.0) {
        mob.getNavigation().moveTo(owner, 1.3);
      }
    }
    if (owner != null && entity instanceof Mob mob) {
      // 让召唤鲨鱼在战斗期间协助主人攻击最近的敌对目标。
      LivingEntity target = resolveOwnerCombatTarget(owner, mob);
      if (target != null && mob.getTarget() != target) {
        mob.setTarget(target);
      }
    }
    return true;
  }

  /** 强制移除实体。用于达到数量上限时清理旧召唤物。 */
  public void discard(Level level) {
    if (!(level instanceof ServerLevel serverLevel)) {
      return;
    }
    Entity entity = serverLevel.getEntity(entityId);
    if (entity != null) {
      entity.discard();
    }
  }

  /**
   * 推导玩家最近的敌对目标。
   *
   * <p>优先取“玩家最近攻击的实体”，若时间更近则改用“最近攻击玩家的实体”；若超过 {@link #COMBAT_MEMORY_TICKS} 则视为战斗结束。
   */
  private static LivingEntity resolveOwnerCombatTarget(Player owner, Mob shark) {
    LivingEntity attacked = owner.getLastHurtMob();
    int attackedAt = owner.getLastHurtMobTimestamp();
    LivingEntity aggressor = owner.getLastHurtByMob();
    int aggressorAt = owner.getLastHurtByMobTimestamp();

    LivingEntity candidate = null;
    int candidateTimestamp = Integer.MIN_VALUE;

    if (attacked != null
        && attacked.isAlive()
        && attacked != owner
        && attacked != shark
        && owner.tickCount - attackedAt <= COMBAT_MEMORY_TICKS) {
      candidate = attacked;
      candidateTimestamp = attackedAt;
    }

    if (aggressor != null
        && aggressor.isAlive()
        && aggressor != owner
        && aggressor != shark
        && owner.tickCount - aggressorAt <= COMBAT_MEMORY_TICKS
        && aggressorAt >= candidateTimestamp) {
      candidate = aggressor;
      candidateTimestamp = aggressorAt;
    }

    if (candidate == null) {
      return null;
    }
    // 避免友方误伤：若玩家视其为队友（例如同队伍/同阵营），则不设为目标。
    if (owner.isAlliedTo(candidate)) {
      return null;
    }
    return candidate;
  }
}
