package net.tigereye.chestcavity.compat.guzhenren.item.du_dao.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NBTCharge;
import net.tigereye.chestcavity.util.NetworkUtil;
import net.tigereye.chestcavity.registration.CCItems;

import java.util.List;

/**
 * Behaviour implementation for 臭屁蛊 (Chou Pi Gu).
 */
public enum ChouPiGuOrganBehavior implements OrganSlowTickListener, OrganIncomingDamageListener {
    INSTANCE;

    public static final double FOOD_TRIGGER_BASE_CHANCE = 0.30;
    public static final double ROTTEN_FOOD_MULTIPLIER = 2.0;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation DU_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/du_dao_increase_effect");
    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final String STATE_KEY = "ChouPiGuTimer";
    private static final int RANDOM_INTERVAL_MIN_TICKS = 100;
    private static final int RANDOM_INTERVAL_MAX_TICKS = 400;
    private static final int SLOW_TICK_STEP = 20;
    private static final double DAMAGE_TRIGGER_BASE_CHANCE = 0.20;
    private static final double SELF_DEBUFF_CHANCE = 0.10;
    private static final double ATTRACT_CHANCE = 0.01;
    private static final double EFFECT_RADIUS = 3.0;
    private static final double PANIC_DISTANCE = 6.0;
    private static final double PARTICLE_BACK_OFFSET = 0.8;
    private static final double PARTICLE_VERTICAL_OFFSET = 0.1;
    private static final int PARTICLE_SMOKE_COUNT = 18;
    private static final int PARTICLE_SNEEZE_COUNT = 10;
    private static final ResourceLocation[] ATTRACTABLE_ENTITIES = new ResourceLocation[] {
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "choupifeichonggu"),
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "aibieli")
    };

    private enum TriggerCause {
        RANDOM,
        DAMAGE,
        FOOD
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity.level().isClientSide()) {
            return;
        }
        if (!(entity instanceof Player player)) {
            handleNonPlayerSlowTick(entity, cc, organ);
            return;
        }
        if (!player.isAlive()) {
            return;
        }

        RandomSource random = player.getRandom();
        int remaining = Math.max(0, NBTCharge.getCharge(organ, STATE_KEY));
        if (remaining <= 0) {
            setNextInterval(cc, organ, randomInterval(random));
            return;
        }

        int updated = Math.max(0, remaining - SLOW_TICK_STEP);
        if (updated > 0) {
            setNextInterval(cc, organ, updated);
            return;
        }

        if (!releaseGas(player, cc, organ, TriggerCause.RANDOM)) {
            setNextInterval(cc, organ, randomInterval(random));
        }
    }

    private void handleNonPlayerSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!entity.isAlive()) {
            return;
        }

        RandomSource random = entity.getRandom();
        int remaining = Math.max(0, NBTCharge.getCharge(organ, STATE_KEY));
        if (remaining <= 0) {
            setNextInterval(cc, organ, randomInterval(random));
            return;
        }

        int updated = Math.max(0, remaining - SLOW_TICK_STEP);
        if (updated > 0) {
            setNextInterval(cc, organ, updated);
            return;
        }

        if (!releaseGas(entity, cc, organ, TriggerCause.RANDOM)) {
            setNextInterval(cc, organ, randomInterval(random));
        }
    }

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (victim.level().isClientSide()) {
            return damage;
        }
        if (victim instanceof Player player) {
            RandomSource random = player.getRandom();
            double increase = Math.max(0.0, getPoisonIncrease(cc));
            double chance = DAMAGE_TRIGGER_BASE_CHANCE * (1.0 + increase);
            if (random.nextDouble() < Math.min(1.0, chance)) {
                releaseGas(player, cc, organ, TriggerCause.DAMAGE);
            }
            return damage;
        }
        handleNonPlayerDamage(victim, cc, organ);
        return damage;
    }

    private void handleNonPlayerDamage(LivingEntity victim, ChestCavityInstance cc, ItemStack organ) {
        if (victim == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!victim.isAlive()) {
            return;
        }

        RandomSource random = victim.getRandom();
        double increase = Math.max(0.0, getPoisonIncrease(cc));
        double chance = DAMAGE_TRIGGER_BASE_CHANCE * (1.0 + increase);
        if (random.nextDouble() < Math.min(1.0, chance)) {
            releaseGas(victim, cc, organ, TriggerCause.DAMAGE);
        }
    }

    public void onFoodConsumed(Player player, ChestCavityInstance cc, ItemStack food, double baseChance) {
        if (player.level().isClientSide() || cc == null || cc.inventory == null) {
            return;
        }
        RandomSource random = player.getRandom();
        double chance = Math.min(1.0, Math.max(0.0, baseChance));
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack organ = cc.inventory.getItem(i);
            if (organ == null || organ.isEmpty() || !organ.is(CCItems.GUZHENREN_CHOU_PI_GU)) {
                continue;
            }
            if (random.nextDouble() < chance) {
                releaseGas(player, cc, organ, TriggerCause.FOOD);
                break;
            }
        }
    }

    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ensurePoisonChannel(cc);
    }

    private boolean releaseGas(LivingEntity entity, ChestCavityInstance cc, ItemStack organ, TriggerCause cause) {
        if (entity == null) {
            return false;
        }
        Level level = entity.level();
        if (!(level instanceof ServerLevel server)) {
            return false;
        }

        RandomSource random = entity.getRandom();
        performEffects(server, entity, cc, organ, random, cause);
        setNextInterval(cc, organ, randomInterval(random));
        return true;
    }

    private void performEffects(ServerLevel level, LivingEntity entity, ChestCavityInstance cc, ItemStack organ, RandomSource random, TriggerCause cause) {
        playSounds(level, entity, random);
        spawnParticles(level, entity, random);
        broadcastMessages(level, entity, random, cause);
        applyDebuffs(level, entity, cc, organ, random);
        maybeDebuffSelf(entity, cc, organ, random);
        panicNearbyCreatures(level, entity, random);
        maybeSummonAttractedCreature(level, entity, random);
    }

    private static void playSounds(Level level, LivingEntity entity, RandomSource random) {
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        float puffPitch = 0.65f + random.nextFloat() * 0.15f;
        float squishPitch = 0.5f + random.nextFloat() * 0.2f;
        level.playSound(null, x, y, z, SoundEvents.PUFFER_FISH_BLOW_UP, SoundSource.PLAYERS, 0.9f, puffPitch);
        level.playSound(null, x, y, z, SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.6f, squishPitch);
    }

    private static void spawnParticles(ServerLevel level, LivingEntity entity, RandomSource random) {
        Vec3 look = entity.getLookAngle();
        Vec3 back = look.normalize().scale(-PARTICLE_BACK_OFFSET);
        Vec3 base = entity.position().add(back).add(0.0, PARTICLE_VERTICAL_OFFSET, 0.0);
        Vec3 lateral = new Vec3(look.z, 0.0, -look.x);
        if (lateral.lengthSqr() < 1.0E-4) {
            lateral = new Vec3(1.0, 0.0, 0.0);
        }
        lateral = lateral.normalize();

        for (int i = 0; i < PARTICLE_SMOKE_COUNT; i++) {
            double sideways = (random.nextDouble() - 0.5) * 0.8;
            double vertical = (random.nextDouble() - 0.5) * 0.2;
            Vec3 offset = lateral.scale(sideways).add(0.0, vertical, 0.0);
            Vec3 pos = base.add(offset);
            double speed = 0.02 + random.nextDouble() * 0.02;
            level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, speed);
        }
        level.sendParticles(ParticleTypes.SNEEZE, base.x, base.y, base.z, PARTICLE_SNEEZE_COUNT, 0.35, 0.15, 0.35, 0.01);
    }

    private static void broadcastMessages(ServerLevel level, LivingEntity entity, RandomSource random, TriggerCause cause) {
        if (!(entity instanceof Player player)) {
            return;
        }
        Component selfMessage = random.nextBoolean()
                ? Component.translatable("message.guzhenren.chou_pi_gu.uncomfortable")
                : Component.translatable("message.guzhenren.chou_pi_gu.stench");
        player.sendSystemMessage(selfMessage);

        Component broadcast = Component.translatable("message.guzhenren.chou_pi_gu.odor_broadcast", player.getDisplayName());
        for (ServerPlayer other : level.players()) {
            if (other == player) {
                continue;
            }
            if (other.distanceTo(player) > 32.0f) {
                continue;
            }
            other.sendSystemMessage(broadcast);
        }
    }

    private void applyDebuffs(ServerLevel level, LivingEntity entity, ChestCavityInstance cc, ItemStack organ, RandomSource random) {
        int stackCount = Math.max(1, organ.getCount());
        int duration = Math.max(20, stackCount * 40);
        int poisonAmplifier = Math.max(0, Mth.floor(getPoisonIncrease(cc)));

        AABB area = entity.getBoundingBox().inflate(EFFECT_RADIUS);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area, candidate ->
                candidate != null && candidate.isAlive() && candidate != entity);
        for (LivingEntity victim : victims) {
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, duration, poisonAmplifier, false, true, true));
            victim.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, 0, false, true, true));
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 0, false, true, true));
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, true, true));
        }
    }

    private void maybeDebuffSelf(LivingEntity entity, ChestCavityInstance cc, ItemStack organ, RandomSource random) {
        if (random.nextDouble() >= SELF_DEBUFF_CHANCE) {
            return;
        }
        int stackCount = Math.max(1, organ.getCount());
        int duration = Math.max(20, stackCount * 40);
        int poisonAmplifier = Math.max(0, Mth.floor(getPoisonIncrease(cc)));
        entity.addEffect(new MobEffectInstance(MobEffects.POISON, duration, poisonAmplifier, false, true, true));
        entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, true, true));
    }

    private void panicNearbyCreatures(ServerLevel level, LivingEntity entity, RandomSource random) {
        AABB area = entity.getBoundingBox().inflate(PANIC_DISTANCE);
        List<PathfinderMob> mobs = level.getEntitiesOfClass(PathfinderMob.class, area, mob ->
                mob != null && mob.isAlive() && mob.distanceToSqr(entity) <= PANIC_DISTANCE * PANIC_DISTANCE
                        && (mob instanceof Animal || mob instanceof AbstractVillager));
        Vec3 entityPos = entity.position();
        for (PathfinderMob mob : mobs) {
            Vec3 away = mob.position().subtract(entityPos);
            if (away.lengthSqr() < 1.0E-4) {
                away = new Vec3(random.nextDouble() - 0.5, 0.0, random.nextDouble() - 0.5);
            }
            Vec3 target = mob.position().add(away.normalize().scale(PANIC_DISTANCE));
            mob.getNavigation().moveTo(target.x, target.y, target.z, 1.4);
        }
    }

    private void maybeSummonAttractedCreature(ServerLevel level, LivingEntity entity, RandomSource random) {
        if (random.nextDouble() >= ATTRACT_CHANCE || ATTRACTABLE_ENTITIES.length == 0) {
            return;
        }
        ResourceLocation id = ATTRACTABLE_ENTITIES[random.nextInt(ATTRACTABLE_ENTITIES.length)];
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        if (type == null) {
            return;
        }

        Vec3 spawnPos = findSpawnPosition(level, entity, random);
        if (spawnPos == null) {
            return;
        }

        Entity spawned = type.create(level);
        if (spawned == null) {
            return;
        }
        spawned.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, random.nextFloat() * 360.0f, 0.0f);
        level.addFreshEntity(spawned);
    }

    private static Vec3 findSpawnPosition(ServerLevel level, LivingEntity entity, RandomSource random) {
        Vec3 origin = entity.position();
        for (int attempt = 0; attempt < 5; attempt++) {
            double distance = 2.5 + random.nextDouble() * 2.5;
            double angle = random.nextDouble() * Math.PI * 2.0;
            double x = origin.x + Math.cos(angle) * distance;
            double z = origin.z + Math.sin(angle) * distance;
            BlockPos sample = BlockPos.containing(x, origin.y, z);
            if (!level.isLoaded(sample)) {
                continue;
            }
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sample);
            if (!level.getWorldBorder().isWithinBounds(surface)) {
                continue;
            }
            return new Vec3(surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5);
        }
        return null;
    }

    private static int randomInterval(RandomSource random) {
        if (RANDOM_INTERVAL_MAX_TICKS <= RANDOM_INTERVAL_MIN_TICKS) {
            return RANDOM_INTERVAL_MIN_TICKS;
        }
        return RANDOM_INTERVAL_MIN_TICKS + random.nextInt(RANDOM_INTERVAL_MAX_TICKS - RANDOM_INTERVAL_MIN_TICKS + 1);
    }

    private static void setNextInterval(ChestCavityInstance cc, ItemStack organ, int ticks) {
        int clamped = Math.max(0, ticks);
        if (NBTCharge.getCharge(organ, STATE_KEY) == clamped) {
            return;
        }
        NBTCharge.setCharge(organ, STATE_KEY, clamped);
        if (cc != null) {
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }
    }

    private static double getPoisonIncrease(ChestCavityInstance cc) {
        if (cc == null) {
            return 0.0;
        }
        return ensurePoisonChannel(cc).get();
    }

    private static LinkageChannel ensurePoisonChannel(ChestCavityInstance cc) {
        ActiveLinkageContext context = GuzhenrenLinkageManager.getContext(cc);
        return context.getOrCreateChannel(DU_DAO_INCREASE_EFFECT).addPolicy(NON_NEGATIVE);
    }
}
