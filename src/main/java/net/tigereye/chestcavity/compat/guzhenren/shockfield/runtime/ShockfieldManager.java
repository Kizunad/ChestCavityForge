package net.tigereye.chestcavity.compat.guzhenren.shockfield.runtime;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.shockfield.api.ShockfieldMath;
import net.tigereye.chestcavity.compat.guzhenren.shockfield.api.ShockfieldState;
import net.tigereye.chestcavity.compat.guzhenren.shockfield.api.WaveId;
import net.tigereye.chestcavity.compat.guzhenren.util.CombatEntityUtil;

/**
 * Shockfield 全局管理器：负责创建、更新、熄灭波场。
 *
 * <p>采用单例模式，全局管理所有活跃的Shockfield实例。
 */
public final class ShockfieldManager {

  private static final ShockfieldManager INSTANCE = new ShockfieldManager();

  // 存储所有活跃的Shockfield：key = WaveId, value = ShockfieldState
  private final Map<WaveId, ShockfieldState> activeShockfields = new HashMap<>();

  // 命中节流器：key = "waveId|targetUUID", value = readyTick
  private final Map<String, Long> hitGates = new HashMap<>();

  private ShockfieldManager() {}

  public static ShockfieldManager getInstance() {
    return INSTANCE;
  }

  /**
   * 创建新的Shockfield实例。
   *
   * @param owner 波源所有者
   * @param center 波源中心位置
   * @param amplitude 初始振幅
   * @param currentTick 当前游戏tick
   * @return 新创建的ShockfieldState
   */
  public ShockfieldState create(
      LivingEntity owner, Vec3 center, double amplitude, long currentTick) {
    UUID ownerId = owner.getUUID();
    int serial = computeSerial(ownerId, currentTick);
    WaveId waveId = WaveId.of(ownerId, currentTick, serial);

    ShockfieldState state =
        new ShockfieldState(
            waveId,
            ownerId,
            center,
            currentTick,
            amplitude,
            ShockfieldMath.BASE_PERIOD_SEC,
            0.0);

    activeShockfields.put(waveId, state);
    return state;
  }

  /**
   * 更新所有活跃的Shockfield：振幅衰减、周期拉伸、半径扩展、命中判定。
   *
   * @param level 服务器世界
   * @param currentTick 当前游戏tick
   */
  public void tickAll(ServerLevel level, long currentTick) {
    if (activeShockfields.isEmpty()) {
      return;
    }

    List<WaveId> toRemove = new ArrayList<>();

    for (Map.Entry<WaveId, ShockfieldState> entry : activeShockfields.entrySet()) {
      ShockfieldState state = entry.getValue();

      // 更新波场参数
      double deltaSeconds = 1.0 / 20.0; // 每tick = 0.05秒
      double newAmplitude = ShockfieldMath.applyDamping(state.getAmplitude(), deltaSeconds);
      double newPeriod = ShockfieldMath.stretchPeriod(state.getPeriod(), deltaSeconds);
      double newRadius = ShockfieldMath.computeRadius(state.getAgeSeconds(currentTick));

      state.setAmplitude(newAmplitude);
      state.setPeriod(newPeriod);
      state.setRadius(newRadius);

      // 检查熄灭条件
      if (ShockfieldMath.shouldExtinguish(newAmplitude)
          || ShockfieldMath.hasExceededLifetime(state.getAgeSeconds(currentTick))) {
        toRemove.add(entry.getKey());
        continue;
      }

      // 命中判定与伤害结算
      processHitDetection(level, state, currentTick);
    }

    // 移除已熄灭的波场
    for (WaveId waveId : toRemove) {
      activeShockfields.remove(waveId);
    }

    // 清理过期的命中节流记录
    cleanupHitGates(currentTick);
  }

  /**
   * 移除指定所有者的所有Shockfield。
   *
   * @param ownerId 所有者UUID
   */
  public void removeByOwner(UUID ownerId) {
    activeShockfields.entrySet().removeIf(entry -> entry.getValue().getOwnerId().equals(ownerId));
  }

  /**
   * 获取所有活跃的Shockfield。
   *
   * @return 活跃Shockfield列表
   */
  public Collection<ShockfieldState> getActiveShockfields() {
    return Collections.unmodifiableCollection(activeShockfields.values());
  }

  /**
   * 检查目标是否在命中冷却期内。
   *
   * @param waveId 波源ID
   * @param targetId 目标UUID
   * @param currentTick 当前游戏tick
   * @return 是否在冷却期
   */
  public boolean isOnCooldown(WaveId waveId, UUID targetId, long currentTick) {
    String key = makeHitGateKey(waveId, targetId);
    Long readyTick = hitGates.get(key);
    if (readyTick == null) {
      return false;
    }
    return currentTick < readyTick;
  }

  /**
   * 记录命中并设置冷却。
   *
   * @param waveId 波源ID
   * @param targetId 目标UUID
   * @param currentTick 当前游戏tick
   */
  public void recordHit(WaveId waveId, UUID targetId, long currentTick) {
    String key = makeHitGateKey(waveId, targetId);
    long cooldownTicks = (long) (ShockfieldMath.PER_TARGET_WAVE_HIT_CD * 20.0);
    hitGates.put(key, currentTick + cooldownTicks);
  }

  // ==================== 私有辅助方法 ====================

  /**
   * 计算同一tick内的波源序列号。
   *
   * @param ownerId 所有者UUID
   * @param currentTick 当前游戏tick
   * @return 序列号
   */
  private int computeSerial(UUID ownerId, long currentTick) {
    int maxSerial = -1;
    for (WaveId waveId : activeShockfields.keySet()) {
      if (waveId.sourceId().equals(ownerId) && waveId.spawnTick() == currentTick) {
        maxSerial = Math.max(maxSerial, waveId.serial());
      }
    }
    return maxSerial + 1;
  }

  /**
   * 处理波场的命中判定与伤害结算。
   *
   * @param level 服务器世界
   * @param state 波场状态
   * @param currentTick 当前游戏tick
   */
  private void processHitDetection(ServerLevel level, ShockfieldState state, long currentTick) {
    // 获取波源所有者
    LivingEntity owner =
        level.getEntity(state.getOwnerId()) instanceof LivingEntity living ? living : null;
    if (owner == null) {
      return;
    }

    // 计算波前覆盖区域
    Vec3 center = state.getCenter();
    double radius = state.getRadius();
    double ringWidth = 1.0; // 波前厚度
    AABB searchBox = new AABB(center, center).inflate(radius + ringWidth);

    // 搜索潜在目标
    List<LivingEntity> targets =
        level.getEntitiesOfClass(
            LivingEntity.class,
            searchBox,
            entity -> {
              if (entity == owner) {
                return false; // 排除所有者
              }
              if (!CombatEntityUtil.areEnemies(owner, entity)) {
                return false; // 排除友方
              }
              double dist = entity.position().distanceTo(center);
              return dist >= radius - ringWidth && dist <= radius + ringWidth;
            });

    // 命中判定与伤害结算
    for (LivingEntity target : targets) {
      UUID targetId = target.getUUID();

      // 同帧自波排除
      if (state.getWaveId().spawnTick() == currentTick) {
        continue;
      }

      // 命中节流检查
      if (isOnCooldown(state.getWaveId(), targetId, currentTick)) {
        continue;
      }

      // 计算伤害（简化版，实际应从器官行为类传入完整参数）
      double coreDamage =
          ShockfieldMath.computeCoreDamage(state.getAmplitude(), 1.0, 0.0, 0.0, 0.0, 0.0);
      double finalDamage = ShockfieldMath.computeFinalDamage(coreDamage, 0.0, 0.0);

      // 应用伤害
      target.hurt(owner.damageSources().generic(), (float) finalDamage);

      // 记录命中
      recordHit(state.getWaveId(), targetId, currentTick);
    }
  }

  /**
   * 生成命中节流器key。
   *
   * @param waveId 波源ID
   * @param targetId 目标UUID
   * @return key字符串
   */
  private String makeHitGateKey(WaveId waveId, UUID targetId) {
    return waveId.toString() + "|" + targetId.toString();
  }

  /**
   * 清理过期的命中节流记录。
   *
   * @param currentTick 当前游戏tick
   */
  private void cleanupHitGates(long currentTick) {
    hitGates.entrySet().removeIf(entry -> entry.getValue() <= currentTick);
  }
}
