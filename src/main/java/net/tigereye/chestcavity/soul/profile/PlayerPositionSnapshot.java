package net.tigereye.chestcavity.soul.profile;

import java.util.Objects;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 玩家位置信息快照（维度 + 坐标 + 朝向）
 *
 * <p>作用 - 为每个灵魂存档保存精确位置，包含维度、坐标、旋转与头部朝向。 - 恢复时使用安全回退坐标避免 NaN/Inf 导致的传送异常。
 */
public final class PlayerPositionSnapshot {

  private static final String DIMENSION_KEY = "dimension";
  private static final String X_KEY = "x";
  private static final String Y_KEY = "y";
  private static final String Z_KEY = "z";
  private static final String YAW_KEY = "yaw";
  private static final String PITCH_KEY = "pitch";
  private static final String HEAD_YAW_KEY = "headYaw";

  private static final double FALLBACK_X = 0.5D;
  private static final double FALLBACK_Y = 64.0D;
  private static final double FALLBACK_Z = 0.5D;

  private final ResourceKey<Level> dimension;
  private final double x;
  private final double y;
  private final double z;
  private final float yaw;
  private final float pitch;
  private final float headYaw;

  private PlayerPositionSnapshot(
      ResourceKey<Level> dimension,
      double x,
      double y,
      double z,
      float yaw,
      float pitch,
      float headYaw) {
    this.dimension = Objects.requireNonNull(dimension);
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
    this.headYaw = headYaw;
  }

  public static PlayerPositionSnapshot of(
      ResourceKey<Level> dimension,
      double x,
      double y,
      double z,
      float yaw,
      float pitch,
      float headYaw) {
    return new PlayerPositionSnapshot(dimension, x, y, z, yaw, pitch, headYaw);
  }

  public static PlayerPositionSnapshot capture(Player player) {
    return new PlayerPositionSnapshot(
        player.level().dimension(),
        player.getX(),
        player.getY(),
        player.getZ(),
        player.getYRot(),
        player.getXRot(),
        player.getYHeadRot());
  }

  public ResourceKey<Level> dimension() {
    return dimension;
  }

  public double x() {
    return x;
  }

  public double y() {
    return y;
  }

  public double z() {
    return z;
  }

  public float yaw() {
    return yaw;
  }

  public float pitch() {
    return pitch;
  }

  public float headYaw() {
    return headYaw;
  }

  public void restore(ServerPlayer player) {
    // 仅服务端允许传送；若维度无效则直接放弃
    ServerLevel target = player.server.getLevel(dimension);
    if (target == null) {
      return;
    }
    double safeX = Double.isFinite(x) ? x : FALLBACK_X;
    double safeY = Double.isFinite(y) ? y : FALLBACK_Y;
    double safeZ = Double.isFinite(z) ? z : FALLBACK_Z;
    player.teleportTo(target, safeX, safeY, safeZ, yaw, pitch);
    player.setYHeadRot(headYaw);
  }

  public CompoundTag save(HolderLookup.Provider provider) {
    CompoundTag tag = new CompoundTag();
    tag.putString(DIMENSION_KEY, dimension.location().toString());
    tag.putDouble(X_KEY, x);
    tag.putDouble(Y_KEY, y);
    tag.putDouble(Z_KEY, z);
    tag.putFloat(YAW_KEY, yaw);
    tag.putFloat(PITCH_KEY, pitch);
    tag.putFloat(HEAD_YAW_KEY, headYaw);
    return tag;
  }

  public static PlayerPositionSnapshot load(CompoundTag tag) {
    ResourceKey<Level> dim = Level.OVERWORLD;
    if (tag.contains(DIMENSION_KEY)) {
      ResourceLocation id = ResourceLocation.tryParse(tag.getString(DIMENSION_KEY));
      if (id != null) {
        dim = ResourceKey.create(Registries.DIMENSION, id);
      }
    }
    double x = tag.contains(X_KEY) ? tag.getDouble(X_KEY) : FALLBACK_X;
    double y = tag.contains(Y_KEY) ? tag.getDouble(Y_KEY) : FALLBACK_Y;
    double z = tag.contains(Z_KEY) ? tag.getDouble(Z_KEY) : FALLBACK_Z;
    float yaw = tag.contains(YAW_KEY) ? tag.getFloat(YAW_KEY) : 0.0F;
    float pitch = tag.contains(PITCH_KEY) ? tag.getFloat(PITCH_KEY) : 0.0F;
    float headYaw = tag.contains(HEAD_YAW_KEY) ? tag.getFloat(HEAD_YAW_KEY) : yaw;
    return new PlayerPositionSnapshot(dim, x, y, z, yaw, pitch, headYaw);
  }

  public static PlayerPositionSnapshot empty() {
    return new PlayerPositionSnapshot(
        Level.OVERWORLD, FALLBACK_X, FALLBACK_Y, FALLBACK_Z, 0.0F, 0.0F, 0.0F);
  }

  /**
   * Restore only the recorded position/orientation if snapshot's dimension equals the player's
   * current one. Does not perform any cross-dimension teleport.
   */
  public void restoreSameDimension(ServerPlayer player) {
    if (player.level().dimension().equals(dimension)) {
      player.moveTo(x, y, z, yaw, pitch);
      player.setYHeadRot(headYaw);
    }
  }
}
