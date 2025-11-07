package net.tigereye.chestcavity.compat.guzhenren.rift;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;

/**
 * 裂隙管理器
 *
 * <p>全局单例，管理所有裂隙实体。
 *
 * <p>功能：
 * <ul>
 *   <li>注册和移除裂隙</li>
 *   <li>管理共鸣链（6格内的裂隙）</li>
 *   <li>共鸣传播</li>
 *   <li>裂隙查询</li>
 * </ul>
 */
public final class RiftManager {

  private static final RiftManager INSTANCE = new RiftManager();

  /** 共鸣链距离（方块距离，欧式） */
  private static final int RESONANCE_CHAIN_DISTANCE = RiftTuning.RESONANCE_CHAIN_DISTANCE;

  /** 共鸣波范围（格） */
  private static final double RESONANCE_WAVE_RADIUS = RiftTuning.RESONANCE_WAVE_RADIUS;

  /** 所有活跃的裂隙（UUID -> RiftEntity） */
  private final Map<UUID, RiftEntity> rifts = new ConcurrentHashMap<>();

  /** 按世界分组的裂隙（用于加速查询） */
  private final Map<ServerLevel, Set<UUID>> riftsByLevel = new ConcurrentHashMap<>();

  /** 伤害限频器：目标UUID -> 上次命中时间(gameTime) */
  private final Map<UUID, Long> damageRateLimiter = new ConcurrentHashMap<>();

  /** 限频器的清理计数器（每N次写入尝试清理） */
  private int rateLimiterWriteCount = 0;

  /** 限频器清理间隔（次数） */
  private static final int RATE_LIMITER_CLEAN_INTERVAL = 100;

  private RiftManager() {}

  public static RiftManager getInstance() {
    return INSTANCE;
  }

  /**
   * 注册裂隙
   *
   * @param rift 裂隙实体
   */
  public void registerRift(RiftEntity rift) {
    if (rift == null || rift.level() == null || rift.level().isClientSide) {
      return;
    }

    UUID id = rift.getUUID();
    rifts.put(id, rift);

    ServerLevel level = (ServerLevel) rift.level();
    riftsByLevel.computeIfAbsent(level, k -> ConcurrentHashMap.newKeySet()).add(id);

    ChestCavity.LOGGER.debug(
        "[RiftManager] Registered rift {} (type: {}) at {}",
        id,
        rift.getRiftType(),
        rift.position());

    // 新裂隙加入：尝试同步共鸣链的剩余时间（平均值），便于链路收敛
    try {
      syncResonanceChainDuration(rift);
    } catch (Exception e) {
      ChestCavity.LOGGER.warn(
          "[RiftManager] Failed syncing chain duration on register for {}", id, e);
    }
  }

  /**
   * 移除裂隙
   *
   * @param rift 裂隙实体
   */
  public void unregisterRift(RiftEntity rift) {
    if (rift == null) {
      return;
    }

    UUID id = rift.getUUID();
    rifts.remove(id);

    if (rift.level() instanceof ServerLevel level) {
      Set<UUID> levelRifts = riftsByLevel.get(level);
      if (levelRifts != null) {
        levelRifts.remove(id);
      }
    }

    ChestCavity.LOGGER.debug("[RiftManager] Unregistered rift {}", id);
  }

  /**
   * 获取指定位置附近的所有裂隙
   *
   * @param level 世界
   * @param pos 位置
   * @param radius 半径
   * @return 裂隙列表
   */
  public List<RiftEntity> getRiftsNear(ServerLevel level, Vec3 pos, double radius) {
    Set<UUID> levelRifts = riftsByLevel.get(level);
    if (levelRifts == null || levelRifts.isEmpty()) {
      return Collections.emptyList();
    }

    List<RiftEntity> result = new ArrayList<>();
    double radiusSq = radius * radius;

    for (UUID riftId : levelRifts) {
      RiftEntity rift = rifts.get(riftId);
      if (rift != null && rift.isAlive()) {
        if (rift.position().distanceToSqr(pos) <= radiusSq) {
          result.add(rift);
        }
      }
    }

    return result;
  }

  /**
   * 获取共鸣链
   *
   * <p>返回与指定裂隙距离≤6格的所有其他裂隙
   *
   * @param rift 裂隙
   * @return 共鸣链中的裂隙列表（不包括自己）
   */
  public List<RiftEntity> getResonanceChain(RiftEntity rift) {
    if (rift == null || !(rift.level() instanceof ServerLevel level)) {
      return Collections.emptyList();
    }

    List<RiftEntity> chain = new ArrayList<>();
    Vec3 pos = rift.position();

    Set<UUID> levelRifts = riftsByLevel.get(level);
    if (levelRifts == null) {
      return chain;
    }

    double radiusSq = RESONANCE_CHAIN_DISTANCE * RESONANCE_CHAIN_DISTANCE;
    for (UUID riftId : levelRifts) {
      RiftEntity other = rifts.get(riftId);
      if (other == null || other == rift || !other.isAlive()) {
        continue;
      }

      if (other.position().distanceToSqr(pos) <= radiusSq) {
        chain.add(other);
      }
    }

    return chain;
  }

  /**
   * 计算共鸣链加成
   *
   * <p>每个共鸣链中的裂隙伤害+10%
   *
   * @param rift 裂隙
   * @return 伤害加成倍率（1.0 = 无加成，1.3 = +30%）
   */
  public double getResonanceChainBonus(RiftEntity rift) {
    List<RiftEntity> chain = getResonanceChain(rift);
    return 1.0 + (chain.size() * RiftTuning.RESONANCE_CHAIN_BONUS_PER_NODE);
  }

  /**
   * 同步共鸣链的剩余时间
   *
   * <p>将共鸣链中所有裂隙的剩余时间同步为平均值
   *
   * @param rift 触发同步的裂隙
   */
  public void syncResonanceChainDuration(RiftEntity rift) {
    List<RiftEntity> chain = getResonanceChain(rift);
    if (chain.isEmpty()) {
      return;
    }

    // 计算平均剩余时间（包括自己）
    int totalTicks = rift.getRemainingTicks();
    for (RiftEntity other : chain) {
      totalTicks += other.getRemainingTicks();
    }

    int avgTicks = totalTicks / (chain.size() + 1);

    // 应用到所有裂隙
    rift.setRemainingTicks(avgTicks);
    for (RiftEntity other : chain) {
      other.setRemainingTicks(avgTicks);
    }

    ChestCavity.LOGGER.debug(
        "[RiftManager] Synced resonance chain duration: {} rifts, avg {} ticks",
        chain.size() + 1,
        avgTicks);
  }

  /**
   * 触发共鸣波
   *
   * <p>从指定裂隙开始，触发共鸣波伤害，并链式传播到共鸣链中的其他裂隙
   *
   * @param originRift 起始裂隙
   * @param instigator 触发者（用于伤害来源）
   */
  public void triggerResonanceWave(RiftEntity originRift, @Nullable LivingEntity instigator) {
    if (!(originRift.level() instanceof ServerLevel level)) {
      return;
    }

    Set<RiftEntity> propagated = new HashSet<>();
    propagateResonance(originRift, instigator, level, propagated);
  }

  /**
   * 递归传播共鸣
   *
   * @param rift 当前裂隙
   * @param instigator 触发者
   * @param level 世界
   * @param propagated 已传播的裂隙集合（防止重复）
   */
  private void propagateResonance(
      RiftEntity rift,
      @Nullable LivingEntity instigator,
      ServerLevel level,
      Set<RiftEntity> propagated) {

    // 防止重复传播
    if (propagated.contains(rift)) {
      return;
    }
    propagated.add(rift);

    // 触发当前裂隙的共鸣
    rift.triggerResonance();

    // 造成共鸣波伤害
    dealResonanceWaveDamage(rift, instigator, level);

    // 传播到共鸣链中的其他裂隙
    List<RiftEntity> chain = getResonanceChain(rift);
    for (RiftEntity nextRift : chain) {
      // 波动特效：显示能量从当前裂隙传播到下一个裂隙
      RiftFx.waveFx(level, rift.position(), nextRift.position());

      // 延迟传播（模拟波动传播效果）
      level
          .getServer()
          .tell(
              new net.minecraft.server.TickTask(
                  level.getServer().getTickCount()
                      + RiftTuning.RESONANCE_PROPAGATION_DELAY_TICKS,
                  () -> propagateResonance(nextRift, instigator, level, propagated)));
    }
  }

  /**
   * 造成共鸣波伤害
   *
   * @param rift 裂隙
   * @param instigator 触发者
   * @param level 世界
   */
  private void dealResonanceWaveDamage(
      RiftEntity rift, @Nullable LivingEntity instigator, ServerLevel level) {

    Vec3 pos = rift.position();
    double radius = RESONANCE_WAVE_RADIUS;

    // 获取范围内的所有生物
    List<LivingEntity> targets =
        level.getEntitiesOfClass(
            LivingEntity.class,
            rift.getBoundingBox().inflate(radius),
            entity -> entity.position().distanceTo(pos) <= radius);

    // 计算伤害（基础伤害 × 链加成 × 衰减倍率 × 道痕增幅）
    double baseWaveDamage = RiftTuning.RESONANCE_WAVE_BASE_DAMAGE;
    double chainBonus = getResonanceChainBonus(rift);

    // 道痕增幅取裂隙所有者
    double daoHen = 0.0;
    LivingEntity owner = rift.getOwner();
    LivingEntity master = null;
    if (owner instanceof net.tigereye.chestcavity.compat.guzhenren.flyingsword
        .FlyingSwordEntity sword) {
      master = sword.getOwner();
    }
    if (owner != null) {
      daoHen =
          net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps
              .openHandle(owner)
              .map(
                  h ->
                      net.tigereye.chestcavity.compat.guzhenren.util.behavior.DaoHenResourceOps.get(
                          h, "daohen_jiandao"))
              .orElse(0.0);
    }
    double per10k =
        (rift.getRiftType() == RiftType.MAJOR)
            ? RiftTuning.DAMAGE_PER_10K_MAJOR
            : RiftTuning.DAMAGE_PER_10K_MINOR;
    double daoHenScale = 1.0 + (daoHen / 10000.0) * per10k;

    float finalDamage =
        (float) (baseWaveDamage * chainBonus * rift.getDamageMultiplier() * daoHenScale);

    // 注：owner 已在上方获取

    long now = currentRateLimitTimestamp(level);
    for (LivingEntity target : targets) {
      // 跳过所有者、触发者及友军（包含释放者以及其Owner的友军）
      if (target == owner || target == instigator) {
        continue;
      }
      if (owner != null && target.isAlliedTo(owner)) {
        continue;
      }
      if (master != null && (target == master || target.isAlliedTo(master))) {
        continue;
      }

      // 限频门：检查是否允许对此目标造成伤害
      if (!tryPassDamageGate(target, now)) {
        continue;
      }

      // 造成伤害
      var damageSource =
          owner != null
              ? level.damageSources().indirectMagic(rift, owner)
              : level.damageSources().magic();

      target.hurt(damageSource, finalDamage);
    }

    ChestCavity.LOGGER.debug(
        "[RiftManager] Resonance wave at {} dealt {} damage to {} targets",
        pos,
        finalDamage,
        targets.size());
  }

  /**
   * 尝试通过伤害限频门
   *
   * <p>用于裂隙伤害限频。同一目标在窗口期内只允许一次伤害通过。
   *
   * @param target 目标实体
   * @param now 当前全局tick（建议使用 {@link ServerLevel#getServer()} 的 tick 计数）
   * @return true 允许伤害；false 拒绝伤害
   */
  public boolean tryPassDamageGate(LivingEntity target, long now) {
    if (!RiftTuning.RATE_LIMIT_ENABLED) {
      return true;
    }

    UUID id = target.getUUID();
    Long last = damageRateLimiter.get(id);

    if (last != null && (now - last) < RiftTuning.RATE_LIMIT_WINDOW_TICKS) {
      // 限频：拒绝本次
      ChestCavity.LOGGER.debug(
          "[RiftManager] Rate limit blocked damage to {} (last hit {} ticks ago)",
          target.getName().getString(),
          (now - last));
      return false;
    }

    // 通过：记录本次命中
    damageRateLimiter.put(id, now);
    ChestCavity.LOGGER.debug(
        "[RiftManager] Rate limit passed damage to {} at tick {}",
        target.getName().getString(),
        now);

    // 惰性清理
    rateLimiterWriteCount++;
    if (rateLimiterWriteCount >= RATE_LIMITER_CLEAN_INTERVAL) {
      rateLimiterWriteCount = 0;
      cleanupRateLimiter(now);
    }

    return true;
  }

  /**
   * 清理限频表中的过期条目
   *
   * @param now 当前世界时间
   */
  private void cleanupRateLimiter(long now) {
    int beforeSize = damageRateLimiter.size();
    damageRateLimiter.entrySet()
        .removeIf(entry -> (now - entry.getValue()) > RiftTuning.RATE_LIMIT_MAX_KEEP_TICKS);
    int afterSize = damageRateLimiter.size();

    if (beforeSize > afterSize) {
      ChestCavity.LOGGER.debug(
          "[RiftManager] Cleaned up rate limiter: {} -> {} entries", beforeSize, afterSize);
    }
  }

  /**
   * 清理世界中的所有裂隙（世界卸载时调用）
   *
   * @param level 世界
   */
  public void clearLevel(ServerLevel level) {
    Set<UUID> levelRifts = riftsByLevel.remove(level);
    if (levelRifts != null) {
      for (UUID riftId : levelRifts) {
        rifts.remove(riftId);
      }
      ChestCavity.LOGGER.info(
          "[RiftManager] Cleared {} rifts from level {}", levelRifts.size(), level.dimension());
    }

    // 同时清理限频表（可选，因为UUID全局唯一，跨维度也可复用）
    // 为简化起见，此处不主动清理，依靠惰性清理机制
  }

  /**
   * 获取所有裂隙
   *
   * @return 裂隙列表（只读）
   */
  public Collection<RiftEntity> getAllRifts() {
    return Collections.unmodifiableCollection(rifts.values());
  }

  /**
   * 获取限频使用的全局时间戳
   *
   * <p>优先使用服务端的全局 tick 计数，若不可用则回退到当前维度的 gameTime。</p>
   */
  public long currentRateLimitTimestamp(ServerLevel level) {
    var server = level.getServer();
    return server != null ? server.getTickCount() : level.getGameTime();
  }
}
