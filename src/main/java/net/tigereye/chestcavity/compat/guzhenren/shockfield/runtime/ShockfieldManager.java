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
import net.tigereye.chestcavity.compat.guzhenren.shockfield.api.ShockfieldFx;
import net.tigereye.chestcavity.compat.guzhenren.shockfield.api.ShockfieldFxService;
import net.tigereye.chestcavity.compat.guzhenren.util.CombatEntityUtil;
import net.tigereye.chestcavity.engine.ServerTickEngine;
import net.tigereye.chestcavity.engine.TickEngineHub;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

/**
 * Shockfield 全局管理器：负责创建、更新、熄灭波场。
 *
 * <p>采用单例模式，全局管理所有活跃的Shockfield实例。
 */
public final class ShockfieldManager {

  private static final ShockfieldManager INSTANCE = new ShockfieldManager();

  // 存储所有活跃的Shockfield：key = WaveId, value = ShockfieldState
  private final Map<WaveId, ShockfieldState> activeShockfields = new HashMap<>();

  // 每个所有者的命中节流器（统一复用 MultiCooldown）
  private final Map<UUID, MultiCooldown> ownerCooldowns = new HashMap<>();

  // 软上限聚合：按秒桶聚合每位所有者的原始DPS
  private final Map<UUID, Long> ownerDpsBucketSec = new HashMap<>();
  private final Map<UUID, Double> ownerDpsRawAgg = new HashMap<>();

  // 触发冷却：主波创建 & 飞剑次波均采用 5 秒限流
  private static final long SPAWN_COOLDOWN_TICKS = 5L * 20L;
  private static final long SUBWAVE_COOLDOWN_TICKS = 5L * 20L;

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
   * @param spawnTargetId 当帧OnHit命中的目标（用于当帧排除）
   * @param cc 胸腔实例（用于冷却与参数快照同步）
   * @param organ 持有的器官实例（用于 MultiCooldown 绑定）
   * @param organStateRoot OrganState 根键（如 "JianDangGu"）
   * @return 新创建的ShockfieldState
   */
  public ShockfieldState create(
      LivingEntity owner,
      Vec3 center,
      double amplitude,
      long currentTick,
      UUID spawnTargetId,
      ChestCavityInstance cc,
      net.minecraft.world.item.ItemStack organ,
      String organStateRoot) {
    UUID ownerId = owner.getUUID();

    // 确保冷却容器存在（与器官状态绑定，便于在移除/同步时持久化）
    if (!ownerCooldowns.containsKey(ownerId) && organ != null && !organ.isEmpty()) {
      OrganState state = OrganState.of(organ, organStateRoot == null ? "JianDangGu" : organStateRoot);
      ownerCooldowns.put(ownerId, MultiCooldown.builder(state).withSync(cc, organ).build());
    }
    // 主波创建触发冷却：5 秒内只能触发一次
    MultiCooldown cd = ownerCooldowns.get(ownerId);
    if (cd != null) {
      MultiCooldown.Entry spawnGate = cd.entry("shockfield:spawn").withDefault(0L);
      if (!spawnGate.isReady(currentTick)) {
        return null; // 冷却中，静默拒绝
      }
      spawnGate.setReadyAt(currentTick + SPAWN_COOLDOWN_TICKS);
    }
    int serial = computeSerial(ownerId, currentTick);
    WaveId waveId = WaveId.of(ownerId, currentTick, serial);

    // 参数快照：剑道道痕 / 流派经验 / 力量分数 / 武器-飞剑阶权
    double jd = 0.0, flow = 0.0;
    try {
      var handleOpt = GuzhenrenResourceBridge.open(owner);
      if (handleOpt.isPresent()) {
        var h = handleOpt.get();
        jd = h.read("daohen_jiandao").orElse(0.0);
        flow = h.read("liupai_jiandao").orElse(0.0);
      }
    } catch (Throwable ignored) {}
    double str = 0.0;
    try {
      var ccOpt = net.tigereye.chestcavity.interfaces.ChestCavityEntity.of(owner);
      if (ccOpt.isPresent()) {
        var ccInst = ccOpt.get().getChestCavityInstance();
        str = ccInst.getOrganScore(net.tigereye.chestcavity.registration.CCOrganScores.STRENGTH);
      }
    } catch (Throwable ignored) {}
    double wTier = 1.0;
    try {
      var main = owner instanceof Player p ? p.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY;
      if (!main.isEmpty() && main.getItem() instanceof net.minecraft.world.item.TieredItem ti) {
        var t = ti.getTier();
        // 采用 tier.level + 1 的保守权重
        int lvl = 0;
        try { lvl = (int) t.getClass().getMethod("getLevel").invoke(t); } catch (Throwable ignored) {}
        wTier = Math.max(1.0, 1.0 + lvl);
      }
    } catch (Throwable ignored) {}

    ShockfieldState state =
        new ShockfieldState(
            waveId,
            ownerId,
            center,
            currentTick,
            amplitude,
            ShockfieldMath.BASE_PERIOD_SEC,
            0.0,
            spawnTargetId,
            1.0,
            jd,
            str,
            flow,
            wTier);

    activeShockfields.put(waveId, state);
    if (owner.level() instanceof ServerLevel sl) {
      ShockfieldFx.service().onWaveCreate(sl, state);
    }
    return state;
  }

  /**
   * 更新所有活跃的Shockfield：振幅衰减、周期拉伸、半径扩展、命中判定。
   *
   * @param level 服务器世界
   * @param currentTick 当前游戏tick
   */
  public void tickLevel(ServerLevel level, long currentTick) {
    if (activeShockfields.isEmpty()) {
      return;
    }

    List<WaveId> toRemove = new ArrayList<>();
    List<ShockfieldState> toAdd = new ArrayList<>();
    Set<UUID> ownersPendingCleanup = new HashSet<>();

    for (Map.Entry<WaveId, ShockfieldState> entry : activeShockfields.entrySet()) {
      ShockfieldState state = entry.getValue();

      // 更新波场参数
      double deltaSeconds = 1.0 / 20.0; // 每tick = 0.05秒
      double newAmplitude = ShockfieldMath.applyDamping(state.getAmplitude(), deltaSeconds);
      double newPeriod = ShockfieldMath.stretchPeriod(state.getPeriod(), deltaSeconds);
      // 半径按每个波场的速度比例计算
      double newRadius = ShockfieldMath.RADIAL_SPEED * state.getRadialSpeedScale() * state.getAgeSeconds(currentTick);

      state.setAmplitude(newAmplitude);
      state.setPeriod(newPeriod);
      state.setRadius(newRadius);

      // 检查熄灭条件
      boolean damped = ShockfieldMath.shouldExtinguish(newAmplitude);
      boolean lifetime = ShockfieldMath.hasExceededLifetime(state.getAgeSeconds(currentTick));
      if (damped || lifetime) {
        toRemove.add(entry.getKey());
        ownersPendingCleanup.add(state.getOwnerId());
        ShockfieldFx.service()
            .onExtinguish(
                level,
                state,
                damped
                    ? ShockfieldFxService.ExtinguishReason.DAMPED_OUT
                    : ShockfieldFxService.ExtinguishReason.LIFETIME_ENDED);
        continue;
      }

      // 命中判定与伤害结算（可能计划生成子波包，延后统一加入，避免并发修改）
      processHitDetection(level, state, currentTick, toAdd);

      // tick 后FX（已更新半径/振幅/周期）
      ShockfieldFx.service().onWaveTick(level, state);
    }

    // 移除已熄灭的波场
    for (WaveId waveId : toRemove) {
      activeShockfields.remove(waveId);
    }

    // 统一加入本tick生成的子波包，避免迭代过程中修改 Map 导致 CME
    if (!toAdd.isEmpty()) {
      for (ShockfieldState sub : toAdd) {
        activeShockfields.put(sub.getWaveId(), sub);
      }
    }

    if (!ownersPendingCleanup.isEmpty()) {
      for (UUID ownerId : ownersPendingCleanup) {
        if (!hasActiveShockfield(ownerId)) {
          cleanupOwnerState(ownerId);
        }
      }
    }

    // MultiCooldown 无需在此清理，冷却自然到期
  }

  /**
   * 移除指定所有者的所有Shockfield。
   *
   * @param ownerId 所有者UUID
   */
  public void removeByOwner(UUID ownerId) {
    activeShockfields.entrySet().removeIf(entry -> entry.getValue().getOwnerId().equals(ownerId));
    cleanupOwnerState(ownerId);
  }

  /**
   * 获取所有活跃的Shockfield。
   *
   * @return 活跃Shockfield列表
   */
  public Collection<ShockfieldState> getActiveShockfields() {
    return Collections.unmodifiableCollection(activeShockfields.values());
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
   * 计算同一tick内的波源序列号（包含待加入的子波包）。
   *
   * @param ownerId 所有者UUID
   * @param currentTick 当前游戏tick
   * @param pendingAdds 本tick待加入的子波列表
   */
  private int computeSerial(UUID ownerId, long currentTick, java.util.Collection<ShockfieldState> pendingAdds) {
    int maxSerial = -1;
    for (WaveId waveId : activeShockfields.keySet()) {
      if (waveId.sourceId().equals(ownerId) && waveId.spawnTick() == currentTick) {
        maxSerial = Math.max(maxSerial, waveId.serial());
      }
    }
    if (pendingAdds != null) {
      for (ShockfieldState s : pendingAdds) {
        WaveId wid = s.getWaveId();
        if (wid != null && wid.sourceId().equals(ownerId) && wid.spawnTick() == currentTick) {
          maxSerial = Math.max(maxSerial, wid.serial());
        }
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
  private void processHitDetection(
      ServerLevel level, ShockfieldState state, long currentTick, List<ShockfieldState> pendingAdds) {
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

      // 自波连带：仅排除“OnHit当帧的那个目标本体”
      if (state.getWaveId().spawnTick() == currentTick && state.getSpawnTargetId() != null
          && state.getSpawnTargetId().equals(targetId)) {
        continue;
      }

      // 命中节流：统一 MultiCooldown（owner organ 绑定）
      MultiCooldown cd = ownerCooldowns.get(state.getOwnerId());
      if (cd != null) {
        String key = makeHitGateKey(state.getWaveId(), targetId);
        MultiCooldown.Entry entry = cd.entry(key).withDefault(0L);
        if (!entry.isReady(currentTick)) {
          continue;
        }
        long cooldownTicks = (long) (ShockfieldMath.PER_TARGET_WAVE_HIT_CD * 20.0);
        entry.setReadyAt(currentTick + cooldownTicks);
      }

      // 计算伤害（简化版，实际应从器官行为类传入完整参数）
      double phaseMul = 1.0; // 暂不做相位干涉
      double coreDamage =
          ShockfieldMath.computeCoreDamage(
              state.getAmplitude(),
              phaseMul,
              state.getJd(),
              state.getStr(),
              state.getFlow(),
              state.getWTier());

      // 目标抗性与护甲（按规范上限与折算）
      double resistPct = computeResistPct(target);
      double armor = 0.0;
      try {
        var ai = target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        armor = ai == null ? 0.0 : ai.getValue();
      } catch (Throwable ignored) {}
      double finalDamageRaw = ShockfieldMath.computeFinalDamage(coreDamage, resistPct, armor);

      // 软上限聚合：以“每秒桶”聚合所有者的原始DPS并应用软封顶
      double dealt =
          applyDpsSoftCap(state.getOwnerId(), currentTick, state.getJd(), finalDamageRaw);

      // 应用伤害
      if (dealt > 0.0) {
        target.hurt(owner.damageSources().generic(), (float) dealt);
        ShockfieldFx.service().onHit(level, state, target, dealt);
      }
    }

    // 飞剑交互：主圈触碰本人的飞剑 → 在命中点生成二级波包，并扣飞剑耐久
    try {
      List<net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity> swords =
          level.getEntitiesOfClass(
              net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity.class,
              searchBox,
              e ->
                  e.isOwnedBy(owner)
                      && e.position().distanceTo(center) >= radius - ringWidth
                      && e.position().distanceTo(center) <= radius + ringWidth);
      if (!swords.isEmpty()) {
        MultiCooldown cd = ownerCooldowns.get(state.getOwnerId());
        for (var sword : swords) {
          String key = "shockfield:subwave|" + sword.getUUID();
          boolean ready = true;
          if (cd != null) {
            MultiCooldown.Entry entry = cd.entry(key).withDefault(0L);
            if (!entry.isReady(currentTick)) {
              ready = false;
            } else {
              entry.setReadyAt(currentTick + SUBWAVE_COOLDOWN_TICKS);
            }
          }
          if (!ready) {
            continue;
          }

          // 新建二级波包（中心=飞剑位置；速度比例=WAVE_SPEED_SCALE；振幅延续当前振幅）
          ShockfieldState sub =
              new ShockfieldState(
                  WaveId.of(
                      state.getOwnerId(),
                      currentTick,
                      computeSerial(state.getOwnerId(), currentTick, pendingAdds)),
                  state.getOwnerId(),
                  sword.position(),
                  currentTick,
                  state.getAmplitude(),
                  state.getPeriod(),
                  0.0,
                  null,
                  ShockfieldMath.WAVE_SPEED_SCALE,
                  state.getJd(),
                  state.getStr(),
                  state.getFlow(),
                  state.getWTier());
          if (pendingAdds != null) {
            pendingAdds.add(sub);
          } else {
            // 理论不走到这里；兜底以防空指针
            activeShockfields.put(sub.getWaveId(), sub);
          }
          ShockfieldFx.service().onSubwaveCreate(level, state, sub);

          // 扣减飞剑耐久：最大耐久的 0.5%
          try {
            double max = Math.max(1.0, sword.getSwordAttributes().maxDurability);
            float loss = (float) (ShockfieldMath.FS_DURA_COST_ON_TOUCH_PCT * max);
            sword.damageDurability(loss);
          } catch (Throwable ignored) {}
        }
      }
    } catch (Throwable ignored) {
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
    return "shockfield:hit|" + waveId.toString() + "|" + targetId;
  }

  private void cleanupOwnerState(UUID ownerId) {
    if (ownerId == null) {
      return;
    }
    MultiCooldown cd = ownerCooldowns.remove(ownerId);
    if (cd != null) {
      cd.purgeAll();
    }
    ownerDpsBucketSec.remove(ownerId);
    ownerDpsRawAgg.remove(ownerId);
  }

  private boolean hasActiveShockfield(UUID ownerId) {
    if (ownerId == null) {
      return false;
    }
    for (ShockfieldState state : activeShockfields.values()) {
      if (ownerId.equals(state.getOwnerId())) {
        return true;
      }
    }
    return false;
  }

  /** 计算目标的抗性百分比（近似）：使用抗性药水，按 20%/级，最高 60%。 */
  private static double computeResistPct(LivingEntity target) {
    try {
      var inst = target.getEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE);
      if (inst == null) return 0.0;
      int amp = Math.max(0, inst.getAmplifier());
      return Math.min(ShockfieldMath.RESIST_PCT_CAP, 0.2 * (amp + 1));
    } catch (Throwable ignored) {
      return 0.0;
    }
  }

  /** 将本次伤害应用DPS软上限并返回实际应造成的值。 */
  private double applyDpsSoftCap(UUID ownerId, long currentTick, double jd, double thisHitRaw) {
    long bucket = Math.floorDiv(currentTick, 20L);
    Long prevBucket = ownerDpsBucketSec.get(ownerId);
    if (prevBucket == null || prevBucket != bucket) {
      ownerDpsBucketSec.put(ownerId, bucket);
      ownerDpsRawAgg.put(ownerId, 0.0);
    }
    double agg = ownerDpsRawAgg.getOrDefault(ownerId, 0.0);
    // 软封顶差分：softCap(agg+hit) - softCap(agg)
    double cappedAfter = ShockfieldMath.applySoftCap(agg + thisHitRaw, jd);
    double cappedBefore = ShockfieldMath.applySoftCap(agg, jd);
    double allowed = Math.max(0.0, cappedAfter - cappedBefore);
    ownerDpsRawAgg.put(ownerId, agg + thisHitRaw);
    return allowed;
  }

  /** 注册到统一 Server Tick 引擎，并初始化特效服务。 */
  public static void bootstrap() {
    TickEngineHub.register(TickEngineHub.PRIORITY_DOT + 10, ShockfieldManager::onServerTick);

    // 注册 FxEngine 优化的 Shockfield FX 方案
    net.tigereye.chestcavity.compat.guzhenren.shockfield.fx.ShockfieldFxOptimized.registerFxSchemes();

    // 注册特效服务（优化版：使用 FxRegistry + fallback）
    ShockfieldFx.set(new net.tigereye.chestcavity.compat.guzhenren.shockfield.fx.ShockfieldFxOptimized());
  }

  private static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
    var server = event.getServer();
    if (server == null) return;
    // 统一使用每个 Level 的 gameTime 作为 Shockfield 的时间域，
    // 避免与 create(...) 中使用的 level.getGameTime() 出现“跨域”差值导致的即刻熄灭或负半径。
    for (ServerLevel level : server.getAllLevels()) {
      long now = level.getGameTime();
      INSTANCE.tickLevel(level, now);
    }
  }
}
