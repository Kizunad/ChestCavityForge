package net.tigereye.chestcavity.soul.entity;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.util.ChestCavityInsertOps;
import net.tigereye.chestcavity.soul.util.EntityChunkLoadOps;
import net.tigereye.chestcavity.soul.util.EntityDensityOps;
import net.tigereye.chestcavity.soul.util.ItemAbsorptionOps;
import net.tigereye.chestcavity.soul.util.ItemFilterOps;
import net.tigereye.chestcavity.soul.util.SurfaceTeleportOps;
import net.tigereye.chestcavity.soul.fakeplayer.brain.personality.SoulPersonality;
import net.tigereye.chestcavity.soul.fakeplayer.brain.personality.SoulPersonalityRegistry;

/**
 * 自定义灵魂实体“Test”，具备谨慎性格与独特拾取/攻击逻辑。
 */
public class TestSoulEntity extends PathfinderMob {

    private static final double ITEM_RADIUS = 4.0;
    private static final double ITEM_PULL = 0.35;
    private static final int CHUNK_RADIUS = 2;
    private final SoulPersonality personality = SoulPersonalityRegistry.resolve(SoulPersonalityRegistry.CAUTIOUS_ID);
    private SoulChunkLoaderEntity chunkLoader;
    private boolean registered;

    public TestSoulEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(2, new TestWanderGoal(this, 1.0));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(4, new AvoidEntityGoal<>(this, Player.class, 32.0f, 1.2, 1.4,
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
        }
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
        }
        super.remove(reason);
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
        return super.finalizeSpawn(world, difficulty, reason, data);
    }

    public SoulPersonality personality() {
        return personality;
    }

    private static class HuntLivingGoal extends Goal {

        private final TestSoulEntity owner;
        private final Class<? extends LivingEntity> type;
        private final double range;
        private final double threshold;
        private LivingEntity candidate;

        HuntLivingGoal(TestSoulEntity owner, Class<? extends LivingEntity> type, double range, double thresholdRatio) {
            this.owner = owner;
            this.type = type;
            this.range = range;
            this.threshold = thresholdRatio;
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
            return true;
        }

        @Override
        public void start() {
            owner.setTarget(candidate);
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            return candidate != null && candidate.isAlive() && owner.distanceToSqr(candidate) <= range * range && filter(candidate);
        }

        private boolean filter(LivingEntity entity) {
            if (entity instanceof TestSoulEntity) {
                return false;
            }
            return entity.getHealth() < owner.getHealth() * threshold;
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

    public static boolean checkSpawnRules(EntityType<TestSoulEntity> type, LevelAccessor world, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return TestSoulManager.canSpawn();
    }
}
