package net.tigereye.chestcavity.compat.guzhenren.entity.summon;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
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
}
