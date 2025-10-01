package net.tigereye.chestcavity.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.registration.CCTags;
import net.tigereye.chestcavity.util.ProjectileParameterReceiver;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative projectile that represents a Jian Dao style sword slash.
 */
public class SwordSlashProjectile extends Entity implements ProjectileParameterReceiver {

    private static final EntityDataAccessor<Float> DATA_LENGTH =
            SynchedEntityData.defineId(SwordSlashProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_THICKNESS =
            SynchedEntityData.defineId(SwordSlashProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFESPAN =
            SynchedEntityData.defineId(SwordSlashProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(SwordSlashProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_BREAK_POWER =
            SynchedEntityData.defineId(SwordSlashProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_MAX_PIERCE =
            SynchedEntityData.defineId(SwordSlashProjectile.class, EntityDataSerializers.INT);

    private static final CCConfig.SwordSlashConfig DEFAULTS = new CCConfig.SwordSlashConfig();

    private int lifespan;
    private int age;
    private double damage;
    private double breakPower;
    private int maxPierce;
    private double speedPerTick;
    private Vec3 direction = Vec3.ZERO;
    private UUID ownerId;
    private LivingEntity cachedOwner;
    private final Set<UUID> damagedEntities = new HashSet<>();
    private int pierceCount;
    private boolean configured;

    public SwordSlashProjectile(EntityType<? extends SwordSlashProjectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.lifespan = DEFAULTS.defaultLifespanTicks;
        this.damage = DEFAULTS.defaultDamage;
        this.breakPower = DEFAULTS.defaultBreakPower;
        this.maxPierce = DEFAULTS.defaultMaxPierce;
        this.speedPerTick = Math.max(0.05D, DEFAULTS.defaultLength / Math.max(1, this.lifespan));
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LENGTH, (float) DEFAULTS.defaultLength);
        builder.define(DATA_THICKNESS, (float) DEFAULTS.defaultThickness);
        builder.define(DATA_LIFESPAN, DEFAULTS.defaultLifespanTicks);
        builder.define(DATA_DAMAGE, (float) DEFAULTS.defaultDamage);
        builder.define(DATA_BREAK_POWER, (float) DEFAULTS.defaultBreakPower);
        builder.define(DATA_MAX_PIERCE, DEFAULTS.defaultMaxPierce);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-6 && direction.lengthSqr() > 1.0E-6) {
            motion = direction.scale(speedPerTick);
        }
        Vec3 start = this.position();
        Vec3 end = start.add(motion);
        if (!this.level().isClientSide) {
            handleEntityHits(start, end);
            handleBlockBreaks(start, end);
        } else {
            spawnClientTrail();
        }
        this.move(MoverType.SELF, motion);
        if (motion.lengthSqr() > 1.0E-6) {
            direction = motion.normalize();
            this.setDeltaMovement(direction.scale(speedPerTick));
            updateRotationFromDirection();
        }
        age++;
        if (age >= lifespan) {
            discard();
        }
    }

    private void handleEntityHits(Vec3 start, Vec3 end) {
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        AABB sweep = new AABB(start, end).inflate(getThickness() * 0.5D);
        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, sweep, this::canHitEntity)) {
            if (damagedEntities.add(target.getUUID())) {
                applyDamage(target);
                pierceCount++;
                if (maxPierce > 0 && pierceCount >= maxPierce) {
                    discard();
                    break;
                }
            }
        }
    }

    private boolean canHitEntity(LivingEntity entity) {
        if (!entity.isAlive()) {
            return false;
        }
        LivingEntity owner = getOwner();
        if (owner != null) {
            if (entity == owner) {
                return false;
            }
            if (entity.isAlliedTo(owner)) {
                return false;
            }
        }
        return true;
    }

    private void applyDamage(LivingEntity target) {
        LivingEntity owner = getOwner();
        DamageSource source;
        if (owner instanceof Player player) {
            source = this.damageSources().playerAttack(player);
        } else if (owner != null) {
            source = this.damageSources().mobAttack(owner);
        } else {
            source = this.damageSources().magic();
        }
        float amount = (float) Math.max(0.0D, damage);
        if (amount <= 0.0F) {
            return;
        }
        boolean hurt = target.hurt(source, amount);
        if (hurt) {
            Vec3 push = direction.lengthSqr() > 1.0E-6 ? direction : this.getForward();
            if (push.lengthSqr() > 1.0E-6) {
                push = push.normalize();
                target.push(push.x * 0.5D, 0.1D + Math.abs(push.y) * 0.2D, push.z * 0.5D);
                target.hurtMarked = true;
            }
        }
    }

    private void handleBlockBreaks(Vec3 start, Vec3 end) {
        Level level = this.level();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        CCConfig.SwordSlashConfig config = config();
        if (!config.enableBlockBreaking) {
            if (ChestCavity.LOGGER.isDebugEnabled()) {
                ChestCavity.LOGGER.debug("[SwordSlash] Block breaking disabled in config");
            }
            return;
        }
        boolean ownerIsPlayer = getOwner() instanceof Player;
        boolean mobGriefAllowed = mobGriefingEnabled(server);
        if (!mobGriefAllowed && !ownerIsPlayer) {
            if (ChestCavity.LOGGER.isDebugEnabled()) {
                ChestCavity.LOGGER.debug("[SwordSlash] Block breaking blocked by mobGriefing gamerule");
            }
            return;
        }
        int cap = Math.max(0, config.blockBreakCapPerTick);
        if (cap == 0) {
            if (ChestCavity.LOGGER.isDebugEnabled()) {
                ChestCavity.LOGGER.debug("[SwordSlash] Block breaking cap is zero");
            }
            return;
        }
        AABB box = new AABB(start, end).inflate(getThickness() * 0.5D);
        if (ChestCavity.LOGGER.isDebugEnabled()) {
            ChestCavity.LOGGER.debug(
                    "[SwordSlash] Checking block sweep start={} end={} thickness={} breakPower={} cap={} mobGriefing={} ownerIsPlayer={}",
                    start,
                    end,
                    getThickness(),
                    breakPower,
                    cap,
                    mobGriefAllowed,
                    ownerIsPlayer
            );
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int broken = 0;
        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX);
        int maxY = Mth.floor(box.maxY);
        int maxZ = Mth.floor(box.maxZ);
        for (int x = minX; x <= maxX && broken < cap; x++) {
            for (int y = minY; y <= maxY && broken < cap; y++) {
                for (int z = minZ; z <= maxZ && broken < cap; z++) {
                    cursor.set(x, y, z);
                    BlockState state = server.getBlockState(cursor);
                    if (state.isAir()) {
                        debugSkipBlock(server, cursor, state, "air", breakPower);
                        continue;
                    }
                    if (!state.is(CCTags.Blocks.BREAKABLE_BY_SWORD_SLASH)) {
                        debugSkipBlock(server, cursor, state, "not_tagged", breakPower);
                        continue;
                    }
                    float hardness = state.getDestroySpeed(server, cursor);
                    if (hardness < 0.0F || hardness > breakPower) {
                        debugSkipBlock(server, cursor, state, "hardness=" + hardness, breakPower);
                        continue;
                    }
                    boolean destroyed = server.destroyBlock(cursor, true);
                    if (destroyed) {
                        broken++;
                    } else {
                        debugSkipBlock(server, cursor, state, "destroy_failed", breakPower);
                    }
                }
            }
        }
        if (ChestCavity.LOGGER.isDebugEnabled()) {
            ChestCavity.LOGGER.debug("[SwordSlash] Block sweep finished broken={} cap={} thickness={} breakPower={}",
                    broken,
                    cap,
                    getThickness(),
                    breakPower
            );
            if (broken >= cap) {
                ChestCavity.LOGGER.debug("[SwordSlash] Block break cap reached ({} per tick)", cap);
            }
        }
    }

    private void debugSkipBlock(ServerLevel level, BlockPos pos, BlockState state, String reason, double breakPower) {
        if (!ChestCavity.LOGGER.isDebugEnabled()) {
            return;
        }
        if (!(state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.LARGE_FERN))) {
            return;
        }
        double hardness = state.getDestroySpeed(level, pos);
        ChestCavity.LOGGER.debug(
                "[SwordSlash] Skipping block {} at {} reason={} hardness={} breakPower={}",
                BuiltInRegistries.BLOCK.getKey(state.getBlock()),
                pos,
                reason,
                hardness,
                breakPower
        );
    }

    private boolean mobGriefingEnabled(ServerLevel level) {
        return level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    private void spawnClientTrail() {
        if (!this.level().isClientSide) {
            return;
        }
        double length = getLength();
        for (int i = 0; i < 3; i++) {
            double t = this.random.nextDouble();
            Vec3 offset = direction.scale(length * t);
            double ox = this.getX() + offset.x;
            double oy = this.getY() + offset.y + 0.2D;
            double oz = this.getZ() + offset.z;
            this.level().addParticle(ParticleTypes.SWEEP_ATTACK, ox, oy, oz, 0.0, 0.0, 0.0);
        }
    }

    private void updateRotationFromDirection() {
        if (direction.lengthSqr() < 1.0E-6) {
            return;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(direction.x, direction.z));
        float pitch = (float) Math.toDegrees(Math.asin(Mth.clamp(direction.y, -1.0D, 1.0D)));
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yRotO = yaw;
        this.xRotO = pitch;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putInt("Age", age);
        tag.putInt("Lifespan", lifespan);
        tag.putDouble("Damage", damage);
        tag.putDouble("BreakPower", breakPower);
        tag.putInt("MaxPierce", maxPierce);
        tag.putDouble("SpeedPerTick", speedPerTick);
        tag.putDouble("DirX", direction.x);
        tag.putDouble("DirY", direction.y);
        tag.putDouble("DirZ", direction.z);
        tag.putBoolean("Configured", configured);
        tag.putDouble("Length", getLength());
        tag.putDouble("Thickness", getThickness());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            ownerId = tag.getUUID("Owner");
        }
        age = tag.getInt("Age");
        lifespan = Math.max(1, tag.getInt("Lifespan"));
        damage = tag.getDouble("Damage");
        breakPower = tag.getDouble("BreakPower");
        maxPierce = Math.max(0, tag.getInt("MaxPierce"));
        speedPerTick = tag.contains("SpeedPerTick") ? tag.getDouble("SpeedPerTick") : speedPerTick;
        double x = tag.getDouble("DirX");
        double y = tag.getDouble("DirY");
        double z = tag.getDouble("DirZ");
        direction = new Vec3(x, y, z);
        configured = tag.getBoolean("Configured");
        this.entityData.set(DATA_LIFESPAN, lifespan);
        this.entityData.set(DATA_DAMAGE, (float) damage);
        this.entityData.set(DATA_BREAK_POWER, (float) breakPower);
        this.entityData.set(DATA_MAX_PIERCE, maxPierce);
        if (tag.contains("Length")) {
            this.entityData.set(DATA_LENGTH, (float) tag.getDouble("Length"));
        }
        if (tag.contains("Thickness")) {
            this.entityData.set(DATA_THICKNESS, (float) tag.getDouble("Thickness"));
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float thickness = Math.max(0.2F, this.entityData.get(DATA_THICKNESS));
        return EntityDimensions.scalable(thickness, thickness);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, serverEntity);
    }

    @Override
    public void applyProjectileParameters(@Nullable LivingEntity owner, CompoundTag parameters, double baseDamage) {
        if (owner != null) {
            this.cachedOwner = owner;
            this.ownerId = owner.getUUID();
        }
        CompoundTag tag = parameters == null ? new CompoundTag() : parameters;
        CCConfig.SwordSlashConfig config = config();
        double length = readDouble(tag, "length", config.defaultLength);
        double thickness = readDouble(tag, "thickness", config.defaultThickness);
        this.lifespan = Math.max(1, (int) Math.round(readDouble(tag, "lifespan", config.defaultLifespanTicks)));
        this.damage = baseDamage;
        if (tag.contains("damage")) {
            this.damage = tag.getDouble("damage");
        }
        this.breakPower = readDouble(tag, "break_power", config.defaultBreakPower);
        this.maxPierce = Math.max(0, tag.contains("max_pierce") ? tag.getInt("max_pierce") : config.defaultMaxPierce);
        this.speedPerTick = Math.max(0.05D, length / (double) this.lifespan);
        this.direction = initialDirection(owner);
        this.age = 0;
        this.damagedEntities.clear();
        this.pierceCount = 0;
        this.entityData.set(DATA_LENGTH, (float) length);
        this.entityData.set(DATA_THICKNESS, (float) thickness);
        this.entityData.set(DATA_LIFESPAN, lifespan);
        this.entityData.set(DATA_DAMAGE, (float) damage);
        this.entityData.set(DATA_BREAK_POWER, (float) breakPower);
        this.entityData.set(DATA_MAX_PIERCE, maxPierce);
        this.refreshDimensions();
        this.setDeltaMovement(direction.scale(speedPerTick));
        updateRotationFromDirection();
        this.configured = true;
    }

    private double readDouble(CompoundTag tag, String key, double fallback) {
        if (tag.contains(key)) {
            return tag.getDouble(key);
        }
        return fallback;
    }

    private Vec3 initialDirection(@Nullable LivingEntity owner) {
        Vec3 forward = this.getForward();
        if (forward.lengthSqr() < 1.0E-4 && owner != null) {
            forward = owner.getLookAngle();
        }
        if (forward.lengthSqr() < 1.0E-4) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        return forward.normalize();
    }

    public double getLength() {
        return Math.max(0.1D, this.entityData.get(DATA_LENGTH));
    }

    public double getThickness() {
        return Math.max(0.1D, this.entityData.get(DATA_THICKNESS));
    }

    @Nullable
    public LivingEntity getOwner() {
        if (cachedOwner != null && cachedOwner.isAlive()) {
            return cachedOwner;
        }
        if (ownerId != null && this.level() instanceof ServerLevel server) {
            Entity entity = server.getEntity(ownerId);
            if (entity instanceof LivingEntity living) {
                cachedOwner = living;
                return living;
            }
        }
        return null;
    }

    private static CCConfig.SwordSlashConfig config() {
        if (ChestCavity.config != null && ChestCavity.config.SWORD_SLASH != null) {
            return ChestCavity.config.SWORD_SLASH;
        }
        return DEFAULTS;
    }
}
