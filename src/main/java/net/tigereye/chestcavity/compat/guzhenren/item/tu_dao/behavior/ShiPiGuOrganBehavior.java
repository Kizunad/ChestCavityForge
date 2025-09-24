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
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
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
                playRechargeCue(player);
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
            applyResistance(player, cc, charge);
        }
    }

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!(victim instanceof Player player) || victim.level().isClientSide()) {
            return damage;
        }
        if (cc == null) {
            return damage;
        }

        int charge = Math.min(MAX_CHARGE, NBTCharge.getCharge(organ, STATE_KEY));
        if (charge <= 0) {
            playDamageCue(player.level(), player, false);
            spawnDamageParticles(player.level(), player);
            return damage;
        }

        int updated = Math.max(0, charge - 1);
        if (updated != charge) {
            NBTCharge.setCharge(organ, STATE_KEY, updated);
            writeRechargeTimer(organ, 0);
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }

        Level level = player.level();
        dropCobblestone(level, player);
        playDamageCue(level, player, true);
        spawnDamageParticles(level, player);

        return damage;
    }

    /** Ensures linkage channels exist for the owning chest cavity. */
    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ensureChannel(cc, TU_DAO_INCREASE_EFFECT);
    }

    private static void applyResistance(Player player, ChestCavityInstance cc, int charge) {
        double ratio = Math.max(0.0, (double) charge / (double) MAX_CHARGE);
        int baseLevel = charge > 0 ? Math.max(1, (int) Math.round(ratio)) : 0;
        LinkageChannel increaseChannel = ensureChannel(cc, TU_DAO_INCREASE_EFFECT);
        double increaseTotal = 1.0 + Math.max(0.0, increaseChannel.get());
        double scaledLevel = baseLevel * increaseTotal;
        int finalLevel = Math.max(1, (int) Math.round(scaledLevel));
        int amplifier = Math.max(0, finalLevel - 1);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, RESIST_EFFECT_DURATION, amplifier, true, true));
    }

    private static void playRechargeCue(Player player) {
        Level level = player.level();
        RandomSource random = level.getRandom();
        float pitch = 0.8f + random.nextFloat() * 0.2f;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 0.5f, pitch);
    }

    private static void dropCobblestone(Level level, Player player) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        RandomSource random = server.getRandom();
        Vec3 center = player.position();
        double offsetX = (random.nextDouble() - 0.5) * 0.6;
        double offsetZ = (random.nextDouble() - 0.5) * 0.6;
        ItemEntity item = new ItemEntity(server, center.x + offsetX, player.getY(0.5), center.z + offsetZ, new ItemStack(Items.COBBLESTONE));
        double motionX = (random.nextDouble() - 0.5) * 0.2;
        double motionY = 0.25 + random.nextDouble() * 0.1;
        double motionZ = (random.nextDouble() - 0.5) * 0.2;
        item.setDeltaMovement(motionX, motionY, motionZ);
        item.setDefaultPickUpDelay();
        server.addFreshEntity(item);
        float pitch = 0.9f + random.nextFloat() * 0.2f;
        server.playSound(null, item.getX(), item.getY(), item.getZ(), SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 0.8f, pitch);
    }

    private static void playDamageCue(Level level, Player player, boolean consumedCharge) {
        RandomSource random = level.getRandom();
        float hurtPitch = 0.9f + random.nextFloat() * 0.2f;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.9f, hurtPitch);
        float gravelPitch = 0.8f + random.nextFloat() * 0.2f;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 0.7f, gravelPitch);
        if (!consumedCharge) {
            return;
        }
        // Stone break handled in dropCobblestone when charge was consumed.
    }

    private static void spawnDamageParticles(Level level, Player player) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        RandomSource random = server.getRandom();
        int blockCount = Mth.nextInt(random, BLOCK_PARTICLE_MIN, BLOCK_PARTICLE_MAX);
        Vec3 center = player.position().add(0.0, 0.8, 0.0);
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
