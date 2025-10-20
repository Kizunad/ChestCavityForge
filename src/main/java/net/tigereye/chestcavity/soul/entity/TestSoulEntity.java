package net.tigereye.chestcavity.soul.entity;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.tigereye.chestcavity.soul.util.ChestCavityInsertOps;
import net.tigereye.chestcavity.soul.util.EntityChunkLoadOps;
import net.tigereye.chestcavity.soul.util.EntityDensityOps;
import net.tigereye.chestcavity.soul.util.ItemAbsorptionOps;
import net.tigereye.chestcavity.soul.util.ItemFilterOps;
import net.tigereye.chestcavity.soul.entity.goal.DynamicGoalDispatcher;
import net.tigereye.chestcavity.soul.util.SurfaceTeleportOps;
import net.tigereye.chestcavity.soul.fakeplayer.brain.personality.SoulPersonality;
import net.tigereye.chestcavity.soul.fakeplayer.brain.personality.SoulPersonalityRegistry;
import net.tigereye.chestcavity.registration.CCItems;

/**
 * 自定义灵魂实体“Test”，具备谨慎性格与独特拾取/攻击逻辑。
 */
public class TestSoulEntity extends PathfinderMob {

    private static final double ITEM_RADIUS = 4.0;
    private static final double ITEM_PULL = 0.35;
    private static final int CHUNK_RADIUS = 2;
    private static final Component DEFAULT_NAME = Component.literal("月道式微");
    private final SoulPersonality personality = SoulPersonalityRegistry.resolve(SoulPersonalityRegistry.CAUTIOUS_ID);
    private SoulChunkLoaderEntity chunkLoader;
    private boolean registered;
    private LivingEntity recentAggressor;
    private int reactiveTicks;
    private ReactiveMode reactiveMode = ReactiveMode.NONE;

    public TestSoulEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 0;
        applyDefaultNameIfNeeded();
    }

    @Override
    protected ResourceKey<LootTable> getDefaultLootTable() {
        // 显式绑定 TestSoulEntity 的 LootTable，避免加载器未按约定路径解析导致无掉落
        return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse("chestcavity:entities/test_soul"));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        Goal reactive = new ReactiveDefenseGoal(this);
        Goal melee = new MeleeAttackGoal(this, 1.0, false);
        Goal wander = new TestWanderGoal(this, 1.0);
        this.goalSelector.addGoal(1, DynamicGoalDispatcher.<TestSoulEntity>builder(this)
                .add("reactive", entity -> entity.reactiveMode != ReactiveMode.NONE, reactive)
                .add("attack", entity -> entity.getTarget() != null, melee)
                .add("wander", wander)
                .build());
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Player.class, 32.0f, 1.2, 1.4,
                player -> player.getHealth() > this.getHealth() * 0.8f));

        this.targetSelector.addGoal(1, new HuntLivingGoal(this, LivingEntity.class, 32.0, 0.5f));
        this.targetSelector.addGoal(2, new HuntPlayerGoal(this, 32.0, 0.7f));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (!registered) {
                TestSoulManager.register(this);
                registered = true;
            }
            chunkLoader = EntityChunkLoadOps.ensureChunkLoader(this, chunkLoader, CHUNK_RADIUS);
            ItemAbsorptionOps.pullAndProcess(this, ITEM_RADIUS, ITEM_PULL,
                    item -> true,
                    this::handleItemPickup);
            if (tickCount % 40 == 0) {
                double surface = level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        this.blockPosition().getX(), this.blockPosition().getZ());
                if (this.getY() < surface) {
                    SurfaceTeleportOps.tryTeleportToSurface(this, 0.02, 32.0);
                }
            }
            if (reactiveTicks > 0) {
                reactiveTicks--;
                if (reactiveTicks <= 0 || recentAggressor == null || !recentAggressor.isAlive()) {
                    clearReactiveState();
                }
            }
        }
    }

    private void clearReactiveState() {
        reactiveMode = ReactiveMode.NONE;
        recentAggressor = null;
        reactiveTicks = 0;
    }

    @Override
    protected void dropEquipment() {
        // 不掉落装备
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    private void handleItemPickup(ItemEntity item) {
        if (!item.isAlive()) {
            return;
        }
        ItemStack stack = item.getItem();
        if (stack.isEmpty()) {
            item.discard();
            return;
        }
        if (ItemFilterOps.isBlockDrop(stack)) {
            item.discard();
            return;
        }
        ItemStack remaining = ChestCavityInsertOps.tryInsert(this, stack);
        if (remaining.isEmpty()) {
            item.playSound(SoundEvents.ITEM_PICKUP, 0.35f, 1.2f);
            item.discard();
        } else if (remaining.getCount() != stack.getCount()) {
            item.setItem(remaining);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean base = super.doHurtTarget(target);
        if (base && target instanceof LivingEntity living) {
            float extra = this.getHealth() * 0.1f;
            if (extra > 0f) {
                living.hurt(this.damageSources().mobAttack(this), extra);
            }
        }
        return base;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && !level().isClientSide) {
            LivingEntity aggressor = resolveAggressor(source);
            if (aggressor != null && aggressor.isAlive() && aggressor != this) {
                this.recentAggressor = aggressor;
                this.reactiveTicks = 5;
                double selfHealth = this.getHealth();
                double aggressorHealth = aggressor.getHealth();
                double ratio = selfHealth <= 0 ? Double.POSITIVE_INFINITY : aggressorHealth / selfHealth;
                if (ratio >= 0.9) {
                    this.reactiveMode = ReactiveMode.EVADE;
                } else if (ratio <= 0.6) {
                    this.reactiveMode = ReactiveMode.RETALIATE;
                } else {
                    this.reactiveMode = this.getRandom().nextDouble() < 0.5 ? ReactiveMode.RETALIATE : ReactiveMode.EVADE;
                }
                if (this.reactiveMode == ReactiveMode.RETALIATE) {
                    this.setTarget(aggressor);
                } else {
                    this.setTarget(null);
                }
            }
        }
        return result;
    }

    private LivingEntity resolveAggressor(DamageSource source) {
        Entity responsible = source.getEntity();
        if (responsible instanceof LivingEntity living) {
            return living;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    @Override
    public void awardKillScore(Entity entity, int score, DamageSource source) {
        super.awardKillScore(entity, score, source);
        if (entity instanceof LivingEntity living) {
            double gained = living.getMaxHealth();
            if (gained > 0.0) {
                var attribute = this.getAttribute(Attributes.MAX_HEALTH);
                if (attribute != null) {
                    double newBase = attribute.getBaseValue() + gained;
                    attribute.setBaseValue(newBase);
                    this.setHealth((float)Math.min(attribute.getValue(), this.getHealth() + (float)gained));
                }
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            if (chunkLoader != null && chunkLoader.isAlive()) {
                chunkLoader.remove(RemovalReason.DISCARDED);
            }
            if (registered) {
                TestSoulManager.unregister(this);
                registered = false;
            }
            clearReactiveState();
        }
        super.remove(reason);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        // 兜底：无论 LootTable 是否命中，确保至少掉落一个开胸蛊
        if (!this.level().isClientSide) {
            this.spawnAtLocation(new ItemStack(CCItems.CHEST_OPENER.get()));
        }
    }

    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData data) {
        this.setHealth(this.getMaxHealth());
        applyDefaultNameIfNeeded();
        return super.finalizeSpawn(world, difficulty, reason, data);
    }

    public SoulPersonality personality() {
        return personality;
    }

    private void applyDefaultNameIfNeeded() {
        if (!this.hasCustomName()) {
            this.setCustomName(DEFAULT_NAME.copy());
        }
        this.setCustomNameVisible(true);
    }

    private static class HuntLivingGoal extends Goal {

        private final TestSoulEntity owner;
        private final Class<? extends LivingEntity> type;
        private final double range;
        private final double threshold;
        private LivingEntity candidate;
        private final int maxTicks;
        private int remainingTicks;

        HuntLivingGoal(TestSoulEntity owner, Class<? extends LivingEntity> type, double range, double thresholdRatio) {
            this.owner = owner;
            this.type = type;
            this.range = range;
            this.threshold = thresholdRatio;
            this.maxTicks = 80;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (owner.getHealth() <= 0) {
                return false;
            }
            List<? extends LivingEntity> entities = owner.level().getEntitiesOfClass(type,
                    owner.getBoundingBox().inflate(range),
                    entity -> entity.isAlive() && entity != owner && filter(entity));
            if (entities.isEmpty()) {
                return false;
            }
            entities.sort(Comparator.comparingDouble(owner::distanceToSqr));
            candidate = entities.get(0);
            remainingTicks = maxTicks;
            return true;
        }

        @Override
        public void start() {
            owner.setTarget(candidate);
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            if (candidate == null || !candidate.isAlive()) {
                return false;
            }
            if (remainingTicks-- <= 0) {
                return false;
            }
            return owner.distanceToSqr(candidate) <= range * range && filter(candidate);
        }

        @Override
        public void tick() {
            super.tick();
            if (candidate == null || !candidate.isAlive()) {
                return;
            }
            if (owner.getTarget() != candidate) {
                owner.setTarget(candidate);
            }
        }

        protected boolean filter(LivingEntity entity) {
            if (entity instanceof TestSoulEntity) {
                return false;
            }
            return entity.getHealth() < owner.getHealth() * threshold;
        }

        @Override
        public void stop() {
            super.stop();
            candidate = null;
            owner.setTarget(null);
        }
    }

    private static class HuntPlayerGoal extends HuntLivingGoal {

        HuntPlayerGoal(TestSoulEntity owner, double range, double thresholdRatio) {
            super(owner, Player.class, range, thresholdRatio);
        }

        @Override
        public boolean canUse() {
            return super.canUse();
        }

        @Override
        protected boolean filter(LivingEntity entity) {
            if (!(entity instanceof Player player)) {
                return false;
            }
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
            return super.filter(entity);
        }
    }

    private static class TestWanderGoal extends RandomStrollGoal {

        private final TestSoulEntity owner;

        TestWanderGoal(TestSoulEntity owner, double speed) {
            super(owner, speed);
            this.owner = owner;
        }

        @Override
        protected Vec3 getPosition() {
            Optional<Vec3> dense = EntityDensityOps.findDensePoint(owner, 12.0, 8);
            return dense.orElseGet(() -> super.getPosition());
        }
    }

    /**
     * Reactive defense goal used by TestSoulEntity.
     *
     * <p>设计意图：
     * - 受击后根据 {@link ReactiveMode} 切换“反击”或“脱离”。
     * - 反击：锁定最近的攻击者，转向并持续寻找攻击机会。
     * - 脱离：向远离攻击者的位置移动，必要时重新取样逃离路径。
     *
     * <p>状态驱动：
     * - {@link ReactiveMode#RETALIATE} 由实体逻辑设置，Goal 负责目标与朝向。
     * - {@link ReactiveMode#EVADE} 在 {@link #canUse()} 时会取样逃离点并持续尝试保持距离。
     * - Goal 在 {@link #canContinueToUse()} 中持续校验攻击者存活与剩余反应时间。
     */
    private static final class ReactiveDefenseGoal extends Goal {

        private static final double EVADE_SPEED = 1.35;
        private static final float RETALIATE_FOCUS = 30.0f;
        private final TestSoulEntity owner;
        private Vec3 evadeTarget;

        ReactiveDefenseGoal(TestSoulEntity owner) {
            this.owner = owner;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (owner.reactiveMode == ReactiveMode.NONE) {
                return false;
            }
            LivingEntity aggressor = owner.recentAggressor;
            if (aggressor == null || !aggressor.isAlive()) {
                owner.clearReactiveState();
                return false;
            }
            if (owner.reactiveMode == ReactiveMode.RETALIATE) {
                return true;
            }
            Vec3 away = DefaultRandomPos.getPosAway(owner, 14, 6, aggressor.position());
            if (away == null) {
                return false;
            }
            evadeTarget = away;
            return true;
        }

        @Override
        public void start() {
            LivingEntity aggressor = owner.recentAggressor;
            if (owner.reactiveMode == ReactiveMode.RETALIATE) {
                // 反击模式：切换攻击目标，交给近战 Goal 处理靠近与挥击。
                owner.setTarget(aggressor);
            } else if (evadeTarget != null) {
                // 脱离模式：朝逃离点移动，优先保持生存。
                owner.getNavigation().moveTo(evadeTarget.x, evadeTarget.y, evadeTarget.z, EVADE_SPEED);
            }
        }

        @Override
        public boolean canContinueToUse() {
            if (owner.reactiveMode == ReactiveMode.NONE || owner.reactiveTicks <= 0) {
                return false;
            }
            LivingEntity aggressor = owner.recentAggressor;
            if (aggressor == null || !aggressor.isAlive()) {
                owner.clearReactiveState();
                return false;
            }
            if (owner.reactiveMode == ReactiveMode.RETALIATE) {
                // 反击模式：仅在攻击者仍在视野范围且剩余反应时间内继续执行。
                return owner.distanceToSqr(aggressor) <= 256.0 && owner.reactiveTicks > 0;
            }
            return owner.reactiveTicks > 0 && owner.getNavigation().isInProgress();
        }

        @Override
        public void tick() {
            LivingEntity aggressor = owner.recentAggressor;
            if (aggressor == null) {
                return;
            }
            if (owner.reactiveMode == ReactiveMode.RETALIATE) {
                // 持续盯准目标，提高近战命中率。
                owner.getLookControl().setLookAt(aggressor, RETALIATE_FOCUS, RETALIATE_FOCUS);
            } else if (owner.reactiveMode == ReactiveMode.EVADE) {
                // 若攻击者逼近但当前路径终止，重新取方向对立的逃离点。
                if (owner.distanceToSqr(aggressor) < 144.0 && owner.getNavigation().isDone()) {
                    Vec3 away = DefaultRandomPos.getPosAway(owner, 16, 7, aggressor.position());
                    if (away != null) {
                        owner.getNavigation().moveTo(away.x, away.y, away.z, EVADE_SPEED);
                    }
                }
            }
        }

        @Override
        public void stop() {
            if (owner.reactiveMode == ReactiveMode.EVADE) {
                owner.getNavigation().stop();
            }
        }
    }

    private enum ReactiveMode {
        NONE,
        RETALIATE,
        EVADE
    }

    public static boolean checkSpawnRules(EntityType<TestSoulEntity> type, LevelAccessor world, MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (world instanceof ServerLevel serverLevel) {
            return TestSoulManager.canSpawn(serverLevel);
        }
        return true;
    }
}
