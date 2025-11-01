package net.tigereye.chestcavity.compat.guzhenren.domain;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 领域抽象基类
 *
 * <p>提供通用的领域实现，子类只需关注具体的效果逻辑。
 */
public abstract class AbstractDomain implements Domain {

  protected final UUID domainId;
  protected final UUID ownerUUID;
  protected Vec3 center;
  protected int level;
  protected boolean valid = true;

  /**
   * 构造领域
   *
   * @param owner 领域主人
   * @param center 中心位置
   * @param level 初始等级
   */
  public AbstractDomain(LivingEntity owner, Vec3 center, int level) {
    this.domainId = UUID.randomUUID();
    this.ownerUUID = owner.getUUID();
    this.center = center;
    this.level = level;
  }

  @Override
  public UUID getDomainId() {
    return domainId;
  }

  @Override
  @Nullable
  public LivingEntity getOwner() {
    // 需要从世界中查找主人（可能已离线或死亡）
    // 子类可以缓存owner引用以提高性能
    return null;
  }

  @Override
  public UUID getOwnerUUID() {
    return ownerUUID;
  }

  @Override
  public int getLevel() {
    return level;
  }

  @Override
  public void setLevel(int level) {
    this.level = Math.max(1, level);
  }

  @Override
  public Vec3 getCenter() {
    return center;
  }

  @Override
  public void setCenter(Vec3 center) {
    this.center = center;
  }

  @Override
  public boolean isFriendly(LivingEntity entity) {
    // 主人本身
    if (entity.getUUID().equals(ownerUUID)) {
      return true;
    }

    LivingEntity owner = getOwner();
    if (owner == null) {
      return false;
    }

    // 玩家队友判定
    if (owner instanceof Player ownerPlayer && entity instanceof Player targetPlayer) {
      // 简化判定：同队伍 = 友方
      return ownerPlayer.getTeam() != null
          && ownerPlayer.getTeam().equals(targetPlayer.getTeam());
    }

    // 其他情况默认为敌方
    return false;
  }

  @Override
  public void tick(ServerLevel level) {
    // 检查主人是否存活
    LivingEntity owner = getOwner();
    if (owner == null || !owner.isAlive() || owner.isRemoved()) {
      valid = false;
      return;
    }

    // 更新中心位置（跟随主人）
    updateCenterPosition(owner);

    // 获取范围内的所有生物
    AABB bounds = getBounds();
    List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, bounds);

    // 对每个实体应用效果
    for (LivingEntity entity : entities) {
      if (isInDomain(entity)) {
        boolean friendly = isFriendly(entity);
        applyEffects(level, entity, friendly);
      }
    }
  }

  /**
   * 更新领域中心位置
   *
   * <p>默认行为：跟随主人位置
   * <p>子类可以覆盖以实现不同的定位逻辑
   *
   * @param owner 主人
   */
  protected void updateCenterPosition(LivingEntity owner) {
    setCenter(owner.position());
  }

  @Override
  public boolean isValid() {
    return valid;
  }

  @Override
  public void destroy() {
    valid = false;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Domain other)) return false;
    return domainId.equals(other.getDomainId());
  }

  @Override
  public int hashCode() {
    return domainId.hashCode();
  }
}
