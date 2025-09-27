package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.guzhenren.util.PlayerSkinUtil;
import net.tigereye.chestcavity.registration.CCEntities;
import org.joml.Vector3f;
import net.tigereye.chestcavity.registration.CCItems;

import java.util.UUID;

/**
 * Lightweight visual entity that represents the sword shadow strike. It does not perform damage
 * directly – that is handled by the owning organ behaviour – but is responsible for client-side
 * particles and timing so the strike can be perceived even without custom models.
 */
public class SingleSwordProjectile extends Entity {

    private static final int LIFETIME_TICKS = 12;
    private static final int EXTEND_TICKS = 6;
    private static final double EXTEND_DISTANCE = 2.0;
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(
            SingleSwordProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> DISPLAY_ITEM = SynchedEntityData.defineId(
            SingleSwordProjectile.class, EntityDataSerializers.ITEM_STACK);

    private UUID ownerId;
    private double anchorX;
    private double anchorY;
    private double anchorZ;
    private double dirX;
    private double dirY;
    private double dirZ;
    private boolean anchorInitialized;

    public SingleSwordProjectile(EntityType<? extends SingleSwordProjectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.anchorInitialized = false;
    }

    public SingleSwordProjectile(Level level, LivingEntity owner, Vec3 origin, Vec3 target, int argbColor,
                                 ItemStack displayItem) {
        this(CCEntities.SINGLE_SWORD_PROJECTILE.get(), level);
        if (owner != null) {
            this.ownerId = owner.getUUID();
        }
        initialiseAnchor(origin);
        initialiseDirection(owner, origin, target);
        this.setPos(origin.x, origin.y, origin.z);
        this.setDeltaMovement(0.0, 0.0, 0.0);
        updateRotation();
        this.entityData.set(COLOR, argbColor);
        this.setDisplayItem(displayItem);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(COLOR, 0x60202030);
        builder.define(DISPLAY_ITEM, ItemStack.EMPTY);
    }

    @Override
    public void tick() {
        super.tick();
        ensureInitialised();

        double progress = Math.min(1.0, (double) Math.min(this.tickCount, EXTEND_TICKS) / EXTEND_TICKS);
        double distance = EXTEND_DISTANCE * progress;
        double newX = anchorX + dirX * distance;
        double newY = anchorY + dirY * distance;
        double newZ = anchorZ + dirZ * distance;
        this.setPos(newX, newY, newZ);

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
        Vec3 motion = new Vec3(dirX, dirY, dirZ);
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
        if (tag.contains("DisplayItem", Tag.TAG_COMPOUND)) {
            HolderLookup.Provider lookup = this.level() != null ? this.level().registryAccess() : null;
            ItemStack parsed = lookup != null
                    ? ItemStack.parseOptional(lookup, tag.getCompound("DisplayItem"))
                    : ItemStack.EMPTY;
        this.entityData.set(DISPLAY_ITEM, parsed.isEmpty() ? defaultDisplayItem() : parsed);
        } else {
            this.entityData.set(DISPLAY_ITEM, defaultDisplayItem());
        }
        this.anchorX = tag.getDouble("AnchorX");
        this.anchorY = tag.getDouble("AnchorY");
        this.anchorZ = tag.getDouble("AnchorZ");
        this.dirX = tag.getDouble("DirX");
        this.dirY = tag.getDouble("DirY");
        this.dirZ = tag.getDouble("DirZ");
        this.anchorInitialized = true;
        updateRotation();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
        tag.putInt("Color", this.entityData.get(COLOR));
        ItemStack stack = this.entityData.get(DISPLAY_ITEM);
        if (!stack.isEmpty()) {
            HolderLookup.Provider lookup = this.level() != null ? this.level().registryAccess() : null;
            if (lookup != null) {
                tag.put("DisplayItem", stack.save(lookup, new CompoundTag()));
            }
        }
        tag.putDouble("AnchorX", this.anchorX);
        tag.putDouble("AnchorY", this.anchorY);
        tag.putDouble("AnchorZ", this.anchorZ);
        tag.putDouble("DirX", this.dirX);
        tag.putDouble("DirY", this.dirY);
        tag.putDouble("DirZ", this.dirZ);
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

    public void setDisplayItem(ItemStack stack) {
        ItemStack copy = stack == null ? ItemStack.EMPTY : stack.copy();
        if (copy.isEmpty()) {
            copy = defaultDisplayItem();
        }
        this.entityData.set(DISPLAY_ITEM, copy);
    }

    public ItemStack getDisplayItem() {
        return this.entityData.get(DISPLAY_ITEM);
    }

    private static Item defaultSwordItem() {
        Item sword = CCItems.GUZHENREN_XIE_NING_JIAN;
        return sword == Items.AIR ? Items.IRON_SWORD : sword;
    }

    public static ItemStack defaultDisplayItem() {
        Item item = defaultSwordItem();
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private void initialiseAnchor(Vec3 origin) {
        this.anchorX = origin.x;
        this.anchorY = origin.y;
        this.anchorZ = origin.z;
    }

    private void initialiseDirection(LivingEntity owner, Vec3 origin, Vec3 target) {
        Vec3 delta = target.subtract(origin);
        if (delta.lengthSqr() < 1.0E-6) {
            Vec3 fallback = owner != null ? owner.getLookAngle() : Vec3.directionFromRotation(this.getXRot(), this.getYRot());
            if (fallback.lengthSqr() < 1.0E-6) {
                fallback = new Vec3(0.0, 0.0, 1.0);
            }
            delta = fallback.normalize().scale(EXTEND_DISTANCE);
        }
        Vec3 normalised = delta.normalize();
        this.dirX = normalised.x;
        this.dirY = normalised.y;
        this.dirZ = normalised.z;
        this.anchorInitialized = true;
    }

    private void ensureInitialised() {
        if (this.anchorInitialized) {
            return;
        }
        initialiseAnchor(this.position());
        Vec3 step = this.getDeltaMovement();
        if (step.lengthSqr() < 1.0E-6) {
            Vec3 fallback = Vec3.directionFromRotation(this.getXRot(), this.getYRot());
            if (fallback.lengthSqr() < 1.0E-6) {
                fallback = new Vec3(0.0, 0.0, 1.0);
            }
            step = fallback.normalize().scale(EXTEND_DISTANCE);
        }
        initialiseDirection(null, this.position(), this.position().add(step));
        updateRotation();
    }

    private void updateRotation() {
        double horizontalMag = Math.sqrt(this.dirX * this.dirX + this.dirZ * this.dirZ);
        this.setYRot((float) (Math.atan2(this.dirZ, this.dirX) * (180F / Math.PI)) - 90.0f);
        this.setXRot((float) (-(Math.atan2(this.dirY, horizontalMag) * (180F / Math.PI))));
    }

    public static SingleSwordProjectile spawn(Level level, LivingEntity owner, Vec3 origin, Vec3 target,
                                              PlayerSkinUtil.SkinSnapshot tint, ItemStack display) {
        SingleSwordProjectile projectile = new SingleSwordProjectile(level, owner, origin, target, 0x80222233,
                display);
        projectile.setTint(tint);
        level.addFreshEntity(projectile);
        return projectile;
    }

    public static SingleSwordProjectile spawn(Level level, LivingEntity owner, Vec3 origin, Vec3 target, PlayerSkinUtil.SkinSnapshot tint) {
        return spawn(level, owner, origin, target, tint, defaultDisplayItem());
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
