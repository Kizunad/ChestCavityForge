package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.guscript.runtime.exec.ProjectileEmission;
import net.tigereye.chestcavity.registration.CCTags;

import java.util.Objects;

/**
 * Data-driven Jian Dao sword slash projectile. Handles sweeping melee damage and fragile block
 * destruction on the server; visuals come from GuScript FX definitions.
 */
public class SwordSlashProjectile extends Projectile {

    private static final EntityDataAccessor<Float> DATA_LENGTH =
            SynchedEntityData.defineId(SwordSlashProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_THICKNESS =
            SynchedEntityData.defineId(SwordSlashProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFESPAN =
            SynchedEntityData.defineId(SwordSlashProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(SwordSlashProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_MAX_PIERCE =
            SynchedEntityData.defineId(SwordSlashProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_BREAK_POWER =
            SynchedEntityData.defineId(SwordSlashProjectile.class, EntityDataSerializers.FLOAT);

    private static final int FALLBACK_LIFESPAN_TICKS = 16;
    private static final double FALLBACK_LENGTH = 6.5D;
    private static final double FALLBACK_THICKNESS = 1.1D;
    private static final double FALLBACK_DAMAGE = 10.0D;
    private static final int FALLBACK_MAX_PIERCE = 4;
    private static final double FALLBACK_BREAK_POWER = 2.5D;
    private static final int FALLBACK_BLOCKS_PER_TICK = 8;

    private final IntSet hitEntityIds = new IntOpenHashSet();
    private int ticksExisted;
    private int pierceCount;

    public SwordSlashProjectile(EntityType<? extends SwordSlashProjectile> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LENGTH, (float) defaultLength());
        builder.define(DATA_THICKNESS, (float) defaultThickness());
        builder.define(DATA_LIFESPAN, defaultLifespanTicks());
        builder.define(DATA_DAMAGE, (float) defaultDamage());
        builder.define(DATA_MAX_PIERCE, defaultMaxPierce());
        builder.define(DATA_BREAK_POWER, (float) defaultBreakPower());
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 direction = forwardDirection();
        Vec3 current = this.position();
        updateRotation(direction);

        if (this.level().isClientSide) {
            spawnClientParticles(direction);
            return;
        }

        sweepForEntities(current, direction);
        breakBlocks(current, direction);

        this.ticksExisted++;
        if (this.ticksExisted >= getLifespanTicks()) {
            this.discard();
        }
    }

    @Override
    protected void onHit(net.minecraft.world.phys.HitResult result) {
        // Ignore vanilla collision handling; sweeping logic governs lifetime and contact.
    }

    public void configureFromEmission(ProjectileEmission emission) {
        Objects.requireNonNull(emission, "emission");
        setBaseDamage(emission.damage());
        if (emission.length() != null) {
            setLength(Math.max(0.5D, emission.length()));
        }
        if (emission.thickness() != null) {
            setThickness(Math.max(0.25D, emission.thickness()));
        }
        if (emission.lifespanTicks() != null) {
            setLifespan(Math.max(1, emission.lifespanTicks()));
        }
        if (emission.maxPierce() != null) {
            setMaxPierce(Math.max(0, emission.maxPierce()));
        }
        if (emission.breakPower() != null) {
            setBreakPower(Math.max(0.0D, emission.breakPower()));
        }
    }

    public double getLength() {
        return Math.max(0.25D, this.entityData.get(DATA_LENGTH));
    }

    public void setLength(double length) {
        this.entityData.set(DATA_LENGTH, (float) length);
    }

    public double getThickness() {
        return Math.max(0.1D, this.entityData.get(DATA_THICKNESS));
    }

    public void setThickness(double thickness) {
        this.entityData.set(DATA_THICKNESS, (float) thickness);
        this.refreshDimensions();
    }

    public int getLifespanTicks() {
        return Math.max(1, this.entityData.get(DATA_LIFESPAN));
    }

    public void setLifespan(int lifespan) {
        this.entityData.set(DATA_LIFESPAN, Math.max(1, lifespan));
    }

    public double getBaseDamage() {
        return Math.max(0.0D, this.entityData.get(DATA_DAMAGE));
    }

    public void setBaseDamage(double damage) {
        this.entityData.set(DATA_DAMAGE, (float) Math.max(0.0D, damage));
    }

    public int getMaxPierce() {
        return Math.max(0, this.entityData.get(DATA_MAX_PIERCE));
    }

    public void setMaxPierce(int maxPierce) {
        this.entityData.set(DATA_MAX_PIERCE, Math.max(0, maxPierce));
    }

    public double getBreakPower() {
        return Math.max(0.0D, this.entityData.get(DATA_BREAK_POWER));
    }

    public void setBreakPower(double breakPower) {
        this.entityData.set(DATA_BREAK_POWER, (float) Math.max(0.0D, breakPower));
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float diameter = (float) Math.max(0.2D, getThickness());
        return EntityDimensions.scalable(diameter, diameter);
    }

    private void sweepForEntities(Vec3 head, Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-6) {
            return;
        }
        double length = getLength();
        double thickness = getThickness();
        Vec3 tail = head.subtract(direction.scale(length));
        AABB sweepBounds = new AABB(head, tail).inflate(thickness, thickness * 0.8D, thickness);

        Entity owner = this.getOwner();
        for (Entity entity : this.level().getEntities(this, sweepBounds, e -> e.isAlive() && e != this)) {
            if (entity == owner) {
                continue;
            }
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (hitEntityIds.contains(entity.getId())) {
                continue;
            }
            if (!isWithinSlash(living, head, tail, direction, thickness)) {
                continue;
            }
            DamageSource source = resolveDamageSource(living);
            boolean damaged = living.hurt(source, (float) getBaseDamage());
            if (damaged) {
                living.push(direction.x * 0.45D, Math.max(0.1D, direction.y * 0.2D), direction.z * 0.45D);
                hitEntityIds.add(entity.getId());
                pierceCount++;
                int maxPierce = getMaxPierce();
                if (maxPierce > 0 && pierceCount >= maxPierce) {
                    this.discard();
                    return;
                }
            }
        }
    }

    private boolean isWithinSlash(LivingEntity entity, Vec3 head, Vec3 tail, Vec3 direction, double thickness) {
        Vec3 center = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        Vec3 closest = closestPointOnSegment(center, tail, head);
        double distanceSq = closest.distanceToSqr(center);
        double limit = thickness * thickness;
        return distanceSq <= limit + 1.0E-4;
    }

    private Vec3 closestPointOnSegment(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        double lengthSq = delta.lengthSqr();
        if (lengthSq < 1.0E-6) {
            return start;
        }
        double t = (point.subtract(start)).dot(delta) / lengthSq;
        t = Mth.clamp(t, 0.0D, 1.0D);
        return start.add(delta.scale(t));
    }

    private DamageSource resolveDamageSource(LivingEntity target) {
        Entity owner = this.getOwner();
        if (owner instanceof Player player) {
            return target.damageSources().playerAttack(player);
        }
        if (owner instanceof LivingEntity living) {
            return target.damageSources().mobAttack(living);
        }
        return target.damageSources().magic();
    }

    private void breakBlocks(Vec3 head, Vec3 direction) {
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        if (!shouldBreakBlocks(server)) {
            return;
        }
        double breakPower = getBreakPower();
        if (breakPower <= 0.0D) {
            return;
        }
        double thickness = getThickness();
        Vec3 tail = head.subtract(direction.scale(getLength()));
        AABB sweep = new AABB(head, tail).inflate(thickness);
        int minX = Mth.floor(sweep.minX);
        int minY = Mth.floor(sweep.minY);
        int minZ = Mth.floor(sweep.minZ);
        int maxX = Mth.floor(sweep.maxX);
        int maxY = Mth.floor(sweep.maxY);
        int maxZ = Mth.floor(sweep.maxZ);

        int broken = 0;
        int blockLimit = blockBreakLimit();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (broken >= blockLimit) {
                        return;
                    }
                    cursor.set(x, y, z);
                    if (!server.isLoaded(cursor)) {
                        continue;
                    }
                    BlockState state = server.getBlockState(cursor);
                    if (state.isAir() || !state.is(CCTags.SWORD_SLASH_BREAKABLE)) {
                        continue;
                    }
                    float hardness = state.getDestroySpeed(server, cursor);
                    if (hardness < 0.0F || hardness > breakPower) {
                        continue;
                    }
                    Vec3 blockCenter = Vec3.atCenterOf(cursor);
                    if (closestPointOnSegment(blockCenter, tail, head).distanceToSqr(blockCenter) > thickness * thickness + 1.0E-4) {
                        continue;
                    }
                    if (server.destroyBlock(cursor, true)) {
                        broken++;
                    }
                }
            }
        }
    }

    private boolean shouldBreakBlocks(ServerLevel server) {
        CCConfig.GuScriptExecutionConfig.SwordSlashConfig config = swordSlashConfig();
        if (config != null && !config.enableBlockBreaking) {
            return false;
        }
        if (!server.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            Entity owner = this.getOwner();
            if (!(owner instanceof Player)) {
                return false;
            }
        }
        return true;
    }

    private void spawnClientParticles(Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-6) {
            return;
        }
        Level level = this.level();
        double length = getLength();
        int segments = Math.max(3, Mth.ceil(length));
        for (int i = 0; i < segments; i++) {
            double t = i / (double) segments;
            Vec3 point = this.position().subtract(direction.scale(t * length));
            level.addParticle(ParticleTypes.SWEEP_ATTACK, point.x, point.y + 0.1D, point.z, 0.0D, 0.0D, 0.0D);
        }
    }

    private Vec3 forwardDirection() {
        Vec3 movement = this.getDeltaMovement();
        if (movement.lengthSqr() > 1.0E-6) {
            return movement.normalize();
        }
        Vec3 look = Vec3.directionFromRotation(this.getXRot(), this.getYRot());
        if (look.lengthSqr() > 1.0E-6) {
            return look.normalize();
        }
        return new Vec3(0.0D, 0.0D, 1.0D);
    }

    private void updateRotation(Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-6) {
            return;
        }
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        this.setYRot((float) (Math.atan2(direction.x, direction.z) * (180.0F / Math.PI)));
        this.setXRot((float) (Math.atan2(direction.y, horizontal) * (180.0F / Math.PI)));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Length", (float) getLength());
        tag.putFloat("Thickness", (float) getThickness());
        tag.putInt("Lifespan", getLifespanTicks());
        tag.putDouble("Damage", getBaseDamage());
        tag.putInt("MaxPierce", getMaxPierce());
        tag.putDouble("BreakPower", getBreakPower());
        tag.putInt("PierceCount", pierceCount);
        tag.putInt("TicksExisted", ticksExisted);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Length")) {
            setLength(tag.getFloat("Length"));
        }
        if (tag.contains("Thickness")) {
            setThickness(tag.getFloat("Thickness"));
        }
        if (tag.contains("Lifespan")) {
            setLifespan(tag.getInt("Lifespan"));
        }
        if (tag.contains("Damage")) {
            setBaseDamage(tag.getDouble("Damage"));
        }
        if (tag.contains("MaxPierce")) {
            setMaxPierce(tag.getInt("MaxPierce"));
        }
        if (tag.contains("BreakPower")) {
            setBreakPower(tag.getDouble("BreakPower"));
        }
        this.pierceCount = tag.getInt("PierceCount");
        this.ticksExisted = tag.getInt("TicksExisted");
    }

    private static CCConfig.GuScriptExecutionConfig.SwordSlashConfig swordSlashConfig() {
        if (ChestCavity.config == null || ChestCavity.config.GUSCRIPT_EXECUTION == null) {
            return null;
        }
        return ChestCavity.config.GUSCRIPT_EXECUTION.swordSlash;
    }

    private static double defaultLength() {
        CCConfig.GuScriptExecutionConfig.SwordSlashConfig config = swordSlashConfig();
        if (config != null && config.defaultLength > 0.0D) {
            return config.defaultLength;
        }
        return FALLBACK_LENGTH;
    }

    private static double defaultThickness() {
        CCConfig.GuScriptExecutionConfig.SwordSlashConfig config = swordSlashConfig();
        if (config != null && config.defaultThickness > 0.0D) {
            return config.defaultThickness;
        }
        return FALLBACK_THICKNESS;
    }

    private static int defaultLifespanTicks() {
        CCConfig.GuScriptExecutionConfig.SwordSlashConfig config = swordSlashConfig();
        if (config != null && config.defaultLifespanTicks > 0) {
            return config.defaultLifespanTicks;
        }
        return FALLBACK_LIFESPAN_TICKS;
    }

    private static double defaultDamage() {
        CCConfig.GuScriptExecutionConfig.SwordSlashConfig config = swordSlashConfig();
        if (config != null && config.defaultDamage > 0.0D) {
            return config.defaultDamage;
        }
        return FALLBACK_DAMAGE;
    }

    private static int defaultMaxPierce() {
        CCConfig.GuScriptExecutionConfig.SwordSlashConfig config = swordSlashConfig();
        if (config != null && config.defaultMaxPierce >= 0) {
            return config.defaultMaxPierce;
        }
        return FALLBACK_MAX_PIERCE;
    }

    private static double defaultBreakPower() {
        CCConfig.GuScriptExecutionConfig.SwordSlashConfig config = swordSlashConfig();
        if (config != null && config.defaultBreakPower >= 0.0D) {
            return config.defaultBreakPower;
        }
        return FALLBACK_BREAK_POWER;
    }

    private static int blockBreakLimit() {
        CCConfig.GuScriptExecutionConfig.SwordSlashConfig config = swordSlashConfig();
        if (config != null && config.maxBlocksBrokenPerTick > 0) {
            return config.maxBlocksBrokenPerTick;
        }
        return FALLBACK_BLOCKS_PER_TICK;
    }
}
