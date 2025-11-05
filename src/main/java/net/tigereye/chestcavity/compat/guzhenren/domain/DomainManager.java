package net.tigereye.chestcavity.compat.guzhenren.domain;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;

/**
 * 领域管理器
 *
 * <p>全局单例，管理所有活跃的领域。
 *
 * <p>功能：
 * <ul>
 *   <li>注册和移除领域</li>
 *   <li>每tick更新所有领域</li>
 *   <li>查询位置或实体所在的领域</li>
 *   <li>领域优先级和冲突处理</li>
 * </ul>
 */
public final class DomainManager {

  private static final DomainManager INSTANCE = new DomainManager();

  // 所有活跃的领域（UUID -> Domain）
  private final Map<UUID, Domain> domains = new ConcurrentHashMap<>();

  // 按世界分组的领域（用于加速查询）
  private final Map<ServerLevel, Set<UUID>> domainsByLevel = new ConcurrentHashMap<>();

  private DomainManager() {}

  public static DomainManager getInstance() {
    return INSTANCE;
  }

  /**
   * 注册领域
   *
   * @param domain 领域实例
   * @return 是否注册成功
   */
  public boolean registerDomain(Domain domain) {
    if (domain == null) {
      ChestCavity.LOGGER.warn("[DomainManager] Attempted to register null domain");
      return false;
    }

    UUID id = domain.getDomainId();
    if (domains.containsKey(id)) {
      ChestCavity.LOGGER.warn(
          "[DomainManager] Domain {} already registered, skipping", id);
      return false;
    }

    domains.put(id, domain);
    ChestCavity.LOGGER.info(
        "[DomainManager] Registered domain {} (type: {}, level: {})",
        id,
        domain.getDomainType(),
        domain.getLevel());
    return true;
  }

  /**
   * 移除领域
   *
   * @param domainId 领域ID
   * @return 被移除的领域，如果不存在则返回null
   */
  @Nullable
  public Domain unregisterDomain(UUID domainId) {
    Domain removed = domains.remove(domainId);
    if (removed != null) {
      removed.destroy();
      ChestCavity.LOGGER.info(
          "[DomainManager] Unregistered domain {} (type: {})",
          domainId,
          removed.getDomainType());
    }
    return removed;
  }

  /**
   * 获取领域
   *
   * @param domainId 领域ID
   * @return 领域实例，如果不存在则返回null
   */
  @Nullable
  public Domain getDomain(UUID domainId) {
    return domains.get(domainId);
  }

  /**
   * 获取所有领域
   *
   * @return 领域列表（只读副本）
   */
  public Collection<Domain> getAllDomains() {
    return new ArrayList<>(domains.values());
  }

  /**
   * 获取指定类型的所有领域
   *
   * @param type 领域类型
   * @return 匹配的领域列表
   */
  public List<Domain> getDomainsByType(String type) {
    List<Domain> result = new ArrayList<>();
    for (Domain domain : domains.values()) {
      if (domain.getDomainType().equals(type)) {
        result.add(domain);
      }
    }
    return result;
  }

  /**
   * 获取实体所在的所有领域
   *
   * @param entity 实体
   * @return 包含该实体的领域列表
   */
  public List<Domain> getDomainsAt(Entity entity) {
    return getDomainsAt(entity.position());
  }

  /**
   * 获取位置所在的所有领域
   *
   * @param pos 位置
   * @return 包含该位置的领域列表
   */
  public List<Domain> getDomainsAt(Vec3 pos) {
    List<Domain> result = new ArrayList<>();
    for (Domain domain : domains.values()) {
      if (domain.isInDomain(pos)) {
        result.add(domain);
      }
    }
    return result;
  }

  /**
   * 获取位置所在的所有领域
   *
   * @param pos 方块坐标
   * @return 包含该位置的领域列表
   */
  public List<Domain> getDomainsAt(BlockPos pos) {
    return getDomainsAt(Vec3.atCenterOf(pos));
  }

  /**
   * 获取实体所在的最高等级领域
   *
   * <p>用于处理多领域重叠时的优先级
   *
   * @param entity 实体
   * @return 最高等级的领域，如果不在任何领域内则返回null
   */
  @Nullable
  public Domain getHighestLevelDomainAt(Entity entity) {
    List<Domain> domainsAt = getDomainsAt(entity);
    if (domainsAt.isEmpty()) {
      return null;
    }

    Domain highest = domainsAt.get(0);
    for (int i = 1; i < domainsAt.size(); i++) {
      Domain current = domainsAt.get(i);
      if (current.getLevel() > highest.getLevel()) {
        highest = current;
      }
    }
    return highest;
  }

  /**
   * 获取主人的所有领域
   *
   * @param ownerUUID 主人UUID
   * @return 该主人创建的所有领域
   */
  public List<Domain> getDomainsByOwner(UUID ownerUUID) {
    List<Domain> result = new ArrayList<>();
    for (Domain domain : domains.values()) {
      if (domain.getOwnerUUID().equals(ownerUUID)) {
        result.add(domain);
      }
    }
    return result;
  }

  /**
   * 检查主人是否已有指定类型的领域
   *
   * @param ownerUUID 主人UUID
   * @param type 领域类型
   * @return 是否存在
   */
  public boolean hasActiveDomain(UUID ownerUUID, String type) {
    for (Domain domain : domains.values()) {
      if (domain.getOwnerUUID().equals(ownerUUID)
          && domain.getDomainType().equals(type)
          && domain.isValid()) {
        return true;
      }
    }
    return false;
  }

  /**
   * 移除主人的所有领域
   *
   * @param ownerUUID 主人UUID
   * @return 移除的领域数量
   */
  public int removeAllDomainsByOwner(UUID ownerUUID) {
    List<Domain> toRemove = getDomainsByOwner(ownerUUID);
    for (Domain domain : toRemove) {
      unregisterDomain(domain.getDomainId());
    }
    return toRemove.size();
  }

  /**
   * 每tick更新所有领域
   *
   * <p>应该从服务端tick事件调用
   *
   * @param level 当前世界
   */
  public void tick(ServerLevel level) {
    // 收集无效的领域
    List<UUID> toRemove = new ArrayList<>();

    for (Domain domain : domains.values()) {
      try {
        // 检查领域是否仍然有效
        if (!domain.isValid()) {
          toRemove.add(domain.getDomainId());
          continue;
        }

        // 更新领域
        domain.tick(level);
      } catch (Exception e) {
        ChestCavity.LOGGER.error(
            "[DomainManager] Error ticking domain {} (type: {})",
            domain.getDomainId(),
            domain.getDomainType(),
            e);
        toRemove.add(domain.getDomainId());
      }
    }

    // 移除无效的领域
    for (UUID id : toRemove) {
      unregisterDomain(id);
    }

    // 裂剑蛊：更新裂隙与剑域的协同状态（每tick）
    net.tigereye.chestcavity.compat.guzhenren.rift.RiftDomainSynergy
        .tickDomainSynergy(level);
  }

  /**
   * 清空所有领域
   *
   * <p>谨慎使用，通常只在服务器关闭时调用
   */
  public void clearAll() {
    for (Domain domain : domains.values()) {
      domain.destroy();
    }
    domains.clear();
    domainsByLevel.clear();
    ChestCavity.LOGGER.info("[DomainManager] Cleared all domains");
  }

  /**
   * 获取领域统计信息
   *
   * @return 统计信息字符串
   */
  public String getStats() {
    Map<String, Integer> typeCount = new HashMap<>();
    for (Domain domain : domains.values()) {
      String type = domain.getDomainType();
      typeCount.put(type, typeCount.getOrDefault(type, 0) + 1);
    }

    StringBuilder sb = new StringBuilder();
    sb.append("DomainManager Stats:\n");
    sb.append("  Total domains: ").append(domains.size()).append("\n");
    sb.append("  By type:\n");
    for (Map.Entry<String, Integer> entry : typeCount.entrySet()) {
      sb.append("    ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
    }
    return sb.toString();
  }
}
