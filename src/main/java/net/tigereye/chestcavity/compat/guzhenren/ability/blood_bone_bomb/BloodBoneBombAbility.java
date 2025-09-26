package net.tigereye.chestcavity.compat.guzhenren.ability.blood_bone_bomb;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.ClampPolicy;
import org.joml.Vector3f;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Runtime implementation of the Bloodbone Bomb active organ ability.
 * Handles the 10 second channel, resource payments and spawns the
 * dedicated projectile entity on completion.
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class BloodBoneBombAbility {

    private static final ResourceLocation TIE_XUE_GU = ResourceLocation.fromNamespaceAndPath("guzhenren", "tiexuegu");
    private static final ResourceLocation XIE_DI_GU = ResourceLocation.fromNamespaceAndPath("guzhenren", "xie_di_gu");
    private static final ResourceLocation XIE_YAN_GU = ResourceLocation.fromNamespaceAndPath("guzhenren", "xie_yan_gu");
    private static final ResourceLocation GU_ZHU_GU = ResourceLocation.fromNamespaceAndPath("guzhenren", "gu_zhu_gu");
    private static final ResourceLocation LUO_XUAN_GU = ResourceLocation.fromNamespaceAndPath("guzhenren", "luo_xuan_gu_qiang_gu");

    private static final ImmutableSet<ResourceLocation> REQUIRED_ORGANS = ImmutableSet.of(
            TIE_XUE_GU,
            XIE_DI_GU,
            XIE_YAN_GU,
            GU_ZHU_GU,
            LUO_XUAN_GU
    );

    private static final ResourceLocation LI_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath("guzhenren", "linkage/li_dao_increase_effect");
    private static final ResourceLocation XUE_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath("guzhenren", "linkage/xue_dao_increase_effect");
    private static final ResourceLocation GU_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath("guzhenren", "linkage/gu_dao_increase_effect");
    static final ResourceLocation BLEED_EFFECT_ID = ResourceLocation.fromNamespaceAndPath("guzhenren", "lliuxue");
    static final ResourceLocation RENDER_ITEM_ID = ResourceLocation.fromNamespaceAndPath("guzhenren", "gu_qiang");

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final int CHARGE_DURATION_TICKS = 200;
    private static final int COST_INTERVAL_TICKS = 20;
    private static final float HEALTH_COST = 2.0f;
    private static final double ZHENYUAN_COST = 20.0;
    private static final double JINGLI_COST = 10.0;

    private static final double PROJECTILE_SPEED = 3.75;

    private static final DustParticleOptions BLOOD_SWIRL =
            new DustParticleOptions(new Vector3f(0.82f, 0.07f, 0.09f), 1.0f);
    private static final DustParticleOptions BONE_GLINT =
            new DustParticleOptions(new Vector3f(0.92f, 0.88f, 0.78f), 0.8f);

    private static final Map<UUID, ChargeState> ACTIVE_CHARGES = new java.util.concurrent.ConcurrentHashMap<>();

    private BloodBoneBombAbility() {
    }

    /** Attempts to begin the Bloodbone Bomb charge sequence. */
    public static void tryActivate(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        if (entity.level().isClientSide()) {
            return;
        }
        if (!entity.isAlive() || player.isSpectator()) {
            return;
        }
        if (GuzhenrenResourceBridge.open(player).isEmpty()) {
            return;
        }
        if (ACTIVE_CHARGES.containsKey(player.getUUID())) {
            return;
        }
        if (!hasRequiredOrgans(cc)) {
            return;
        }
        ChargeState state = new ChargeState(cc);
        ACTIVE_CHARGES.put(player.getUUID(), state);
        applyChannelImmobilise(player);
        playChargeStartCue(player);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ChargeState state = ACTIVE_CHARGES.get(player.getUUID());
        if (state != null) {
            tickCharge(player, state);
        }
    }

    private static void tickCharge(ServerPlayer player, ChargeState state) {
        if (!player.isAlive()) {
            ACTIVE_CHARGES.remove(player.getUUID());
            return;
        }

        maintainImmobility(player);
        state.ticksElapsed++;
        state.ticksRemaining = Math.max(0, state.ticksRemaining - 1);
        state.costTimer = Math.max(0, state.costTimer - 1);

        spawnChargingParticles(player, state);

        if (state.costTimer == 0) {
            state.costTimer = COST_INTERVAL_TICKS;
            if (!payChargeCosts(player, state)) {
                triggerCatastrophicFailure(player);
                return;
            }
            state.soundStep++;
            playHeartbeatCue(player, state);
        }

        if (state.ticksRemaining <= 0) {
            ACTIVE_CHARGES.remove(player.getUUID());
            releaseImmobilise(player);
            launchProjectile(player, state);
        }
    }

    private static boolean payChargeCosts(ServerPlayer player, ChargeState state) {
        if (player.getHealth() + player.getAbsorptionAmount() <= HEALTH_COST) {
            return false;
        }
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return false;
        }
        GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();

        OptionalDouble zhenBeforeOpt = handle.getZhenyuan();
        OptionalDouble jingliBeforeOpt = handle.getJingli();
        if (zhenBeforeOpt.isEmpty() || jingliBeforeOpt.isEmpty()) {
            return false;
        }
        double zhenBefore = zhenBeforeOpt.getAsDouble();
        double jingliBefore = jingliBeforeOpt.getAsDouble();
        if (jingliBefore + 1.0E-6 < JINGLI_COST) {
            return false;
        }

        OptionalDouble zhenAfterOpt = handle.consumeScaledZhenyuan(ZHENYUAN_COST);
        if (zhenAfterOpt.isEmpty()) {
            return false;
        }
        double zhenAfter = zhenAfterOpt.getAsDouble();
        double consumedZhenyuan = zhenBefore - zhenAfter;

        OptionalDouble jingliAfterOpt = handle.adjustJingli(-JINGLI_COST, true);
        if (jingliAfterOpt.isEmpty()) {
            handle.adjustZhenyuan(consumedZhenyuan, true);
            return false;
        }
        double jingliAfter = jingliAfterOpt.getAsDouble();
        if ((jingliBefore - jingliAfter) + 1.0E-5 < JINGLI_COST) {
            handle.adjustZhenyuan(consumedZhenyuan, true);
            handle.setJingli(jingliBefore);
            return false;
        }

        if (!drainHealth(player, HEALTH_COST)) {
            handle.adjustZhenyuan(consumedZhenyuan, true);
            handle.setJingli(jingliBefore);
            return false;
        }

        return true;
    }

    private static boolean drainHealth(ServerPlayer player, float amount) {
        if (amount <= 0.0f) {
            return true;
        }
        float startingHealth = player.getHealth();
        float startingAbsorption = player.getAbsorptionAmount();
        if (startingHealth + startingAbsorption <= amount) {
            return false;
        }
        player.invulnerableTime = 0;
        DamageSource source = player.damageSources().generic();
        player.hurt(source, amount);
        player.invulnerableTime = 0;

        float remaining = amount;
        float absorptionConsumed = Math.min(startingAbsorption, remaining);
        remaining -= absorptionConsumed;
        player.setAbsorptionAmount(Math.max(0.0f, startingAbsorption - absorptionConsumed));
        if (remaining > 0.0f && player.getHealth() > startingHealth - remaining) {
            player.setHealth(Math.max(0.0f, startingHealth - remaining));
        }
        player.hurtTime = 0;
        player.hurtDuration = 0;
        return true;
    }

    private static void launchProjectile(ServerPlayer player, ChargeState state) {
        ServerLevel level = player.serverLevel();
        ActiveLinkageContext context = GuzhenrenLinkageManager.getContext(state.chestCavity);
        double liIncrease = ensureChannel(context, LI_DAO_INCREASE_EFFECT).get();
        double xueIncrease = ensureChannel(context, XUE_DAO_INCREASE_EFFECT).get();
        double guIncrease = ensureChannel(context, GU_DAO_INCREASE_EFFECT).get();
        double multiplier = Math.max(0.0, (1.0 + liIncrease) * (1.0 + xueIncrease) * (1.0 + guIncrease));
        double damage = 80.0 * multiplier;

        Vec3 origin = player.getEyePosition().add(player.getLookAngle().scale(0.4));
        Vec3 velocity = player.getLookAngle().normalize().scale(PROJECTILE_SPEED);

        BoneGunProjectile projectile = new BoneGunProjectile(level, player, createProjectileStack());
        projectile.configurePayload((float) damage, multiplier);
        projectile.setPos(origin);
        projectile.shoot(velocity.x, velocity.y, velocity.z, (float) PROJECTILE_SPEED, 0.0f);
        level.addFreshEntity(projectile);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.1f, 0.7f + player.getRandom().nextFloat() * 0.2f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.6f, 0.5f);
        level.gameEvent(player, GameEvent.EXPLODE, player.blockPosition());
        spawnProjectileIgnition(level, origin, velocity);
    }

    private static ItemStack createProjectileStack() {
        return BuiltInRegistries.ITEM.getOptional(RENDER_ITEM_ID)
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }

    private static LinkageChannel ensureChannel(ActiveLinkageContext context, ResourceLocation id) {
        return context.getOrCreateChannel(id).addPolicy(NON_NEGATIVE);
    }

    private static void triggerCatastrophicFailure(ServerPlayer player) {
        ACTIVE_CHARGES.remove(player.getUUID());
        releaseImmobilise(player);
        ServerLevel level = player.serverLevel();
        Vec3 pos = player.position();
        level.explode(player, pos.x, pos.y, pos.z, 4.0f, Level.ExplosionInteraction.MOB);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 0.6f);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GHAST_SCREAM, SoundSource.PLAYERS, 1.0f, 0.5f);
        level.sendParticles(ParticleTypes.CRIMSON_SPORE, pos.x, pos.y + player.getBbHeight() * 0.5, pos.z, 120, 0.6, 0.5, 0.6, 0.1);
        applyTrueDamage(player, player, 50.0f);
    }

    private static void spawnProjectileIgnition(ServerLevel level, Vec3 origin, Vec3 velocity) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < 30; i++) {
            double progress = i / 30.0;
            Vec3 point = origin.add(velocity.scale(progress * 0.2));
            level.sendParticles(BONE_GLINT, point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0.01);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, origin.x, origin.y, origin.z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    private static void spawnChargingParticles(ServerPlayer player, ChargeState state) {
        ServerLevel level = player.serverLevel();
        double centerY = player.getY() + player.getBbHeight() * 0.5;
        double radius = 0.7 + 0.25 * Math.sin(state.ticksElapsed / 6.0);
        int strands = 6;
        for (int i = 0; i < strands; i++) {
            double angle = (state.ticksElapsed * 0.25) + (i * (Math.PI * 2.0 / strands));
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            level.sendParticles(BLOOD_SWIRL, x, centerY, z, 1, 0.0, 0.05, 0.0, 0.0);
        }
        level.sendParticles(BONE_GLINT, player.getX(), centerY - 0.3, player.getZ(), 2, 0.25, 0.15, 0.25, 0.02);
    }

    private static void playChargeStartCue(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.8f, 0.55f);
        level.sendParticles(ParticleTypes.CRIMSON_SPORE, player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(), 20, 0.3, 0.3, 0.3, 0.05);
    }

    private static void playHeartbeatCue(ServerPlayer player, ChargeState state) {
        float pitch = 0.6f + (state.soundStep / 10.0f);
        float volume = 0.6f + (state.soundStep / 12.0f);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, volume, pitch);
    }

    private static void applyChannelImmobilise(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CHARGE_DURATION_TICKS + 40, 255, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, CHARGE_DURATION_TICKS + 40, 4, false, false, true));
        player.setDeltaMovement(Vec3.ZERO);
        player.getAbilities().flying = false;
        player.hurtMarked = true;
    }

    private static void maintainImmobility(ServerPlayer player) {
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0f;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, 255, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 6, 4, false, false, true));
    }

    private static void releaseImmobilise(ServerPlayer player) {
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
    }

    private static boolean hasRequiredOrgans(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return false;
        }
        java.util.Set<ResourceLocation> found = new java.util.HashSet<>();
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id != null && REQUIRED_ORGANS.contains(id)) {
                found.add(id);
                if (found.size() == REQUIRED_ORGANS.size()) {
                    return true;
                }
            }
        }
        return false;
    }

    static void applyTrueDamage(ServerPlayer source, LivingEntity target, float amount) {
        if (target == null || amount <= 0.0f) {
            return;
        }
        float startingHealth = target.getHealth();
        float startingAbsorption = target.getAbsorptionAmount();
        target.invulnerableTime = 0;
        DamageSource damageSource = source == null
                ? target.damageSources().magic()
                : target.damageSources().playerAttack(source);
        target.hurt(damageSource, amount);
        target.invulnerableTime = 0;

        float remaining = amount;
        float absorptionConsumed = Math.min(startingAbsorption, remaining);
        remaining -= absorptionConsumed;
        target.setAbsorptionAmount(Math.max(0.0f, startingAbsorption - absorptionConsumed));

        if (!target.isDeadOrDying() && remaining > 0.0f) {
            float expectedHealth = Math.max(0.0f, startingHealth - remaining);
            if (target.getHealth() > expectedHealth) {
                target.setHealth(expectedHealth);
            }
        }
        target.hurtTime = 0;
    }

    private static final class ChargeState {
        private final ChestCavityInstance chestCavity;
        private int ticksElapsed;
        private int ticksRemaining = CHARGE_DURATION_TICKS;
        private int costTimer = COST_INTERVAL_TICKS;
        private int soundStep;

        private ChargeState(ChestCavityInstance chestCavity) {
            this.chestCavity = chestCavity;
        }
    }


}
