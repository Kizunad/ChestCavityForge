package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.behavior;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;

/**
 * 目标搜索工具
 *
 * <p>负责在范围内搜索最近的敌对实体
 */
public final class TargetFinder {

  private TargetFinder() {}

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

    Player owner = sword.getOwner();
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

      // 排除主人、其他玩家、自己
      if (living == owner || living instanceof Player || living == sword) {
        continue;
      }

      // 排除已死亡的实体
      if (!living.isAlive()) {
        continue;
      }

      boolean isHostile = false;

      // 检查是否敌对
      if (living instanceof Mob mob) {
        // 正在攻击主人的怪物 - 最高优先级
        if (mob.getTarget() == owner) {
          isHostile = true;
        }
        // 或者是怪物类别的Mob
        else if (mob.getType().getCategory() == MobCategory.MONSTER) {
          isHostile = true;
        }
      }
      // 非Mob但是怪物类别的生物（如末影龙、凋灵）
      else if (living.getType().getCategory() == MobCategory.MONSTER) {
        isHostile = true;
      }

      if (isHostile) {
        double distSq = living.distanceToSqr(center);
        if (distSq < minDistSq) {
          minDistSq = distSq;
          nearest = living;
        }
      }
    }

    return nearest;
  }
}
