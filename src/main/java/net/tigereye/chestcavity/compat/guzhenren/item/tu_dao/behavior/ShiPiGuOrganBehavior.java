package net.tigereye.chestcavity.compat.guzhenren.item.tu_dao.behavior;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NBTCharge;
import net.tigereye.chestcavity.util.NBTWriter;
import net.tigereye.chestcavity.util.NetworkUtil;
import org.joml.Vector3f;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.component.CustomData;

/**
 * Behaviour implementation for 石皮蛊 (Shi Pi Gu).
 *
 * Notes:
 * - Works for both players and mobs (LivingEntity) on the server side.
 * - Players simply receive the timed absorption and damage mitigation; no
 *   Guzhenren resource (zhenyuan/jingli) cost is applied by this organ.
 * - Mobs do not have Guzhenren resources. Per request, an embedded
 *   non‑player handler is provided at the bottom of this file to convert any
 *   future Tu Dao costs into health (HP) at 100:1.
 */
public enum ShiPiGuOrganBehavior implements OrganSlowTickListener, OrganIncomingDamageListener {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation TU_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/tu_dao_increase_effect");

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final String STATE_KEY = "ShiPiGu";
    private static final String TIMER_KEY = "RechargeTimer";

    private static final int MAX_CHARGE = 10;
    private static final int RECOVERY_INTERVAL_SLOW_TICKS = 10; // 10 seconds (slow tick fires once per second)
    private static final int RESIST_EFFECT_DURATION = 40; // maintain coverage between slow ticks

    private static final int BLOCK_PARTICLE_MIN = 15;
    private static final int BLOCK_PARTICLE_MAX = 20;
    private static final int DUST_PARTICLE_COUNT = 5;

    private static final DustParticleOptions STONE_DUST =
            new DustParticleOptions(new Vector3f(100f / 255f, 100f / 255f, 100f / 255f), 1.0f);
    private static final BlockState COBBLESTONE_STATE = Blocks.COBBLESTONE.defaultBlockState();

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        if (cc == null) {
            return;
        }

        int charge = Math.min(MAX_CHARGE, NBTCharge.getCharge(organ, STATE_KEY));
        int timer = readRechargeTimer(organ);
        boolean chargeChanged = false;
        boolean timerChanged = false;

        if (charge < MAX_CHARGE) {
            timer += 1;
            if (timer >= RECOVERY_INTERVAL_SLOW_TICKS) {
                timer = 0;
                charge = Math.min(MAX_CHARGE, charge + 1);
                chargeChanged = true;
                timerChanged = true;
                playRechargeCue(entity);
            } else {
                timerChanged = true;
            }
        } else if (timer != 0) {
            timer = 0;
            timerChanged = true;
        }

        if (chargeChanged) {
            NBTCharge.setCharge(organ, STATE_KEY, charge);
        }
        if (timerChanged) {
            writeRechargeTimer(organ, timer);
        }
        if (chargeChanged) {
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }

        if (charge > 0) {
            applyAbsorption(entity, cc, charge);
        }
    }

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (victim == null || victim.level().isClientSide()) {
            return damage;
        }
        if (cc == null) {
            return damage;
        }

        int charge = Math.min(MAX_CHARGE, NBTCharge.getCharge(organ, STATE_KEY));
        if (charge <= 0) {
            playDamageCue(victim.level(), victim, false);
            spawnDamageParticles(victim.level(), victim);
            return damage;
        }

        int updated = Math.max(0, charge - 1);
        if (updated != charge) {
            NBTCharge.setCharge(organ, STATE_KEY, updated);
            writeRechargeTimer(organ, 0);
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }

        Level level = victim.level();
        dropCobblestone(level, victim);
        playDamageCue(level, victim, true);
        spawnDamageParticles(level, victim);

        return damage;
    }

    /** Ensures linkage channels exist for the owning chest cavity. */
    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ensureChannel(cc, TU_DAO_INCREASE_EFFECT);
    }

    private static void applyAbsorption(LivingEntity entity, ChestCavityInstance cc, int charge) {
        double ratio = Math.max(0.0, (double) charge / (double) MAX_CHARGE);
        int baseLevel = charge > 0 ? Math.max(1, (int) Math.round(ratio)) : 0;
        LinkageChannel increaseChannel = ensureChannel(cc, TU_DAO_INCREASE_EFFECT);
        double increaseTotal = 1.0 + Math.max(0.0, increaseChannel.get());
        double scaledLevel = baseLevel * increaseTotal;
        int finalLevel = Math.max(1, (int) Math.round(scaledLevel));
        int amplifier = Math.max(0, finalLevel - 1);
        entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, RESIST_EFFECT_DURATION, amplifier, true, true));
    }

    private static void playRechargeCue(LivingEntity entity) {
        Level level = entity.level();
        RandomSource random = level.getRandom();
        float pitch = 0.8f + random.nextFloat() * 0.2f;
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 0.5f, pitch);
    }

    private static void dropCobblestone(Level level, LivingEntity entity) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        RandomSource random = server.getRandom();
        Vec3 center = entity.position();
        double offsetX = (random.nextDouble() - 0.5) * 0.6;
        double offsetZ = (random.nextDouble() - 0.5) * 0.6;
        ItemEntity item = new ItemEntity(server, center.x + offsetX, entity.getY(0.5), center.z + offsetZ, new ItemStack(Items.COBBLESTONE));
        double motionX = (random.nextDouble() - 0.5) * 0.2;
        double motionY = 0.25 + random.nextDouble() * 0.1;
        double motionZ = (random.nextDouble() - 0.5) * 0.2;
        item.setDeltaMovement(motionX, motionY, motionZ);
        item.setDefaultPickUpDelay();
        server.addFreshEntity(item);
        float pitch = 0.9f + random.nextFloat() * 0.2f;
        server.playSound(null, item.getX(), item.getY(), item.getZ(), SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 0.8f, pitch);
    }

    private static void playDamageCue(Level level, LivingEntity entity, boolean consumedCharge) {
        RandomSource random = level.getRandom();
        float hurtPitch = 0.9f + random.nextFloat() * 0.2f;
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.9f, hurtPitch);
        float gravelPitch = 0.8f + random.nextFloat() * 0.2f;
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 0.7f, gravelPitch);
        if (!consumedCharge) {
            return;
        }
        // Stone break handled in dropCobblestone when charge was consumed.
    }

    private static void spawnDamageParticles(Level level, LivingEntity entity) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        RandomSource random = server.getRandom();
        int blockCount = Mth.nextInt(random, BLOCK_PARTICLE_MIN, BLOCK_PARTICLE_MAX);
        Vec3 center = entity.position().add(0.0, 0.8, 0.0);
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, COBBLESTONE_STATE),
                center.x,
                center.y,
                center.z,
                blockCount,
                0.6,
                0.4,
                0.6,
                0.15);
        server.sendParticles(STONE_DUST,
                center.x,
                center.y,
                center.z,
                DUST_PARTICLE_COUNT,
                0.5,
                0.3,
                0.5,
                0.05);
    }

    /**
     * Embedded non-player handler for Tu Dao style health fallback.
     *
     * Why this exists here (instead of using a shared helper):
     * - The user asked for TuDaoNonPlayerHandler to be embedded into the
     *   organ behavior to ensure logic is self-contained and remains stable
     *   even if external helpers change.
     * - Mobs lack Guzhenren attachments (no zhenyuan/jingli). When this
     *   organ (or future Tu Dao logic) requires a resource payment, convert
     *   the resource total to HP at a fixed 100:1 ratio and drain after
     *   absorption, without outright killing the entity.
     *
     * Usage:
     *   EmbeddedTuDaoNonPlayerHandler.payWithHealth(entity, zhenyuanBaseCost, jingliBaseCost)
     *
     * Threading:
     * - Call from the server thread only (no client mutations).
     */
    private static final class EmbeddedTuDaoNonPlayerHandler {
        private static final double DEFAULT_RATIO = 100.0;
        private static final float EPS = 1.0E-4f;

        static boolean payWithHealth(LivingEntity entity, double zhenyuanCost, double jingliCost) {
            if (entity == null || !entity.isAlive()) {
                return false;
            }
            double total = Math.max(0.0, zhenyuanCost) + Math.max(0.0, jingliCost);
            if (!Double.isFinite(total) || total <= 0.0) {
                return true;
            }
            float healthCost = (float)(total / DEFAULT_RATIO);
            if (!Float.isFinite(healthCost) || healthCost <= 0.0f) {
                return false;
            }
            float startingHealth = entity.getHealth();
            float startingAbsorption = Math.max(0.0f, entity.getAbsorptionAmount());
            float available = startingHealth + startingAbsorption;
            if (available <= healthCost + EPS) {
                return false;
            }
            entity.invulnerableTime = 0;
            entity.hurt(entity.damageSources().generic(), healthCost);
            entity.invulnerableTime = 0;
            float remaining = healthCost;
            float absorbed = Math.min(startingAbsorption, remaining);
            remaining -= absorbed;
            if (!entity.isDeadOrDying()) {
                float targetAbs = Math.max(0.0f, startingAbsorption - absorbed);
                entity.setAbsorptionAmount(targetAbs);
                if (remaining > 0.0f) {
                    float targetHp = Math.max(0.0f, startingHealth - remaining);
                    if (entity.getHealth() > targetHp) {
                        entity.setHealth(targetHp);
                    }
                }
                entity.hurtTime = 0;
                entity.hurtDuration = 0;
            }
            return true;
        }
    }

    private static LinkageChannel ensureChannel(ChestCavityInstance cc, ResourceLocation id) {
        ActiveLinkageContext context = GuzhenrenLinkageManager.getContext(cc);
        return context.getOrCreateChannel(id).addPolicy(NON_NEGATIVE);
    }

    private static int readRechargeTimer(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return 0;
        }
        CompoundTag root = data.copyTag();
        if (!root.contains(STATE_KEY, Tag.TAG_COMPOUND)) {
            return 0;
        }
        CompoundTag state = root.getCompound(STATE_KEY);
        return state.getInt(TIMER_KEY);
    }

    private static void writeRechargeTimer(ItemStack stack, int value) {
        int clamped = Math.max(0, value);
        NBTWriter.updateCustomData(stack, tag -> {
            CompoundTag state = tag.contains(STATE_KEY, Tag.TAG_COMPOUND) ? tag.getCompound(STATE_KEY) : new CompoundTag();
            state.putInt(TIMER_KEY, clamped);
            tag.put(STATE_KEY, state);
        });
    }
}
