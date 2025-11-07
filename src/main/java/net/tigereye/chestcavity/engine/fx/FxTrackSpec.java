package net.tigereye.chestcavity.engine.fx;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;

/**
 * FX Track 规格（Spec）：用于构建 FxTrack 的 Builder 模式。
 *
 * <p>Stage 2 核心类：支持 mergeKey、预算控制、合并策略等高级功能。
 */
public final class FxTrackSpec {

  private final String id;
  private final int ttlTicks;
  private final int tickInterval;
  private final UUID ownerId;
  private final String mergeKey;
  private final MergeStrategy mergeStrategy;

  // 生命周期回调
  private final Consumer<ServerLevel> onStart;
  private final BiConsumer<ServerLevel, Integer> onTick;
  private final BiConsumer<ServerLevel, StopReason> onStop;

  private FxTrackSpec(Builder builder) {
    this.id = builder.id;
    this.ttlTicks = builder.ttlTicks;
    this.tickInterval = builder.tickInterval;
    this.ownerId = builder.ownerId;
    this.mergeKey = builder.mergeKey;
    this.mergeStrategy = builder.mergeStrategy;
    this.onStart = builder.onStart;
    this.onTick = builder.onTick;
    this.onStop = builder.onStop;
  }

  /** 创建新的 Builder。 */
  public static Builder builder(String id) {
    return new Builder(id);
  }

  /** 转换为 FxTrack 实例。 */
  public FxTrack toTrack() {
    return new SpecBasedTrack(this);
  }

  // Getters
  public String getId() {
    return id;
  }

  public int getTtlTicks() {
    return ttlTicks;
  }

  public int getTickInterval() {
    return tickInterval;
  }

  public UUID getOwnerId() {
    return ownerId;
  }

  public String getMergeKey() {
    return mergeKey;
  }

  public MergeStrategy getMergeStrategy() {
    return mergeStrategy;
  }

  Consumer<ServerLevel> getOnStart() {
    return onStart;
  }

  BiConsumer<ServerLevel, Integer> getOnTick() {
    return onTick;
  }

  BiConsumer<ServerLevel, StopReason> getOnStop() {
    return onStop;
  }

  /** FxTrackSpec Builder。 */
  public static final class Builder {
    private final String id;
    private int ttlTicks = 100; // 默认 5 秒
    private int tickInterval = 1;
    private UUID ownerId = null;
    private String mergeKey = null;
    private MergeStrategy mergeStrategy = MergeStrategy.EXTEND_TTL;

    private Consumer<ServerLevel> onStart = level -> {};
    private BiConsumer<ServerLevel, Integer> onTick = (level, elapsed) -> {};
    private BiConsumer<ServerLevel, StopReason> onStop = (level, reason) -> {};

    private Builder(String id) {
      if (id == null || id.isEmpty()) {
        throw new IllegalArgumentException("Track ID cannot be null or empty");
      }
      this.id = id;
    }

    public Builder ttl(int ticks) {
      this.ttlTicks = Math.max(1, ticks);
      return this;
    }

    public Builder tickInterval(int interval) {
      this.tickInterval = Math.max(1, interval);
      return this;
    }

    public Builder owner(UUID ownerId) {
      this.ownerId = ownerId;
      return this;
    }

    public Builder mergeKey(String mergeKey) {
      this.mergeKey = mergeKey;
      return this;
    }

    public Builder mergeStrategy(MergeStrategy strategy) {
      this.mergeStrategy = strategy != null ? strategy : MergeStrategy.EXTEND_TTL;
      return this;
    }

    public Builder onStart(Consumer<ServerLevel> callback) {
      this.onStart = callback != null ? callback : level -> {};
      return this;
    }

    public Builder onTick(BiConsumer<ServerLevel, Integer> callback) {
      this.onTick = callback != null ? callback : (level, elapsed) -> {};
      return this;
    }

    public Builder onStop(BiConsumer<ServerLevel, StopReason> callback) {
      this.onStop = callback != null ? callback : (level, reason) -> {};
      return this;
    }

    public FxTrackSpec build() {
      return new FxTrackSpec(this);
    }
  }

  /** 基于 Spec 的 Track 实现。 */
  private static final class SpecBasedTrack implements FxTrack {
    private final FxTrackSpec spec;

    SpecBasedTrack(FxTrackSpec spec) {
      this.spec = spec;
    }

    @Override
    public String getId() {
      return spec.getId();
    }

    @Override
    public int getTtlTicks() {
      return spec.getTtlTicks();
    }

    @Override
    public int getTickInterval() {
      return spec.getTickInterval();
    }

    @Override
    public UUID getOwnerId() {
      return spec.getOwnerId();
    }

    @Override
    public void onStart(ServerLevel level) {
      if (spec.getOnStart() != null) {
        spec.getOnStart().accept(level);
      }
    }

    @Override
    public void onTick(ServerLevel level, int elapsedTicks) {
      if (spec.getOnTick() != null) {
        spec.getOnTick().accept(level, elapsedTicks);
      }
    }

    @Override
    public void onStop(ServerLevel level, StopReason reason) {
      if (spec.getOnStop() != null) {
        spec.getOnStop().accept(level, reason);
      }
    }
  }
}
