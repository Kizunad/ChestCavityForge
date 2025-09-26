package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.util.PlayerSkinUtil;
import net.tigereye.chestcavity.registration.CCEntities;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * Lightweight visual entity that represents the sword shadow strike. It does not perform damage
 * directly – that is handled by the owning organ behaviour – but is responsible for client-side
 * particles and timing so the strike can be perceived even without custom models.
 */
public class SingleSwordProjectile extends Entity {

    private static final int LIFETIME_TICKS = 12;
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(
            SingleSwordProjectile.class, EntityDataSerializers.INT);

    private UUID ownerId;

    public SingleSwordProjectile(EntityType<? extends SingleSwordProjectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public SingleSwordProjectile(Level level, LivingEntity owner, Vec3 origin, Vec3 target, int argbColor) {
        this(CCEntities.SINGLE_SWORD_PROJECTILE.get(), level);
        if (owner != null) {
            this.ownerId = owner.getUUID();
        }
        this.setPos(origin.x, origin.y, origin.z);
        Vec3 delta = target.subtract(origin);
        if (delta.lengthSqr() > 1.0E-4) {
            Vec3 normalised = delta.normalize();
            this.setDeltaMovement(normalised.scale(0.2));
            this.setYRot((float) (Math.atan2(normalised.z, normalised.x) * (180F / Math.PI)) - 90.0f);
            this.setXRot((float) (-(Math.atan2(normalised.y, Math.sqrt(normalised.x * normalised.x + normalised.z * normalised.z)) * (180F / Math.PI))));
        }
        this.entityData.set(COLOR, argbColor);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(COLOR, 0x80222233);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            spawnParticles();
        } else if (this.tickCount > LIFETIME_TICKS) {
            if (this.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 6, 0.15, 0.1, 0.15, 0.01);
            }
            this.discard();
        }
    }

    private void spawnParticles() {
        int argb = this.entityData.get(COLOR);
        float alpha = ((argb >> 24) & 0xFF) / 255.0f;
        float red = ((argb >> 16) & 0xFF) / 255.0f;
        float green = ((argb >> 8) & 0xFF) / 255.0f;
        float blue = (argb & 0xFF) / 255.0f;

        Vec3 pos = this.position();
        Vec3 motion = this.getDeltaMovement();
        Vec3 lateral = motion.lengthSqr() > 1.0E-4
                ? new Vec3(-motion.z, 0.0, motion.x).normalize().scale(0.25)
                : new Vec3(0.25, 0.0, 0.0);

        double baseX = pos.x;
        double baseY = pos.y + 0.5;
        double baseZ = pos.z;

        Vector3f tint = new Vector3f(red, green, blue);
        DustParticleOptions haze = new DustParticleOptions(tint, Math.max(0.1f, alpha));

        for (int i = 0; i < 4; i++) {
            double offset = (i / 3.0) - 0.5;
            double ox = baseX + lateral.x * offset;
            double oz = baseZ + lateral.z * offset;
            this.level().addParticle(haze, ox, baseY, oz, 0.0, 0.0, 0.0);
        }
        this.level().addParticle(ParticleTypes.SWEEP_ATTACK, baseX, baseY, baseZ, 0.0, 0.0, 0.0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerId = tag.getUUID("Owner");
        }
        if (tag.contains("Color")) {
            this.entityData.set(COLOR, tag.getInt("Color"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
        tag.putInt("Color", this.entityData.get(COLOR));
    }

    public LivingEntity getOwner() {
        if (this.ownerId == null) {
            return null;
        }
        if (!(this.level() instanceof ServerLevel server)) {
            return null;
        }
        return (LivingEntity) server.getEntity(this.ownerId);
    }

    public void setTint(PlayerSkinUtil.SkinSnapshot skin) {
        if (skin == null) {
            return;
        }
        int argb = ((int) (skin.alpha() * 255) & 0xFF) << 24
                | ((int) (skin.red() * 255) & 0xFF) << 16
                | ((int) (skin.green() * 255) & 0xFF) << 8
                | ((int) (skin.blue() * 255) & 0xFF);
        this.entityData.set(COLOR, argb);
    }

    public static SingleSwordProjectile spawn(Level level, LivingEntity owner, Vec3 origin, Vec3 target, PlayerSkinUtil.SkinSnapshot tint) {
        SingleSwordProjectile projectile = new SingleSwordProjectile(level, owner, origin, target, 0x80222233);
        projectile.setTint(tint);
        level.addFreshEntity(projectile);
        return projectile;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 64.0;
    }

    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(
                this.getId(),
                this.getUUID(),
                this.getX(),
                this.getY(),
                this.getZ(),
                this.getYRot(),
                this.getXRot(),
                this.getType(),
                0,
                this.getDeltaMovement(),
                this.getYHeadRot()
        );
    }
}

