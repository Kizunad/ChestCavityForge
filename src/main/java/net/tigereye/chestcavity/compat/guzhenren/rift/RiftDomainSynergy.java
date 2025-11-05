package net.tigereye.chestcavity.compat.guzhenren.rift;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.domain.Domain;
import net.tigereye.chestcavity.compat.guzhenren.domain.DomainManager;

/**
 * 裂隙与剑域协同系统
 *
 * <p>处理裂隙与剑域的通用协同效果：
 * <ul>
 *   <li>剑域内裂隙衰减速率减半</li>
 *   <li>剑域内裂隙共鸣频率加倍</li>
 *   <li>剑域激活时：移动裂隙到剑域边缘形成环状剑阵</li>
 * </ul>
 *
 * <p>适用于所有类型的剑域（青莲、幻剑、等）
 */
public final class RiftDomainSynergy {

  private RiftDomainSynergy() {}

  /** 环状剑阵：裂隙之间的最小角度间隔（度） */
  private static final double MIN_ANGLE_SPACING = 15.0;

  /**
   * 应用剑域协同效果
   *
   * <p>将附近的裂隙移动到剑域边缘，形成环状剑阵
   *
   * @param domain 剑域（任意类型）
   * @param level 世界
   */
  public static void applyDomainSynergy(Domain domain, ServerLevel level) {
    Vec3 center = domain.getCenter();
    double radius = domain.getRadius();

    // 查找剑域附近的所有裂隙
    List<RiftEntity> nearbyRifts =
        RiftManager.getInstance().getRiftsNear(level, center, radius * 1.5);

    if (nearbyRifts.isEmpty()) {
      return;
    }

    // 计算每个裂隙应该放置的位置（均匀分布在圆周上）
    int riftCount = nearbyRifts.size();
    double angleStep = 360.0 / riftCount;

    // 确保角度间隔不小于最小值
    if (angleStep < MIN_ANGLE_SPACING) {
      angleStep = MIN_ANGLE_SPACING;
    }

    for (int i = 0; i < nearbyRifts.size(); i++) {
      RiftEntity rift = nearbyRifts.get(i);

      // 计算目标位置（剑域边缘）
      double angle = Math.toRadians(i * angleStep);
      Vec3 targetPos =
          center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);

      // 贴地
      net.minecraft.core.BlockPos blockPos = net.minecraft.core.BlockPos.containing(targetPos);
      net.minecraft.core.BlockPos groundPos =
          level.getHeightmapPos(
              net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
              blockPos);
      Vec3 finalPos = new Vec3(targetPos.x, groundPos.getY() + 0.1, targetPos.z);

      // 平滑移动裂隙（传送）
      rift.setPos(finalPos);

      // 标记裂隙在剑域内（影响衰减速率）
      rift.setInDomain(true);

      ChestCavity.LOGGER.debug(
          "[RiftDomainSynergy] Moved rift to domain edge at angle {} degrees, pos {}",
          i * angleStep,
          finalPos);
    }

    ChestCavity.LOGGER.info(
        "[RiftDomainSynergy] Arranged {} rifts in circular formation around {} domain",
        nearbyRifts.size(),
        domain.getDomainType());
  }

  /**
   * 更新裂隙的剑域状态
   *
   * <p>定期调用，检查裂隙是否在剑域内，更新其状态
   *
   * @param level 世界
   */
  public static void updateRiftDomainStatus(ServerLevel level) {
    List<Domain> domains = DomainManager.getInstance().getAllDomains().stream().toList();

    for (RiftEntity rift : RiftManager.getInstance().getAllRifts()) {
      if (!rift.isAlive() || !(rift.level() instanceof ServerLevel)) {
        continue;
      }

      boolean inAnyDomain = false;

      // 检查是否在任何剑域内
      for (Domain domain : domains) {
        if (domain.isInDomain(rift.position())) {
          inAnyDomain = true;
          break;
        }
      }

      rift.setInDomain(inAnyDomain);
    }
  }

  /**
   * 获取共鸣频率加成
   *
   * <p>如果裂隙在任何剑域内，共鸣频率加倍
   *
   * @param rift 裂隙
   * @return 共鸣频率倍数（1.0 = 正常，2.0 = 加倍）
   */
  public static double getResonanceFrequencyMultiplier(RiftEntity rift) {
    // 检查是否在剑域内
    if (!(rift.level() instanceof ServerLevel level)) {
      return 1.0;
    }

    List<Domain> domains = DomainManager.getInstance().getDomainsAt(rift.position());

    // 如果在任何剑域内，频率加倍
    if (!domains.isEmpty()) {
      return 2.0;
    }

    return 1.0;
  }

  /**
   * 当剑域激活时调用
   *
   * <p>自动将附近的裂隙排列成环状剑阵
   *
   * @param owner 领域所有者
   * @param level 世界
   */
  public static void onDomainActivated(LivingEntity owner, ServerLevel level) {
    // 查找所有者的所有剑域
    List<Domain> domains = DomainManager.getInstance().getAllDomains().stream()
        .filter(d -> d.getOwner() == owner)
        .toList();

    for (Domain domain : domains) {
      applyDomainSynergy(domain, level);
    }
  }

  /**
   * Tick所有剑域协同效果
   *
   * <p>定期更新裂隙与剑域的协同状态
   *
   * @param level 世界
   */
  public static void tickDomainSynergy(ServerLevel level) {
    // 更新所有裂隙的剑域状态
    updateRiftDomainStatus(level);
  }
}
