package net.tigereye.chestcavity.engine.fx.examples;

import java.util.UUID;
import net.tigereye.chestcavity.engine.fx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shockfield FX 集成示例。
 *
 * <p>演示如何使用 FxRegistry 注册 Shockfield 相关的 FX 方案，并在合适的时机播放。
 *
 * <p>Stage 3 示例代码（文档级）。
 *
 * <p>使用方式：
 *
 * <ol>
 *   <li>在模组初始化阶段调用 {@link #registerShockfieldFx()} 注册 FX 方案
 *   <li>在 Shockfield 回调中调用 {@link #playWaveCreateFx} / {@link #playSubwaveCreateFx} / {@link
 *       #playExtinguishFx}
 * </ol>
 */
public final class ShockfieldFxExample {

  private static final Logger LOGGER = LoggerFactory.getLogger(ShockfieldFxExample.class);

  /** 注册所有 Shockfield 相关的 FX 方案。 */
  public static void registerShockfieldFx() {
    FxRegistry registry = FxEngine.registry();

    // 1. 环纹扩张 FX（onWaveCreate 时触发）
    registry.register(
        "chestcavity:fx/shockfield/ring",
        context -> {
          String trackId =
              "shockfield-ring-"
                  + context.getOwnerId()
                  + "-"
                  + System.currentTimeMillis();

          return FxTrackSpec.builder(trackId)
              .ttl(100) // 5 秒
              .tickInterval(2) // 每 2 tick 执行一次
              .owner(context.getOwnerId())
              .mergeKey(
                  "shockfield:ring@"
                      + context.getOwnerId()
                      + "/"
                      + context.getWaveId()) // 按 owner+wave 合并
              .mergeStrategy(MergeStrategy.EXTEND_TTL) // 延长 TTL
              .onStart(
                  level -> {
                    LOGGER.debug(
                        "[ShockfieldFx] Ring FX started for wave: {}", context.getWaveId());
                  })
              .onTick(
                  (level, elapsed) -> {
                    // TODO: 实际实现应采样 Wave 半径并生成粒子
                    // 示例：在波场半径处生成环形粒子
                    double radius = context.getCustomParam("radius", 0.0);
                    LOGGER.debug(
                        "[ShockfieldFx] Ring FX tick {} - radius: {}", elapsed, radius);
                  })
              .onStop(
                  (level, reason) -> {
                    LOGGER.debug(
                        "[ShockfieldFx] Ring FX stopped for wave: {} (reason: {})",
                        context.getWaveId(),
                        reason);
                  })
              .build();
        });

    // 2. 次级扩散 FX（onSubwaveCreate 时触发）
    registry.register(
        "chestcavity:fx/shockfield/subwave_pulse",
        context -> {
          String trackId =
              "shockfield-subwave-"
                  + context.getOwnerId()
                  + "-"
                  + System.currentTimeMillis();

          return FxTrackSpec.builder(trackId)
              .ttl(40) // 2 秒
              .tickInterval(1) // 每 tick 执行
              .owner(context.getOwnerId())
              .mergeKey(
                  "shockfield:subwave@"
                      + context.getOwnerId()
                      + "/"
                      + context.getWaveId())
              .mergeStrategy(MergeStrategy.DROP) // 丢弃新请求（避免过多次级波）
              .onStart(
                  level -> {
                    LOGGER.debug(
                        "[ShockfieldFx] Subwave pulse FX started at: {}",
                        context.getPosition());
                  })
              .onTick(
                  (level, elapsed) -> {
                    // TODO: 实际实现应生成快速扩散的粒子效果
                    // 示例：在次级波中心生成脉冲效果
                    LOGGER.debug("[ShockfieldFx] Subwave pulse FX tick {}", elapsed);
                  })
              .onStop(
                  (level, reason) -> {
                    LOGGER.debug(
                        "[ShockfieldFx] Subwave pulse FX stopped (reason: {})", reason);
                  })
              .build();
        });

    // 3. 收束消散 FX（onExtinguish 时触发）
    registry.register(
        "chestcavity:fx/shockfield/extinguish",
        context -> {
          String trackId =
              "shockfield-extinguish-"
                  + context.getOwnerId()
                  + "-"
                  + System.currentTimeMillis();

          return FxTrackSpec.builder(trackId)
              .ttl(30) // 1.5 秒
              .tickInterval(1)
              .owner(context.getOwnerId())
              // 无 mergeKey，每次熄灭都播放
              .onStart(
                  level -> {
                    LOGGER.debug(
                        "[ShockfieldFx] Extinguish FX started for wave: {}",
                        context.getWaveId());
                  })
              .onTick(
                  (level, elapsed) -> {
                    // TODO: 实际实现应生成向中心收缩的粒子效果
                    // 示例：从波场边缘向中心收束
                    double progress = elapsed / 30.0;
                    LOGGER.debug(
                        "[ShockfieldFx] Extinguish FX tick {} - progress: {}", elapsed, progress);
                  })
              .onStop(
                  (level, reason) -> {
                    LOGGER.debug(
                        "[ShockfieldFx] Extinguish FX stopped for wave: {} (reason: {})",
                        context.getWaveId(),
                        reason);
                  })
              .build();
        });

    LOGGER.info("[ShockfieldFx] Registered 3 Shockfield FX schemes");
  }

  /**
   * 播放波场创建 FX（在 ShockfieldFxService.onWaveCreate 中调用）。
   *
   * @param level 服务器世界
   * @param waveId 波场 ID
   * @param ownerId 所有者 ID
   * @param centerX 中心 X 坐标
   * @param centerY 中心 Y 坐标
   * @param centerZ 中心 Z 坐标
   */
  public static void playWaveCreateFx(
      net.minecraft.server.level.ServerLevel level,
      String waveId,
      UUID ownerId,
      double centerX,
      double centerY,
      double centerZ) {

    FxContext context =
        FxContext.builder(level)
            .owner(ownerId)
            .wave(waveId)
            .position(centerX, centerY, centerZ)
            .customParam("radius", 0.0) // 初始半径
            .build();

    FxEngine.registry().play("chestcavity:fx/shockfield/ring", context);
  }

  /**
   * 播放次级波创建 FX（在 ShockfieldFxService.onSubwaveCreate 中调用）。
   *
   * @param level 服务器世界
   * @param parentWaveId 父波场 ID
   * @param subWaveId 子波场 ID
   * @param ownerId 所有者 ID
   * @param centerX 中心 X 坐标
   * @param centerY 中心 Y 坐标
   * @param centerZ 中心 Z 坐标
   */
  public static void playSubwaveCreateFx(
      net.minecraft.server.level.ServerLevel level,
      String parentWaveId,
      String subWaveId,
      UUID ownerId,
      double centerX,
      double centerY,
      double centerZ) {

    FxContext context =
        FxContext.builder(level)
            .owner(ownerId)
            .wave(subWaveId)
            .position(centerX, centerY, centerZ)
            .customParam("parentWaveId", parentWaveId)
            .build();

    FxEngine.registry().play("chestcavity:fx/shockfield/subwave_pulse", context);
  }

  /**
   * 播放波场熄灭 FX（在 ShockfieldFxService.onExtinguish 中调用）。
   *
   * @param level 服务器世界
   * @param waveId 波场 ID
   * @param ownerId 所有者 ID
   * @param centerX 中心 X 坐标
   * @param centerY 中心 Y 坐标
   * @param centerZ 中心 Z 坐标
   * @param finalRadius 最终半径
   */
  public static void playExtinguishFx(
      net.minecraft.server.level.ServerLevel level,
      String waveId,
      UUID ownerId,
      double centerX,
      double centerY,
      double centerZ,
      double finalRadius) {

    FxContext context =
        FxContext.builder(level)
            .owner(ownerId)
            .wave(waveId)
            .position(centerX, centerY, centerZ)
            .customParam("finalRadius", finalRadius)
            .build();

    FxEngine.registry().play("chestcavity:fx/shockfield/extinguish", context);
  }
}
