package net.tigereye.chestcavity.compat.guzhenren.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnGroundListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NBTWriter;

/**
 * Shuishengu (水肾蛊)
 *
 * Design summary
 * - In water, spends zhenyuan each slow tick to accumulate a per-item shield charge (server-side).
 * - Charge capacity and growth scale with stack size (BASE_MAX_CHARGE * stackCount; +stackCount per slow tick).
 * - On full charge, emits a celebratory audio/particle cue. On damage, consumes charge via an exponential mapping
 *   to reduce damage: small hits eat little charge; large hits approach full consumption.
 * - Always produces a splash on mitigation; plays a shield-break sound if the shield is fully depleted.
 *
 * Implementation
 * - Zhenyuan cost per slow tick: BASE_ZHENYUAN_COST * stackCount (scaled by GuzhenrenResourceBridge rules).
 * - Visuals: charging bubbles rising near feet; conduit-power for a subtle water outline; END_ROD/SOUL bursts on full.
 * - Sounds: bubble-column during charge, beacon select on full, splash every mitigation, shield-break on depletion.
 */
public enum ShuishenguOrganBehavior implements OrganOnGroundListener, OrganSlowTickListener, OrganIncomingDamageListener {
    INSTANCE;

    /** Root NBT key that stores per-item state. */
    private static final String STATE_KEY = "ChestCavityShuishengu";
    /** Charge counter key within STATE_KEY compound. */
    private static final String COUNTER_KEY = "Charge";
    /** Base shield capacity; final cap is this times the stack count. */
    private static final int BASE_MAX_CHARGE = 20;
    /** Full-cap damage absorption per stack (hearts); total = DAMAGE_REDUCTION * stackCount. */
    private static final float DAMAGE_REDUCTION = 40.0f;
    /** Per slow-tick zhenyuan base cost per item stack to advance charge. */
    private static final double BASE_ZHENYUAN_COST = 80.0;
    /** Exponential smoothness factor used by computeShieldCost. */
    private static final double SHIELD_ALPHA = 3.0;
    private static final Component READY_MESSAGE = Component.translatable("message.chestcavity.shuishengu.ready");

    @Override
    public void onGroundTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        // 行为集中在慢速 tick 中，避免每 tick 重复判定。
    }

    /**
     * Slow tick (once per second) charging while in water.
     * - Consumes scaled zhenyuan before charging; failure to pay halts progress for this tick.
     * - Increases charge by stackCount up to an effective cap.
     * - Emits subtle audio/particle cues while charging; stronger cues when reaching full charge.
     */
    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide() || !entity.isInWater()) {
            return;
        }

        // Determine capacity and current charge (clamped) for this stack size
        int stackCount = Math.max(1, organ.getCount());
        int effectiveMaxCharge = getEffectiveMaxCharge(stackCount);

        int current = Math.min(getCharge(organ), effectiveMaxCharge);
        if (current >= effectiveMaxCharge) {
            return;
        }

        // Attempt to pay zhenyuan cost proportional to stack count
        var handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return;
        }

        if (handleOpt.get().consumeScaledZhenyuan(BASE_ZHENYUAN_COST * stackCount).isEmpty()) {
            return;
        }

        // Successful payment: advance charge by stack count
        int updated = Math.min(effectiveMaxCharge, current + stackCount);
        setCharge(organ, updated, stackCount);

        if (updated > current) {
            playSound(entity, SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 0.35f, 1.0f);
            spawnChargingParticles(entity, stackCount);
            applyChargingEffect(player);
        }

        if (updated == effectiveMaxCharge) {
            player.displayClientMessage(READY_MESSAGE, true);
            playSound(entity, SoundEvents.BEACON_POWER_SELECT, 0.6f, 1.2f);
            spawnFullChargeBurst(entity, stackCount);
        }
    }

    /**
     * Mitigates incoming damage by consuming charge.
     * - Smooth consumption model: small damage eats little charge; large damage approaches full-cap usage.
     * - Always produces splash; adds shield-break sound if shield is fully depleted.
     */
    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (damage <= 0f || victim.level().isClientSide()) {
            return damage;
        }

        int stackCount = Math.max(1, organ.getCount());
        int effectiveMaxCharge = getEffectiveMaxCharge(stackCount);

        int current = Math.min(getCharge(organ), effectiveMaxCharge);
        if (current <= 0) {
            return damage;
        }

        // Compute the number of charge units to spend based on damage magnitude
        float maxReduction = DAMAGE_REDUCTION * stackCount;
        int cost = computeShieldCost(damage, maxReduction, effectiveMaxCharge);
        if (cost <= 0) {
            return damage;
        }

        if (cost > current) {
            cost = current;
        }

        // Effective reduction is proportional to the fraction of charge consumed
        float reduction = maxReduction * (cost / (float) effectiveMaxCharge);
        float mitigated = Math.max(0f, damage - reduction);

        int remaining = Math.max(0, current - cost);
        setCharge(organ, remaining, stackCount);
        boolean shieldBroken = remaining == 0;
        if (shieldBroken) {
            playSound(victim, SoundEvents.SHIELD_BREAK, 0.8f, 1.0f);
        }
        playSound(victim, SoundEvents.GENERIC_SPLASH, shieldBroken ? 0.75f : 0.5f, shieldBroken ? 1.0f : 1.1f);
        spawnDamageParticles(victim, stackCount, shieldBroken);
        return mitigated;
    }

    /**
     * Maps a damage amount into discrete charge units using an exponential approach function.
     *
     * Parameters
     * - damage: incoming damage (hearts)
     * - reductionPerFullCharge: maximum hearts reduced when spending full charge for the current stack
     * - maxCharge: current capacity; result is clamped to [1, maxCharge] when damage > 0
     *
     * Behavior
     * - If damage >= reductionPerFullCharge, consume full charge (cap).
     * - Else consume round(maxCharge * (1 - exp(-alpha * damage / reductionPerFullCharge))).
     */
    private static int computeShieldCost(float damage, float reductionPerFullCharge, int maxCharge) {
        if (damage >= reductionPerFullCharge) {
            return maxCharge;
        }
        if (damage <= 0f) {
            return 0;
        }
        double x = damage / reductionPerFullCharge;
        double scaled = maxCharge * (1 - Math.exp(-SHIELD_ALPHA * x));
        int rounded = (int)Math.round(scaled);
        return Math.max(1, Math.min(maxCharge, rounded));
    }

    /** Reads current charge from the organ's NBT. */
    private static int getCharge(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return 0;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(STATE_KEY, Tag.TAG_COMPOUND)) {
            return 0;
        }
        CompoundTag state = tag.getCompound(STATE_KEY);
        return state.getInt(COUNTER_KEY);
    }

    /** Writes clamped charge to the organ's NBT for a given stack size. */
    private static void setCharge(ItemStack stack, int value, int stackCount) {
        int clamped = Math.max(0, Math.min(getEffectiveMaxCharge(stackCount), value));
        NBTWriter.updateCustomData(stack, tag -> {
            CompoundTag state = tag.contains(STATE_KEY, Tag.TAG_COMPOUND) ? tag.getCompound(STATE_KEY) : new CompoundTag();
            state.putInt(COUNTER_KEY, clamped);
            tag.put(STATE_KEY, state);
        });
    }

    /** Effective capacity scales with the number of stacked organs. */
    private static int getEffectiveMaxCharge(int stackCount) {
        return BASE_MAX_CHARGE * Math.max(1, stackCount);
    }

    /** Utility: plays a positional sound for the given entity. */
    private static void playSound(LivingEntity entity, SoundEvent sound, float volume, float pitch) {
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** Emits subtle bubble particles around the feet while charging. */
    private static void spawnChargingParticles(LivingEntity entity, int stackCount) {
        if (!(entity.level() instanceof ServerLevel server)) {
            return;
        }
        double radius = 0.4 + 0.2 * (stackCount - 1);
        for (int i = 0; i < 3 * stackCount; i++) {
            double angle = entity.getRandom().nextDouble() * Math.PI * 2;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double baseX = entity.getX() + offsetX;
            double baseY = entity.getY() + 0.1 + entity.getRandom().nextDouble() * 0.6;
            double baseZ = entity.getZ() + offsetZ;
            server.sendParticles(ParticleTypes.BUBBLE, baseX, baseY, baseZ, 1, 0.0, 0.05, 0.0, 0.0);
            server.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, baseX, baseY, baseZ, 1, 0.0, 0.04, 0.0, 0.01);
            if (entity.getRandom().nextBoolean()) {
                server.sendParticles(ParticleTypes.BUBBLE_POP, baseX, baseY + 0.2, baseZ, 1, 0.0, 0.05, 0.0, 0.0);
            }
        }
    }

    /** Emits a soft celebratory ring of particles when the shield reaches full charge. */
    private static void spawnFullChargeBurst(LivingEntity entity, int stackCount) {
        if (!(entity.level() instanceof ServerLevel server)) {
            return;
        }
        double radius = 0.6 + 0.3 * (stackCount - 1);
        int segments = 16 + stackCount * 4;
        for (int i = 0; i < segments; i++) {
            double angle = (Math.PI * 2 * i) / segments;
            double x = entity.getX() + Math.cos(angle) * radius;
            double y = entity.getY() + entity.getBbHeight() * 0.5;
            double z = entity.getZ() + Math.sin(angle) * radius;
            server.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0, 0.03, 0.0, 0.0);
            if (i % 2 == 0) {
                server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y + 0.1, z, 1, 0.0, 0.02, 0.0, 0.0);
            }
        }
    }

    /** Emits splash/foam particles on damage mitigation; stronger burst if the shield fully breaks. */
    private static void spawnDamageParticles(LivingEntity entity, int stackCount, boolean shieldBroken) {
        if (!(entity.level() instanceof ServerLevel server)) {
            return;
        }
        double spread = shieldBroken ? 0.9 : 0.6;
        int splashCount = shieldBroken ? 12 + stackCount * 3 : 6 + stackCount * 2;
        server.sendParticles(ParticleTypes.SPLASH, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(), splashCount,
                spread, 0.3, spread, 0.2);
        server.sendParticles(ParticleTypes.BUBBLE_POP, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(), splashCount / 2,
                spread * 0.5, 0.2, spread * 0.5, 0.1);
    }

    /**
     * Applies a brief conduit-power effect as a faint visual cue during charging.
     * Chosen for its subtle water-themed outline and to avoid intrusive visuals.
     */
    private static void applyChargingEffect(Player player) {
        MobEffectInstance existing = player.getEffect(MobEffects.CONDUIT_POWER);
        int amplifier = 0;
        int duration = 60;
        if (existing == null || existing.getDuration() <= 30) {
            player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, duration, amplifier, false, false, true));
        }
    }
}
