package net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior;

import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.OrganPresenceUtil;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.IncreaseEffectContributor;
import net.tigereye.chestcavity.linkage.IncreaseEffectLedger;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.ChestCavityUtil;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;

/**
 * Behaviour implementation for 火人蛊 (Huorengu) – provides regenerative flames and jet flight.
 */
public final class HuorenguOrganBehavior extends AbstractGuzhenrenOrganBehavior implements
        OrganSlowTickListener, OrganOnHitListener, OrganRemovalListener, IncreaseEffectContributor {

    public static final HuorenguOrganBehavior INSTANCE = new HuorenguOrganBehavior();

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LOG_PREFIX = "[compat/guzhenren][yan_dao][huorengu]";

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huorengu");
    private static final ResourceLocation HUOXINGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huoxingu");
    private static final ResourceLocation YAN_DAO_INCREASE_CHANNEL =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/yan_dao_increase_effect");

    private static final String STATE_ROOT = "Huorengu";
    private static final String SLOT_KEY = "Slot";
    private static final String INCREASE_KEY = "YanDaoIncrease";
    private static final String FLIGHT_GRANTED_KEY = "FlightGranted";
    private static final String WAS_FLYING_KEY = "WasFlying";
    private static final String SYNERGY_KEY = "FireLinkActive";
    private static final String NEXT_BURN_SOUND_KEY = "NextBurnSound";

    private static final double HEALTH_RESTORE_RATIO = 0.005;
    private static final double JINGLI_RESTORE = 4.0;
    private static final int FIRE_RESIST_DURATION_TICKS = 60;
    private static final int HASTE_DURATION_TICKS = 60;
    private static final double SYNERGY_INCREASE = 0.26;
    private static final int FIRE_LINGER_SECONDS = 600;
    private static final int BURN_SOUND_INTERVAL_TICKS = 40;

    private static final float SHOOT_SOUND_VOLUME = 0.8f;
    private static final float BURN_SOUND_VOLUME_BASE = 0.25f;

    private static final double EPSILON = 1.0E-6;

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private HuorenguOrganBehavior() {
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide() || cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }

        OrganState state = organState(organ, STATE_ROOT);
        updateSlotIndex(cc, organ, state);

        boolean primary = isPrimaryHuorengu(cc, organ);
        if (!primary) {
            handleDormantState(entity, cc, organ, state);
            return;
        }

        applyRegeneration(entity);
        restoreJingli(entity);
        applyFireResistance(entity);
        handleFlight(entity, state, cc, organ);

        boolean synergyActive = OrganPresenceUtil.has(cc, HUOXINGU_ID);
        handleSynergyState(entity, cc, organ, state, synergyActive);
        spawnPassiveParticles(entity, synergyActive);
    }

    @Override
    public float onHit(DamageSource source, LivingEntity attacker, LivingEntity target,
                       ChestCavityInstance chestCavity, ItemStack organ, float damage) {
        if (attacker == null || target == null || attacker.level().isClientSide()) {
            return damage;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return damage;
        }
        if (!isPrimaryHuorengu(chestCavity, organ)) {
            return damage;
        }
        OrganState state = organState(organ, STATE_ROOT);
        if (!state.getBoolean(SYNERGY_KEY, false)) {
            return damage;
        }
        igniteTarget(attacker, target);
        return damage;
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        OrganState state = organState(organ, STATE_ROOT);
        disableFlight(entity, state);
        double storedIncrease = state.getDouble(INCREASE_KEY, 0.0);
        if (Math.abs(storedIncrease) > EPSILON) {
            applyIncreaseDelta(cc, organ, -storedIncrease);
        }
        if (cc != null) {
            ActiveLinkageContext context = LinkageManager.getContext(cc);
            context.increaseEffects().remove(organ, YAN_DAO_INCREASE_CHANNEL);
            context.increaseEffects().unregisterContributor(organ);
        }
        resetState(state);
    }

    @Override
    public void rebuildIncreaseEffects(ChestCavityInstance chestCavity, ActiveLinkageContext context,
                                       ItemStack organ, IncreaseEffectLedger.Registrar registrar) {
        if (organ == null || organ.isEmpty() || registrar == null) {
            return;
        }
        OrganState state = organState(organ, STATE_ROOT);
        double effect = state.getDouble(INCREASE_KEY, 0.0);
        if (Math.abs(effect) <= EPSILON) {
            return;
        }
        registrar.record(YAN_DAO_INCREASE_CHANNEL, Math.max(1, organ.getCount()), effect);
    }

    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        ensureIncreaseChannel(context);
    }

    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        context.increaseEffects().registerContributor(organ, this, YAN_DAO_INCREASE_CHANNEL);
        RemovalRegistration registration = registerRemovalHook(cc, organ, this, staleRemovalContexts);
        if (!registration.alreadyRegistered()) {
            OrganState state = organState(organ, STATE_ROOT);
            var change = state.setInt(SLOT_KEY, registration.slotIndex(), value -> Math.max(-1, value), -1);
            logStateChange(LOGGER, LOG_PREFIX, organ, SLOT_KEY, change);
            if (change.changed()) {
                sendSlotUpdate(cc, organ);
            }
        }
    }

    private void handleDormantState(LivingEntity entity, ChestCavityInstance cc, ItemStack organ, OrganState state) {
        boolean changed = false;
        if (state.getBoolean(SYNERGY_KEY, false)) {
            var change = state.setBoolean(SYNERGY_KEY, false, false);
            logStateChange(LOGGER, LOG_PREFIX, organ, SYNERGY_KEY, change);
            changed |= change.changed();
        }
        double storedIncrease = state.getDouble(INCREASE_KEY, 0.0);
        if (Math.abs(storedIncrease) > EPSILON) {
            applyIncreaseDelta(cc, organ, -storedIncrease);
            var change = state.setDouble(INCREASE_KEY, 0.0, value -> Math.max(0.0, value), 0.0);
            logStateChange(LOGGER, LOG_PREFIX, organ, INCREASE_KEY, change);
            changed |= change.changed();
        }
        if (state.getBoolean(FLIGHT_GRANTED_KEY, false) || state.getBoolean(WAS_FLYING_KEY, false)) {
            disableFlight(entity, state);
            var grantChange = state.setBoolean(FLIGHT_GRANTED_KEY, false, false);
            var flyingChange = state.setBoolean(WAS_FLYING_KEY, false, false);
            logStateChange(LOGGER, LOG_PREFIX, organ, FLIGHT_GRANTED_KEY, grantChange);
            logStateChange(LOGGER, LOG_PREFIX, organ, WAS_FLYING_KEY, flyingChange);
            changed |= grantChange.changed() || flyingChange.changed();
        }
        if (changed) {
            sendSlotUpdate(cc, organ);
        }
    }

    private void applyRegeneration(LivingEntity entity) {
        double maxHealth = entity.getMaxHealth();
        if (maxHealth <= 0.0) {
            return;
        }
        float amount = (float) (maxHealth * HEALTH_RESTORE_RATIO);
        if (amount <= 0.0f) {
            return;
        }
        entity.heal(amount);
    }

    private void restoreJingli(LivingEntity entity) {
        if (!(entity instanceof Player player) || JINGLI_RESTORE <= 0.0) {
            return;
        }
        GuzhenrenResourceBridge.open(player).ifPresent(handle -> handle.adjustJingli(JINGLI_RESTORE, true));
    }

    private void applyFireResistance(LivingEntity entity) {
        MobEffectInstance existing = entity.getEffect(MobEffects.FIRE_RESISTANCE);
        if (existing != null && existing.getDuration() > FIRE_RESIST_DURATION_TICKS / 2) {
            return;
        }
        entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, FIRE_RESIST_DURATION_TICKS, 0, false, false, true));
    }

    private void handleFlight(LivingEntity entity, OrganState state, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player)) {
            return;
        }
        if (player.isSpectator() || player.isCreative()) {
            boolean changed = false;
            var grantChange = state.setBoolean(FLIGHT_GRANTED_KEY, false, false);
            var flyingChange = state.setBoolean(WAS_FLYING_KEY, false, false);
            changed |= grantChange.changed() || flyingChange.changed();
            if (changed) {
                sendSlotUpdate(cc, organ);
            }
            return;
        }
        boolean abilityChanged = false;
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
            abilityChanged = true;
        }
        if (player.getAbilities().flying) {
            player.fallDistance = 0.0f;
        }
        boolean wasFlying = state.getBoolean(WAS_FLYING_KEY, false);
        boolean isFlying = player.getAbilities().flying;
        if (isFlying && !wasFlying) {
            playSound(player.level(), player, SoundEvents.BLAZE_SHOOT, SHOOT_SOUND_VOLUME,
                    1.05f + player.getRandom().nextFloat() * 0.15f);
        }
        var grantChange = state.setBoolean(FLIGHT_GRANTED_KEY, true, false);
        var flyingChange = state.setBoolean(WAS_FLYING_KEY, isFlying, false);
        logStateChange(LOGGER, LOG_PREFIX, organ, FLIGHT_GRANTED_KEY, grantChange);
        logStateChange(LOGGER, LOG_PREFIX, organ, WAS_FLYING_KEY, flyingChange);
        if (abilityChanged || grantChange.changed() || flyingChange.changed()) {
            sendSlotUpdate(cc, organ);
        }
        boolean synergyActive = state.getBoolean(SYNERGY_KEY, false);
        if (isFlying) {
            spawnJetParticles(player, synergyActive);
        }
        playLoopingBurn(player.level(), player, state, synergyActive);
    }

    private void handleSynergyState(LivingEntity entity, ChestCavityInstance cc, ItemStack organ,
                                    OrganState state, boolean active) {
        boolean previous = state.getBoolean(SYNERGY_KEY, false);
        if (previous != active) {
            var change = state.setBoolean(SYNERGY_KEY, active, false);
            logStateChange(LOGGER, LOG_PREFIX, organ, SYNERGY_KEY, change);
            if (change.changed()) {
                sendSlotUpdate(cc, organ);
            }
        }

        double targetIncrease = active ? SYNERGY_INCREASE : 0.0;
        double storedIncrease = state.getDouble(INCREASE_KEY, 0.0);
        if (Math.abs(storedIncrease - targetIncrease) > EPSILON) {
            applyIncreaseDelta(cc, organ, targetIncrease - storedIncrease);
            var change = state.setDouble(INCREASE_KEY, targetIncrease, value -> Math.max(0.0, value), 0.0);
            logStateChange(LOGGER, LOG_PREFIX, organ, INCREASE_KEY, change);
            if (change.changed()) {
                sendSlotUpdate(cc, organ);
            }
        }

        if (!active) {
            return;
        }
        playLoopingBurn(entity.level(), entity, state, true);
        entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, HASTE_DURATION_TICKS, 0, false, false, true));
    }

    private void igniteTarget(LivingEntity attacker, LivingEntity target) {
        if (target.isAlive() && !target.fireImmune()) {
            target.setSecondsOnFire(FIRE_LINGER_SECONDS);
        }
        playSound(attacker.level(), attacker, SoundEvents.BLAZE_SHOOT, SHOOT_SOUND_VOLUME,
                0.95f + attacker.getRandom().nextFloat() * 0.1f);
        if (attacker.level() instanceof ServerLevel server) {
            spawnImpactParticles(server, target);
        }
    }

    private void disableFlight(LivingEntity entity, OrganState state) {
        if (!(entity instanceof Player player)) {
            return;
        }
        boolean granted = state.getBoolean(FLIGHT_GRANTED_KEY, false);
        if (!granted) {
            return;
        }
        if (!player.isCreative() && !player.isSpectator()) {
            if (player.getAbilities().flying) {
                player.getAbilities().flying = false;
            }
            if (player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
            }
            player.onUpdateAbilities();
        }
    }

    private void resetState(OrganState state) {
        state.setBoolean(SYNERGY_KEY, false, false);
        state.setBoolean(FLIGHT_GRANTED_KEY, false, false);
        state.setBoolean(WAS_FLYING_KEY, false, false);
        state.setDouble(INCREASE_KEY, 0.0, value -> Math.max(0.0, value), 0.0);
        state.setLong(NEXT_BURN_SOUND_KEY, 0L, value -> Math.max(0L, value), 0L);
    }

    private void updateSlotIndex(ChestCavityInstance cc, ItemStack organ, OrganState state) {
        int slotIndex = ChestCavityUtil.findOrganSlot(cc, organ);
        int storedSlot = state.getInt(SLOT_KEY, -1);
        if (storedSlot != slotIndex) {
            var change = state.setInt(SLOT_KEY, slotIndex, value -> Math.max(-1, value), -1);
            logStateChange(LOGGER, LOG_PREFIX, organ, SLOT_KEY, change);
            if (change.changed()) {
                sendSlotUpdate(cc, organ);
            }
        }
    }

    private void spawnPassiveParticles(LivingEntity entity, boolean synergyActive) {
        Level level = entity.level();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        RandomSource random = entity.getRandom();
        Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.6, 0.0);
        double radius = Math.max(0.3, entity.getBbWidth() * 0.45);
        for (int i = 0; i < 8; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double yOffset = -0.05 + random.nextDouble() * 0.4;
            spawnParticle(server, ParticleTypes.FLAME, center.x + offsetX, center.y + yOffset, center.z + offsetZ,
                    0.0, 0.0, 0.0, 0.01);
            if (random.nextBoolean()) {
                spawnParticle(server, ParticleTypes.SMALL_FLAME, center.x + offsetX * 0.6,
                        center.y + yOffset * 0.6, center.z + offsetZ * 0.6, 0.0, 0.01, 0.0, 0.005);
            }
            if (i % 3 == 0) {
                spawnParticle(server, ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x + offsetX * 0.5,
                        center.y + yOffset + 0.2, center.z + offsetZ * 0.5, 0.0, 0.02, 0.0, 0.01);
            }
            if (synergyActive && random.nextFloat() < 0.45f) {
                spawnParticle(server, ParticleTypes.LAVA, center.x + offsetX * 0.35,
                        center.y + yOffset * 0.6, center.z + offsetZ * 0.35, 0.0, 0.0, 0.0, 0.0);
                if (random.nextBoolean()) {
                    spawnParticle(server, ParticleTypes.SOUL_FIRE_FLAME, center.x + offsetX * 0.4,
                            center.y + yOffset * 0.6, center.z + offsetZ * 0.4, 0.0, 0.0, 0.0, 0.01);
                }
            }
        }
    }

    private void spawnJetParticles(LivingEntity entity, boolean synergyActive) {
        Level level = entity.level();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        RandomSource random = entity.getRandom();
        Vec3 base = entity.position();
        double spread = Math.max(0.25, entity.getBbWidth() * 0.4);
        double y = base.y + 0.1;
        for (int i = 0; i < 6; i++) {
            double offsetX = (random.nextDouble() - 0.5) * spread;
            double offsetZ = (random.nextDouble() - 0.5) * spread;
            spawnParticle(server, ParticleTypes.FLAME, base.x + offsetX, y, base.z + offsetZ,
                    0.0, -0.18 - random.nextDouble() * 0.1, 0.0, 0.02);
            spawnParticle(server, ParticleTypes.SMALL_FLAME, base.x + offsetX * 0.8, y - 0.05,
                    base.z + offsetZ * 0.8, 0.0, -0.25 - random.nextDouble() * 0.1, 0.0, 0.01);
            if (synergyActive && random.nextFloat() < 0.5f) {
                spawnParticle(server, ParticleTypes.LAVA, base.x + offsetX * 0.6, y - 0.1,
                        base.z + offsetZ * 0.6, 0.0, -0.18, 0.0, 0.0);
            }
        }
    }

    private void spawnImpactParticles(ServerLevel server, LivingEntity target) {
        Vec3 pos = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        for (int i = 0; i < 12; i++) {
            double angle = i * (Mth.TWO_PI / 12.0);
            double radius = 0.4 + target.getRandom().nextDouble() * 0.2;
            double x = pos.x + Math.cos(angle) * radius;
            double z = pos.z + Math.sin(angle) * radius;
            spawnParticle(server, ParticleTypes.FLAME, x, pos.y, z, 0.0, 0.02, 0.0, 0.02);
            spawnParticle(server, ParticleTypes.SMALL_FLAME, x, pos.y + 0.1, z, 0.0, 0.01, 0.0, 0.01);
            if (target.getRandom().nextBoolean()) {
                spawnParticle(server, ParticleTypes.SOUL_FIRE_FLAME, x, pos.y + 0.05, z, 0.0, 0.02, 0.0, 0.01);
            }
        }
    }

    private void spawnParticle(ServerLevel server, ParticleOptions particle, double x, double y, double z,
                               double vx, double vy, double vz, double speed) {
        server.sendParticles(particle, x, y, z, 1, vx, vy, vz, speed);
    }

    private void playLoopingBurn(Level level, LivingEntity entity, OrganState state, boolean synergyActive) {
        if (level == null) {
            return;
        }
        long gameTime = level.getGameTime();
        long nextAllowed = state.getLong(NEXT_BURN_SOUND_KEY, 0L);
        if (gameTime < nextAllowed) {
            return;
        }
        SoundEvent sound = synergyActive ? SoundEvents.BLAZE_BURN : SoundEvents.FIRE_AMBIENT;
        float volume = synergyActive ? BURN_SOUND_VOLUME_BASE : 0.18f;
        float pitch = synergyActive ? 0.9f + entity.getRandom().nextFloat() * 0.1f
                : 0.95f + entity.getRandom().nextFloat() * 0.1f;
        playSound(level, entity, sound, volume, pitch);
        state.setLong(NEXT_BURN_SOUND_KEY, gameTime + BURN_SOUND_INTERVAL_TICKS, value -> Math.max(0L, value), 0L);
    }

    private void playSound(Level level, LivingEntity entity, SoundEvent sound, float volume, float pitch) {
        if (level == null || sound == null) {
            return;
        }
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private void applyIncreaseDelta(ChestCavityInstance cc, ItemStack organ, double delta) {
        if (cc == null || Math.abs(delta) <= EPSILON) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        LinkageChannel channel = ensureIncreaseChannel(context);
        if (channel != null) {
            channel.adjust(delta);
        }
        context.increaseEffects().adjust(organ, YAN_DAO_INCREASE_CHANNEL, delta);
    }

    private LinkageChannel ensureIncreaseChannel(ActiveLinkageContext context) {
        if (context == null) {
            return null;
        }
        LinkageChannel channel = ensureChannel(context, YAN_DAO_INCREASE_CHANNEL);
        if (channel != null) {
            channel.addPolicy(NON_NEGATIVE);
        }
        return channel;
    }

    private static boolean isPrimaryHuorengu(ChestCavityInstance cc, ItemStack organ) {
        if (cc == null || cc.inventory == null || organ == null || organ.isEmpty()) {
            return false;
        }
        Item expectedItem = organ.getItem();
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (stack == organ) {
                return true;
            }
            Item stackItem = stack.getItem();
            if (stackItem == expectedItem) {
                return false;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stackItem);
            if (Objects.equals(id, ORGAN_ID)) {
                return false;
            }
        }
        return false;
    }
}
