package net.tigereye.chestcavity.world.spawn;

import com.google.common.collect.ImmutableList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 自定义生物生成定义。
 * <p>
 * 该定义描述了单类自定义生物的生成规则（最大存活数、刷新窗口、位置选择器等），
 * 并允许在实体生成后通过 {@link SpawnConfigurator} / {@link BrainConfigurator}
 * 统一配置 ChestCavity、鼓真刃附着体以及行为脑优先级。
 */
public final class CustomMobSpawnDefinition {

    public static final int NO_CAP = -1;

    private final ResourceLocation id;
    private final Supplier<EntityType<? extends Mob>> entityType;
    private final int maxAlive;
    private final int spawnBatchSize;
    private final int attemptIntervalTicks;
    private final double spawnChance;
    private final double horizontalRange;
    private final int verticalRange;
    private final double minPlayerDistance;
    private final boolean forcePersistence;
    private final SpawnLocationProvider locationProvider;
    private final SpawnValidator spawnValidator;
    private final ImmutableList<SpawnConfigurator> configurators;
    private final ImmutableList<BrainConfigurator> brainConfigurators;
    private final CustomMobMessageEmitter messageEmitter;

    private CustomMobSpawnDefinition(Builder builder) {
        this.id = builder.id;
        this.entityType = builder.entityType;
        this.maxAlive = builder.maxAlive;
        this.spawnBatchSize = builder.spawnBatchSize;
        this.attemptIntervalTicks = builder.attemptIntervalTicks;
        this.spawnChance = builder.spawnChance;
        this.horizontalRange = builder.horizontalRange;
        this.verticalRange = builder.verticalRange;
        this.minPlayerDistance = builder.minPlayerDistance;
        this.forcePersistence = builder.forcePersistence;
        this.locationProvider = builder.locationProvider == null
                ? SpawnLocationProviders.nearRandomPlayer()
                : builder.locationProvider;
        this.spawnValidator = builder.spawnValidator == null
                ? SpawnValidator.ALWAYS_ALLOW
                : builder.spawnValidator;
        this.configurators = ImmutableList.copyOf(builder.configurators);
        this.brainConfigurators = ImmutableList.copyOf(builder.brainConfigurators);
        this.messageEmitter = builder.messageEmitter;
    }

    public ResourceLocation id() {
        return id;
    }

    public Supplier<EntityType<? extends Mob>> entityType() {
        return entityType;
    }

    public int maxAlive() {
        return maxAlive;
    }

    public int spawnBatchSize() {
        return spawnBatchSize;
    }

    public int attemptIntervalTicks() {
        return attemptIntervalTicks;
    }

    public double spawnChance() {
        return spawnChance;
    }

    public double horizontalRange() {
        return horizontalRange;
    }

    public int verticalRange() {
        return verticalRange;
    }

    public double minPlayerDistance() {
        return minPlayerDistance;
    }

    public boolean forcePersistence() {
        return forcePersistence;
    }

    public SpawnLocationProvider locationProvider() {
        return locationProvider;
    }

    public SpawnValidator spawnValidator() {
        return spawnValidator;
    }

    public List<SpawnConfigurator> configurators() {
        return configurators;
    }

    public List<BrainConfigurator> brainConfigurators() {
        return brainConfigurators;
    }

    public Optional<CustomMobMessageEmitter> messageEmitter() {
        return Optional.ofNullable(messageEmitter);
    }

    public static Builder builder(ResourceLocation id, Supplier<EntityType<? extends Mob>> entityType) {
        return new Builder(id, entityType);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final Supplier<EntityType<? extends Mob>> entityType;
        private int maxAlive = NO_CAP;
        private int spawnBatchSize = 1;
        private int attemptIntervalTicks = 200;
        private double spawnChance = 1.0D;
        private double horizontalRange = 8.0D;
        private int verticalRange = 4;
        private double minPlayerDistance = 2.0D;
        private boolean forcePersistence = true;
        private SpawnLocationProvider locationProvider;
        private SpawnValidator spawnValidator;
        private final List<SpawnConfigurator> configurators = new ArrayList<>();
        private final List<BrainConfigurator> brainConfigurators = new ArrayList<>();
        private CustomMobMessageEmitter messageEmitter;

        private Builder(ResourceLocation id, Supplier<EntityType<? extends Mob>> entityType) {
            this.id = Objects.requireNonNull(id, "id");
            this.entityType = Objects.requireNonNull(entityType, "entityType");
        }

        public Builder withMaxAlive(int maxAlive) {
            this.maxAlive = maxAlive < NO_CAP ? NO_CAP : maxAlive;
            return this;
        }

        public Builder withSpawnBatchSize(int batchSize) {
            this.spawnBatchSize = Math.max(1, batchSize);
            return this;
        }

        public Builder withAttemptIntervalTicks(int ticks) {
            this.attemptIntervalTicks = Math.max(1, ticks);
            return this;
        }

        public Builder withSpawnChance(double chance) {
            this.spawnChance = Math.max(0D, Math.min(1D, chance));
            return this;
        }

        public Builder withHorizontalRange(double range) {
            this.horizontalRange = Math.max(0.0D, range);
            return this;
        }

        public Builder withVerticalRange(int range) {
            this.verticalRange = Math.max(0, range);
            return this;
        }

        public Builder withMinPlayerDistance(double distance) {
            this.minPlayerDistance = Math.max(0.0D, distance);
            return this;
        }

        public Builder forcePersistence(boolean force) {
            this.forcePersistence = force;
            return this;
        }

        public Builder withLocationProvider(SpawnLocationProvider provider) {
            this.locationProvider = provider;
            return this;
        }

        public Builder withSpawnValidator(SpawnValidator validator) {
            this.spawnValidator = validator;
            return this;
        }

        public Builder addConfigurator(SpawnConfigurator configurator) {
            if (configurator != null) {
                this.configurators.add(configurator);
            }
            return this;
        }

        public Builder addBrainConfigurator(BrainConfigurator configurator) {
            if (configurator != null) {
                this.brainConfigurators.add(configurator);
            }
            return this;
        }

        public Builder withChestCavityConfigurator(SpawnConfigurators.ChestCavityConfigurator configurator) {
            Objects.requireNonNull(configurator, "configurator");
            return addConfigurator(configurator);
        }

        public Builder withGuScriptConfigurator(SpawnConfigurators.GuScriptConfigurator configurator) {
            Objects.requireNonNull(configurator, "configurator");
            return addConfigurator(configurator);
        }

        public Builder withMobConfigurator(SpawnConfigurators.MobConfigurator configurator) {
            Objects.requireNonNull(configurator, "configurator");
            return addConfigurator(configurator);
        }

        public Builder withMessageEmitter(CustomMobMessageEmitter emitter) {
            this.messageEmitter = emitter;
            return this;
        }

        public CustomMobSpawnDefinition build() {
            return new CustomMobSpawnDefinition(this);
        }
    }
}
