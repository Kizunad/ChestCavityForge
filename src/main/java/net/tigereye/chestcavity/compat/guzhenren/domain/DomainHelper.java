package net.tigereye.chestcavity.compat.guzhenren.domain;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.JianXinDomain;

/**
 * 领域辅助工具
 *
 * <p>提供便捷的领域创建、查询和管理方法。
 */
public final class DomainHelper {

  private DomainHelper() {}

  /**
   * 创建剑心域
   *
   * <p>如果玩家已有剑心域，则返回已存在的领域。
   *
   * @param owner 主人
   * @param jiandaoDaohen 剑道道痕值
   * @param schoolExperience 流派经验值
   * @return 剑心域实例
   */
  public static JianXinDomain createOrGetJianXinDomain(
      Player owner, int jiandaoDaohen, int schoolExperience) {
    // 检查是否已有剑心域
    JianXinDomain existing = getJianXinDomain(owner);
    if (existing != null && existing.isValid()) {
      // 更新数值
      existing.updateJiandaoStats(jiandaoDaohen, schoolExperience);
      return existing;
    }

    // 创建新的剑心域
    int level = calculateJianXinLevel(jiandaoDaohen, schoolExperience);
    JianXinDomain domain =
        new JianXinDomain(owner, owner.position(), level, jiandaoDaohen, schoolExperience);

    // 注册到管理器
    DomainManager.getInstance().registerDomain(domain);

    // 如果在服务端，立即同步到客户端（使用通用系统）
    if (owner.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
      Vec3 center = domain.getCenter();
      net.minecraft.resources.ResourceLocation texturePath = domain.getTexturePath();
      if (texturePath != null) {
        net.tigereye.chestcavity.compat.guzhenren.domain.network.DomainSyncPayload syncPayload =
            new net.tigereye.chestcavity.compat.guzhenren.domain.network.DomainSyncPayload(
                domain.getDomainId(),
                domain.getOwnerUUID(),
                center.x,
                center.y,
                center.z,
                domain.getRadius(),
                domain.getLevel(),
                texturePath.toString(),
                domain.getPngHeightOffset(),
                domain.getPngAlpha(),
                domain.getPngRotationSpeed());
        net.tigereye.chestcavity.compat.guzhenren.domain.network.DomainNetworkHandler.sendDomainSync(
            syncPayload, center, serverLevel);
      }
    }

    return domain;
  }

  /**
   * 获取玩家的剑心域
   *
   * @param owner 主人
   * @return 剑心域，如果不存在则返回null
   */
  @Nullable
  public static JianXinDomain getJianXinDomain(Player owner) {
    for (Domain domain : DomainManager.getInstance().getDomainsByOwner(owner.getUUID())) {
      if (domain instanceof JianXinDomain jianXinDomain && domain.isValid()) {
        return jianXinDomain;
      }
    }
    return null;
  }

  /**
   * 移除玩家的剑心域
   *
   * @param owner 主人
   * @return 是否成功移除
   */
  public static boolean removeJianXinDomain(Player owner) {
    JianXinDomain domain = getJianXinDomain(owner);
    if (domain != null) {
      DomainManager.getInstance().unregisterDomain(domain.getDomainId());
      return true;
    }
    return false;
  }

  /**
   * 检查玩家是否有激活的剑心域
   *
   * @param owner 主人
   * @return 是否有剑心域
   */
  public static boolean hasActiveJianXinDomain(Player owner) {
    return getJianXinDomain(owner) != null;
  }

  /**
   * 计算剑心域等级
   *
   * @param jiandaoDaohen 剑道道痕值
   * @param schoolExperience 流派经验值
   * @return 领域等级（5-6）
   */
  private static int calculateJianXinLevel(int jiandaoDaohen, int schoolExperience) {
    double totalPower =
        jiandaoDaohen
                * net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning
                    .JianXinDomainTuning.DAOHEN_WEIGHT
            + schoolExperience
                * net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning
                    .JianXinDomainTuning.SCHOOL_EXP_WEIGHT;
    return totalPower
            > net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning
                .JianXinDomainTuning.LEVEL_THRESHOLD
        ? net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning
            .JianXinDomainTuning.MAX_LEVEL
        : net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning
            .JianXinDomainTuning.MIN_LEVEL;
  }

  /**
   * 检查实体是否在任意领域内
   *
   * @param entity 实体
   * @return 是否在领域内
   */
  public static boolean isInAnyDomain(LivingEntity entity) {
    return !DomainManager.getInstance().getDomainsAt(entity).isEmpty();
  }

  /**
   * 获取实体所在的最高等级领域
   *
   * @param entity 实体
   * @return 最高等级领域，如果不在任何领域内则返回null
   */
  @Nullable
  public static Domain getHighestLevelDomainAt(LivingEntity entity) {
    return DomainManager.getInstance().getHighestLevelDomainAt(entity);
  }

  /**
   * 检查两个玩家的剑心域是否冲突
   *
   * <p>领域冲突规则：
   * <ul>
   *   <li>高等级领域压制低等级</li>
   *   <li>同等级则双方效果都生效</li>
   * </ul>
   *
   * @param player1 玩家1
   * @param player2 玩家2
   * @return 获胜的领域，如果平局则返回null
   */
  @Nullable
  public static JianXinDomain resolveDomainConflict(Player player1, Player player2) {
    JianXinDomain domain1 = getJianXinDomain(player1);
    JianXinDomain domain2 = getJianXinDomain(player2);

    if (domain1 == null) return domain2;
    if (domain2 == null) return domain1;

    int level1 = domain1.getLevel();
    int level2 = domain2.getLevel();

    if (level1 > level2) return domain1;
    if (level2 > level1) return domain2;

    // 平局
    return null;
  }

  /**
   * 同步领域标签
   *
   * <p>遍历所有实体，更新其领域标签状态
   *
   * @param domain 领域
   */
  public static void syncDomainTags(Domain domain) {
    if (domain == null || !domain.isValid()) {
      return;
    }
    var owner = domain.getOwner();
    if (!(owner != null && owner.level() instanceof net.minecraft.server.level.ServerLevel level)) {
      return;
    }

    // 收集边界内外的实体，并同步 in/out 标签
    var bounds = domain.getBounds().inflate(1.0); // 略微放大，减少抖动
    java.util.List<net.minecraft.world.entity.LivingEntity> nearby =
        level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, bounds);

    java.util.Set<java.util.UUID> seen = new java.util.HashSet<>();
    for (var e : nearby) {
      if (!domain.isInDomain(e)) continue;
      DomainTags.markEnterSwordDomain(e, domain.getDomainId(), domain.getOwnerUUID(), domain.getLevel());
      seen.add(e.getUUID());
    }

    // 对同世界内的带有本领域 owner 的实体，若不在范围则移除标签
    // 仅检查视距内的实体避免 O(N) 全表扫描
    var looseBounds = bounds.inflate(8.0);
    java.util.List<net.minecraft.world.entity.LivingEntity> maybeTagged =
        level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, looseBounds);
    for (var e : maybeTagged) {
      java.util.UUID tagOwner = DomainTags.getSwordDomainOwner(e);
      if (tagOwner == null) continue;
      if (!tagOwner.equals(domain.getOwnerUUID())) continue;
      if (!domain.isInDomain(e)) {
        DomainTags.markLeaveSwordDomain(e);
      }
    }
  }
}
