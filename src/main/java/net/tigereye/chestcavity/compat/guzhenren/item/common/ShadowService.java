package net.tigereye.chestcavity.compat.guzhenren.item.common;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.SingleSwordProjectile;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.SwordShadowClone;
import net.tigereye.chestcavity.guzhenren.util.PlayerSkinUtil;
import net.tigereye.chestcavity.registration.CCItems;

/**
 * Shared helpers for spawning and commanding sword-shadow style replicas. Jian Dao and future
 * muscle organs (黑/白豕蛊) can use this utility instead of duplicating clone management.
 */
public final class ShadowService {

  private static final double DEFAULT_COMMAND_RADIUS = 16.0;
  private static final double DEFAULT_COMMAND_VERTICAL_RANGE = 6.0;

  // 轻量 SkinSnapshot 缓存（短 TTL），减少高频生成成本
  private static final java.util.concurrent.ConcurrentHashMap<String, SkinCacheEntry> SKIN_CACHE =
      new java.util.concurrent.ConcurrentHashMap<>();
  private static final long SKIN_CACHE_TTL_MS = 3000; // 3 秒 TTL

  private static final class SkinCacheEntry {
    final net.tigereye.chestcavity.guzhenren.util.PlayerSkinUtil.SkinSnapshot snapshot;
    final long expiryAtMs;

    SkinCacheEntry(
        net.tigereye.chestcavity.guzhenren.util.PlayerSkinUtil.SkinSnapshot snapshot,
        long expiryAtMs) {
      this.snapshot = snapshot;
      this.expiryAtMs = expiryAtMs;
    }
  }

  private static String skinCacheKey(java.util.UUID ownerId, ReplicaStyle style) {
    // 包含样式ID与颜色通道，确保不同色值不会误用
    return new StringBuilder(64)
        .append(ownerId)
        .append('|')
        .append(style.id())
        .append('|')
        .append(style.tintR())
        .append(',')
        .append(style.tintG())
        .append(',')
        .append(style.tintB())
        .append(',')
        .append(style.tintAlpha())
        .toString();
  }

  private ShadowService() {}

  /**
   * Configuration describing how a replica should appear.
   *
   * @param id Stable identifier used for logging/state keys.
   * @param captureBaseSkin Whether the player's skin should be captured for tinting.
   * @param tintR Red tint component (0-1 range).
   * @param tintG Green tint component (0-1 range).
   * @param tintB Blue tint component (0-1 range).
   * @param tintAlpha Alpha multiplier (0-1 range).
   */
  public record ReplicaStyle(
      String id, boolean captureBaseSkin, float tintR, float tintG, float tintB, float tintAlpha) {

    public ReplicaStyle {
      Objects.requireNonNull(id, "ReplicaStyle id");
      // Clamp tint channels to [0,1] for robustness
      tintR = clamp01(tintR);
      tintG = clamp01(tintG);
      tintB = clamp01(tintB);
      tintAlpha = clamp01(tintAlpha);
    }

    private static float clamp01(float v) {
      if (v < 0f) return 0f;
      if (v > 1f) return 1f;
      return v;
    }

    public static ReplicaStyle tinted(
        String id, boolean captureBaseSkin, float r, float g, float b, float alpha) {
      return new ReplicaStyle(id, captureBaseSkin, r, g, b, alpha);
    }
  }

  /** Default Jian Dao clone tint capturing the player's base skin. */
  public static final ReplicaStyle JIAN_DAO_CLONE =
      ReplicaStyle.tinted("jian_dao_clone", true, 0.12f, 0.05f, 0.22f, 0.6f);

  /** Jian Dao afterimage tint – uses a flat spectral colour without capturing the player's skin. */
  public static final ReplicaStyle JIAN_DAO_AFTERIMAGE =
      ReplicaStyle.tinted("jian_dao_afterimage", false, 0.10f, 0.05f, 0.20f, 0.45f);

  /** Darker overlay suited for黑豕蛊（不捕获玩家皮肤，仅使用暗色谱覆盖）。 */
  public static final ReplicaStyle HEI_ZHU_CLONE =
      ReplicaStyle.tinted("hei_zhu_clone", false, 0.0f, 0.0f, 0.0f, 0.50f);

  /** Brighter overlay suited for白豕蛊（不捕获玩家皮肤，仅使用亮色谱覆盖）。 */
  public static final ReplicaStyle BAI_ZHU_CLONE =
      ReplicaStyle.tinted("bai_zhu_clone", false, 1.0f, 1.0f, 1.0f, 0.50f);

  /** Captures a tinted skin snapshot according to the supplied style (player owner). */
  public static PlayerSkinUtil.SkinSnapshot captureTint(Player player, ReplicaStyle style) {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(style, "style");
    // 缓存命中优先
    String key = skinCacheKey(player.getUUID(), style);
    long now = System.currentTimeMillis();
    SkinCacheEntry cached = SKIN_CACHE.get(key);
    if (cached != null && cached.expiryAtMs > now) {
      return cached.snapshot;
    }

    PlayerSkinUtil.SkinSnapshot base =
        style.captureBaseSkin ? PlayerSkinUtil.capture(player) : null;
    PlayerSkinUtil.SkinSnapshot tinted =
        PlayerSkinUtil.withTint(base, style.tintR, style.tintG, style.tintB, style.tintAlpha);
    SKIN_CACHE.put(key, new SkinCacheEntry(tinted, now + SKIN_CACHE_TTL_MS));
    return tinted;
  }

  /**
   * Captures a tinted skin snapshot for any living entity. For non-player entities where a
   * Minecraft skin cannot be resolved, the snapshot falls back to the default Steve texture.
   */
  public static PlayerSkinUtil.SkinSnapshot captureTint(LivingEntity owner, ReplicaStyle style) {
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(style, "style");
    if (owner instanceof Player player) {
      return captureTint(player, style);
    }
    // Non-player: use tinted default skin as a generic shadow（同样走短TTL缓存）。
    String key = skinCacheKey(owner.getUUID(), style);
    long now = System.currentTimeMillis();
    SkinCacheEntry cached = SKIN_CACHE.get(key);
    if (cached != null && cached.expiryAtMs > now) {
      return cached.snapshot;
    }
    PlayerSkinUtil.SkinSnapshot tinted =
        PlayerSkinUtil.withTint(null, style.tintR, style.tintG, style.tintB, style.tintAlpha);
    SKIN_CACHE.put(key, new SkinCacheEntry(tinted, now + SKIN_CACHE_TTL_MS));
    return tinted;
  }

  /** Resolves the item stack displayed by the replica when rendering sword trails. */
  public static ItemStack resolveDisplayStack(Player player) {
    ItemStack mainHand = player.getMainHandItem();
    if (!mainHand.isEmpty()) {
      return mainHand.copy();
    }
    ItemStack offHand = player.getOffhandItem();
    if (!offHand.isEmpty()) {
      return offHand.copy();
    }
    if (CCItems.GUZHENREN_XIE_NING_JIAN != Items.AIR) {
      return new ItemStack(CCItems.GUZHENREN_XIE_NING_JIAN);
    }
    return SingleSwordProjectile.defaultDisplayItem();
  }

  /** Non-player overload: fall back to the default display sword. */
  public static ItemStack resolveDisplayStack(LivingEntity owner) {
    if (owner instanceof Player player) {
      return resolveDisplayStack(player);
    }
    return SingleSwordProjectile.defaultDisplayItem();
  }

  /** Calculates a default anchor position for sword trails relative to an entity. */
  public static Vec3 swordAnchor(LivingEntity entity) {
    return entity.position().add(0.0, entity.getBbHeight() * 0.7, 0.0);
  }

  /**
   * Calculates a sword tip position using the entity look vector, falling back to rotation if
   * needed.
   */
  public static Vec3 swordTip(LivingEntity entity, Vec3 anchor) {
    Vec3 look = entity.getLookAngle();
    if (look.lengthSqr() < 1.0E-4) {
      look = Vec3.directionFromRotation(entity.getXRot(), entity.getYRot());
    }
    if (look.lengthSqr() < 1.0E-4) {
      look = new Vec3(0.0, 0.0, 1.0);
    }
    return anchor.add(look.normalize().scale(2.0));
  }

  /** Computes a spawn position relative to the owner, moving forward and sideways in look-space. */
  public static Vec3 offsetFromOwner(
      Player owner, double forward, double vertical, double sideways) {
    Vec3 forwardDir = owner.getLookAngle();
    if (forwardDir.lengthSqr() < 1.0E-4) {
      forwardDir = Vec3.directionFromRotation(owner.getXRot(), owner.getYRot());
    }
    if (forwardDir.lengthSqr() < 1.0E-4) {
      forwardDir = new Vec3(0.0, 0.0, 1.0);
    }
    Vec3 horizontalForward = new Vec3(forwardDir.x, 0.0, forwardDir.z);
    if (horizontalForward.lengthSqr() < 1.0E-4) {
      horizontalForward = new Vec3(0.0, 0.0, 1.0);
    } else {
      horizontalForward = horizontalForward.normalize();
    }
    Vec3 sidewaysDir = new Vec3(-horizontalForward.z, 0.0, horizontalForward.x);
    return owner
        .position()
        .add(horizontalForward.scale(forward))
        .add(0.0, vertical, 0.0)
        .add(sidewaysDir.scale(sideways));
  }

  /**
   * Spawns a sword shadow clone at the specified position with the given lifetime. Returns the
   * created clone if successful.
   */
  public static Optional<SwordShadowClone> spawn(
      ServerLevel level,
      Player owner,
      Vec3 position,
      ReplicaStyle style,
      float damage,
      int lifetimeTicks,
      Consumer<SwordShadowClone> config) {
    Objects.requireNonNull(level, "level");
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(position, "position");
    Objects.requireNonNull(style, "style");
    PlayerSkinUtil.SkinSnapshot tint = captureTint(owner, style);
    SwordShadowClone clone = SwordShadowClone.spawn(level, owner, position, tint, damage);
    if (clone == null) {
      return Optional.empty();
    }
    clone.setLifetime(lifetimeTicks);
    if (config != null) {
      config.accept(clone);
    }
    return Optional.of(clone);
  }

  /** Convenience overload without additional configuration callback. */
  public static Optional<SwordShadowClone> spawn(
      ServerLevel level,
      Player owner,
      Vec3 position,
      ReplicaStyle style,
      float damage,
      int lifetimeTicks) {
    return spawn(level, owner, position, style, damage, lifetimeTicks, null);
  }

  /** Commands existing clones owned by the player within the default radius to strike a target. */
  public static void commandOwnedClones(ServerLevel level, Player owner, LivingEntity target) {
    commandOwnedClones(
        level, owner, target, DEFAULT_COMMAND_RADIUS, DEFAULT_COMMAND_VERTICAL_RANGE, clone -> {});
  }

  /** Commands existing clones owned by the player within a radius to strike a target. */
  public static void commandOwnedClones(
      ServerLevel level,
      Player owner,
      LivingEntity target,
      double horizontalRadius,
      double verticalRadius,
      Consumer<SwordShadowClone> extraConfig) {
    Objects.requireNonNull(level, "level");
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(target, "target");
    AABB area = owner.getBoundingBox().inflate(horizontalRadius, verticalRadius, horizontalRadius);
    List<SwordShadowClone> clones =
        level.getEntitiesOfClass(SwordShadowClone.class, area, clone -> clone.isOwnedBy(owner));
    for (SwordShadowClone clone : clones) {
      if (extraConfig != null) {
        extraConfig.accept(clone);
      }
      clone.commandStrike(target);
    }
  }
}
