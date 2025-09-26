package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.SingleSwordProjectile;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.compat.guzhenren.util.PlayerSkinUtil;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Behaviour for 剑影蛊. Handles passive shadow strikes, afterimages, and the sword clone ability.
 */
public enum JianYingGuOrganBehavior implements OrganOnHitListener {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "jian_ying_gu");
    public static final ResourceLocation ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "jian_ying_fenshen");

    private static final ResourceLocation JIAN_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/jian_dao_increase_effect");

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final double COST_ZHENYUAN = 2000.0;
    private static final double PASSIVE_COST_RATIO = 0.10;
    private static final double PASSIVE_ZHENYUAN_COST = COST_ZHENYUAN * PASSIVE_COST_RATIO;
    private static final double ACTIVE_ZHENYUAN_MULTIPLIER = 2.0;
    private static final double ACTIVE_JINGLI_COST = 50.0;

    private static final float BASE_DAMAGE = 100.0f;
    private static final float PASSIVE_INITIAL_MULTIPLIER = 0.40f;
    private static final float PASSIVE_MIN_MULTIPLIER = 0.15f;
    private static final float PASSIVE_DECAY_STEP = 0.05f;
    private static final long PASSIVE_RESET_WINDOW = 40L;

    private static final float CLONE_DAMAGE_RATIO = 0.25f;
    private static final int CLONE_DURATION_TICKS = 100;
    private static final int CLONE_COOLDOWN_TICKS = 400;

    private static final double AFTERIMAGE_CHANCE = 0.10;
    private static final int AFTERIMAGE_DELAY_TICKS = 20;
    private static final double AFTERIMAGE_DAMAGE_RATIO = 0.20;
    private static final double AFTERIMAGE_RADIUS = 3.0;

    private static final int SLOW_DURATION_TICKS = 20;
    private static final int SLOW_AMPLIFIER = 2; // Level III (0-indexed)

    private static final Map<UUID, SwordShadowState> SWORD_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, CloneState> CLONE_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();
    private static final List<AfterimageTask> AFTERIMAGES = Collections.synchronizedList(new ArrayList<>());

    static {
        OrganActivationListeners.register(ABILITY_ID, JianYingGuOrganBehavior::activateAbility);
    }

    @Override
    public float onHit(
            DamageSource source,
            LivingEntity attacker,
            LivingEntity target,
            ChestCavityInstance cc,
            ItemStack organ,
            float damage
    ) {
        if (!(attacker instanceof Player player) || attacker.level().isClientSide()) {
            return damage;
        }
        if (source == null || source.is(DamageTypeTags.IS_PROJECTILE)) {
            return damage;
        }
        if (target == null || !target.isAlive()) {
            return damage;
        }
        if (!isMeleeAttack(source)) {
            return damage;
        }

        double efficiency = 1.0 + ensureChannel(cc, JIAN_DAO_INCREASE_EFFECT).get();

        double passiveDamage = triggerSwordShadow(player, target, efficiency);
        if (passiveDamage > 0.0) {
            applyTrueDamage(player, target, (float) passiveDamage);
        }

        double cloneDamage = triggerClones(player, target, efficiency);
        if (cloneDamage > 0.0) {
            applyTrueDamage(player, target, (float) cloneDamage);
            applySlow(target);
        }

        trySpawnAfterimage(player, target, source);

        return damage;
    }

    public void ensureAttached(ChestCavityInstance cc) {
        ensureChannel(cc, JIAN_DAO_INCREASE_EFFECT);
    }

    public void tickLevel(ServerLevel level) {
        long gameTime = level.getGameTime();
        List<AfterimageTask> pending;
        synchronized (AFTERIMAGES) {
            pending = new ArrayList<>(AFTERIMAGES);
        }
        for (AfterimageTask task : pending) {
            if (!task.level().equals(level.dimension())) {
                continue;
            }
            if (task.executeTick() > gameTime) {
                continue;
            }
            if (executeAfterimage(level, task)) {
                AFTERIMAGES.remove(task);
            }
        }

        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, CloneState> entry : CLONE_STATES.entrySet()) {
            CloneState state = entry.getValue();
            if (state.expiresAt >= gameTime) {
                continue;
            }
            Player player = level.getPlayerByUUID(entry.getKey());
            if (player != null) {
                playCloneVanishEffects(level, player);
            }
            expired.add(entry.getKey());
        }
        expired.forEach(CLONE_STATES::remove);
    }

    private static void playCloneVanishEffects(ServerLevel level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.6f, 0.9f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.4f, 1.4f);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY(0.5), player.getZ(), 10, 0.4, 0.4, 0.4, 0.02);
        level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY(0.5), player.getZ(), 12, 0.4, 0.6, 0.4, 0.2);
    }

    private static boolean executeAfterimage(ServerLevel level, AfterimageTask task) {
        Player player = level.getPlayerByUUID(task.playerId());
        if (player == null) {
            return true;
        }

        ChestCavityInstance cc = ChestCavityEntity.of(player)
                .map(ChestCavityEntity::getChestCavityInstance)
                .orElse(null);
        double efficiency = 1.0;
        if (cc != null) {
            LinkageChannel channel = ensureChannel(cc, JIAN_DAO_INCREASE_EFFECT);
            efficiency += channel.get();
        }

        Vec3 centre = task.origin();
        AABB area = new AABB(centre, centre).inflate(AFTERIMAGE_RADIUS);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area, entity ->
                entity.isAlive() && entity != player && !entity.isAlliedTo(player));
        if (victims.isEmpty()) {
            return true;
        }

        float damage = (float) (BASE_DAMAGE * AFTERIMAGE_DAMAGE_RATIO * efficiency);
        for (LivingEntity victim : victims) {
            applyTrueDamage(player, victim, damage);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, victim.getX(), victim.getY(0.5), victim.getZ(), 2, 0.1, 0.1, 0.1, 0.01);
        }
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 0.7f);

        return true;
    }

    private static void trySpawnAfterimage(Player player, LivingEntity target, DamageSource source) {
        if (player == null || target == null) {
            return;
        }
        if (!isCritical(player, source)) {
            return;
        }
        if (player.getRandom().nextDouble() >= AFTERIMAGE_CHANCE) {
            return;
        }
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        Vec3 origin = target.position();
        PlayerSkinUtil.SkinSnapshot snapshot = PlayerSkinUtil.capture(player);
        PlayerSkinUtil.SkinSnapshot tinted = PlayerSkinUtil.withTint(snapshot, 0.1f, 0.05f, 0.2f, 0.45f);
        SingleSwordProjectile.spawn(level, player, origin.add(0, target.getBbHeight() * 0.5, 0), origin, tinted);
        AFTERIMAGES.add(new AfterimageTask(player.getUUID(), level.dimension(), level.getGameTime() + AFTERIMAGE_DELAY_TICKS, origin));
    }

    private static double triggerSwordShadow(Player player, LivingEntity target, double efficiency) {
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return 0.0;
        }
        GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
        if (handle.consumeScaledZhenyuan(PASSIVE_ZHENYUAN_COST).isEmpty()) {
            return 0.0;
        }

        long now = player.level().getGameTime();
        SwordShadowState state = SWORD_STATES.computeIfAbsent(player.getUUID(), unused -> new SwordShadowState());
        float multiplier;
        if (now - state.lastTriggerTick > PASSIVE_RESET_WINDOW) {
            multiplier = PASSIVE_INITIAL_MULTIPLIER;
        } else {
            multiplier = Math.max(PASSIVE_MIN_MULTIPLIER, state.lastMultiplier - PASSIVE_DECAY_STEP);
        }
        state.lastTriggerTick = now;
        state.lastMultiplier = multiplier;

        PlayerSkinUtil.SkinSnapshot tint = PlayerSkinUtil.withTint(PlayerSkinUtil.capture(player), 0.12f, 0.05f, 0.22f, 0.6f);
        SingleSwordProjectile.spawn(player.level(), player, player.position().add(0, player.getBbHeight() * 0.7, 0), target.position().add(0, target.getBbHeight() * 0.5, 0), tint);

        return BASE_DAMAGE * multiplier * efficiency;
    }

    private static double triggerClones(Player player, LivingEntity target, double efficiency) {
        CloneState state = CLONE_STATES.get(player.getUUID());
        if (state == null) {
            return 0.0;
        }
        if (player.level().getGameTime() > state.expiresAt) {
            CLONE_STATES.remove(player.getUUID());
            return 0.0;
        }
        if (state.count <= 0) {
            return 0.0;
        }
        double damagePerClone = BASE_DAMAGE * CLONE_DAMAGE_RATIO * efficiency;
        Level level = player.level();
        PlayerSkinUtil.SkinSnapshot tint = state.tint;
        for (int i = 0; i < state.count; i++) {
            Vec3 offset = randomOffset(player.getRandom());
            Vec3 origin = player.position().add(offset);
            SingleSwordProjectile.spawn(level, player, origin, target.position().add(0, target.getBbHeight() * 0.5, 0), tint);
        }
        return damagePerClone * state.count;
    }

    private static Vec3 randomOffset(RandomSource random) {
        double radius = 1.2 + random.nextDouble() * 0.4;
        double angle = random.nextDouble() * Math.PI * 2.0;
        return new Vec3(Math.cos(angle) * radius, 0.2, Math.sin(angle) * radius);
    }

    private static void applySlow(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_DURATION_TICKS, SLOW_AMPLIFIER, false, true, true));
    }

    private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }
        if (!hasOrgan(cc)) {
            return;
        }
        long now = entity.level().getGameTime();
        long last = COOLDOWNS.getOrDefault(player.getUUID(), Long.MIN_VALUE);
        if (now - last < CLONE_COOLDOWN_TICKS) {
            return;
        }

        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return;
        }
        GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();

        OptionalDouble jingliBeforeOpt = handle.getJingli();
        if (jingliBeforeOpt.isEmpty() || jingliBeforeOpt.getAsDouble() < ACTIVE_JINGLI_COST) {
            return;
        }
        double jingliBefore = jingliBeforeOpt.getAsDouble();
        if (handle.adjustJingli(-ACTIVE_JINGLI_COST, true).isEmpty()) {
            return;
        }
        if (handle.consumeScaledZhenyuan(COST_ZHENYUAN * ACTIVE_ZHENYUAN_MULTIPLIER).isEmpty()) {
            handle.setJingli(jingliBefore);
            return;
        }

        int clones = 2 + player.getRandom().nextInt(2);
        CloneState state = new CloneState();
        state.count = clones;
        state.expiresAt = now + CLONE_DURATION_TICKS;
        state.tint = PlayerSkinUtil.withTint(PlayerSkinUtil.capture(player), 0.05f, 0.05f, 0.1f, 0.55f);
        CLONE_STATES.put(player.getUUID(), state);
        COOLDOWNS.put(player.getUUID(), now);

        Level level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 0.8f, 0.6f);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY(0.5), player.getZ(), 30, 0.4, 0.6, 0.4, 0.2);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY(0.4), player.getZ(), 20, 0.35, 0.35, 0.35, 0.01);
        }
    }

    private static boolean hasOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return false;
        }
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (ORGAN_ID.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static LinkageChannel ensureChannel(ChestCavityInstance cc, ResourceLocation id) {
        ActiveLinkageContext context = GuzhenrenLinkageManager.getContext(cc);
        return context.getOrCreateChannel(id).addPolicy(NON_NEGATIVE);
    }

    private static boolean isMeleeAttack(DamageSource source) {
        return !source.is(DamageTypeTags.IS_PROJECTILE);
    }

    private static boolean isCritical(Player player, DamageSource source) {
        if (player == null) {
            return false;
        }
        if (source != null && source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return false;
        }
        boolean airborne = !player.onGround() && !player.onClimbable() && !player.isInWaterOrBubble();
        boolean strongSwing = player.getAttackStrengthScale(0.5f) > 0.9f;
        boolean descending = player.getDeltaMovement().y < 0.0;
        return airborne && strongSwing && descending && !player.isSprinting();
    }

    private static void applyTrueDamage(Player player, LivingEntity target, float amount) {
        if (target == null || amount <= 0.0f) {
            return;
        }
        float startHealth = target.getHealth();
        float startAbsorption = target.getAbsorptionAmount();
        target.invulnerableTime = 0;
        DamageSource source = player instanceof ServerPlayer serverPlayer
                ? target.damageSources().playerAttack(serverPlayer)
                : target.damageSources().magic();
        target.hurt(source, amount);
        target.invulnerableTime = 0;

        float remaining = amount;
        float absorbed = Math.min(startAbsorption, remaining);
        remaining -= absorbed;
        target.setAbsorptionAmount(Math.max(0.0f, startAbsorption - absorbed));

        if (!target.isDeadOrDying() && remaining > 0.0f) {
            float expected = Math.max(0.0f, startHealth - remaining);
            if (target.getHealth() > expected) {
                target.setHealth(expected);
            }
        }
        target.hurtTime = 0;
    }

    private static final class SwordShadowState {
        private long lastTriggerTick;
        private float lastMultiplier = PASSIVE_INITIAL_MULTIPLIER;
    }

    private static final class CloneState {
        private int count;
        private long expiresAt;
        private PlayerSkinUtil.SkinSnapshot tint;
    }

    private record AfterimageTask(
            UUID playerId,
            ResourceKey<Level> level,
            long executeTick,
            Vec3 origin
    ) {
    }
}

