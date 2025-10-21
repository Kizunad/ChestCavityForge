package net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.behavior;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.AttributeOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.NetworkUtil;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.util.ChestCavityUtil;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

/**
 * 清风轮蛊：位移、滑翔与风势管理器官。
 */
public final class QingFengLunOrganBehavior extends AbstractGuzhenrenOrganBehavior
        implements OrganSlowTickListener, OrganIncomingDamageListener, OrganOnHitListener, OrganRemovalListener {

    public static final QingFengLunOrganBehavior INSTANCE = new QingFengLunOrganBehavior();

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "qing_feng_lun_gu");
    private static final ResourceLocation DASH_ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "qing_feng_lun_gu/dash");
    private static final ResourceLocation WIND_SLASH_ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "qing_feng_lun_gu/wind_slash");
    private static final ResourceLocation DOMAIN_ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "qing_feng_lun_gu/wind_domain");

    private static final ResourceLocation WIND_STACK_CHANNEL = ResourceLocation.fromNamespaceAndPath(MOD_ID, "qingfenglun/wind_stacks");
    private static final ResourceLocation WIND_RING_CHANNEL = ResourceLocation.fromNamespaceAndPath(MOD_ID, "qingfenglun/windring_cd");

    private static final String STATE_ROOT = "qing_feng_lun";
    private static final String KEY_TIER = "tier";
    private static final String KEY_RUN_M = "run_m";
    private static final String KEY_DASH_USED = "dash_used";
    private static final String KEY_DASH_HIT = "dash_hit";
    private static final String KEY_NEAR_MISS = "near_miss";
    private static final String KEY_AIR_TIME = "air_time";
    private static final String KEY_RING_BLOCK = "ring_block";
    private static final String KEY_STACK10_HOLD = "stack10_hold";
    private static final String KEY_GLIDE_CHAIN = "glide_chain";
    private static final String KEY_WIND_STACKS = "wind_stacks";
    private static final String KEY_LAST_SAMPLE_X = "last_sample_x";
    private static final String KEY_LAST_SAMPLE_Y = "last_sample_y";
    private static final String KEY_LAST_SAMPLE_Z = "last_sample_z";
    private static final String KEY_LAST_SAMPLE_TICK = "last_sample_tick";
    private static final String KEY_WIND_STACK_TIMER = "wind_stack_timer";
    private static final String KEY_STOPPED_TICKS = "stopped_ticks";
    private static final String KEY_PASSIVE_TICKS = "passive_ticks";
    private static final String KEY_RUN_CHAIN_TICKS = "run_chain_ticks";
    private static final String KEY_GLIDE_TICKS = "current_glide_ticks";
    private static final String KEY_FULL_STACK_TICKS = "full_stack_ticks";
    private static final String KEY_DOMAIN_COST_TICKS = "domain_cost_ticks";
    private static final String KEY_LAST_DASH_COUNT = "last_dash_count";
    private static final String KEY_LAST_DASH_HIT = "last_dash_hit";
    private static final String KEY_LAST_RING_BLOCK = "last_ring_block";
    private static final String KEY_WIND_SLASH_WINDOW = "wind_slash_window";
    private static final String KEY_DOMAIN_ACTIVE_UNTIL = "domain_active_until";
    private static final String KEY_DOMAIN_READY_AT = "domain_ready";
    private static final String KEY_DASH_READY_AT = "dash_ready";
    private static final String KEY_DASH_ACTIVE_UNTIL = "dash_active";
    private static final String KEY_RING_READY_AT = "wind_ring_ready";

    private static final int BASE_STAGE = 1;
    private static final int MAX_STAGE = 5;

    private static final long STAGE2_RUN_TARGET = 3000L;
    private static final int STAGE2_DASH_USED_TARGET = 50;
    private static final int STAGE3_DASH_HIT_TARGET = 50;
    private static final int STAGE3_NEAR_MISS_TARGET = 30;
    private static final long STAGE4_AIR_TIME_TICKS = 300L * 20L;
    private static final int STAGE4_RING_BLOCK_TARGET = 25;
    private static final int STAGE5_STACK10_TARGET = 12;
    private static final int STAGE5_GLIDE_CHAIN_TARGET = 10;

    private static final int PASSIVE_COST_INTERVAL = 4;
    private static final double PASSIVE_BASE_COST = 60.0D;
    private static final double DASH_BASE_COST = 2200.0D;
    private static final double WIND_SLASH_BASE_COST = 38000.0D;
    private static final double GLIDE_BASE_COST = 140000.0D;
    private static final double DOMAIN_START_COST = 120000.0D;
    private static final double DOMAIN_TICK_COST = 20000.0D;

    private static final int DASH_COOLDOWN_TICKS = 6 * 20;
    private static final int DASH_DURATION_TICKS = 12;
    private static final double DASH_SPEED_PER_TICK = 0.5D;
    private static final float DASH_KNOCKBACK = 0.35F;
    private static final float DASH_DAMAGE = 4.0F;

    private static final int WIND_SLASH_SEGMENTS = 5;
    private static final double WIND_SLASH_RANGE = 1.0D;
    private static final float WIND_SLASH_DAMAGE = 5.0F;
    private static final int WIND_SLASH_WINDOW_TICKS = 5;

    private static final int WIND_RING_COOLDOWN_TICKS = 8 * 20;
    private static final float WIND_RING_DAMAGE_REDUCTION = 0.0F;

    private static final int WIND_STACK_MAX = 10;
    private static final int WIND_STACK_GAIN_INTERVAL_TICKS = 20;
    private static final int WIND_STACK_RESET_TICKS = 40;
    private static final double WIND_STACK_SPEED_PER_LAYER = 0.02D;

    private static final int STACK_FULL_REQUIRED_TICKS = 5 * 20;
    private static final int GLIDE_CHAIN_REQUIRED_TICKS = 12 * 20;

    private static final int DOMAIN_DURATION_TICKS = 10 * 20;
    private static final int DOMAIN_COOLDOWN_TICKS = 45 * 20;
    private static final double DOMAIN_PROJECTILE_SCALE = 0.70D;
    private static final double DOMAIN_SPEED_BONUS = 0.20D;

    private static final ResourceLocation WIND_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifiers/qing_feng_lun_wind_speed");
    private static final ResourceLocation DOMAIN_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifiers/qing_feng_lun_domain_speed");

    private static final java.util.function.DoubleUnaryOperator IDENTITY_DOUBLE = value -> value;
    private static final java.util.function.LongUnaryOperator NON_NEGATIVE_LONG = value -> Math.max(0L, value);
    private static final java.util.function.IntUnaryOperator NON_NEGATIVE_INT = value -> Math.max(0, value);

    private static final Map<UUID, DashSession> ACTIVE_DASHES = new ConcurrentHashMap<>();
    private static final Map<UUID, Int2LongOpenHashMap> NEAR_MISS_TRACKERS = new ConcurrentHashMap<>();

    static {
        OrganActivationListeners.register(DASH_ABILITY_ID, QingFengLunOrganBehavior::activateDash);
        OrganActivationListeners.register(WIND_SLASH_ABILITY_ID, QingFengLunOrganBehavior::activateWindSlash);
        OrganActivationListeners.register(DOMAIN_ABILITY_ID, QingFengLunOrganBehavior::activateDomain);
    }

    private QingFengLunOrganBehavior() {
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }

        Level genericLevel = entity.level();
        if (!(genericLevel instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();

        ensureAttached(cc);

        OrganState state = organState(organ, STATE_ROOT);
        MultiCooldown cooldown = createCooldown(cc, organ, state);
        OrganStateOps.Collector collector = OrganStateOps.collector(cc, organ);

        int storedStage = Mth.clamp(state.getInt(KEY_TIER, BASE_STAGE), BASE_STAGE, MAX_STAGE);

        updatePassiveCost(player, cc, organ, state, collector);
        updateMovementCounters(player, cc, organ, state, collector, now, storedStage);
        updateNearMiss(player, cc, organ, state, collector, now);
        updateGlideAndAirTime(player, cc, organ, state, collector, now, storedStage);
        boolean domainActive = updateDomainState(player, cc, organ, state, collector, cooldown, now, storedStage);

        int stage = determineStage(state);
        if (stage != storedStage) {
            collector.record(OrganStateOps.setInt(state, cc, organ, KEY_TIER, stage,
                    value -> Mth.clamp(value, BASE_STAGE, MAX_STAGE), BASE_STAGE));
            announceStage(player, storedStage, stage);
        }

        int windStacks = state.getInt(KEY_WIND_STACKS, 0);
        updateWindSpeedModifier(player, windStacks, domainActive);
        updateChannels(cc, cooldown, windStacks, now);
        spawnTrailParticles(level, player, stage, windStacks, domainActive);

        collector.commit();
    }

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!(victim instanceof Player player) || player.level().isClientSide()) {
            return damage;
        }
        if (cc == null || organ == null || organ.isEmpty() || !matchesOrgan(organ, ORGAN_ID)) {
            return damage;
        }

        Level level = player.level();
        long now = level.getGameTime();
        OrganState state = organState(organ, STATE_ROOT);
        int stage = determineStage(state);
        MultiCooldown cooldown = createCooldown(cc, organ, state);

        Entity direct = source.getDirectEntity();
        boolean projectile = direct instanceof Projectile;
        boolean domainActive = isDomainActive(state, now);

        if (projectile && domainActive && stage >= 5) {
            damage *= DOMAIN_PROJECTILE_SCALE;
        }

        if (stage >= 4 && source == victim.damageSources().fall()) {
            return 0.0F;
        }

        if (stage >= 4 && projectile) {
            MultiCooldown.Entry ringReady = cooldown.entry(KEY_RING_READY_AT).withClamp(NON_NEGATIVE_LONG).withDefault(0L);
            if (ringReady.isReady(now)) {
                ringReady.setReadyAt(now + WIND_RING_COOLDOWN_TICKS);
                long last = state.getLong(KEY_LAST_RING_BLOCK, 0L);
                if (now - last >= 15L) {
                    int blocks = state.getInt(KEY_RING_BLOCK, 0) + 1;
                    OrganStateOps.setIntSync(cc, organ, STATE_ROOT, KEY_RING_BLOCK, blocks, NON_NEGATIVE_INT, 0);
                    OrganStateOps.setLongSync(cc, organ, STATE_ROOT, KEY_LAST_RING_BLOCK, now, NON_NEGATIVE_LONG, 0L);
                }
                if (player.level() instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.5D, player.getZ(),
                            10, 0.4D, 0.2D, 0.4D, 0.02D);
                    server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WIND_CHARGE_BURST,
                            SoundSource.PLAYERS, 0.7F, 1.1F);
                }
                if (direct != null) {
                    direct.discard();
                }
                return WIND_RING_DAMAGE_REDUCTION;
            }
        }

        if (stage >= 3) {
            Vec3 motion = victim.getDeltaMovement();
            if (motion.horizontalDistanceSqr() > 0.01D && player.getRandom().nextFloat() < 0.1F) {
                if (player.level() instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.4D, player.getZ(),
                            4, 0.2D, 0.1D, 0.2D, 0.0D);
                }
                return 0.0F;
            }
        }

        return damage;
    }

    @Override
    public float onHit(DamageSource source, LivingEntity attacker, LivingEntity target, ChestCavityInstance cc,
                      ItemStack organ, float damage) {
        if (cc == null || organ == null || organ.isEmpty() || target == null) {
            return damage;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return damage;
        }
        if (!(attacker instanceof ServerPlayer player)) {
            return damage;
        }
        DashSession session = ACTIVE_DASHES.get(player.getUUID());
        if (session != null && target.isAlive()) {
            double dx = target.getX() - player.getX();
            double dz = target.getZ() - player.getZ();
            target.knockback(DASH_KNOCKBACK * 0.5F, dx, dz);
        }
        return damage;
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        clearAttachments(cc, organ);
    }

    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        if (cc == null || organ == null || organ.isEmpty() || !matchesOrgan(organ, ORGAN_ID)) {
            return;
        }
        registerRemovalHook(cc, organ, this, staleRemovalContexts);
        ensureAttached(cc);
    }

    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ActiveLinkageContext context = LedgerOps.context(cc);
        if (context != null) {
            LedgerOps.ensureChannel(context, WIND_STACK_CHANNEL);
            LedgerOps.ensureChannel(context, WIND_RING_CHANNEL);
        }
    }

    private void clearAttachments(ChestCavityInstance cc, ItemStack organ) {
        if (cc == null) {
            return;
        }
        ActiveLinkageContext context = LedgerOps.context(cc);
        if (context != null) {
            LedgerOps.lookupChannel(context, WIND_STACK_CHANNEL).ifPresent(channel -> channel.set(0.0D));
            LedgerOps.lookupChannel(context, WIND_RING_CHANNEL).ifPresent(channel -> channel.set(0.0D));
        }
        if (cc.owner != null) {
            ACTIVE_DASHES.remove(cc.owner.getUUID());
            NEAR_MISS_TRACKERS.remove(cc.owner.getUUID());
            removeSpeedModifiers(cc.owner);
        }
    }

    private MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ, OrganState state) {
        return MultiCooldown.builder(state)
                .withSync(cc, organ)
                .build();
    }

    private int determineStage(OrganState state) {
        int stage = Mth.clamp(state.getInt(KEY_TIER, BASE_STAGE), BASE_STAGE, MAX_STAGE);
        long run = state.getLong(KEY_RUN_M, 0L);
        int dash = state.getInt(KEY_DASH_USED, 0);
        if (stage < 2 && run >= STAGE2_RUN_TARGET && dash >= STAGE2_DASH_USED_TARGET) {
            stage = 2;
        }
        int dashHit = state.getInt(KEY_DASH_HIT, 0);
        int nearMiss = state.getInt(KEY_NEAR_MISS, 0);
        if (stage < 3 && dashHit >= STAGE3_DASH_HIT_TARGET && nearMiss >= STAGE3_NEAR_MISS_TARGET) {
            stage = 3;
        }
        long air = state.getLong(KEY_AIR_TIME, 0L);
        int ring = state.getInt(KEY_RING_BLOCK, 0);
        if (stage < 4 && air >= STAGE4_AIR_TIME_TICKS && ring >= STAGE4_RING_BLOCK_TARGET) {
            stage = 4;
        }
        int hold = state.getInt(KEY_STACK10_HOLD, 0);
        int glide = state.getInt(KEY_GLIDE_CHAIN, 0);
        if (stage < 5 && hold >= STAGE5_STACK10_TARGET && glide >= STAGE5_GLIDE_CHAIN_TARGET) {
            stage = 5;
        }
        return stage;
    }

    private void announceStage(Player player, int previousStage, int newStage) {
        if (player == null || newStage <= previousStage) {
            return;
        }
        switch (newStage) {
            case 2 -> player.sendSystemMessage(net.minecraft.network.chat.Component.literal("【清风轮·风轮形】已觉醒。获得：疾风冲刺、风行加速。"));
            case 3 -> player.sendSystemMessage(net.minecraft.network.chat.Component.literal("【清风轮·破阵风】已觉醒。获得：风裂步、移动闪避。"));
            case 4 -> player.sendSystemMessage(net.minecraft.network.chat.Component.literal("【清风轮·御风轮】已觉醒。获得：免疫摔落、风环护盾、御风翔行。"));
            case 5 -> player.sendSystemMessage(net.minecraft.network.chat.Component.literal("【清风轮·风神轮】已觉醒。可启：风神领域；风势层数蓄积生效。"));
            default -> {
            }
        }
    }

    private void removeSpeedModifiers(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        AttributeInstance attr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeOps.removeById(attr, WIND_SPEED_MODIFIER_ID);
        AttributeOps.removeById(attr, DOMAIN_SPEED_MODIFIER_ID);
    }

    private void updateWindSpeedModifier(LivingEntity entity, int stacks, boolean domainActive) {
        if (entity == null) {
            return;
        }
        AttributeInstance attr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) {
            return;
        }
        AttributeOps.removeById(attr, WIND_SPEED_MODIFIER_ID);
        AttributeOps.removeById(attr, DOMAIN_SPEED_MODIFIER_ID);
        if (stacks > 0) {
            AttributeModifier modifier = new AttributeModifier(WIND_SPEED_MODIFIER_ID,
                    stacks * WIND_STACK_SPEED_PER_LAYER, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            AttributeOps.replaceTransient(attr, WIND_SPEED_MODIFIER_ID, modifier);
        }
        if (domainActive) {
            AttributeModifier domainModifier = new AttributeModifier(DOMAIN_SPEED_MODIFIER_ID,
                    DOMAIN_SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            AttributeOps.replaceTransient(attr, DOMAIN_SPEED_MODIFIER_ID, domainModifier);
        }
    }

    private void updatePassiveCost(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state,
                                   OrganStateOps.Collector collector) {
        int ticks = state.getInt(KEY_PASSIVE_TICKS, 0) + 1;
        if (ticks >= PASSIVE_COST_INTERVAL) {
            ticks = 0;
            Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
            handleOpt.ifPresent(handle -> ResourceOps.tryConsumeScaledZhenyuan(handle, PASSIVE_BASE_COST));
        }
        collector.record(OrganStateOps.setInt(state, cc, organ, KEY_PASSIVE_TICKS, ticks, NON_NEGATIVE_INT, 0));
    }

    private void updateMovementCounters(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state,
                                        OrganStateOps.Collector collector, long now, int stage) {
        Vec3 pos = player.position();
        double lastX = state.getDouble(KEY_LAST_SAMPLE_X, pos.x);
        double lastY = state.getDouble(KEY_LAST_SAMPLE_Y, pos.y);
        double lastZ = state.getDouble(KEY_LAST_SAMPLE_Z, pos.z);
        long lastTick = state.getLong(KEY_LAST_SAMPLE_TICK, Long.MIN_VALUE);

        double distance = 0.0D;
        if (lastTick != Long.MIN_VALUE) {
            Vec3 last = new Vec3(lastX, lastY, lastZ);
            distance = pos.distanceTo(last);
        }

        collector.recordAll(
                OrganStateOps.setDouble(state, cc, organ, KEY_LAST_SAMPLE_X, pos.x, IDENTITY_DOUBLE, pos.x),
                OrganStateOps.setDouble(state, cc, organ, KEY_LAST_SAMPLE_Y, pos.y, IDENTITY_DOUBLE, pos.y),
                OrganStateOps.setDouble(state, cc, organ, KEY_LAST_SAMPLE_Z, pos.z, IDENTITY_DOUBLE, pos.z),
                OrganStateOps.setLong(state, cc, organ, KEY_LAST_SAMPLE_TICK, now, NON_NEGATIVE_LONG, now)
        );

        boolean sprinting = player.isSprinting() && !player.isPassenger() && !player.isSwimming()
                && !player.isSpectator() && !player.isFallFlying();
        if (sprinting && distance >= 0.2D) {
            long current = state.getLong(KEY_RUN_M, 0L);
            long gained = Math.max(1L, Math.round(distance * 100.0D));
            collector.record(OrganStateOps.setLong(state, cc, organ, KEY_RUN_M, current + gained, NON_NEGATIVE_LONG, 0L));
        }

        int chain = sprinting ? state.getInt(KEY_RUN_CHAIN_TICKS, 0) + WIND_STACK_GAIN_INTERVAL_TICKS : 0;
        if (chain >= 2 * 20 && stage >= 2) {
            chain = 0;
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, false, false, true));
        }
        collector.record(OrganStateOps.setInt(state, cc, organ, KEY_RUN_CHAIN_TICKS, chain, NON_NEGATIVE_INT, 0));

        updateWindStacks(player, cc, organ, state, collector, distance);
    }

    private void updateWindStacks(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state,
                                  OrganStateOps.Collector collector, double distance) {
        boolean moving = distance >= 0.2D || player.getDeltaMovement().horizontalDistanceSqr() > 0.01D;
        if (player.isPassenger() || player.isInWaterOrBubble()) {
            moving = false;
        }

        int stackTimer = state.getInt(KEY_WIND_STACK_TIMER, 0);
        int stopped = state.getInt(KEY_STOPPED_TICKS, 0);
        int stacks = Mth.clamp(state.getInt(KEY_WIND_STACKS, 0), 0, WIND_STACK_MAX);

        if (moving) {
            stackTimer += WIND_STACK_GAIN_INTERVAL_TICKS;
            stopped = 0;
            if (stackTimer >= WIND_STACK_GAIN_INTERVAL_TICKS) {
                stackTimer -= WIND_STACK_GAIN_INTERVAL_TICKS;
                if (stacks < WIND_STACK_MAX) {
                    stacks++;
                }
            }
        } else {
            stackTimer = 0;
            stopped = Math.min(WIND_STACK_RESET_TICKS, stopped + WIND_STACK_GAIN_INTERVAL_TICKS);
            if (stopped >= WIND_STACK_RESET_TICKS) {
                stacks = 0;
            }
        }

        collector.recordAll(
                OrganStateOps.setInt(state, cc, organ, KEY_WIND_STACK_TIMER, stackTimer, NON_NEGATIVE_INT, 0),
                OrganStateOps.setInt(state, cc, organ, KEY_STOPPED_TICKS, stopped, NON_NEGATIVE_INT, 0),
                OrganStateOps.setInt(state, cc, organ, KEY_WIND_STACKS, stacks, value -> Mth.clamp(value, 0, WIND_STACK_MAX), 0)
        );

        int fullTicks = stacks >= WIND_STACK_MAX ? state.getInt(KEY_FULL_STACK_TICKS, 0) + WIND_STACK_GAIN_INTERVAL_TICKS : 0;
        if (stacks < WIND_STACK_MAX) {
            fullTicks = 0;
        }
        if (fullTicks >= STACK_FULL_REQUIRED_TICKS) {
            fullTicks = 0;
            int count = state.getInt(KEY_STACK10_HOLD, 0) + 1;
            collector.record(OrganStateOps.setInt(state, cc, organ, KEY_STACK10_HOLD, count, NON_NEGATIVE_INT, 0));
        }
        collector.record(OrganStateOps.setInt(state, cc, organ, KEY_FULL_STACK_TICKS, fullTicks, NON_NEGATIVE_INT, 0));
    }

    private void updateNearMiss(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state,
                                OrganStateOps.Collector collector, long now) {
        Int2LongOpenHashMap tracker = NEAR_MISS_TRACKERS.computeIfAbsent(player.getUUID(), uuid -> {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.defaultReturnValue(Long.MIN_VALUE);
            return map;
        });
        java.util.Iterator<Int2LongMap.Entry> it = tracker.int2LongEntrySet().iterator();
        while (it.hasNext()) {
            Int2LongMap.Entry entry = it.next();
            if (now - entry.getLongValue() > 10L) {
                it.remove();
            }
        }

        AABB box = player.getBoundingBox().inflate(1.2D);
        Level level = player.level();
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, box,
                entity -> entity.isAlive() && entity.getOwner() != player)) {
            if (projectile.getBoundingBox().intersects(player.getBoundingBox())) {
                continue;
            }
            int id = projectile.getId();
            long lastTick = tracker.get(id);
            if (lastTick != Long.MIN_VALUE) {
                continue;
            }
            tracker.put(id, now);
            int count = state.getInt(KEY_NEAR_MISS, 0) + 1;
            collector.record(OrganStateOps.setInt(state, cc, organ, KEY_NEAR_MISS, count, NON_NEGATIVE_INT, 0));
        }
    }

    private void updateGlideAndAirTime(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state,
                                       OrganStateOps.Collector collector, long now, int stage) {
        boolean gliding = player.isFallFlying();
        int glideTicks = state.getInt(KEY_GLIDE_TICKS, 0);
        if (gliding) {
            glideTicks += WIND_STACK_GAIN_INTERVAL_TICKS;
            long air = state.getLong(KEY_AIR_TIME, 0L) + WIND_STACK_GAIN_INTERVAL_TICKS;
            collector.record(OrganStateOps.setLong(state, cc, organ, KEY_AIR_TIME, air, NON_NEGATIVE_LONG, 0L));
            if (glideTicks >= GLIDE_CHAIN_REQUIRED_TICKS) {
                glideTicks = 0;
                int chain = state.getInt(KEY_GLIDE_CHAIN, 0) + 1;
                collector.record(OrganStateOps.setInt(state, cc, organ, KEY_GLIDE_CHAIN, chain, NON_NEGATIVE_INT, 0));
            }

            if (stage >= 4) {
                Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
                if (handleOpt.isPresent()) {
                    OptionalDouble result = ResourceOps.tryConsumeScaledZhenyuan(handleOpt.get(), GLIDE_BASE_COST);
                    if (result.isEmpty()) {
                        player.sendSystemMessage(Component.literal("【清风轮】真元不足，技能中止。"));
                        player.stopFallFlying();
                        gliding = false;
                        glideTicks = 0;
                    }
                }
            }
        } else {
            glideTicks = 0;
        }
        collector.record(OrganStateOps.setInt(state, cc, organ, KEY_GLIDE_TICKS, glideTicks, NON_NEGATIVE_INT, 0));

        if (stage >= 4) {
            player.resetFallDistance();
            if (!player.hasEffect(MobEffects.SLOW_FALLING)) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, true));
            }
        }
    }

    private boolean updateDomainState(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state,
                                      OrganStateOps.Collector collector, MultiCooldown cooldown, long now, int stage) {
        MultiCooldown.Entry domainReady = cooldown.entry(KEY_DOMAIN_READY_AT).withClamp(NON_NEGATIVE_LONG).withDefault(0L);
        MultiCooldown.Entry domainActive = cooldown.entry(KEY_DOMAIN_ACTIVE_UNTIL).withClamp(NON_NEGATIVE_LONG).withDefault(0L);
        MultiCooldown.Entry dashReady = cooldown.entry(KEY_DASH_READY_AT).withClamp(NON_NEGATIVE_LONG).withDefault(0L);

        long activeUntil = domainActive.getReadyTick();
        boolean active = activeUntil > now;
        if (!active) {
            collector.record(OrganStateOps.setInt(state, cc, organ, KEY_DOMAIN_COST_TICKS, 0, NON_NEGATIVE_INT, 0));
            return false;
        }

        int accum = state.getInt(KEY_DOMAIN_COST_TICKS, 0) + WIND_STACK_GAIN_INTERVAL_TICKS;
        if (accum >= WIND_STACK_GAIN_INTERVAL_TICKS) {
            accum -= WIND_STACK_GAIN_INTERVAL_TICKS;
            Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
            if (handleOpt.isPresent()) {
                OptionalDouble result = ResourceOps.tryConsumeScaledZhenyuan(handleOpt.get(), DOMAIN_TICK_COST);
                if (result.isEmpty()) {
                    domainActive.setReadyAt(0L);
                    domainReady.setReadyAt(now + DOMAIN_COOLDOWN_TICKS);
                    player.sendSystemMessage(Component.literal("【清风轮】真元不足，技能中止。"));
                    collector.record(OrganStateOps.setInt(state, cc, organ, KEY_DOMAIN_COST_TICKS, 0, NON_NEGATIVE_INT, 0));
                    return false;
                }
            }
        }
        collector.record(OrganStateOps.setInt(state, cc, organ, KEY_DOMAIN_COST_TICKS, accum, NON_NEGATIVE_INT, 0));

        dashReady.setReadyAt(now);
        return true;
    }

    private void updateChannels(ChestCavityInstance cc, MultiCooldown cooldown, int windStacks, long now) {
        if (cc == null) {
            return;
        }
        ActiveLinkageContext context = LedgerOps.context(cc);
        if (context == null) {
            return;
        }
        LinkageChannel stackChannel = LedgerOps.ensureChannel(context, WIND_STACK_CHANNEL);
        if (stackChannel != null) {
            stackChannel.set(windStacks);
        }
        LinkageChannel ringChannel = LedgerOps.ensureChannel(context, WIND_RING_CHANNEL);
        if (ringChannel != null) {
            long remaining = Math.max(0L, cooldown.entry(KEY_RING_READY_AT).getReadyTick() - now);
            ringChannel.set(remaining);
        }
    }

    private void spawnTrailParticles(ServerLevel level, Player player, int stage, int windStacks, boolean domainActive) {
        if (level == null) {
            return;
        }
        Vec3 velocity = player.getDeltaMovement();
        if (player.isSprinting()) {
            Vec3 back = velocity.normalize().scale(-0.3D);
            Vec3 base = player.position().add(back.x, 0.1D, back.z);
            level.sendParticles(ParticleTypes.CLOUD, base.x, base.y, base.z, 2,
                    0.05D, 0.0D, 0.05D, 0.0D);
        }
        if (windStacks > 0 && stage >= 5) {
            Vec3 pos = player.position();
            double radius = 0.3D + windStacks * 0.05D;
            level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 0.4D, pos.z,
                    2, radius, 0.1D, radius, 0.0D);
        }
        if (domainActive) {
            Vec3 pos = player.position();
            level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.5D, pos.z,
                    6, 0.4D, 0.2D, 0.4D, 0.01D);
        }
    }

    private static void activateDash(LivingEntity living, ChestCavityInstance cc) {
        INSTANCE.handleDashActivation(living, cc);
    }

    private static void activateWindSlash(LivingEntity living, ChestCavityInstance cc) {
        INSTANCE.handleWindSlashActivation(living, cc);
    }

    private static void activateDomain(LivingEntity living, ChestCavityInstance cc) {
        INSTANCE.handleDomainActivation(living, cc);
    }

    private boolean isDomainActive(OrganState state, long now) {
        return state.getLong(KEY_DOMAIN_ACTIVE_UNTIL, 0L) > now;
    }

    private void handleDashActivation(LivingEntity living, ChestCavityInstance cc) {
        if (!(living instanceof ServerPlayer player) || cc == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (level == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        OrganState state = organState(organ, STATE_ROOT);
        int stage = determineStage(state);
        if (stage < 2) {
            return;
        }
        MultiCooldown cooldown = createCooldown(cc, organ, state);
        long now = level.getGameTime();
        boolean domainActive = isDomainActive(state, now);

        MultiCooldown.Entry dashReady = cooldown.entry(KEY_DASH_READY_AT).withClamp(NON_NEGATIVE_LONG).withDefault(0L);
        if (!domainActive && !dashReady.isReady(now)) {
            return;
        }
        MultiCooldown.Entry dashActive = cooldown.entry(KEY_DASH_ACTIVE_UNTIL).withClamp(NON_NEGATIVE_LONG).withDefault(0L);
        if (dashActive.getReadyTick() > now) {
            return;
        }

        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return;
        }
        OptionalDouble costResult = ResourceOps.tryConsumeScaledZhenyuan(handleOpt.get(), DASH_BASE_COST);
        if (costResult.isEmpty()) {
            player.sendSystemMessage(Component.literal("【清风轮】真元不足，技能中止。"));
            return;
        }

        dashActive.setReadyAt(now + DASH_DURATION_TICKS);
        if (!domainActive) {
            dashReady.setReadyAt(now + DASH_COOLDOWN_TICKS);
            ActiveSkillRegistry.scheduleReadyToast(player, DASH_ABILITY_ID, dashReady.getReadyTick(), now);
        } else {
            dashReady.setReadyAt(now);
        }

        MultiCooldown.Entry windowEntry = cooldown.entry(KEY_WIND_SLASH_WINDOW).withClamp(NON_NEGATIVE_LONG).withDefault(0L);
        windowEntry.setReadyAt(now + DASH_DURATION_TICKS + WIND_SLASH_WINDOW_TICKS);

        long lastCountTick = state.getLong(KEY_LAST_DASH_COUNT, 0L);
        if (now - lastCountTick >= 10L) {
            int used = state.getInt(KEY_DASH_USED, 0) + 1;
            OrganStateOps.setIntSync(cc, organ, STATE_ROOT, KEY_DASH_USED, used, NON_NEGATIVE_INT, 0);
            OrganStateOps.setLongSync(cc, organ, STATE_ROOT, KEY_LAST_DASH_COUNT, now, NON_NEGATIVE_LONG, 0L);
        }

        Vec3 dir = resolveDashDirection(player);
        if (dir.lengthSqr() < 1.0E-4D) {
            return;
        }

        int organSlot = ChestCavityUtil.findOrganSlot(cc, organ);
        DashSession session = new DashSession(player.getUUID(), level, organSlot, dir.normalize(), DASH_SPEED_PER_TICK,
                DASH_DURATION_TICKS, now, new IntOpenHashSet());
        ACTIVE_DASHES.put(player.getUUID(), session);

        spawnDashStartFx(level, player);
        applyDashMotion(player, dir.normalize(), DASH_SPEED_PER_TICK);
        scheduleDashTick(session, 1);
    }

    private void handleWindSlashActivation(LivingEntity living, ChestCavityInstance cc) {
        if (!(living instanceof ServerPlayer player) || cc == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (level == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        OrganState state = organState(organ, STATE_ROOT);
        int stage = determineStage(state);
        if (stage < 3) {
            return;
        }
        MultiCooldown cooldown = createCooldown(cc, organ, state);
        long now = level.getGameTime();
        MultiCooldown.Entry window = cooldown.entry(KEY_WIND_SLASH_WINDOW).withClamp(NON_NEGATIVE_LONG).withDefault(0L);
        if (window.getReadyTick() <= now) {
            return;
        }
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return;
        }
        if (ResourceOps.tryConsumeScaledZhenyuan(handleOpt.get(), WIND_SLASH_BASE_COST).isEmpty()) {
            player.sendSystemMessage(Component.literal("【清风轮】真元不足，技能中止。"));
            return;
        }

        window.setReadyAt(0L);
        MultiCooldown.Entry dashReady = cooldown.entry(KEY_DASH_READY_AT).withClamp(NON_NEGATIVE_LONG).withDefault(0L);
        dashReady.setReadyAt(now + DASH_COOLDOWN_TICKS);
        ActiveSkillRegistry.scheduleReadyToast(player, WIND_SLASH_ABILITY_ID, dashReady.getReadyTick(), now);
        cooldown.entry(KEY_DASH_ACTIVE_UNTIL).withClamp(NON_NEGATIVE_LONG).withDefault(0L).setReadyAt(now);

        performWindSlash(level, player);
    }

    private void handleDomainActivation(LivingEntity living, ChestCavityInstance cc) {
        if (!(living instanceof ServerPlayer player) || cc == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (level == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        OrganState state = organState(organ, STATE_ROOT);
        int stage = determineStage(state);
        if (stage < 5) {
            return;
        }
        if (state.getInt(KEY_WIND_STACKS, 0) < WIND_STACK_MAX) {
            player.sendSystemMessage(Component.literal("【风神领域】需要满 10 层风势。"));
            return;
        }

        MultiCooldown cooldown = createCooldown(cc, organ, state);
        long now = level.getGameTime();
        MultiCooldown.Entry ready = cooldown.entry(KEY_DOMAIN_READY_AT).withClamp(NON_NEGATIVE_LONG).withDefault(0L);
        if (!ready.isReady(now)) {
            return;
        }
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return;
        }
        if (ResourceOps.tryConsumeScaledZhenyuan(handleOpt.get(), DOMAIN_START_COST).isEmpty()) {
            player.sendSystemMessage(Component.literal("【清风轮】真元不足，技能中止。"));
            return;
        }

        long activeUntil = now + DOMAIN_DURATION_TICKS;
        ready.setReadyAt(now + DOMAIN_COOLDOWN_TICKS);
        ActiveSkillRegistry.scheduleReadyToast(player, DOMAIN_ABILITY_ID, ready.getReadyTick(), now);

        MultiCooldown.Entry activeEntry = cooldown.entry(KEY_DOMAIN_ACTIVE_UNTIL).withClamp(NON_NEGATIVE_LONG).withDefault(0L);
        activeEntry.setReadyAt(activeUntil);
        OrganStateOps.setLongSync(cc, organ, STATE_ROOT, KEY_DOMAIN_ACTIVE_UNTIL, activeUntil, NON_NEGATIVE_LONG, 0L);
        OrganStateOps.setIntSync(cc, organ, STATE_ROOT, KEY_DOMAIN_COST_TICKS, 0, NON_NEGATIVE_INT, 0);
        OrganStateOps.setIntSync(cc, organ, STATE_ROOT, KEY_WIND_STACKS, 0, value -> Mth.clamp(value, 0, WIND_STACK_MAX), 0);

        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.6D, player.getZ(),
                16, 0.6D, 0.3D, 0.6D, 0.02D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WIND_CHARGE_BURST,
                SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private static ItemStack findOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (!stack.isEmpty() && INSTANCE.matchesOrgan(stack, ORGAN_ID)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack getOrganFromSlot(ChestCavityInstance cc, int slot) {
        if (cc == null || cc.inventory == null || slot < 0 || slot >= cc.inventory.getContainerSize()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = cc.inventory.getItem(slot);
        if (stack.isEmpty() || !matchesOrgan(stack, ORGAN_ID)) {
            return ItemStack.EMPTY;
        }
        return stack;
    }

    private Vec3 resolveDashDirection(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            horizontal = Vec3.directionFromRotation(0.0F, player.getYRot());
            horizontal = new Vec3(horizontal.x, 0.0D, horizontal.z);
        }
        return horizontal.normalize();
    }

    private void spawnDashStartFx(ServerLevel level, Player player) {
        Vec3 pos = player.position();
        level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.1D, pos.z,
                8, 0.2D, 0.1D, 0.2D, 0.01D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ELYTRA_FLYING,
                SoundSource.PLAYERS, 0.6F, 1.2F);
    }

    private void spawnDashTrailFx(ServerLevel level, Player player) {
        Vec3 pos = player.position();
        level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.05D, pos.z,
                4, 0.15D, 0.05D, 0.15D, 0.01D);
    }

    private void applyDashMotion(ServerPlayer player, Vec3 direction, double speed) {
        Vec3 motion = new Vec3(direction.x * speed, player.getDeltaMovement().y * 0.2D, direction.z * speed);
        player.setDeltaMovement(motion);
        player.hurtMarked = true;
        player.hasImpulse = true;
        player.resetFallDistance();
    }

    private void scheduleDashTick(DashSession session, int tick) {
        TickOps.schedule(session.level(), () -> runDashTick(session, tick), 1);
    }

    private void runDashTick(DashSession session, int tick) {
        ServerLevel level = session.level();
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(session.playerId());
        if (player == null || player.isRemoved()) {
            ACTIVE_DASHES.remove(session.playerId(), session);
            return;
        }
        ChestCavityInstance cc = ChestCavityEntity.of(player)
                .map(ChestCavityEntity::getChestCavityInstance)
                .orElse(null);
        if (cc == null) {
            ACTIVE_DASHES.remove(session.playerId(), session);
            return;
        }
        ItemStack organ = getOrganFromSlot(cc, session.organSlot());
        if (organ.isEmpty()) {
            ACTIVE_DASHES.remove(session.playerId(), session);
            return;
        }
        OrganState state = organState(organ, STATE_ROOT);
        MultiCooldown cooldown = createCooldown(cc, organ, state);
        long now = level.getGameTime();
        if (tick > session.duration()) {
            ACTIVE_DASHES.remove(session.playerId(), session);
            return;
        }

        applyDashMotion(player, session.direction(), session.speed());
        spawnDashTrailFx(level, player);
        applyDashHits(player, cc, organ, state, cooldown, session, now);

        if (tick < session.duration()) {
            scheduleDashTick(session, tick + 1);
        } else {
            ACTIVE_DASHES.remove(session.playerId(), session);
        }
    }

    private boolean applyDashHits(ServerPlayer player, ChestCavityInstance cc, ItemStack organ, OrganState state,
                                  MultiCooldown cooldown, DashSession session, long now) {
        ServerLevel level = session.level();
        Vec3 dir = session.direction();
        AABB box = player.getBoundingBox().expandTowards(dir.scale(0.6D)).inflate(0.75D, 0.5D, 0.75D);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity.isAlive() && entity != player && !entity.isAlliedTo(player)
                        && session.hitEntities().add(entity.getId()));
        if (targets.isEmpty()) {
            return false;
        }
        boolean domainActive = isDomainActive(state, now);
        for (LivingEntity target : targets) {
            double dx = target.getX() - player.getX();
            double dz = target.getZ() - player.getZ();
            target.hurt(player.damageSources().playerAttack(player), DASH_DAMAGE);
            target.knockback(DASH_KNOCKBACK, dx, dz);
            if (domainActive) {
                level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + target.getBbHeight() * 0.5D,
                        target.getZ(), 6, 0.3D, 0.2D, 0.3D, 0.02D);
            }
        }

        long lastHitTick = state.getLong(KEY_LAST_DASH_HIT, 0L);
        if (now - lastHitTick >= 10L) {
            int hits = state.getInt(KEY_DASH_HIT, 0) + targets.size();
            OrganStateOps.setIntSync(cc, organ, STATE_ROOT, KEY_DASH_HIT, hits, NON_NEGATIVE_INT, 0);
            OrganStateOps.setLongSync(cc, organ, STATE_ROOT, KEY_LAST_DASH_HIT, now, NON_NEGATIVE_LONG, 0L);
        }
        return true;
    }

    private void performWindSlash(ServerLevel level, ServerPlayer player) {
        Vec3 dir = resolveDashDirection(player);
        Vec3 origin = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        boolean hitAny = false;
        for (int i = 1; i <= WIND_SLASH_SEGMENTS; i++) {
            Vec3 center = origin.add(dir.scale(i * WIND_SLASH_RANGE));
            AABB box = new AABB(center, center).inflate(0.75D, 0.5D, 0.75D);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                    entity -> entity.isAlive() && entity != player && !entity.isAlliedTo(player));
            if (targets.isEmpty()) {
                continue;
            }
            for (LivingEntity target : targets) {
                double dx = target.getX() - player.getX();
                double dz = target.getZ() - player.getZ();
                target.hurt(player.damageSources().playerAttack(player), WIND_SLASH_DAMAGE);
                target.knockback(0.3F, dx, dz);
                hitAny = true;
            }
            level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, 4, 0.2D, 0.1D, 0.2D, 0.01D);
        }
        if (hitAny) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 0.6F, 1.0F);
        }
    }

    private record DashSession(UUID playerId, ServerLevel level, int organSlot, Vec3 direction, double speed, int duration,
                               long startTick, IntOpenHashSet hitEntities) {
    }
}
