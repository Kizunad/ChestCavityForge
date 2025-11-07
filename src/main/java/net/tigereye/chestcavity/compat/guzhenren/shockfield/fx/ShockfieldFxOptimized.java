package net.tigereye.chestcavity.compat.guzhenren.shockfield.fx;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.shockfield.api.ShockfieldFxService;
import net.tigereye.chestcavity.compat.guzhenren.shockfield.api.ShockfieldState;
import net.tigereye.chestcavity.engine.fx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shockfield 特效优化版：使用 FxRegistry 实现延迟特效与预算控制。
 *
 * <p>设计思路：
 *
 * <ul>
 *   <li>原版 ShockfieldFxImpl：每次事件立即生成粒子（无法控制总量）
 *   <li>优化版：通过 FxEngine 时间线系统管理特效生命周期
 *   <li>支持预算控制：限制同时播放的特效数量
 *   <li>支持合并策略：避免重复特效（如同一波场的多次 tick）
 * </ul>
 *
 * <p>Stage 3 实际集成示例。
 */
public final class ShockfieldFxOptimized implements ShockfieldFxService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ShockfieldFxOptimized.class);

  /**
   * 波场脉冲运行时参数表：按 mergeKey 存储最新半径/振幅，供 EXTEND_TTL 合并后的 Track
   * 读取最新的 Shockfield 扩散数据。
   */
  private static final Map<String, PulseRuntimeState> PULSE_RUNTIME = new ConcurrentHashMap<>();

  /** 原版特效实现（用于底层粒子生成）。 */
  private final ShockfieldFxImpl fallback;

  public ShockfieldFxOptimized() {
    this.fallback = new ShockfieldFxImpl();
  }

  /** 注册所有 Shockfield FX 方案（在模组初始化时调用）。 */
  public static void registerFxSchemes() {
    FxRegistry registry = FxEngine.registry();

    // ========== 1. 波场扩散特效（onWaveTick 优化 - 波荡效果） ==========
    // 新版：真正的波荡效果 - 多层波纹从中心向外扩散，垂直波动
    registry.register(
        "chestcavity:fx/shockfield/wave_pulse",
        context -> {
          String waveId = context.getWaveId();
          double radius = context.getCustomParam("radius", 0.0);
          double amplitude = context.getCustomParam("amplitude", 1.0);
          String mergeKey = "shockfield:pulse@" + waveId;

          // 更新运行时参数：确保 EXTEND_TTL 合并后仍能读取最新半径/振幅。
          PULSE_RUNTIME.compute(
              mergeKey,
              (key, existing) -> {
                if (existing == null) {
                  return new PulseRuntimeState(radius, amplitude);
                }
                existing.update(radius, amplitude);
                return existing;
              });

          return FxTrackSpec.builder("shockfield-pulse-" + waveId)
              .ttl(10) // 仅持续 0.5 秒（避免长时间占用）
              .tickInterval(2) // 每 2 tick 生成一次粒子（更频繁以形成连续波纹）
              .owner(context.getOwnerId())
              .mergeKey(mergeKey) // 按波场合并
              .mergeStrategy(MergeStrategy.EXTEND_TTL) // 延长 TTL（持续生成）
              .onTick(
                  (level, elapsed) -> {
                    Vec3 center = context.getPosition();
                    if (center == null) return;

                    PulseRuntimeState runtimeState = PULSE_RUNTIME.get(mergeKey);
                    double effectiveRadius = runtimeState != null ? runtimeState.radius : radius;
                    double effectiveAmplitude =
                        runtimeState != null ? runtimeState.amplitude : amplitude;

                    // 波纹参数
                    double time = elapsed * 0.1; // 时间因子（控制波动速度）
                    int waveCount = 3; // 同时显示的波纹层数
                    double waveSpacing = 0.8; // 波纹间距

                    // 生成多层波纹
                    for (int wave = 0; wave < waveCount; wave++) {
                      double waveRadius =
                          effectiveRadius - (wave * waveSpacing); // 从外向内的波纹
                      if (waveRadius < 0.3) continue; // 跳过太小的半径

                      // 每层波纹的粒子数量（根据半径和振幅调整）
                      int particleCount = Math.max(16, (int) (waveRadius * 12.0));

                      for (int i = 0; i < particleCount; i++) {
                        double angle = (Math.PI * 2.0 * i) / particleCount;
                        double x = center.x + Math.cos(angle) * waveRadius;
                        double z = center.z + Math.sin(angle) * waveRadius;

                        // 波动高度：使用正弦波模拟波荡效果
                        // 相位 = 角度 + 时间 + 波纹层偏移
                        double phase = angle * 2.0 + time * 3.0 + wave * 1.0;
                        double waveHeight = Math.sin(phase) * effectiveAmplitude * 0.15;

                        // 距离衰减：越远的波纹高度越小
                        double distanceFactor =
                            1.0 - (wave / (double) waveCount) * 0.5;
                        double y = center.y + 0.1 + waveHeight * distanceFactor;

                        // 主波纹粒子（明亮的核心）
                        level.sendParticles(
                            ParticleTypes.END_ROD,
                            x,
                            y,
                            z,
                            1,
                            0.0,
                            0.0,
                            0.0,
                            0.0);

                        // 波峰强化：在波峰位置添加额外粒子
                        if (waveHeight > effectiveAmplitude * 0.1) {
                          level.sendParticles(
                              ParticleTypes.ELECTRIC_SPARK,
                              x,
                              y + 0.1,
                              z,
                              1,
                              0.0,
                              0.02,
                              0.0,
                              0.01);
                        }

                        // 波谷装饰：在波谷添加灵魂粒子
                        if (i % 4 == 0 && waveHeight < 0) {
                          level.sendParticles(
                              ParticleTypes.SOUL,
                              x,
                              y,
                              z,
                              1,
                              0.0,
                              0.03,
                              0.0,
                              0.005);
                        }

                        // 波纹边缘：添加扩散粒子
                        if (wave == 0 && i % 3 == 0) {
                          double spreadRadius = effectiveRadius + 0.2;
                          double spreadX = center.x + Math.cos(angle) * spreadRadius;
                          double spreadZ = center.z + Math.sin(angle) * spreadRadius;
                          level.sendParticles(
                              ParticleTypes.SOUL_FIRE_FLAME,
                              spreadX,
                              y,
                              spreadZ,
                              1,
                              Math.cos(angle) * 0.05,
                              0.0,
                              Math.sin(angle) * 0.05,
                              0.02);
                        }
                      }
                    }

                    // 中心脉冲：在波场中心添加向上的能量脉冲
                    if (elapsed % 5 == 0) {
                      for (int i = 0; i < 8; i++) {
                        double angle = (Math.PI * 2.0 * i) / 8.0;
                        double spiralRadius = 0.3 * Math.sin(time * 2.0);
                        level.sendParticles(
                            ParticleTypes.SOUL_FIRE_FLAME,
                            center.x + Math.cos(angle) * spiralRadius,
                            center.y + 0.1,
                            center.z + Math.sin(angle) * spiralRadius,
                            1,
                            0.0,
                            0.2 * effectiveAmplitude,
                            0.0,
                            0.03);
                      }
                    }
                  })
              .onStop(
                  (level, reason) -> {
                    if (mergeKey != null) {
                      PULSE_RUNTIME.remove(mergeKey);
                    }
                  })
              .build();
        });

    // ========== 2. 波场创建特效（onWaveCreate 优化） ==========
    registry.register(
        "chestcavity:fx/shockfield/wave_create",
        context -> {
          String waveId = context.getWaveId();

          return FxTrackSpec.builder("shockfield-create-" + waveId)
              .ttl(1) // 立即触发（一次性特效）
              .tickInterval(1)
              .owner(context.getOwnerId())
              // 无 mergeKey（每次创建都播放）
              .onStart(
                  level -> {
                    Vec3 center = context.getPosition();
                    if (center == null) return;

                    // 真元爆发（复用原版逻辑）
                    for (int i = 0; i < 8; i++) {
                      double angle = (Math.PI * 2.0 * i) / 8.0;
                      double dx = Math.cos(angle) * 0.5;
                      double dz = Math.sin(angle) * 0.5;
                      level.sendParticles(
                          ParticleTypes.CLOUD,
                          center.x + dx,
                          center.y + 0.1,
                          center.z + dz,
                          1,
                          dx * 0.3,
                          0.05,
                          dz * 0.3,
                          0.1);
                    }

                    // 剑意涌动
                    for (int i = 0; i < 12; i++) {
                      double angle = (Math.PI * 2.0 * i) / 12.0 + Math.random() * 0.3;
                      double radius = 0.3 + Math.random() * 0.2;
                      double dx = Math.cos(angle) * radius;
                      double dz = Math.sin(angle) * radius;
                      level.sendParticles(
                          ParticleTypes.SOUL_FIRE_FLAME,
                          center.x + dx,
                          center.y + 0.1,
                          center.z + dz,
                          1,
                          0.0,
                          0.15,
                          0.0,
                          0.01);
                    }

                    // 音效
                    level.playSound(
                        null,
                        center.x,
                        center.y,
                        center.z,
                        SoundEvents.TRIDENT_RIPTIDE_1,
                        SoundSource.PLAYERS,
                        0.6F,
                        0.7F);
                  })
              .build();
        });

    // ========== 3. 次级波包特效（onSubwaveCreate 优化） ==========
    registry.register(
        "chestcavity:fx/shockfield/subwave_create",
        context -> {
          String waveId = context.getWaveId();

          return FxTrackSpec.builder("shockfield-subwave-" + waveId)
              .ttl(40) // 2 秒持续特效
              .tickInterval(5) // 每 5 tick 生成一次
              .owner(context.getOwnerId())
              .mergeKey("shockfield:subwave@" + context.getOwnerId()) // 按 owner 合并
              .mergeStrategy(MergeStrategy.DROP) // 丢弃新请求（限制次级波数量）
              .onStart(
                  level -> {
                    Vec3 center = context.getPosition();
                    if (center == null) return;

                    // 涟光闪烁
                    for (int i = 0; i < 8; i++) {
                      double angle = (Math.PI * 2.0 * i) / 8.0;
                      double radius = 0.25;
                      double dx = Math.cos(angle) * radius;
                      double dz = Math.sin(angle) * radius;
                      level.sendParticles(
                          ParticleTypes.ELECTRIC_SPARK,
                          center.x + dx,
                          center.y + 0.1,
                          center.z + dz,
                          1,
                          0.0,
                          0.05,
                          0.0,
                          0.01);
                    }

                    // 音效
                    level.playSound(
                        null,
                        center.x,
                        center.y,
                        center.z,
                        SoundEvents.TRIDENT_HIT,
                        SoundSource.PLAYERS,
                        0.4F,
                        1.5F);
                  })
              .onTick(
                  (level, elapsed) -> {
                    Vec3 center = context.getPosition();
                    if (center == null) return;

                    // 持续的扩散效果
                    double expandRadius = (elapsed / 40.0) * 2.0;
                    for (int i = 0; i < 4; i++) {
                      double angle = (Math.PI * 2.0 * i) / 4.0;
                      double x = center.x + Math.cos(angle) * expandRadius;
                      double z = center.z + Math.sin(angle) * expandRadius;
                      level.sendParticles(
                          ParticleTypes.SOUL, x, center.y + 0.1, z, 1, 0.0, 0.05, 0.0, 0.01);
                    }
                  })
              .build();
        });

    // ========== 4. 熄灭特效（onExtinguish 优化） ==========
    registry.register(
        "chestcavity:fx/shockfield/extinguish",
        context -> {
          String waveId = context.getWaveId();
          double finalRadius = context.getCustomParam("finalRadius", 1.0);
          String reason = context.getCustomParam("reason", "NATURAL");

          return FxTrackSpec.builder("shockfield-extinguish-" + waveId)
              .ttl(30) // 1.5 秒收束动画
              .tickInterval(2) // 每 2 tick 生成
              .owner(context.getOwnerId())
              // 无 mergeKey（每次熄灭都播放）
              .onTick(
                  (level, elapsed) -> {
                    Vec3 center = context.getPosition();
                    if (center == null) return;

                    // 收束进度（从外向内）
                    double progress = elapsed / 30.0;
                    double currentRadius = finalRadius * (1.0 - progress);

                    // 向中心收束的粒子
                    int count = Math.max(4, (int) (currentRadius * 2.0));
                    for (int i = 0; i < count; i++) {
                      double angle = (Math.PI * 2.0 * i) / count;
                      double x = center.x + Math.cos(angle) * currentRadius;
                      double z = center.z + Math.sin(angle) * currentRadius;
                      double y = center.y + 0.1;

                      // 向中心移动
                      double dx = (center.x - x) * 0.1;
                      double dz = (center.z - z) * 0.1;

                      level.sendParticles(
                          ParticleTypes.SOUL, x, y, z, 1, dx, 0.0, dz, 0.05);
                    }
                  })
              .onStop(
                  (level, stopReason) -> {
                    if (level == null) return;
                    Vec3 center = context.getPosition();
                    if (center == null) return;

                    // 音效
                    level.playSound(
                        null,
                        center.x,
                        center.y,
                        center.z,
                        SoundEvents.BEACON_DEACTIVATE,
                        SoundSource.PLAYERS,
                        0.3F,
                        0.8F);
                  })
              .build();
        });

    LOGGER.info(
        "[ShockfieldFxOptimized] Registered 4 optimized FX schemes using FxRegistry");
  }

  // ==================== ShockfieldFxService 实现（使用 FxRegistry 播放） ====================

  @Override
  public void onWaveCreate(ServerLevel level, ShockfieldState state) {
    // 检查 FxEngine 是否启用
    if (!FxEngine.getConfig().enabled) {
      // 降级：使用原版实现
      fallback.onWaveCreate(level, state);
      return;
    }

    // 通过 FxRegistry 播放
    FxContext context =
        FxContext.builder(level)
            .owner(state.getOwnerId())
            .wave(state.getWaveId().toString())
            .position(state.getCenter())
            .build();

    FxEngine.registry().play("chestcavity:fx/shockfield/wave_create", context);
  }

  @Override
  public void onWaveTick(ServerLevel level, ShockfieldState state) {
    // 检查 FxEngine 是否启用
    if (!FxEngine.getConfig().enabled) {
      // 降级：使用原版实现（但限制频率）
      long age = state.getAge(level.getGameTime());
      if (age % 10 == 0) {
        fallback.onWaveTick(level, state);
      }
      return;
    }

    // 通过 FxRegistry 播放（合并策略会自动处理重复）
    FxContext context =
        FxContext.builder(level)
            .owner(state.getOwnerId())
            .wave(state.getWaveId().toString())
            .position(state.getCenter())
            .customParam("radius", state.getRadius())
            .customParam("amplitude", state.getAmplitude())
            .build();

    FxEngine.registry().play("chestcavity:fx/shockfield/wave_pulse", context);
  }

  @Override
  public void onHit(
      ServerLevel level, ShockfieldState state, LivingEntity target, double damageApplied) {
    // 命中特效保持原版实现（一次性特效，无需 FxEngine 管理）
    fallback.onHit(level, state, target, damageApplied);
  }

  @Override
  public void onSubwaveCreate(ServerLevel level, ShockfieldState parent, ShockfieldState sub) {
    // 检查 FxEngine 是否启用
    if (!FxEngine.getConfig().enabled) {
      fallback.onSubwaveCreate(level, parent, sub);
      return;
    }

    // 通过 FxRegistry 播放
    FxContext context =
        FxContext.builder(level)
            .owner(sub.getOwnerId())
            .wave(sub.getWaveId().toString())
            .position(sub.getCenter())
            .customParam("parentWaveId", parent.getWaveId().toString())
            .build();

    FxEngine.registry().play("chestcavity:fx/shockfield/subwave_create", context);
  }

  @Override
  public void onExtinguish(ServerLevel level, ShockfieldState state, ExtinguishReason reason) {
    // 检查 FxEngine 是否启用
    if (!FxEngine.getConfig().enabled) {
      fallback.onExtinguish(level, state, reason);
      return;
    }

    // 通过 FxRegistry 播放
    FxContext context =
        FxContext.builder(level)
            .owner(state.getOwnerId())
            .wave(state.getWaveId().toString())
            .position(state.getCenter())
            .customParam("finalRadius", state.getRadius())
            .customParam("reason", reason.name())
            .build();

    FxEngine.registry().play("chestcavity:fx/shockfield/extinguish", context);
  }

  /** 波场脉冲运行时参数：存储最新的半径与振幅。 */
  private static final class PulseRuntimeState {
    volatile double radius;
    volatile double amplitude;

    PulseRuntimeState(double radius, double amplitude) {
      this.radius = radius;
      this.amplitude = amplitude;
    }

    void update(double radius, double amplitude) {
      this.radius = radius;
      this.amplitude = amplitude;
    }
  }
}
