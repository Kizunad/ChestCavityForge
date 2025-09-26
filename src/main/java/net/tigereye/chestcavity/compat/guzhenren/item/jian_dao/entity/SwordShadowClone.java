package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.JianYingGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.SingleSwordProjectile;
import net.tigereye.chestcavity.compat.guzhenren.util.PlayerSkinUtil;
import net.tigereye.chestcavity.registration.CCEntities;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Short-lived illusionary ally that mirrors the owner's strikes and hunts nearby targets.
 */
public class SwordShadowClone extends PathfinderMob {

    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(
            SwordShadowClone.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(
            SwordShadowClone.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> TEXTURE = SynchedEntityData.defineId(
            SwordShadowClone.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> MODEL = SynchedEntityData.defineId(
            SwordShadowClone.class, EntityDataSerializers.STRING);

    private static final int DEFAULT_COLOR = 0x60202030;
    private static final int MAX_LIFETIME_TICKS = 100;
    private static final int ATTACK_COOLDOWN_TICKS = 12;
    private static final double SEARCH_RADIUS = 12.0;
    private static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/steve.png");

    private int lifetime = MAX_LIFETIME_TICKS;
    private int attackCooldown;
    private float damage;
    private PlayerSkinUtil.SkinSnapshot skinTint;

    public SwordShadowClone(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.setNoAi(false);
    }

    public static SwordShadowClone spawn(ServerLevel level, Player owner, Vec3 position, PlayerSkinUtil.SkinSnapshot tint, float damage) {
        SwordShadowClone clone = CCEntities.SWORD_SHADOW_CLONE.get().create(level);
        if (clone == null) {
            return null;
        }
        clone.moveTo(position.x, position.y, position.z, owner.getYRot(), owner.getXRot());
        clone.setOwner(owner);
        clone.setSkin(tint);
        clone.damage = damage;
        level.addFreshEntity(clone);
        return clone;
    }

    public void setLifetime(int ticks) {
        this.lifetime = Math.max(1, ticks);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, Optional.empty());
        builder.define(COLOR, DEFAULT_COLOR);
        builder.define(TEXTURE, "");
        builder.define(MODEL, PlayerSkinUtil.SkinSnapshot.MODEL_DEFAULT);
    }

    @Override
    protected void registerGoals() {
        // No traditional goals – behaviour is handled manually in aiStep.
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }

        if (--lifetime <= 0) {
            disperse();
            return;
        }

        if (attackCooldown > 0) {
            attackCooldown--;
        }

        Player owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            disperse();
            return;
        }

        LivingEntity target = getTargetEntity(owner);
        if (target != null) {
            pursueTarget(target);
            tryStrike(target, owner);
        } else {
            guardOwner(owner);
        }
    }

    private void pursueTarget(LivingEntity target) {
        this.getNavigation().moveTo(target, 1.35);
    }

    private void guardOwner(Player owner) {
        if (this.distanceToSqr(owner) > 9.0) {
            this.getNavigation().moveTo(owner, 1.1);
        } else {
            this.getNavigation().stop();
        }
    }

    @Nullable
    private LivingEntity getTargetEntity(Player owner) {
        LivingEntity current = this.getTarget();
        if (isValidTarget(owner, current)) {
            return current;
        }

        AABB area = this.getBoundingBox().inflate(SEARCH_RADIUS, 4.0, SEARCH_RADIUS);
        List<LivingEntity> candidates = this.level().getEntitiesOfClass(LivingEntity.class, area, entity ->
                isValidTarget(owner, entity));
        LivingEntity nearest = null;
        double closest = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            double distance = this.distanceToSqr(candidate);
            if (distance < closest) {
                closest = distance;
                nearest = candidate;
            }
        }
        if (nearest != null) {
            this.setTarget(nearest);
        }
        return nearest;
    }

    private void tryStrike(LivingEntity target, Player owner) {
        if (attackCooldown > 0 || target == null) {
            return;
        }
        if (this.distanceToSqr(target) > 3.5) {
            return;
        }
        performStrike(target, owner);
    }

    private void performStrike(LivingEntity target, Player owner) {
        attackCooldown = ATTACK_COOLDOWN_TICKS;
        this.swing(InteractionHand.MAIN_HAND);
        if (skinTint != null) {
            SingleSwordProjectile.spawn(
                    this.level(),
                    owner,
                    this.position().add(0, this.getBbHeight() * 0.6, 0),
                    target.position().add(0, target.getBbHeight() * 0.5, 0),
                    skinTint
            );
        }
        JianYingGuOrganBehavior.applyTrueDamage(owner, target, damage);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2, false, true, true));
    }

    public void commandStrike(LivingEntity target) {
        if (this.level().isClientSide || target == null) {
            return;
        }
        Player owner = getOwner();
        if (owner == null || !isValidTarget(owner, target)) {
            return;
        }
        this.setTarget(target);
        this.attackCooldown = 0;
        performStrike(target, owner);
    }

    private boolean isValidTarget(Player owner, @Nullable LivingEntity target) {
        return target != null && target.isAlive() && target != owner && !target.isAlliedTo(owner);
    }

    private void disperse() {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel server) {
            BlockPos pos = this.blockPosition();
            server.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.6f, 0.9f);
            server.playSound(null, pos, SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.4f, 1.4f);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 10, 0.4, 0.4, 0.4, 0.02);
            server.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 12, 0.4, 0.6, 0.4, 0.2);
        }
        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        return false;
    }

    public void setOwner(@Nullable Player owner) {
        this.entityData.set(OWNER, owner == null ? Optional.empty() : Optional.of(owner.getUUID()));
    }

    @Nullable
    public Player getOwner() {
        if (!(this.level() instanceof ServerLevel server)) {
            return null;
        }
        Optional<UUID> ownerId = this.entityData.get(OWNER);
        return ownerId.map(server::getPlayerByUUID).orElse(null);
    }

    public boolean isOwnedBy(Player player) {
        Optional<UUID> id = this.entityData.get(OWNER);
        return id.isPresent() && id.get().equals(player.getUUID());
    }

    public void setSkin(PlayerSkinUtil.SkinSnapshot snapshot) {
        this.skinTint = snapshot;
        int argb = ((int) (snapshot.alpha() * 255) & 0xFF) << 24
                | ((int) (snapshot.red() * 255) & 0xFF) << 16
                | ((int) (snapshot.green() * 255) & 0xFF) << 8
                | ((int) (snapshot.blue() * 255) & 0xFF);
        this.entityData.set(COLOR, argb);
        this.entityData.set(TEXTURE, snapshot.texture().toString());
        this.entityData.set(MODEL, snapshot.model());
    }

    public float[] getTintComponents() {
        int argb = this.entityData.get(COLOR);
        return new float[]{
                ((argb >> 16) & 0xFF) / 255.0f,
                ((argb >> 8) & 0xFF) / 255.0f,
                (argb & 0xFF) / 255.0f,
                ((argb >> 24) & 0xFF) / 255.0f
        };
    }

    public ResourceLocation getSkinTexture() {
        String raw = this.entityData.get(TEXTURE);
        ResourceLocation parsed = raw.isEmpty() ? null : ResourceLocation.tryParse(raw);
        return parsed == null ? DEFAULT_TEXTURE : parsed;
    }

    public String getSkinModel() {
        String model = this.entityData.get(MODEL);
        return model == null || model.isBlank() ? PlayerSkinUtil.SkinSnapshot.MODEL_DEFAULT : model;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.entityData.set(OWNER, Optional.of(tag.getUUID("Owner")));
        }
        this.lifetime = tag.getInt("Lifetime");
        this.attackCooldown = tag.getInt("Cooldown");
        this.damage = tag.getFloat("CloneDamage");
        if (tag.contains("Color")) {
            this.entityData.set(COLOR, tag.getInt("Color"));
        }
        if (tag.contains("Texture")) {
            this.entityData.set(TEXTURE, tag.getString("Texture"));
        }
        if (tag.contains("Model")) {
            this.entityData.set(MODEL, tag.getString("Model"));
        }
        rebuildSkinFromData();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.entityData.get(OWNER).ifPresent(uuid -> tag.putUUID("Owner", uuid));
        tag.putInt("Lifetime", this.lifetime);
        tag.putInt("Cooldown", this.attackCooldown);
        tag.putFloat("CloneDamage", this.damage);
        tag.putInt("Color", this.entityData.get(COLOR));
        tag.putString("Texture", this.entityData.get(TEXTURE));
        tag.putString("Model", this.entityData.get(MODEL));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128.0;
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        if (entity == this) {
            return true;
        }
        if (entity instanceof SwordShadowClone clone) {
            Optional<UUID> myOwner = this.entityData.get(OWNER);
            Optional<UUID> otherOwner = clone.entityData.get(OWNER);
            return myOwner.isPresent() && myOwner.equals(otherOwner);
        }
        Player owner = getOwner();
        if (owner != null) {
            if (entity == owner) {
                return true;
            }
            if (entity instanceof LivingEntity living && living.isAlliedTo(owner)) {
                return true;
            }
        }
        return super.isAlliedTo(entity);
    }

    private void rebuildSkinFromData() {
        int argb = this.entityData.get(COLOR);
        float red = ((argb >> 16) & 0xFF) / 255.0f;
        float green = ((argb >> 8) & 0xFF) / 255.0f;
        float blue = (argb & 0xFF) / 255.0f;
        float alpha = ((argb >> 24) & 0xFF) / 255.0f;
        this.skinTint = new PlayerSkinUtil.SkinSnapshot(
                null,
                null,
                getSkinTexture(),
                getSkinModel(),
                null,
                red,
                green,
                blue,
                alpha
        );
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, 1.0);
    }
}
