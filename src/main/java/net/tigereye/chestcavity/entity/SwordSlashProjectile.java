package net.tigereye.chestcavity.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.util.ProjectileParameterReceiver;
import net.tigereye.chestcavity.util.AnimationPathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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
    private static final ResourceLocation SLASH_ANIMATION_FILE = ChestCavity.id("animations/vanquisher_sword_x.animation.json");
    private static final String SLASH_ANIMATION_NAME = "animation.chestcavity.vanquisher_sword.slash_two";
    private static final String SLASH_BONE_NAME = "bone";
    private static final double POSITION_UNIT_SCALE = 1.0 / 16.0;
    private static final java.util.concurrent.atomic.AtomicBoolean TAG_DIAGNOSTICS_LOGGED = new java.util.concurrent.atomic.AtomicBoolean();
    private static final java.util.Set<Block> BREAKABLE_BLOCKS = java.util.Set.of(
            Blocks.GRASS_BLOCK,
            Blocks.DIRT,
            Blocks.COARSE_DIRT,
            Blocks.PODZOL,
            Blocks.SAND,
            Blocks.RED_SAND,
            Blocks.GLASS,
            Blocks.GLASS_PANE,
            Blocks.WHITE_STAINED_GLASS,
            Blocks.WHITE_STAINED_GLASS_PANE,
            Blocks.ICE,
            Blocks.PACKED_ICE,
            Blocks.SNOW_BLOCK,
            Blocks.MELON,
            Blocks.PUMPKIN,
            Blocks.CARVED_PUMPKIN,
            Blocks.JACK_O_LANTERN,
            Blocks.HONEY_BLOCK,
            Blocks.SCAFFOLDING,
            Blocks.BOOKSHELF
    );

    private int lifespan;
    private int age;
    private double damage;
    private double breakPower;
    private int maxPierce;
    private Vec3 direction = Vec3.ZERO;
    private UUID ownerId;
    private LivingEntity cachedOwner;
    private final Set<UUID> damagedEntities = new HashSet<>();
    private int pierceCount;
    private boolean configured;

    private List<AnimationPathHelper.AnimationKeyframe> positionKeyframes = new ArrayList<>();
    private double animationDuration;
    private double animationTime;
    private Vec3 baseOrigin = Vec3.ZERO;
    private Vec3 basisRight = new Vec3(1.0D, 0.0D, 0.0D);
    private Vec3 basisUp = new Vec3(0.0D, 1.0D, 0.0D);
    private Vec3 basisForward = new Vec3(0.0D, 0.0D, 1.0D);
    private boolean pathActive;

    public SwordSlashProjectile(EntityType<? extends SwordSlashProjectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.lifespan = DEFAULTS.defaultLifespanTicks;
        this.damage = DEFAULTS.defaultDamage;
        this.breakPower = DEFAULTS.defaultBreakPower;
        this.maxPierce = DEFAULTS.defaultMaxPierce;
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
        ensurePathAvailable();

        double step = 1.0D / 20.0D;
        double nextTime = Math.min(animationTime + step, animationDuration);
        Vec3 currentPos = this.position();
        Vec3 targetPos = baseOrigin.add(convertRelativeToWorld(sampleRelativePosition(nextTime)));
        Vec3 motion = targetPos.subtract(currentPos);

        if (!this.level().isClientSide) {
            handleEntityHits(currentPos, targetPos);
            handleBlockBreaks(currentPos, targetPos);
        } else {
            spawnClientTrail();
        }

        this.move(MoverType.SELF, motion);
        if (motion.lengthSqr() > 1.0E-6) {
            direction = motion.normalize();
            this.setDeltaMovement(Vec3.ZERO);
            updateRotationFromDirection();
        }

        animationTime = nextTime;
        age++;
        if (animationTime >= animationDuration - 1.0E-4 || age >= lifespan) {
            if (!this.level().isClientSide) {
                discard();
            }
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
        if (isDebugLoggingEnabled()) {
            logBreakableDiagnostics(server);
        }
        if (!config.enableBlockBreaking) {
            if (isDebugLoggingEnabled()) {
                logDebug("[SwordSlash] Block breaking disabled in config");
            }
            return;
        }
        boolean ownerIsPlayer = getOwner() instanceof Player;
        boolean mobGriefAllowed = mobGriefingEnabled(server);
        if (!mobGriefAllowed && !ownerIsPlayer) {
            if (isDebugLoggingEnabled()) {
                logDebug("[SwordSlash] Block breaking blocked by mobGriefing gamerule");
            }
            return;
        }
        int cap = Math.max(0, config.blockBreakCapPerTick);
        if (cap == 0) {
            if (isDebugLoggingEnabled()) {
                logDebug("[SwordSlash] Block breaking cap is zero");
            }
            return;
        }
        AABB box = new AABB(start, end).inflate(getThickness() * 0.5D);
        if (isDebugLoggingEnabled()) {
            logDebug(
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
                    if (!isBreakableBlock(state)) {
                        debugSkipBlock(server, cursor, state, "not_breakable", breakPower);
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
        if (isDebugLoggingEnabled()) {
            logDebug("[SwordSlash] Block sweep finished broken={} cap={} thickness={} breakPower={}",
                    broken,
                    cap,
                    getThickness(),
                    breakPower
            );
            if (broken >= cap) {
                logDebug("[SwordSlash] Block break cap reached ({} per tick)", cap);
            }
        }
    }

    private void logBreakableDiagnostics(ServerLevel server) {
        if (!TAG_DIAGNOSTICS_LOGGED.compareAndSet(false, true)) {
            return;
        }
        long total = 0L;
        long matches = 0L;
        for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            total++;
            if (BREAKABLE_BLOCKS.contains(block)) {
                matches++;
            }
        }
        logDebug("[SwordSlash] Java breakable list size={}/{} blocks", matches, total);
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState sand = Blocks.SAND.defaultBlockState();
        logDebug("[SwordSlash] Breakable check grass_block={} dirt={} sand={}",
                isBreakableBlock(grass),
                isBreakableBlock(dirt),
                isBreakableBlock(sand));
    }

    private void logDebug(String message, Object... args) {
        if (isDebugLoggingEnabled()) {
            ChestCavity.LOGGER.info(message, args);
        }
    }

    private boolean isDebugLoggingEnabled() {
        return true;
        /* 
        if (ChestCavity.config != null && ChestCavity.config.SWORD_SLASH != null) {
            return ChestCavity.config.SWORD_SLASH.debugLogging;
        }
        return false;*/
    }

    private void debugSkipBlock(ServerLevel level, BlockPos pos, BlockState state, String reason, double breakPower) {
        if (!isDebugLoggingEnabled()) {
            return;
        }
        if (!(isBreakableBlock(state) || state.is(Blocks.LARGE_FERN))) {
            return;
        }
        double hardness = state.getDestroySpeed(level, pos);
        logDebug(
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
        tag.putDouble("DirX", direction.x);
        tag.putDouble("DirY", direction.y);
        tag.putDouble("DirZ", direction.z);
        tag.putBoolean("Configured", configured);
        tag.putDouble("Length", getLength());
        tag.putDouble("Thickness", getThickness());
        tag.putDouble("AnimTime", animationTime);
        tag.putDouble("AnimDuration", animationDuration);
        tag.putDouble("BaseOriginX", baseOrigin.x);
        tag.putDouble("BaseOriginY", baseOrigin.y);
        tag.putDouble("BaseOriginZ", baseOrigin.z);
        tag.putDouble("BasisRightX", basisRight.x);
        tag.putDouble("BasisRightY", basisRight.y);
        tag.putDouble("BasisRightZ", basisRight.z);
        tag.putDouble("BasisUpX", basisUp.x);
        tag.putDouble("BasisUpY", basisUp.y);
        tag.putDouble("BasisUpZ", basisUp.z);
        tag.putDouble("BasisForwardX", basisForward.x);
        tag.putDouble("BasisForwardY", basisForward.y);
        tag.putDouble("BasisForwardZ", basisForward.z);
        tag.putBoolean("PathActive", pathActive);
        ListTag list = new ListTag();
        for (AnimationPathHelper.AnimationKeyframe frame : positionKeyframes) {
            CompoundTag frameTag = new CompoundTag();
            frameTag.putDouble("t", frame.time());
            frameTag.putDouble("x", frame.position().x);
            frameTag.putDouble("y", frame.position().y);
            frameTag.putDouble("z", frame.position().z);
            list.add(frameTag);
        }
        tag.put("AnimPath", list);
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
        direction = new Vec3(tag.getDouble("DirX"), tag.getDouble("DirY"), tag.getDouble("DirZ"));
        configured = tag.getBoolean("Configured");
        animationTime = tag.getDouble("AnimTime");
        animationDuration = tag.getDouble("AnimDuration");
        baseOrigin = new Vec3(tag.getDouble("BaseOriginX"), tag.getDouble("BaseOriginY"), tag.getDouble("BaseOriginZ"));
        basisRight = new Vec3(tag.getDouble("BasisRightX"), tag.getDouble("BasisRightY"), tag.getDouble("BasisRightZ"));
        basisUp = new Vec3(tag.getDouble("BasisUpX"), tag.getDouble("BasisUpY"), tag.getDouble("BasisUpZ"));
        basisForward = new Vec3(tag.getDouble("BasisForwardX"), tag.getDouble("BasisForwardY"), tag.getDouble("BasisForwardZ"));
        pathActive = tag.getBoolean("PathActive");
        positionKeyframes = new ArrayList<>();
        if (tag.contains("AnimPath", Tag.TAG_LIST)) {
            ListTag list = tag.getList("AnimPath", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag frameTag = list.getCompound(i);
                double t = frameTag.getDouble("t");
                Vec3 pos = new Vec3(frameTag.getDouble("x"), frameTag.getDouble("y"), frameTag.getDouble("z"));
                positionKeyframes.add(new AnimationPathHelper.AnimationKeyframe(t, pos));
            }
        }
        positionKeyframes.sort(Comparator.comparingDouble(AnimationPathHelper.AnimationKeyframe::time));
        if (positionKeyframes.isEmpty()) {
            ensurePathAvailable();
            animationDuration = positionKeyframes.get(positionKeyframes.size() - 1).time();
        } else if (animationDuration <= 0.0D) {
            animationDuration = positionKeyframes.get(positionKeyframes.size() - 1).time();
        }
        pathActive = pathActive || !positionKeyframes.isEmpty();
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
        this.damage = tag.contains("damage") ? tag.getDouble("damage") : baseDamage;
        this.breakPower = readDouble(tag, "break_power", config.defaultBreakPower);
        this.maxPierce = Math.max(0, tag.contains("max_pierce") ? tag.getInt("max_pierce") : config.defaultMaxPierce);

        this.age = 0;
        this.damagedEntities.clear();
        this.pierceCount = 0;

        setupAnimationBasis(owner);
        loadAnimationPath(owner);
        if (animationDuration > 0.0D) {
            int requiredTicks = (int) Math.ceil(animationDuration * 20.0D);
            this.lifespan = Math.max(this.lifespan, requiredTicks);
        }

        this.entityData.set(DATA_LENGTH, (float) length);
        this.entityData.set(DATA_THICKNESS, (float) thickness);
        this.entityData.set(DATA_LIFESPAN, lifespan);
        this.entityData.set(DATA_DAMAGE, (float) damage);
        this.entityData.set(DATA_BREAK_POWER, (float) breakPower);
        this.entityData.set(DATA_MAX_PIERCE, maxPierce);
        this.refreshDimensions();

        Vec3 initialRelative = sampleRelativePosition(0.0D);
        Vec3 initialWorld = baseOrigin.add(convertRelativeToWorld(initialRelative));
        this.setPos(initialWorld.x, initialWorld.y, initialWorld.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.direction = basisForward;
        updateRotationFromDirection();
        this.configured = true;
    }

    private double readDouble(CompoundTag tag, String key, double fallback) {
        if (tag.contains(key)) {
            return tag.getDouble(key);
        }
        return fallback;
    }

    private void setupAnimationBasis(@Nullable LivingEntity owner) {
        Vec3 forward = owner != null ? owner.getLookAngle().normalize() : this.getForward();
        if (forward.lengthSqr() < 1.0E-6) {
            forward = this.getViewVector(1.0F).normalize();
        }
        if (forward.lengthSqr() < 1.0E-6) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 upHint = new Vec3(0.0D, 1.0D, 0.0D);
        if (Math.abs(forward.dot(upHint)) > 0.98D) {
            upHint = owner != null ? owner.getUpVector(1.0F) : new Vec3(0.0D, 1.0D, 0.0D);
        }
        Vec3 right = forward.cross(upHint);
        if (right.lengthSqr() < 1.0E-6) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        right = right.normalize();
        Vec3 up = right.cross(forward).normalize();
        this.basisForward = forward;
        this.basisRight = right;
        this.basisUp = up;
        if (owner != null) {
            this.baseOrigin = owner.position();
        } else {
            this.baseOrigin = this.position();
        }
    }

    private void loadAnimationPath(@Nullable LivingEntity owner) {
        this.positionKeyframes = new ArrayList<>();
        this.animationDuration = 0.0D;
        this.animationTime = 0.0D;
        this.pathActive = false;
        if (owner != null && owner.level() instanceof ServerLevel ownerServer) {
            var manager = ownerServer.getServer().getServerResources().resourceManager();
            var frames = AnimationPathHelper.loadPositionKeyframes(manager, SLASH_ANIMATION_FILE, SLASH_ANIMATION_NAME, SLASH_BONE_NAME);
            if (!frames.isEmpty()) {
                this.positionKeyframes = new ArrayList<>(frames);
                this.pathActive = true;
            }
        } else if (this.level() instanceof ServerLevel serverLevel) {
            var manager = serverLevel.getServer().getServerResources().resourceManager();
            var frames = AnimationPathHelper.loadPositionKeyframes(manager, SLASH_ANIMATION_FILE, SLASH_ANIMATION_NAME, SLASH_BONE_NAME);
            if (!frames.isEmpty()) {
                this.positionKeyframes = new ArrayList<>(frames);
                this.pathActive = true;
            }
        }
        ensurePathAvailable();
        this.animationDuration = this.positionKeyframes.get(this.positionKeyframes.size() - 1).time();
        this.animationTime = 0.0D;
    }

    private void ensurePathAvailable() {
        if (this.pathActive && !this.positionKeyframes.isEmpty()) {
            return;
        }
        if (this.positionKeyframes.isEmpty()) {
            this.positionKeyframes = new ArrayList<>();
            this.positionKeyframes.add(new AnimationPathHelper.AnimationKeyframe(0.0D, Vec3.ZERO));
            this.positionKeyframes.add(new AnimationPathHelper.AnimationKeyframe(1.0D, new Vec3(0.0D, 0.0D, 16.0D)));
        }
        this.positionKeyframes.sort(Comparator.comparingDouble(AnimationPathHelper.AnimationKeyframe::time));
        this.animationDuration = this.positionKeyframes.get(this.positionKeyframes.size() - 1).time();
        if (this.baseOrigin.equals(Vec3.ZERO)) {
            this.baseOrigin = this.position();
        }
        this.pathActive = true;
    }

    private Vec3 sampleRelativePosition(double time) {
        if (this.positionKeyframes.isEmpty()) {
            return Vec3.ZERO;
        }
        if (time <= this.positionKeyframes.get(0).time()) {
            return this.positionKeyframes.get(0).position();
        }
        for (int i = 1; i < this.positionKeyframes.size(); i++) {
            AnimationPathHelper.AnimationKeyframe next = this.positionKeyframes.get(i);
            AnimationPathHelper.AnimationKeyframe prev = this.positionKeyframes.get(i - 1);
            if (time <= next.time()) {
                double span = next.time() - prev.time();
                double alpha = span <= 0.0D ? 0.0D : (time - prev.time()) / span;
                return prev.position().lerp(next.position(), alpha);
            }
        }
        return this.positionKeyframes.get(this.positionKeyframes.size() - 1).position();
    }

    private boolean isBreakableBlock(BlockState state) {
        if (BREAKABLE_BLOCKS.contains(state.getBlock())) {
            return true;
        }
        return state.is(BlockTags.LEAVES)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SMALL_FLOWERS)
                || state.is(BlockTags.TALL_FLOWERS)
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.WOOL)
                || state.is(BlockTags.MINEABLE_WITH_AXE)
                || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
    }

    private Vec3 convertRelativeToWorld(Vec3 relative) {
        double scale = POSITION_UNIT_SCALE;
        Vec3 right = this.basisRight.scale(relative.x * scale);
        Vec3 up = this.basisUp.scale(relative.y * scale);
        Vec3 forward = this.basisForward.scale(relative.z * scale);
        return right.add(up).add(forward);
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
