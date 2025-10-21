package net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.behavior;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.DamageTypeTags;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.AttributeOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TeleportOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NetworkUtil;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 清风轮蛊（风道·腿部）— 位移与风势管理。
 */
public final class QingFengLunOrganBehavior extends AbstractGuzhenrenOrganBehavior
        implements OrganSlowTickListener, OrganIncomingDamageListener, OrganOnHitListener, OrganRemovalListener {

    public static final QingFengLunOrganBehavior INSTANCE = new QingFengLunOrganBehavior();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "qing_feng_lun_gu");

    public static final ResourceLocation DASH_ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "qing_feng_lun_gu/dash");
    public static final ResourceLocation WIND_SLASH_ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "qing_feng_lun_gu/wind_slash");
    public static final ResourceLocation WIND_DOMAIN_ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "qing_feng_lun_gu/wind_domain");

    private static final ResourceLocation WIND_STACKS_CHANNEL = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/qingfenglun/wind_stacks");
    private static final ResourceLocation WIND_RING_CD_CHANNEL = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/qingfenglun/windring_cd");

    private static final ResourceLocation WIND_WALK_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifiers/qing_feng_lun/wind_walk");
    private static final ResourceLocation WIND_STACKS_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifiers/qing_feng_lun/wind_stacks");
    private static final ResourceLocation DOMAIN_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifiers/qing_feng_lun/wind_domain");

    private static final String STATE_ROOT = "qing_feng_lun";

    private static final String KEY_STAGE = "stage";
    private static final String KEY_RUN_M = "run_m";
    private static final String KEY_DASH_USED = "dash_used";
    private static final String KEY_DASH_HIT = "dash_hit";
    private static final String KEY_NEAR_MISS = "near_miss";
    private static final String KEY_AIR_TIME = "air_time";
    private static final String KEY_RING_BLOCK = "ring_block";
    private static final String KEY_STACK10_HOLD = "stack10_hold";
    private static final String KEY_GLIDE_CHAIN = "glide_chain";
    private static final String KEY_WIND_STACKS = "wind_stacks";
    private static final String KEY_LAST_MOVE_TICK = "last_move_tick";
    private static final String KEY_STACK10_START = "stack10_start";
    private static final String KEY_GLIDE_TICKS = "glide_ticks";
    private static final String KEY_DASH_WINDOW_UNTIL = "dash_window_until";
    private static final String KEY_LAST_DASH_TICK = "last_dash_tick";
    private static final String KEY_LAST_DASH_DIR_X = "last_dash_dir_x";
    private static final String KEY_LAST_DASH_DIR_Y = "last_dash_dir_y";
    private static final String KEY_LAST_DASH_DIR_Z = "last_dash_dir_z";
    private static final String KEY_SPRINT_SECONDS = "sprint_seconds";
    private static final String KEY_WIND_WALK_UNTIL = "wind_walk_until";
    private static final String KEY_PASSIVE_READY = "passive_ready";
    private static final String KEY_DOMAIN_ACTIVE_UNTIL = "domain_active_until";

    private static final String CD_KEY_DASH_READY = "cd_dash_ready";
    private static final String CD_KEY_DOMAIN_READY = "cd_domain_ready";
    private static final String CD_KEY_WIND_RING_READY = "cd_wind_ring_ready";
    private static final String CD_KEY_DEDUP_DASH_USE = "dedup_dash_use";
    private static final String CD_KEY_DEDUP_DASH_HIT = "dedup_dash_hit";
    private static final String CD_KEY_DEDUP_NEAR_MISS = "dedup_near_miss";
    private static final String CD_KEY_DEDUP_RING_BLOCK = "dedup_ring_block";

    private static final double DASH_DISTANCE = 6.0D;
    private static final int DASH_COOLDOWN_TICKS = 6 * 20;
    private static final int DASH_WINDOW_TICKS = 5;
    private static final double DASH_COST = 2200.0D;
    private static final double WIND_SLASH_COST = 38000.0D;
    private static final int WIND_SLASH_RANGE = 5;
    private static final double WIND_SLASH_DAMAGE = 6.0D;

    private static final double DOMAIN_START_COST = 120000.0D;
    private static final double DOMAIN_MAINTAIN_COST_PER_SECOND = 20000.0D;
    private static final int DOMAIN_DURATION_TICKS = 10 * 20;
    private static final int DOMAIN_COOLDOWN_TICKS = 45 * 20;
    private static final double DOMAIN_PARTICLE_RADIUS = 4.0D;

    private static final int PASSIVE_INTERVAL_TICKS = 4 * 20;
    private static final double PASSIVE_COST = 60.0D;

    private static final int WIND_RING_COOLDOWN_TICKS = 8 * 20;
    private static final double WIND_RING_PUSH = 0.45D;

    private static final double RUN_SAMPLE_THRESHOLD = 0.2D;
    private static final double MOVE_EPSILON = 1.0E-3D;

    private static final int RUN_THRESHOLD = 3000;
    private static final int DASH_USED_THRESHOLD = 50;
    private static final int DASH_HIT_THRESHOLD = 50;
    private static final int NEAR_MISS_THRESHOLD = 30;
    private static final long AIR_TIME_THRESHOLD_MS = 300_000L;
    private static final int RING_BLOCK_THRESHOLD = 25;
    private static final int STACK10_HOLD_THRESHOLD = 12;
    private static final int GLIDE_CHAIN_THRESHOLD = 10;

    private static final double WIND_STACK_SPEED_BONUS_PER_STACK = 0.02D;
    private static final double WIND_STACK_DODGE_PER_STACK = 0.01D;
    private static final double BASE_DODGE_STAGE3 = 0.10D;

    private static final ClampPolicy WIND_STACK_CLAMP = new ClampPolicy(0.0D, 10.0D);
    private static final ClampPolicy WIND_RING_CD_CLAMP = new ClampPolicy(0.0D, Double.MAX_VALUE);

    private static final Object2ObjectMap<UUID, Vec3> LAST_POSITION = new Object2ObjectOpenHashMap<>();
    private static final Object2LongMap<UUID> NEAR_MISS_CACHE = new Object2LongOpenHashMap<>();

    private static final long NEAR_MISS_DEDUP_TICKS = 10L;

    static {
        OrganActivationListeners.register(DASH_ABILITY_ID, QingFengLunOrganBehavior::activateDash);
        OrganActivationListeners.register(WIND_SLASH_ABILITY_ID, QingFengLunOrganBehavior::activateWindSlash);
        OrganActivationListeners.register(WIND_DOMAIN_ABILITY_ID, QingFengLunOrganBehavior::activateWindDomain);
    }

    private QingFengLunOrganBehavior() {
    }

    public void ensureAttached(ChestCavityInstance cc) {
        ActiveLinkageContext context = LedgerOps.context(cc);
        LedgerOps.ensureChannel(context, WIND_STACKS_CHANNEL, WIND_STACK_CLAMP);
        LedgerOps.ensureChannel(context, WIND_RING_CD_CHANNEL, WIND_RING_CD_CLAMP);
    }

    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        ensureAttached(cc);
        registerRemovalHook(cc, organ, this, staleRemovalContexts);
        sendSlotUpdate(cc, organ);
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || organ == null || organ.isEmpty()) {
            return;
        }
        clearAttributeModifiers(player, true);
        OrganState state = organState(organ, STATE_ROOT);
        OrganStateOps.setInt(state, cc, organ, KEY_STAGE, 1, value -> Mth.clamp(value, 1, 5), 1);
        OrganStateOps.setInt(state, cc, organ, KEY_WIND_STACKS, 0, value -> Mth.clamp(value, 0, 10), 0);
        OrganStateOps.setLong(state, cc, organ, KEY_WIND_WALK_UNTIL, 0L, value -> Math.max(0L, value), 0L);
        OrganStateOps.setLong(state, cc, organ, KEY_DOMAIN_ACTIVE_UNTIL, 0L, value -> Math.max(0L, value), 0L);
        OrganStateOps.setLong(state, cc, organ, KEY_PASSIVE_READY, 0L, value -> Math.max(0L, value), 0L);
        ActiveLinkageContext linkage = LedgerOps.context(cc);
        if (linkage != null) {
            LinkageChannel stacksChannel = LedgerOps.ensureChannel(linkage, WIND_STACKS_CHANNEL, WIND_STACK_CLAMP);
            if (stacksChannel != null) {
                stacksChannel.set(0.0D);
            }
            LinkageChannel cdChannel = LedgerOps.ensureChannel(linkage, WIND_RING_CD_CHANNEL, WIND_RING_CD_CLAMP);
            if (cdChannel != null) {
                cdChannel.set(0.0D);
            }
        }
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }
        Level level = entity.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        OrganState state = organState(organ, STATE_ROOT);
        MultiCooldown cooldown = createCooldown(cc, organ, state);
        long gameTime = level.getGameTime();

        ensureAttached(cc);
        updatePassiveMaintenance(player, organ, cc, state, gameTime);
        updateWindStacks(player, cc, organ, state, gameTime);
        updateRunDistance(player, state, cc, organ);
        updateSprintMomentum(player, state, cc, organ, gameTime);
        updateGlideState(player, state, cc, organ, gameTime);
        detectNearMiss(player, level, state, cc, organ, gameTime, cooldown);
        updateDomainEffects(player, cc, organ, state, cooldown, serverLevel, gameTime);
        updateStageProgress(player, state, cc, organ, gameTime);
        syncCooldownChannels(cc, cooldown, gameTime);
    }

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!(victim instanceof Player player) || victim.level().isClientSide()) {
            return damage;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return damage;
        }
        OrganState state = organState(organ, STATE_ROOT);
        int stage = Math.max(1, state.getInt(KEY_STAGE, 1));
        MultiCooldown cooldown = createCooldown(cc, organ, state);
        long gameTime = victim.level().getGameTime();

        if (source != null && source.is(DamageTypeTags.IS_FALL)) {
            player.fallDistance = 0.0F;
            if (stage >= 4) {
                return 0.0F;
            }
        }

        double dodgeChance = 0.0D;
        if (stage >= 3) {
            double movementSq = player.getDeltaMovement().horizontalDistanceSqr();
            if (movementSq > MOVE_EPSILON) {
                dodgeChance += BASE_DODGE_STAGE3;
            }
        }
        if (stage >= 5) {
            int stacks = Mth.clamp(state.getInt(KEY_WIND_STACKS, 0), 0, 10);
            dodgeChance += stacks * WIND_STACK_DODGE_PER_STACK;
        }
        if (dodgeChance > 0.0D && ThreadLocalRandom.current().nextDouble() < dodgeChance) {
            spawnDodgeParticles((ServerLevel) player.level(), player);
            return 0.0F;
        }

        if (stage >= 4 && source != null && source.is(DamageTypeTags.IS_PROJECTILE)) {
            MultiCooldown.Entry readyEntry = cooldown.entry(CD_KEY_WIND_RING_READY);
            MultiCooldown.Entry dedupEntry = cooldown.entry(CD_KEY_DEDUP_RING_BLOCK);
            if (readyEntry.isReady(gameTime) && dedupEntry.isReady(gameTime)) {
                readyEntry.setReadyAt(gameTime + WIND_RING_COOLDOWN_TICKS);
                dedupEntry.setReadyAt(gameTime + 15);
                incrementCounter(state, cc, organ, KEY_RING_BLOCK, 1, Integer.MAX_VALUE);
                playWindRing((ServerLevel) player.level(), player.position());
                pushProjectileBack(source, player);
                return 0.0F;
            }
        }
        return damage;
    }

    @Override
    public float onHit(DamageSource source, LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!(attacker instanceof Player player) || attacker.level().isClientSide()) {
            return damage;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return damage;
        }
        OrganState state = organState(organ, STATE_ROOT);
        int stage = Math.max(1, state.getInt(KEY_STAGE, 1));
        if (stage < 3) {
            return damage;
        }
        long now = attacker.level().getGameTime();
        long window = state.getLong(KEY_DASH_WINDOW_UNTIL, 0L);
        if (window > 0L && now <= window) {
            MultiCooldown cooldown = createCooldown(cc, organ, state);
            MultiCooldown.Entry dedup = cooldown.entry(CD_KEY_DEDUP_DASH_HIT);
            if (dedup.isReady(now)) {
                incrementCounter(state, cc, organ, KEY_DASH_HIT, 1, Integer.MAX_VALUE);
                dedup.setReadyAt(now + 10L);
            }
        }
        return damage;
    }

    private static void activateDash(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof ServerPlayer player) || cc == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty() || entity.level().isClientSide()) {
            return;
        }
        Level level = entity.level();
        OrganState state = OrganState.of(organ, STATE_ROOT);
        int stage = Math.max(1, state.getInt(KEY_STAGE, 1));
        if (stage < 2) {
            return;
        }
        MultiCooldown cooldown = INSTANCE.createCooldown(cc, organ, state);
        long now = level.getGameTime();
        MultiCooldown.Entry readyEntry = cooldown.entry(CD_KEY_DASH_READY);
        long domainActiveUntil = state.getLong(KEY_DOMAIN_ACTIVE_UNTIL, 0L);
        boolean domainActive = domainActiveUntil > now;
        if (!domainActive && !readyEntry.isReady(now)) {
            return;
        }
        if (!consumeZhenyuan(player, DASH_COST)) {
            sendInsufficientMessage(player, "【清风轮】真元不足，技能中止。");
            return;
        }
        if (!domainActive) {
            readyEntry.setReadyAt(now + DASH_COOLDOWN_TICKS);
        }
        cooldown.entry(CD_KEY_DEDUP_DASH_USE).setReadyAt(now + 10L);
        performDash(player, organ, state, now);
        incrementCounter(state, cc, organ, KEY_DASH_USED, 1, Integer.MAX_VALUE);
        state.setLong(KEY_DASH_WINDOW_UNTIL, now + DASH_WINDOW_TICKS, value -> Math.max(0L, value), 0L);
        state.setLong(KEY_LAST_DASH_TICK, now, value -> Math.max(0L, value), 0L);
        Vec3 look = player.getLookAngle();
        state.setDouble(KEY_LAST_DASH_DIR_X, look.x, d -> d, 0.0D);
        state.setDouble(KEY_LAST_DASH_DIR_Y, look.y, d -> d, 0.0D);
        state.setDouble(KEY_LAST_DASH_DIR_Z, look.z, d -> d, 0.0D);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }

    private static void activateWindSlash(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof ServerPlayer player) || cc == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty() || entity.level().isClientSide()) {
            return;
        }
        Level level = entity.level();
        OrganState state = OrganState.of(organ, STATE_ROOT);
        int stage = Math.max(1, state.getInt(KEY_STAGE, 1));
        if (stage < 3) {
            return;
        }
        long now = level.getGameTime();
        long window = state.getLong(KEY_DASH_WINDOW_UNTIL, 0L);
        if (window <= 0L || now > window) {
            return;
        }
        if (!consumeZhenyuan(player, WIND_SLASH_COST)) {
            sendInsufficientMessage(player, "【清风轮】真元不足，技能中止。");
            return;
        }
        Vec3 origin = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        Vec3 dir = new Vec3(state.getDouble(KEY_LAST_DASH_DIR_X, player.getLookAngle().x),
                state.getDouble(KEY_LAST_DASH_DIR_Y, player.getLookAngle().y),
                state.getDouble(KEY_LAST_DASH_DIR_Z, player.getLookAngle().z)).normalize();
        if (dir.lengthSqr() < 1.0E-4D) {
            dir = player.getLookAngle();
        }
        performWindSlash(player.serverLevel(), player, origin, dir);
        state.setLong(KEY_DASH_WINDOW_UNTIL, 0L, value -> Math.max(0L, value), 0L);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }

    private static void activateWindDomain(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof ServerPlayer player) || cc == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty() || entity.level().isClientSide()) {
            return;
        }
        Level level = entity.level();
        OrganState state = OrganState.of(organ, STATE_ROOT);
        int stage = Math.max(1, state.getInt(KEY_STAGE, 1));
        if (stage < 5) {
            return;
        }
        MultiCooldown cooldown = INSTANCE.createCooldown(cc, organ, state);
        long now = level.getGameTime();
        MultiCooldown.Entry domainReady = cooldown.entry(CD_KEY_DOMAIN_READY);
        if (!domainReady.isReady(now)) {
            return;
        }
        int stacks = Mth.clamp(state.getInt(KEY_WIND_STACKS, 0), 0, 10);
        if (stacks < 10) {
            sendInsufficientMessage(player, "【风神领域】需要满层风势。");
            return;
        }
        if (!consumeZhenyuan(player, DOMAIN_START_COST)) {
            sendInsufficientMessage(player, "【清风轮】真元不足，技能中止。");
            return;
        }
        domainReady.setReadyAt(now + DOMAIN_COOLDOWN_TICKS);
        state.setLong(KEY_DOMAIN_ACTIVE_UNTIL, now + DOMAIN_DURATION_TICKS, value -> Math.max(0L, value), 0L);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }

    private MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ, OrganState state) {
        MultiCooldown.Builder builder = MultiCooldown.builder(state).withSync(cc, organ);
        return builder.build();
    }

    private static void updatePassiveMaintenance(Player player, ItemStack organ, ChestCavityInstance cc, OrganState state, long gameTime) {
        long nextTick = state.getLong(KEY_PASSIVE_READY, 0L);
        if (nextTick <= 0L) {
            OrganStateOps.setLong(state, cc, organ, KEY_PASSIVE_READY, gameTime + PASSIVE_INTERVAL_TICKS,
                    value -> Math.max(0L, value), 0L);
            return;
        }
        if (gameTime >= nextTick) {
            if (!consumeZhenyuan(player, PASSIVE_COST)) {
                if (gameTime % 100 == 0) {
                    sendInsufficientMessage(player, "【清风轮】真元不足，技能中止。");
                }
            } else {
                OrganStateOps.setLong(state, cc, organ, KEY_PASSIVE_READY, gameTime + PASSIVE_INTERVAL_TICKS,
                        value -> Math.max(0L, value), 0L);
            }
        }
    }

    private void updateWindStacks(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, long gameTime) {
        int stage = Math.max(1, state.getInt(KEY_STAGE, 1));
        double horizontal = player.getDeltaMovement().horizontalDistance();
        boolean moving = horizontal > 0.05D || player.isSprinting();
        int stacks = Mth.clamp(state.getInt(KEY_WIND_STACKS, 0), 0, 10);
        long lastMoveTick = state.getLong(KEY_LAST_MOVE_TICK, 0L);
        boolean dirty = false;
        if (moving) {
            int newStacks = Math.min(10, stacks + 1);
            if (newStacks != stacks) {
                stacks = newStacks;
                dirty |= OrganStateOps.setInt(state, cc, organ, KEY_WIND_STACKS, stacks, value -> Mth.clamp(value, 0, 10), 0).changed();
            }
            state.setLong(KEY_LAST_MOVE_TICK, gameTime, value -> Math.max(0L, value), 0L);
        } else {
            if (lastMoveTick > 0L && gameTime - lastMoveTick >= 40L && stacks > 0) {
                stacks = 0;
                dirty |= OrganStateOps.setInt(state, cc, organ, KEY_WIND_STACKS, 0, value -> Mth.clamp(value, 0, 10), 0).changed();
            }
        }
        applyWindStackModifier(player, stacks);
        handleWindStackHold(cc, organ, state, gameTime, stacks);
        updateWindStackChannel(cc, stacks);
        if (dirty) {
            sendSlotUpdate(cc, organ);
        }
    }

    private void handleWindStackHold(ChestCavityInstance cc, ItemStack organ, OrganState state, long gameTime, int stacks) {
        long start = state.getLong(KEY_STACK10_START, 0L);
        if (stacks >= 10) {
            if (start <= 0L) {
                state.setLong(KEY_STACK10_START, gameTime, value -> Math.max(0L, value), 0L);
            } else if (gameTime - start >= 100L) {
                incrementCounter(state, cc, organ, KEY_STACK10_HOLD, 1, Integer.MAX_VALUE);
                state.setLong(KEY_STACK10_START, gameTime, value -> Math.max(0L, value), 0L);
            }
        } else if (start > 0L) {
            state.setLong(KEY_STACK10_START, 0L, value -> Math.max(0L, value), 0L);
        }
    }

    private void updateRunDistance(Player player, OrganState state, ChestCavityInstance cc, ItemStack organ) {
        UUID id = player.getUUID();
        Vec3 previous = LAST_POSITION.get(id);
        Vec3 current = player.position();
        LAST_POSITION.put(id, current);
        if (!player.isSprinting() || player.isPassenger() || player.isSpectator() || player.isSwimming()) {
            return;
        }
        if (previous == null) {
            return;
        }
        double distance = new Vec3(current.x - previous.x, 0.0D, current.z - previous.z).length();
        if (distance < RUN_SAMPLE_THRESHOLD) {
            return;
        }
        long existing = state.getLong(KEY_RUN_M, 0L);
        long added = Math.round(distance * 100.0D);
        long updated = Math.min(Integer.MAX_VALUE, existing + added);
        OrganStateOps.setLong(state, cc, organ, KEY_RUN_M, updated, value -> Math.max(0L, value), 0L);
    }

    private void updateSprintMomentum(Player player, OrganState state, ChestCavityInstance cc, ItemStack organ, long gameTime) {
        int stage = Math.max(1, state.getInt(KEY_STAGE, 1));
        if (stage < 2) {
            removeWindWalkModifier(player);
            return;
        }
        int seconds = state.getInt(KEY_SPRINT_SECONDS, 0);
        if (player.isSprinting() && player.onGround()) {
            seconds = Math.min(30, seconds + 1);
        } else {
            seconds = 0;
        }
        state.setInt(KEY_SPRINT_SECONDS, seconds, value -> Math.max(0, value), 0);
        long activeUntil = state.getLong(KEY_WIND_WALK_UNTIL, 0L);
        if (seconds >= 2) {
            state.setLong(KEY_WIND_WALK_UNTIL, gameTime + 60L, value -> Math.max(0L, value), 0L);
            activeUntil = gameTime + 60L;
        }
        if (activeUntil > gameTime) {
            applyWindWalkModifier(player);
        } else {
            removeWindWalkModifier(player);
        }
    }

    private void updateGlideState(Player player, OrganState state, ChestCavityInstance cc, ItemStack organ, long gameTime) {
        int stage = Math.max(1, state.getInt(KEY_STAGE, 1));
        boolean airborne = !player.onGround() && !player.isInWater() && !player.isPassenger();
        if (airborne) {
            player.fallDistance = 0.0F;
            if (stage >= 4) {
                Vec3 motion = player.getDeltaMovement();
                if (motion.y < -0.25D) {
                    player.setDeltaMovement(motion.x * 0.98D, -0.25D, motion.z * 0.98D);
                }
            }
            long glideTicks = state.getLong(KEY_GLIDE_TICKS, 0L);
            glideTicks += 20L;
            state.setLong(KEY_GLIDE_TICKS, glideTicks, value -> Math.max(0L, value), 0L);
            long airTime = state.getLong(KEY_AIR_TIME, 0L);
            airTime = Math.min(Long.MAX_VALUE / 2, airTime + 1000L);
            state.setLong(KEY_AIR_TIME, airTime, value -> Math.max(0L, value), 0L);
        } else {
            long glideTicks = state.getLong(KEY_GLIDE_TICKS, 0L);
            if (glideTicks >= 240L) {
                incrementCounter(state, cc, organ, KEY_GLIDE_CHAIN, 1, Integer.MAX_VALUE);
            }
            state.setLong(KEY_GLIDE_TICKS, 0L, value -> Math.max(0L, value), 0L);
        }
    }

    private void detectNearMiss(Player player, Level level, OrganState state, ChestCavityInstance cc, ItemStack organ, long gameTime, MultiCooldown cooldown) {
        AABB box = player.getBoundingBox().inflate(1.4D);
        List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, box, entity -> entity != null && entity.isAlive());
        for (Projectile projectile : projectiles) {
            UUID id = projectile.getUUID();
            long lastTick = NEAR_MISS_CACHE.getOrDefault(id, Long.MIN_VALUE);
            if (gameTime - lastTick < NEAR_MISS_DEDUP_TICKS) {
                continue;
            }
            Vec3 toPlayer = player.position().subtract(projectile.position());
            if (toPlayer.lengthSqr() > 1.44D * 1.44D) {
                continue;
            }
            Vec3 velocity = projectile.getDeltaMovement();
            if (velocity.dot(toPlayer) < 0.0D) {
                continue;
            }
            MultiCooldown.Entry dedup = cooldown.entry(CD_KEY_DEDUP_NEAR_MISS);
            if (!dedup.isReady(gameTime)) {
                continue;
            }
            dedup.setReadyAt(gameTime + 10L);
            NEAR_MISS_CACHE.put(id, gameTime);
            incrementCounter(state, cc, organ, KEY_NEAR_MISS, 1, Integer.MAX_VALUE);
        }
    }

    private void updateDomainEffects(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, MultiCooldown cooldown, ServerLevel level, long gameTime) {
        long activeUntil = state.getLong(KEY_DOMAIN_ACTIVE_UNTIL, 0L);
        if (activeUntil <= gameTime) {
            clearDomainModifier(player);
            return;
        }
        applyDomainModifier(player);
        if ((gameTime % 20L) == 0L) {
            if (!consumeZhenyuan(player, DOMAIN_MAINTAIN_COST_PER_SECOND)) {
                state.setLong(KEY_DOMAIN_ACTIVE_UNTIL, 0L, value -> Math.max(0L, value), 0L);
                clearDomainModifier(player);
                sendInsufficientMessage(player, "【清风轮】真元不足，技能中止。");
                return;
            }
            applyDomainSupport(level, player);
        }
        spawnDomainParticles(level, player);
    }

    private void updateStageProgress(Player player, OrganState state, ChestCavityInstance cc, ItemStack organ, long gameTime) {
        int stage = Math.max(1, state.getInt(KEY_STAGE, 1));
        int newStage = stage;
        long run = state.getLong(KEY_RUN_M, 0L);
        int dashUsed = state.getInt(KEY_DASH_USED, 0);
        int dashHit = state.getInt(KEY_DASH_HIT, 0);
        int nearMiss = state.getInt(KEY_NEAR_MISS, 0);
        long airTime = state.getLong(KEY_AIR_TIME, 0L);
        int ringBlock = state.getInt(KEY_RING_BLOCK, 0);
        int stackHold = state.getInt(KEY_STACK10_HOLD, 0);
        int glideChain = state.getInt(KEY_GLIDE_CHAIN, 0);
        if (stage < 2 && run >= RUN_THRESHOLD && dashUsed >= DASH_USED_THRESHOLD) {
            newStage = 2;
            notifyStage(player, "【清风轮·风轮形】已觉醒。获得：疾风冲刺、风行加速。");
        }
        if (newStage < 3 && dashHit >= DASH_HIT_THRESHOLD && nearMiss >= NEAR_MISS_THRESHOLD) {
            newStage = 3;
            notifyStage(player, "【清风轮·破阵风】已觉醒。获得：风裂步、移动闪避。");
        }
        if (newStage < 4 && airTime >= AIR_TIME_THRESHOLD_MS && ringBlock >= RING_BLOCK_THRESHOLD) {
            newStage = 4;
            notifyStage(player, "【清风轮·御风轮】已觉醒。获得：免疫摔落、风环护盾、御风翔行。");
        }
        if (newStage < 5 && stackHold >= STACK10_HOLD_THRESHOLD && glideChain >= GLIDE_CHAIN_THRESHOLD) {
            newStage = 5;
            notifyStage(player, "【清风轮·风神轮】已觉醒。可启：风神领域；风势层数蓄积生效。");
        }
        if (newStage != stage) {
            OrganStateOps.setInt(state, cc, organ, KEY_STAGE, newStage, value -> Mth.clamp(value, 1, 5), 1);
        }
    }

    private void syncCooldownChannels(ChestCavityInstance cc, MultiCooldown cooldown, long gameTime) {
        ActiveLinkageContext context = LedgerOps.context(cc);
        if (context == null) {
            return;
        }
        LinkageChannel ringChannel = LedgerOps.ensureChannel(context, WIND_RING_CD_CHANNEL, WIND_RING_CD_CLAMP);
        if (ringChannel != null) {
            long ready = cooldown.entry(CD_KEY_WIND_RING_READY).getReadyTick();
            long remaining = Math.max(0L, ready - gameTime);
            ringChannel.set(remaining);
        }
    }

    private void updateWindStackChannel(ChestCavityInstance cc, int stacks) {
        ActiveLinkageContext context = LedgerOps.context(cc);
        if (context == null) {
            return;
        }
        LinkageChannel channel = LedgerOps.ensureChannel(context, WIND_STACKS_CHANNEL, WIND_STACK_CLAMP);
        if (channel != null) {
            channel.set(Math.max(0.0D, (double) stacks));
        }
    }

    private void applyWindStackModifier(Player player, int stacks) {
        AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (instance == null) {
            return;
        }
        if (stacks > 0) {
            AttributeModifier modifier = new AttributeModifier(WIND_STACKS_MODIFIER_ID,
                    stacks * WIND_STACK_SPEED_BONUS_PER_STACK, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            AttributeOps.replaceTransient(instance, WIND_STACKS_MODIFIER_ID, modifier);
        } else {
            AttributeOps.removeById(instance, WIND_STACKS_MODIFIER_ID);
        }
    }

    private void applyWindWalkModifier(Player player) {
        AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (instance == null) {
            return;
        }
        AttributeModifier modifier = new AttributeModifier(WIND_WALK_MODIFIER_ID,
                0.25D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        AttributeOps.replaceTransient(instance, WIND_WALK_MODIFIER_ID, modifier);
    }

    private void removeWindWalkModifier(Player player) {
        AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeOps.removeById(instance, WIND_WALK_MODIFIER_ID);
    }

    private void applyDomainModifier(Player player) {
        AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (instance == null) {
            return;
        }
        AttributeModifier modifier = new AttributeModifier(DOMAIN_MODIFIER_ID,
                0.20D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        AttributeOps.replaceTransient(instance, DOMAIN_MODIFIER_ID, modifier);
    }

    private void clearDomainModifier(Player player) {
        AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeOps.removeById(instance, DOMAIN_MODIFIER_ID);
    }

    private void clearAttributeModifiers(LivingEntity entity, boolean stacksToo) {
        if (!(entity instanceof Player player)) {
            return;
        }
        removeWindWalkModifier(player);
        clearDomainModifier(player);
        if (stacksToo) {
            AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
            AttributeOps.removeById(instance, WIND_STACKS_MODIFIER_ID);
        }
    }

    private static void spawnDodgeParticles(ServerLevel level, Player player) {
        Vec3 pos = player.position();
        for (int i = 0; i < 6; i++) {
            double ox = (level.random.nextDouble() - 0.5D) * 0.6D;
            double oy = level.random.nextDouble() * 0.4D + player.getBbHeight() * 0.5D;
            double oz = (level.random.nextDouble() - 0.5D) * 0.6D;
            level.sendParticles(ParticleTypes.CLOUD, pos.x + ox, pos.y + oy, pos.z + oz, 1, 0.0D, 0.0D, 0.0D, 0.05D);
        }
    }

    private static void playWindRing(ServerLevel level, Vec3 pos) {
        for (int i = 0; i < 12; i++) {
            double angle = (Math.PI * 2 * i) / 12.0D;
            double radius = 1.1D;
            double x = pos.x + Math.cos(angle) * radius;
            double z = pos.z + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.CLOUD, x, pos.y + 1.0D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private static void pushProjectileBack(DamageSource source, Player player) {
        Entity attacker = source == null ? null : source.getDirectEntity();
        if (attacker instanceof Projectile projectile) {
            Vec3 dir = projectile.position().subtract(player.position()).normalize();
            projectile.setDeltaMovement(dir.scale(WIND_RING_PUSH));
        }
    }

    private static void performDash(ServerPlayer player, ItemStack organ, OrganState state, long now) {
        Vec3 look = player.getLookAngle().normalize();
        if (look.lengthSqr() < 1.0E-4D) {
            look = player.getDeltaMovement().normalize();
        }
        if (look.lengthSqr() < 1.0E-4D) {
            look = new Vec3(1.0D, 0.0D, 0.0D);
        }

        Vec3 origin = player.position();
        Vec3 maxTarget = origin.add(look.scale(DASH_DISTANCE));
        HitResult hit = player.level().clip(new ClipContext(origin, maxTarget, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        Vec3 dashVector = maxTarget.subtract(origin);
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 hitVector = hit.getLocation().subtract(origin);
            double clearance = player.getBbWidth() * 0.5D + 0.15D;
            double adjustedLength = Math.max(0.0D, hitVector.length() - clearance);
            dashVector = look.scale(Math.min(DASH_DISTANCE, adjustedLength));
        }

        double yOffset = player.getBbHeight() * 0.1D;
        Vec3 desiredDestination = origin.add(dashVector).add(0.0D, yOffset, 0.0D);

        Vec3 destination = attemptSafeTeleport(player, desiredDestination, look, yOffset, origin);

        player.setDeltaMovement(look.x * 0.45D, Math.max(look.y * 0.2D, 0.1D), look.z * 0.45D);
        player.hasImpulse = true;
        spawnDashParticles(player.serverLevel(), origin, destination);
        dashKnockback(player, origin, destination);
    }


    private static Vec3 attemptSafeTeleport(ServerPlayer player, Vec3 desiredDestination, Vec3 fallbackDirection, double yOffset, Vec3 origin) {
        Vec3 destination = desiredDestination;
        Vec3 offset = destination.subtract(origin);
        double distance = offset.length();
        Vec3 direction = distance > 1.0E-4D ? offset.normalize() : fallbackDirection.normalize();
        if (direction.lengthSqr() < 1.0E-4D) {
            direction = new Vec3(1.0D, 0.0D, 0.0D);
        }

        Optional<Vec3> result = TeleportOps.blinkTo(player, destination, 2, 0.25D);
        if (result.isPresent()) {
            return result.get();
        }

        double remaining = distance;
        double retreatStep = Math.max(0.25D, player.getBbWidth() * 0.5D + 0.1D);
        for (int i = 0; i < 4 && remaining > 0.05D; i++) {
            remaining = Math.max(0.0D, remaining - retreatStep);
            Vec3 fallbackTarget = origin.add(direction.scale(remaining));
            result = TeleportOps.blinkTo(player, fallbackTarget, 2, 0.25D);
            if (result.isPresent()) {
                return result.get();
            }
        }

        player.teleportTo(origin.x, origin.y + yOffset, origin.z);
        return player.position();
    }

    private static void spawnDashParticles(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        int steps = 12;
        for (int i = 0; i < steps; i++) {
            double t = i / (double) steps;
            Vec3 pos = start.add(delta.scale(t));
            level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.1D, pos.z, 4, 0.0D, 0.0D, 0.0D, 0.01D);
        }
        level.playSound(null, start.x, start.y, start.z, SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.6F, 1.2F);
    }

    private static void dashKnockback(ServerPlayer player, Vec3 start, Vec3 end) {
        ServerLevel level = player.serverLevel();
        Vec3 center = end;
        AABB box = player.getBoundingBox().inflate(1.2D);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box, entity -> entity != player);
        for (LivingEntity target : targets) {
            Vec3 push = target.position().subtract(center).normalize().scale(0.6D);
            target.push(push.x, 0.25D, push.z);
        }
    }

    private static void performWindSlash(ServerLevel level, Player player, Vec3 origin, Vec3 dir) {
        Vec3 step = dir.scale(1.0D);
        Vec3 pos = origin;
        for (int i = 0; i < WIND_SLASH_RANGE; i++) {
            pos = pos.add(step);
            level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 4, 0.1D, 0.0D, 0.1D, 0.0D);
            AABB box = new AABB(pos.x - 0.6D, pos.y - 0.6D, pos.z - 0.6D, pos.x + 0.6D, pos.y + 0.6D, pos.z + 0.6D);
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box, entity -> entity != player);
            for (LivingEntity entity : entities) {
                entity.hurt(player.damageSources().playerAttack(player), (float) WIND_SLASH_DAMAGE);
                Vec3 push = dir.scale(0.4D);
                entity.push(push.x, 0.1D, push.z);
            }
        }
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.3F);
    }

    private static boolean consumeZhenyuan(Player player, double baseCost) {
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return false;
        }
        return handleOpt.get().consumeScaledZhenyuan(baseCost).isPresent();
    }

    private static void sendInsufficientMessage(Player player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }

    private static void incrementCounter(OrganState state, ChestCavityInstance cc, ItemStack organ, String key, int delta, int max) {
        int current = state.getInt(key, 0);
        int updated = Math.min(max, current + delta);
        OrganStateOps.setInt(state, cc, organ, key, updated, value -> Math.max(0, Math.min(max, value)), 0);
    }

    private static void applyDomainSupport(ServerLevel level, Player player) {
        AABB area = player.getBoundingBox().inflate(DOMAIN_PARTICLE_RADIUS);
        List<Player> allies = level.getEntitiesOfClass(Player.class, area,
                candidate -> candidate != null && candidate.isAlive() && candidate.distanceTo(player) <= DOMAIN_PARTICLE_RADIUS);
        for (Player ally : allies) {
            ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, true, false, false));
        }
        List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class,
                area, projectile -> projectile != null && projectile.isAlive());
        for (Projectile projectile : projectiles) {
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(0.7D));
        }
    }

    private static void spawnDomainParticles(ServerLevel level, Player player) {
        Vec3 center = player.position();
        for (int i = 0; i < 20; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double radius = DOMAIN_PARTICLE_RADIUS * level.random.nextDouble();
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = center.y + 0.1D + level.random.nextDouble() * player.getBbHeight();
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 0, 0.0D, 0.02D, 0.0D, 0.01D);
        }
    }

    private static void notifyStage(Player player, String message) {
        player.displayClientMessage(Component.literal(message), false);
    }

    private static ItemStack findOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (Objects.equals(id, ORGAN_ID)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
