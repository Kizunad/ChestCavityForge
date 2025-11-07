package net.tigereye.chestcavity.engine.fx;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * FX 上下文：提供创建 FxTrack 所需的上下文信息。
 *
 * <p>用于 FxRegistry 的 factory 模式，在"定义处"和"调用处"解耦。
 *
 * <p>Stage 3 核心类。
 */
public final class FxContext {

  private final ServerLevel level;
  private final UUID ownerId;
  private final String waveId;
  private final Vec3 position;
  private final Map<String, Object> customParams;

  private FxContext(Builder builder) {
    this.level = builder.level;
    this.ownerId = builder.ownerId;
    this.waveId = builder.waveId;
    this.position = builder.position;
    this.customParams = builder.customParams;
  }

  /** 创建新的 Builder。 */
  public static Builder builder(ServerLevel level) {
    return new Builder(level);
  }

  // Getters

  public ServerLevel getLevel() {
    return level;
  }

  public UUID getOwnerId() {
    return ownerId;
  }

  public String getWaveId() {
    return waveId;
  }

  public Vec3 getPosition() {
    return position;
  }

  /**
   * 获取自定义参数。
   *
   * @param key 参数键
   * @param defaultValue 默认值
   * @return 参数值，如果不存在则返回默认值
   */
  @SuppressWarnings("unchecked")
  public <T> T getCustomParam(String key, T defaultValue) {
    Object value = customParams.get(key);
    if (value == null) {
      return defaultValue;
    }
    try {
      return (T) value;
    } catch (ClassCastException e) {
      return defaultValue;
    }
  }

  /**
   * 获取自定义参数（无默认值）。
   *
   * @param key 参数键
   * @return 参数值，如果不存在则返回 null
   */
  public Object getCustomParam(String key) {
    return customParams.get(key);
  }

  /** FxContext Builder。 */
  public static final class Builder {
    private final ServerLevel level;
    private UUID ownerId = null;
    private String waveId = null;
    private Vec3 position = null;
    private Map<String, Object> customParams = new HashMap<>();

    private Builder(ServerLevel level) {
      if (level == null) {
        throw new IllegalArgumentException("ServerLevel cannot be null");
      }
      this.level = level;
    }

    public Builder owner(UUID ownerId) {
      this.ownerId = ownerId;
      return this;
    }

    public Builder wave(String waveId) {
      this.waveId = waveId;
      return this;
    }

    public Builder position(Vec3 position) {
      this.position = position;
      return this;
    }

    public Builder position(double x, double y, double z) {
      this.position = new Vec3(x, y, z);
      return this;
    }

    public Builder customParam(String key, Object value) {
      this.customParams.put(key, value);
      return this;
    }

    public Builder customParams(Map<String, Object> params) {
      if (params != null) {
        this.customParams.putAll(params);
      }
      return this;
    }

    public FxContext build() {
      return new FxContext(this);
    }
  }
}
