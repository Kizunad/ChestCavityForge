package net.tigereye.chestcavity.engine.fx;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 简单的 FX Track 示例实现。
 *
 * <p>用于演示 FxEngine 的基本用法。Stage 1 最小验证用例。
 */
public class SimpleFxTrack implements FxTrack {

  private static final Logger LOGGER = LoggerFactory.getLogger(SimpleFxTrack.class);

  private final String id;
  private final int ttlTicks;
  private final int tickInterval;
  private final UUID ownerId;

  public SimpleFxTrack(String id, int ttlTicks, int tickInterval, UUID ownerId) {
    this.id = id;
    this.ttlTicks = ttlTicks;
    this.tickInterval = tickInterval;
    this.ownerId = ownerId;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public int getTtlTicks() {
    return ttlTicks;
  }

  @Override
  public int getTickInterval() {
    return tickInterval;
  }

  @Override
  public UUID getOwnerId() {
    return ownerId;
  }

  @Override
  public void onStart(ServerLevel level) {
    LOGGER.debug("[SimpleFxTrack] Started: {} (TTL: {}, Interval: {})", id, ttlTicks, tickInterval);
  }

  @Override
  public void onTick(ServerLevel level, int elapsedTicks) {
    LOGGER.debug("[SimpleFxTrack] Tick {}/{}: {}", elapsedTicks, ttlTicks, id);
  }

  @Override
  public void onStop(ServerLevel level, StopReason reason) {
    // 注意：level 可能为 null（在替换/冲突场景下）
    String levelName = level != null ? level.dimension().location().toString() : "null";
    LOGGER.debug("[SimpleFxTrack] Stopped: {} (Reason: {}, Level: {})", id, reason, levelName);
  }
}
