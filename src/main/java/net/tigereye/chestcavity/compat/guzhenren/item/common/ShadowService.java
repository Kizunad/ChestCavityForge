package net.tigereye.chestcavity.compat.guzhenren.item.common;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.SwordShadowClone;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.SingleSwordProjectile;
import net.tigereye.chestcavity.guzhenren.util.PlayerSkinUtil;
import net.tigereye.chestcavity.registration.CCItems;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Shared helpers for spawning and commanding sword-shadow style replicas. Jian Dao and future
 * muscle organs (黑/白豕蛊) can use this utility instead of duplicating clone management.
 */
public final class ShadowService {

    private static final double DEFAULT_COMMAND_RADIUS = 16.0;
    private static final double DEFAULT_COMMAND_VERTICAL_RANGE = 6.0;

    private ShadowService() {
    }

    /**
     * Configuration describing how a replica should appear.
     *
     * @param id                Stable identifier used for logging/state keys.
     * @param captureBaseSkin   Whether the player's skin should be captured for tinting.
     * @param tintR             Red tint component (0-1 range).
     * @param tintG             Green tint component (0-1 range).
     * @param tintB             Blue tint component (0-1 range).
     * @param tintAlpha         Alpha multiplier (0-1 range).
     */
    public record ReplicaStyle(String id, boolean captureBaseSkin, float tintR, float tintG, float tintB, float tintAlpha) {

        public ReplicaStyle {
            Objects.requireNonNull(id, "ReplicaStyle id");
        }

        public static ReplicaStyle tinted(String id, boolean captureBaseSkin, float r, float g, float b, float alpha) {
            return new ReplicaStyle(id, captureBaseSkin, r, g, b, alpha);
        }
    }

    /** Default Jian Dao clone tint capturing the player's base skin. */
    public static final ReplicaStyle JIAN_DAO_CLONE = ReplicaStyle.tinted("jian_dao_clone", true, 0.12f, 0.05f, 0.22f, 0.6f);

    /** Jian Dao afterimage tint – uses a flat spectral colour without capturing the player's skin. */
    public static final ReplicaStyle JIAN_DAO_AFTERIMAGE = ReplicaStyle.tinted("jian_dao_afterimage", false, 0.10f, 0.05f, 0.20f, 0.45f);

    /** Darker overlay suited for黑豕蛊 (captures base skin then desaturates heavily). */
    public static final ReplicaStyle HEI_ZHU_CLONE = ReplicaStyle.tinted("hei_zhu_clone", true, 0.05f, 0.05f, 0.06f, 0.85f);

    /** Brighter overlay suited for白豕蛊 (captures base skin with a light aura). */
    public static final ReplicaStyle BAI_ZHU_CLONE = ReplicaStyle.tinted("bai_zhu_clone", true, 0.80f, 0.82f, 0.88f, 0.65f);

    /**
     * Captures a tinted skin snapshot according to the supplied style.
     */
    public static PlayerSkinUtil.SkinSnapshot captureTint(Player player, ReplicaStyle style) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(style, "style");
        PlayerSkinUtil.SkinSnapshot base = style.captureBaseSkin ? PlayerSkinUtil.capture(player) : null;
        return PlayerSkinUtil.withTint(base, style.tintR, style.tintG, style.tintB, style.tintAlpha);
    }

    /**
     * Resolves the item stack displayed by the replica when rendering sword trails.
     */
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

    /**
     * Calculates a default anchor position for sword trails relative to an entity.
     */
    public static Vec3 swordAnchor(LivingEntity entity) {
        return entity.position().add(0.0, entity.getBbHeight() * 0.7, 0.0);
    }

    /**
     * Calculates a sword tip position using the entity look vector, falling back to rotation if needed.
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

    /**
     * Computes a spawn position relative to the owner, moving forward and sideways in look-space.
     */
    public static Vec3 offsetFromOwner(Player owner, double forward, double vertical, double sideways) {
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
        return owner.position()
                .add(horizontalForward.scale(forward))
                .add(0.0, vertical, 0.0)
                .add(sidewaysDir.scale(sideways));
    }

    /**
     * Spawns a sword shadow clone at the specified position with the given lifetime. Returns the created clone if successful.
     */
    public static Optional<SwordShadowClone> spawn(ServerLevel level, Player owner, Vec3 position, ReplicaStyle style, float damage, int lifetimeTicks, Consumer<SwordShadowClone> config) {
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

    /**
     * Convenience overload without additional configuration callback.
     */
    public static Optional<SwordShadowClone> spawn(ServerLevel level, Player owner, Vec3 position, ReplicaStyle style, float damage, int lifetimeTicks) {
        return spawn(level, owner, position, style, damage, lifetimeTicks, null);
    }

    /**
     * Commands existing clones owned by the player within the default radius to strike a target.
     */
    public static void commandOwnedClones(ServerLevel level, Player owner, LivingEntity target) {
        commandOwnedClones(level, owner, target, DEFAULT_COMMAND_RADIUS, DEFAULT_COMMAND_VERTICAL_RANGE, clone -> {});
    }

    /**
     * Commands existing clones owned by the player within a radius to strike a target.
     */
    public static void commandOwnedClones(ServerLevel level, Player owner, LivingEntity target, double horizontalRadius, double verticalRadius, Consumer<SwordShadowClone> extraConfig) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(target, "target");
        AABB area = owner.getBoundingBox().inflate(horizontalRadius, verticalRadius, horizontalRadius);
        List<SwordShadowClone> clones = level.getEntitiesOfClass(
                SwordShadowClone.class,
                area,
                clone -> clone.isOwnedBy(owner)
        );
        for (SwordShadowClone clone : clones) {
            if (extraConfig != null) {
                extraConfig.accept(clone);
            }
            clone.commandStrike(target);
        }
    }
}
