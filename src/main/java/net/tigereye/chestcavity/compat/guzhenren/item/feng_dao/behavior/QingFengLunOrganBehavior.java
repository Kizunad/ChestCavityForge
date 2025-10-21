package net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.behavior;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.AttributeOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
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
import net.tigereye.chestcavity.compat.guzhenren.util.CombatEntityUtil;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 清风轮蛊 - 以移动与疾风主题的位移器官。
 */
public final class QingFengLunOrganBehavior extends AbstractGuzhenrenOrganBehavior
        implements OrganSlowTickListener, OrganIncomingDamageListener, OrganOnHitListener, OrganRemovalListener {

    public static final QingFengLunOrganBehavior INSTANCE = new QingFengLunOrganBehavior();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "qing_feng_lun_gu");

    private static final ResourceLocation ABILITY_DASH_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "qing_feng_lun/dash");
    private static final ResourceLocation ABILITY_CLEAVE_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "qing_feng_lun/wind_cleave");
    private static final ResourceLocation ABILITY_DOMAIN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "qing_feng_lun/domain");

    private static final ResourceLocation CHANNEL_WIND_STACKS = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/qingfenglun/wind_stacks");
    private static final ResourceLocation CHANNEL_WIND_RING = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/qingfenglun/windring_cd");

    private static final ClampPolicy CLAMP_STACKS = new ClampPolicy(0.0D, 10.0D);
    private static final ClampPolicy CLAMP_RING = new ClampPolicy(0.0D, 20.0D);

    private static final String STATE_ROOT = "qing_feng_lun";
    private static final String KEY_TIER = "tier";
    private static final String KEY_RUN = "run_m";
    private static final String KEY_DASH_USED = "dash_used";
    private static final String KEY_DASH_HIT = "dash_hit";
    private static final String KEY_NEAR_MISS = "near_miss";
    private static final String KEY_AIR_TIME = "air_time";
    private static final String KEY_RING_BLOCK = "ring_block";
    private static final String KEY_STACK_HOLD = "stack10_hold";
    private static final String KEY_GLIDE_CHAIN = "glide_chain";
    private static final String KEY_WIND_STACKS = "wind_stacks";
    private static final String KEY_LAST_MOVED = "last_moved";
    private static final String KEY_PASSIVE_READY = "passive_ready";
    private static final String KEY_LAST_DASH_USED_TICK = "last_dash_used_tick";
    private static final String KEY_LAST_DASH_HIT_TICK = "last_dash_hit_tick";
    private static final String KEY_LAST_NEAR_MISS_TICK = "last_near_miss_tick";
    private static final String KEY_RING_READY = "wind_ring_ready";
    private static final String KEY_DASH_READY = "dash_ready";
    private static final String KEY_CLEAVE_WINDOW = "cleave_window";
    private static final String KEY_DOMAIN_READY = "domain_ready";
    private static final String KEY_DOMAIN_END = "domain_end";
    private static final String KEY_GLIDE_SESSION = "glide_session";
    private static final String KEY_STACK_MAX_TICK = "stack_max_tick";
    private static final String KEY_WINDRUN_START = "windrun_start";
    private static final String KEY_WINDRUN_END = "windrun_end";

    private static final double PASSIVE_COST = 60.0D;
    private static final double DASH_COST = 2200.0D;
    private static final double CLEAVE_COST = 38000.0D;
    private static final double GLIDE_COST_PER_SEC = 140000.0D;
    private static final double DOMAIN_START_COST = 120000.0D;
    private static final double DOMAIN_TICK_COST = 20000.0D;

    private static final int PASSIVE_INTERVAL = 4 * 20;
    private static final int DASH_COOLDOWN_TICKS = 6 * 20;
    private static final int CLEAVE_WINDOW_TICKS = Mth.ceil(0.25F * 20.0F);
    private static final int RING_COOLDOWN_TICKS = 8 * 20;
    private static final int DOMAIN_COOLDOWN_TICKS = 45 * 20;
    private static final int DOMAIN_DURATION_TICKS = 10 * 20;
    private static final int MAX_TIER = 5;
    private static final int MIN_TIER = 1;
    private static final int STACKS_MAX = 10;
    private static final int STACK_RESET_DELAY = 2 * 20;
    private static final double MOVE_THRESHOLD = 0.04D;

    private static final long RUN_THRESHOLD = 3_000L;
    private static final int DASH_USED_THRESHOLD = 50;
    private static final int DASH_HIT_THRESHOLD = 50;
    private static final int NEAR_MISS_THRESHOLD = 30;
    private static final long AIR_TIME_THRESHOLD = 300L * 20L; // stored in ticks
    private static final int RING_BLOCK_THRESHOLD = 25;
    private static final int STACK_HOLD_THRESHOLD = 12;
    private static final int GLIDE_CHAIN_THRESHOLD = 10;

    private static final double DASH_DISTANCE = 6.0D;
    private static final double DASH_KNOCKBACK = 0.4D;
    private static final double CLEAVE_DAMAGE = 6.0D;
    private static final double CLEAVE_KNOCKBACK = 0.5D;
    private static final double DOMAIN_SPEED_BONUS = 0.20D;
    private static final double DOMAIN_PROJECTILE_SLOW = 0.30D;
    private static final double WIND_STACK_SPEED = 0.02D;
    private static final double WIND_STACK_DODGE = 0.01D;
    private static final double WINDRUN_SPEED_MULTIPLIER = 0.25D;

    private static final ResourceLocation STACK_SPEED_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifier/qing_feng_lun/speed");
    private static final ResourceLocation DOMAIN_SPEED_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifier/qing_feng_lun/domain_speed");
    private static final ResourceLocation WINDRUN_SPEED_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifier/qing_feng_lun/windrun");
    private static final int WINDRUN_TRIGGER_TICKS = 2 * 20;
    private static final int WINDRUN_DURATION_TICKS = 3 * 20;

    private static final Map<UUID, Vec3> LAST_SAMPLE_POS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_SAMPLE_TICK = new ConcurrentHashMap<>();

    private QingFengLunOrganBehavior() {
        registerAbilities();
        NeoForge.EVENT_BUS.addListener(this::onPlayerClone);
    }

    private void registerAbilities() {
        OrganActivationListeners.register(ABILITY_DASH_ID, (entity, cc) -> {
            if (entity instanceof ServerPlayer serverPlayer) {
                activateDash(serverPlayer, cc);
            }
        });
        OrganActivationListeners.register(ABILITY_CLEAVE_ID, (entity, cc) -> {
            if (entity instanceof ServerPlayer serverPlayer) {
                activateCleave(serverPlayer, cc);
            }
        });
        OrganActivationListeners.register(ABILITY_DOMAIN_ID, (entity, cc) -> {
            if (entity instanceof ServerPlayer serverPlayer) {
                activateDomain(serverPlayer, cc);
            }
        });
    }

    private void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        if (original != null) {
            UUID id = original.getUUID();
            LAST_SAMPLE_POS.remove(id);
            LAST_SAMPLE_TICK.remove(id);
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
        long gameTime = level.getGameTime();

        OrganState state = organState(organ, STATE_ROOT);
        MultiCooldown cooldown = createCooldown(cc, organ, state);

        ensureChannels(cc);

        tickPassiveCost(player, state, cooldown, cc, organ, gameTime);
        sampleMovement(player, cc, organ, state, gameTime);
        updateWindStacks(player, cc, organ, state, cooldown, gameTime);
        updateGlide(player, cc, organ, state, cooldown, gameTime);
        updateDomain(player, cc, organ, state, cooldown, gameTime);
        updateTier(player, cc, organ, state, gameTime);
        pushLinkage(player, cc, organ, state, cooldown, gameTime);
    }

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!(victim instanceof Player player) || victim.level().isClientSide() || damage <= 0.0F) {
            return damage;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return damage;
        }
        Level level = victim.level();
        OrganState state = organState(organ, STATE_ROOT);
        MultiCooldown cooldown = createCooldown(cc, organ, state);
        long gameTime = level.getGameTime();
        int tier = resolveTier(state);

        if (source != null && source.is(DamageTypeTags.IS_PROJECTILE)) {
            maybeRecordNearMiss(player, cc, organ, state, gameTime);
        }

        if (tier >= 4 && source != null && source.is(DamageTypeTags.IS_PROJECTILE)) {
            MultiCooldown.Entry ringReady = cooldown.entry(KEY_RING_READY);
            if (ringReady.isReady(gameTime)) {
                ringReady.setReadyAt(gameTime + RING_COOLDOWN_TICKS);
                OrganStateOps.setInt(state, cc, organ, KEY_RING_BLOCK,
                        state.getInt(KEY_RING_BLOCK, 0) + 1, value -> Math.max(0, value), 0);
                spawnWindRing(player);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WIND_CHARGE_BURST,
                        player.getSoundSource(), 0.25F, 1.0F);
                return 0.0F;
            }
        }

        if (tier >= 3 && source != null && source.is(DamageTypeTags.IS_PROJECTILE)) {
            double chance = 0.10D;
            if (tier >= 5) {
                chance += state.getInt(KEY_WIND_STACKS, 0) * WIND_STACK_DODGE;
            }
            chance = Mth.clamp(chance, 0.0D, 0.75D);
            if (player.getDeltaMovement().horizontalDistanceSqr() > MOVE_THRESHOLD
                    && player.getRandom().nextDouble() < chance) {
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
        MultiCooldown cooldown = createCooldown(cc, organ, state);
        long now = attacker.level().getGameTime();
        MultiCooldown.Entry window = cooldown.entry(KEY_CLEAVE_WINDOW);
        if (window.getReadyTick() > now) {
            long lastHit = state.getLong(KEY_LAST_DASH_HIT_TICK, 0L);
            if (now - lastHit >= 10L) {
                OrganStateOps.setInt(state, cc, organ, KEY_DASH_HIT,
                        state.getInt(KEY_DASH_HIT, 0) + 1,
                        value -> Math.max(0, value), 0);
                state.setLong(KEY_LAST_DASH_HIT_TICK, now, value -> Math.max(0L, value), 0L);
            }
        }
        return damage;
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }
        ActiveLinkageContext linkageContext = LedgerOps.context(cc);
        if (linkageContext != null) {
            linkageContext.lookupChannel(CHANNEL_WIND_STACKS).ifPresent(channel -> channel.set(0.0D));
            linkageContext.lookupChannel(CHANNEL_WIND_RING).ifPresent(channel -> channel.set(0.0D));
        }
        if (entity instanceof Player owner) {
            UUID id = owner.getUUID();
            LAST_SAMPLE_POS.remove(id);
            LAST_SAMPLE_TICK.remove(id);
            AttributeInstance attr = owner.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attr != null) {
                AttributeOps.removeById(attr, STACK_SPEED_MOD);
                AttributeOps.removeById(attr, DOMAIN_SPEED_MOD);
                AttributeOps.removeById(attr, WINDRUN_SPEED_MOD);
            }
        }
    }

    public void ensureAttached(ChestCavityInstance cc) {
        ensureChannels(cc);
    }

    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }
        ensureChannels(cc);
        registerRemovalHook(cc, organ, this, staleRemovalContexts);
    }

    private void ensureChannels(ChestCavityInstance cc) {
        ActiveLinkageContext context = LedgerOps.context(cc);
        if (context == null) {
            return;
        }
        LedgerOps.ensureChannel(context, CHANNEL_WIND_STACKS, CLAMP_STACKS);
        LedgerOps.ensureChannel(context, CHANNEL_WIND_RING, CLAMP_RING);
    }

    private MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ, OrganState state) {
        MultiCooldown.Builder builder = MultiCooldown.builder(state)
                .withLongClamp(value -> Math.max(0L, value), 0L)
                .withIntClamp(value -> Math.max(0, value), 0);
        if (cc != null) {
            builder.withSync(cc, organ);
        } else {
            builder.withOrgan(organ);
        }
        return builder.build();
    }

    private void tickPassiveCost(Player player, OrganState state, MultiCooldown cooldown,
                                 ChestCavityInstance cc, ItemStack organ, long gameTime) {
        MultiCooldown.Entry passiveReady = cooldown.entry(KEY_PASSIVE_READY);
        if (passiveReady.getReadyTick() == 0L) {
            passiveReady.setReadyAt(gameTime + PASSIVE_INTERVAL);
            return;
        }
        if (!passiveReady.isReady(gameTime)) {
            return;
        }
        passiveReady.setReadyAt(gameTime + PASSIVE_INTERVAL);
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return;
        }
        OptionalDouble consumed = handleOpt.get().consumeScaledZhenyuan(PASSIVE_COST);
        if (consumed.isEmpty()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("【清风轮】真元不足，技能中止。")
                    .withStyle(ChatFormatting.GRAY), true);
        }
    }

    private void sampleMovement(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, long gameTime) {
        UUID id = player.getUUID();
        Vec3 current = player.position();
        Vec3 last = LAST_SAMPLE_POS.put(id, current);
        long lastTick = LAST_SAMPLE_TICK.getOrDefault(id, gameTime);
        LAST_SAMPLE_TICK.put(id, gameTime);
        if (last == null) {
            return;
        }
        if (player.isPassenger()) {
            return;
        }
        if (gameTime - lastTick <= 0L) {
            return;
        }
        Vec3 delta = current.subtract(last);
        double horizontal = new Vec3(delta.x, 0.0D, delta.z).length();
        if (horizontal < 0.2D) {
            return;
        }
        if (!player.isSprinting()) {
            return;
        }
        double meters = horizontal;
        long hundred = Mth.floor(meters * 100.0D);
        if (hundred <= 0L) {
            return;
        }
        long currentRun = state.getLong(KEY_RUN, 0L);
        long updated = Math.max(0L, currentRun + hundred);
        OrganStateOps.setLong(state, cc, organ, KEY_RUN, updated, value -> Math.max(0L, value), 0L);
        OrganStateOps.setLong(state, cc, organ, KEY_LAST_MOVED, gameTime, value -> Math.max(0L, value), 0L);
    }

    private void updateWindStacks(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state,
                                  MultiCooldown cooldown, long gameTime) {
        int tier = resolveTier(state);
        int stacks = Mth.clamp(state.getInt(KEY_WIND_STACKS, 0), 0, STACKS_MAX);
        long lastMoved = state.getLong(KEY_LAST_MOVED, 0L);
        boolean moving = player.isSprinting() || player.getDeltaMovement().horizontalDistanceSqr() > MOVE_THRESHOLD;
        long windrunEnd = state.getLong(KEY_WINDRUN_END, 0L);
        if (moving) {
            if (gameTime > lastMoved) {
                OrganStateOps.setLong(state, cc, organ, KEY_LAST_MOVED, gameTime, value -> Math.max(0L, value), 0L);
            }
            if (gameTime - lastMoved >= 20L) {
                stacks = Math.min(STACKS_MAX, stacks + 1);
                OrganStateOps.setInt(state, cc, organ, KEY_WIND_STACKS, stacks, value -> Mth.clamp(value, 0, STACKS_MAX), 0);
            }
            if (tier >= 2) {
                long start = state.getLong(KEY_WINDRUN_START, 0L);
                if (player.isSprinting()) {
                    if (start == 0L) {
                        OrganStateOps.setLong(state, cc, organ, KEY_WINDRUN_START, gameTime, value -> Math.max(0L, value), 0L);
                    } else if (gameTime - start >= WINDRUN_TRIGGER_TICKS && windrunEnd <= gameTime) {
                        long newEnd = gameTime + WINDRUN_DURATION_TICKS;
                        OrganStateOps.setLong(state, cc, organ, KEY_WINDRUN_END, newEnd, value -> Math.max(0L, value), 0L);
                        OrganStateOps.setLong(state, cc, organ, KEY_WINDRUN_START, gameTime, value -> Math.max(0L, value), 0L);
                        windrunEnd = newEnd;
                    }
                }
            }
        } else {
            if (gameTime - lastMoved >= STACK_RESET_DELAY) {
                stacks = 0;
                OrganStateOps.setInt(state, cc, organ, KEY_WIND_STACKS, 0, value -> Mth.clamp(value, 0, STACKS_MAX), 0);
            }
            if (tier >= 2) {
                OrganStateOps.setLong(state, cc, organ, KEY_WINDRUN_START, 0L, value -> Math.max(0L, value), 0L);
            }
        }

        if (stacks >= STACKS_MAX) {
            long recorded = state.getLong(KEY_STACK_MAX_TICK, 0L);
            if (recorded == 0L) {
                OrganStateOps.setLong(state, cc, organ, KEY_STACK_MAX_TICK, gameTime, value -> Math.max(0L, value), 0L);
            } else if (gameTime - recorded >= 5 * 20L) {
                int achieved = state.getInt(KEY_STACK_HOLD, 0) + 1;
                OrganStateOps.setInt(state, cc, organ, KEY_STACK_HOLD, achieved, value -> Math.max(0, value), 0);
                OrganStateOps.setLong(state, cc, organ, KEY_STACK_MAX_TICK, gameTime, value -> Math.max(0L, value), 0L);
            }
        } else {
            OrganStateOps.setLong(state, cc, organ, KEY_STACK_MAX_TICK, 0L, value -> Math.max(0L, value), 0L);
        }

        windrunEnd = state.getLong(KEY_WINDRUN_END, 0L);
        if (windrunEnd > 0L && windrunEnd <= gameTime) {
            OrganStateOps.setLong(state, cc, organ, KEY_WINDRUN_END, 0L, value -> Math.max(0L, value), 0L);
            windrunEnd = 0L;
        }
        applyStackAttributes(player, stacks, tier >= 5 && isDomainActive(state, gameTime), windrunEnd, gameTime);
    }

    private void updateGlide(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state,
                             MultiCooldown cooldown, long gameTime) {
        int tier = resolveTier(state);
        if (tier < 4) {
            return;
        }
        if (player.onGround()) {
            OrganStateOps.setLong(state, cc, organ, KEY_GLIDE_SESSION, 0L, value -> Math.max(0L, value), 0L);
            return;
        }
        boolean isGliding = player.isFallFlying();
        long sessionStart = state.getLong(KEY_GLIDE_SESSION, 0L);
        if (!isGliding) {
            if (sessionStart != 0L) {
                long duration = gameTime - sessionStart;
                if (duration >= 12 * 20L) {
                    OrganStateOps.setInt(state, cc, organ, KEY_GLIDE_CHAIN,
                            state.getInt(KEY_GLIDE_CHAIN, 0) + 1,
                            value -> Math.max(0, value), 0);
                }
                OrganStateOps.setLong(state, cc, organ, KEY_GLIDE_SESSION, 0L, value -> Math.max(0L, value), 0L);
            }
            return;
        }

        if (sessionStart == 0L) {
            OrganStateOps.setLong(state, cc, organ, KEY_GLIDE_SESSION, gameTime, value -> Math.max(0L, value), 0L);
        }

        OrganStateOps.setLong(state, cc, organ, KEY_AIR_TIME,
                state.getLong(KEY_AIR_TIME, 0L) + 20L,
                value -> Math.max(0L, value), 0L);

        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return;
        }
        OptionalDouble consumed = handleOpt.get().consumeScaledZhenyuan(GLIDE_COST_PER_SEC);
        if (consumed.isEmpty()) {
            player.stopFallFlying();
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("【清风轮】真元不足，技能中止。")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (tier >= 4) {
            player.fallDistance = 0.0F;
        }
    }

    private void updateDomain(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state,
                              MultiCooldown cooldown, long gameTime) {
        long domainEnd = state.getLong(KEY_DOMAIN_END, 0L);
        if (domainEnd <= gameTime) {
            removeDomainEffects(player);
            if (domainEnd != 0L) {
                OrganStateOps.setLong(state, cc, organ, KEY_DOMAIN_END, 0L, value -> Math.max(0L, value), 0L);
            }
            return;
        }
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return;
        }
        OptionalDouble consumed = handleOpt.get().consumeScaledZhenyuan(DOMAIN_TICK_COST);
        if (consumed.isEmpty()) {
            endDomain(player, cc, organ, state, gameTime);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("【清风轮】真元不足，技能中止。")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (player.tickCount % 20 == 0) {
            applyDomainAura(player);
        }
    }

    private void updateTier(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, long gameTime) {
        int tier = resolveTier(state);
        int newTier = tier;
        long run = state.getLong(KEY_RUN, 0L);
        int dashUsed = state.getInt(KEY_DASH_USED, 0);
        int dashHit = state.getInt(KEY_DASH_HIT, 0);
        int nearMiss = state.getInt(KEY_NEAR_MISS, 0);
        long airTime = state.getLong(KEY_AIR_TIME, 0L);
        int ringBlock = state.getInt(KEY_RING_BLOCK, 0);
        int stackHold = state.getInt(KEY_STACK_HOLD, 0);
        int glideChain = state.getInt(KEY_GLIDE_CHAIN, 0);

        if (newTier < 2 && run >= RUN_THRESHOLD && dashUsed >= DASH_USED_THRESHOLD) {
            newTier = 2;
            toastTier(player, "【清风轮·风轮形】已觉醒。获得：疾风冲刺、风行加速。");
        }
        if (newTier < 3 && dashHit >= DASH_HIT_THRESHOLD && nearMiss >= NEAR_MISS_THRESHOLD) {
            newTier = 3;
            toastTier(player, "【清风轮·破阵风】已觉醒。获得：风裂步、移动闪避。");
        }
        if (newTier < 4 && airTime >= AIR_TIME_THRESHOLD && ringBlock >= RING_BLOCK_THRESHOLD) {
            newTier = 4;
            toastTier(player, "【清风轮·御风轮】已觉醒。获得：免疫摔落、风环护盾、御风翔行。");
        }
        if (newTier < 5 && stackHold >= STACK_HOLD_THRESHOLD && glideChain >= GLIDE_CHAIN_THRESHOLD) {
            newTier = 5;
            toastTier(player, "【清风轮·风神轮】已觉醒。可启：风神领域；风势层数蓄积生效。");
        }
        if (newTier != tier) {
            OrganStateOps.setInt(state, cc, organ, KEY_TIER, newTier, value -> Mth.clamp(value, MIN_TIER, MAX_TIER), MIN_TIER);
        }
    }

    private void pushLinkage(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state,
                              MultiCooldown cooldown, long gameTime) {
        ActiveLinkageContext context = LedgerOps.context(cc);
        if (context == null) {
            return;
        }
        int stacks = Mth.clamp(state.getInt(KEY_WIND_STACKS, 0), 0, STACKS_MAX);
        LinkageChannel stackChannel = LedgerOps.ensureChannel(context, CHANNEL_WIND_STACKS, CLAMP_STACKS);
        if (stackChannel != null) {
            stackChannel.set(stacks);
        }
        MultiCooldown.Entry ringReady = cooldown.entry(KEY_RING_READY);
        long remaining = Math.max(0L, ringReady.getReadyTick() - gameTime);
        LinkageChannel ringChannel = LedgerOps.ensureChannel(context, CHANNEL_WIND_RING, CLAMP_RING);
        if (ringChannel != null) {
            ringChannel.set(remaining / 20.0D);
        }
    }

    private void applyStackAttributes(Player player, int stacks, boolean domainActive, long windrunEnd, long gameTime) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        double bonus = stacks * WIND_STACK_SPEED;
        if (bonus > 0.0D) {
            AttributeOps.replaceTransient(speed, STACK_SPEED_MOD,
                    new AttributeModifier(STACK_SPEED_MOD, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else {
            AttributeOps.removeById(speed, STACK_SPEED_MOD);
        }
        if (domainActive) {
            AttributeOps.replaceTransient(speed, DOMAIN_SPEED_MOD,
                    new AttributeModifier(DOMAIN_SPEED_MOD, DOMAIN_SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else {
            AttributeOps.removeById(speed, DOMAIN_SPEED_MOD);
        }
        if (windrunEnd > gameTime) {
            AttributeOps.replaceTransient(speed, WINDRUN_SPEED_MOD,
                    new AttributeModifier(WINDRUN_SPEED_MOD, WINDRUN_SPEED_MULTIPLIER, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else {
            AttributeOps.removeById(speed, WINDRUN_SPEED_MOD);
        }
    }

    private void removeDomainEffects(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            AttributeOps.removeById(speed, DOMAIN_SPEED_MOD);
        }
    }

    private void applyDomainAura(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        AABB area = serverPlayer.getBoundingBox().inflate(4.0D);
        List<LivingEntity> allies = serverPlayer.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive() && !CombatEntityUtil.areEnemies(serverPlayer, entity));
        for (LivingEntity ally : allies) {
            if (ally == serverPlayer) {
                continue;
            }
            ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false, true));
        }
        List<LivingEntity> enemies = serverPlayer.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive() && CombatEntityUtil.areEnemies(serverPlayer, entity));
        for (LivingEntity enemy : enemies) {
            enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true));
        }
    }

    private void spawnWindRing(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = player.position();
        for (int i = 0; i < 12; i++) {
            double angle = (Math.PI * 2.0D * i) / 12.0D;
            double dx = Math.cos(angle) * 1.2D;
            double dz = Math.sin(angle) * 1.2D;
            level.sendParticles(ParticleTypes.CLOUD, center.x + dx, center.y + 0.1D, center.z + dz,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private void toastTier(Player player, String message) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal(message).withStyle(ChatFormatting.AQUA), true);
    }

    private void maybeRecordNearMiss(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, long gameTime) {
        long last = state.getLong(KEY_LAST_NEAR_MISS_TICK, 0L);
        if (gameTime - last < 10L) {
            return;
        }
        int count = state.getInt(KEY_NEAR_MISS, 0) + 1;
        OrganStateOps.setInt(state, cc, organ, KEY_NEAR_MISS, count, value -> Math.max(0, value), 0);
        OrganStateOps.setLong(state, cc, organ, KEY_LAST_NEAR_MISS_TICK, gameTime, value -> Math.max(0L, value), 0L);
    }

    private int resolveTier(OrganState state) {
        int tier = Mth.clamp(state.getInt(KEY_TIER, MIN_TIER), MIN_TIER, MAX_TIER);
        state.setInt(KEY_TIER, tier, value -> Mth.clamp(value, MIN_TIER, MAX_TIER), MIN_TIER);
        return tier;
    }

    private boolean isDomainActive(OrganState state, long gameTime) {
        return state.getLong(KEY_DOMAIN_END, 0L) > gameTime;
    }

    private void activateDash(ServerPlayer player, ChestCavityInstance cc) {
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        OrganState state = organState(organ, STATE_ROOT);
        int tier = resolveTier(state);
        if (tier < 2) {
            return;
        }
        MultiCooldown cooldown = createCooldown(cc, organ, state);
        long now = player.level().getGameTime();
        MultiCooldown.Entry ready = cooldown.entry(KEY_DASH_READY);
        if (!ready.isReady(now) && !isDomainActive(state, now)) {
            return;
        }
        long lastDash = state.getLong(KEY_LAST_DASH_USED_TICK, 0L);
        if (now - lastDash < 10L && !isDomainActive(state, now)) {
            return;
        }
        OptionalDouble consumed = ResourceOps.tryConsumeScaledZhenyuan(player, DASH_COST);
        if (consumed.isEmpty()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("【清风轮】真元不足，技能中止。")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        ready.setReadyAt(now + DASH_COOLDOWN_TICKS);
        MultiCooldown.Entry window = cooldown.entry(KEY_CLEAVE_WINDOW);
        window.setReadyAt(now + CLEAVE_WINDOW_TICKS);
        performDash(player);
        OrganStateOps.setInt(state, cc, organ, KEY_DASH_USED,
                state.getInt(KEY_DASH_USED, 0) + 1,
                value -> Math.max(0, value), 0);
        state.setLong(KEY_LAST_DASH_USED_TICK, now, value -> Math.max(0L, value), 0L);
        if (isDomainActive(state, now)) {
            ready.setReadyAt(now);
        }
    }

    private void performDash(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            return;
        }
        Vec3 direction = horizontal.normalize();
        Vec3 dest = player.position().add(direction.scale(DASH_DISTANCE));
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        Level level = player.level();
        for (int i = 1; i <= DASH_DISTANCE * 2; i++) {
            Vec3 step = player.position().add(direction.scale(i * 0.5D));
            cursor.set(step.x(), step.y(), step.z());
            BlockState state = level.getBlockState(cursor);
            if (!state.isAir()) {
                dest = step.subtract(direction.scale(0.5D));
                break;
            }
        }
        player.teleportTo(dest.x, dest.y, dest.z);
        player.level().gameEvent(GameEvent.TELEPORT, dest, GameEvent.Context.of(player));
        knockbackAround(player, DASH_KNOCKBACK);
        player.level().playSound(null, dest.x, dest.y, dest.z, SoundEvents.ELYTRA_FLYING, player.getSoundSource(), 0.4F, 1.2F);
    }

    private void knockbackAround(ServerPlayer player, double strength) {
        AABB area = player.getBoundingBox().inflate(1.5D);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && CombatEntityUtil.areEnemies(player, entity));
        for (LivingEntity target : targets) {
            Vec3 delta = target.position().subtract(player.position());
            Vec3 push = new Vec3(delta.x, 0.2D, delta.z).normalize().scale(strength);
            target.push(push.x, push.y, push.z);
            target.hurtMarked = true;
        }
    }

    private void activateCleave(ServerPlayer player, ChestCavityInstance cc) {
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        OrganState state = organState(organ, STATE_ROOT);
        int tier = resolveTier(state);
        if (tier < 3) {
            return;
        }
        MultiCooldown cooldown = createCooldown(cc, organ, state);
        long now = player.level().getGameTime();
        MultiCooldown.Entry window = cooldown.entry(KEY_CLEAVE_WINDOW);
        if (window.getReadyTick() <= now) {
            return;
        }
        OptionalDouble consumed = ResourceOps.tryConsumeScaledZhenyuan(player, CLEAVE_COST);
        if (consumed.isEmpty()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("【清风轮】真元不足，技能中止。")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        performCleave(player);
        window.setReadyAt(now);
    }

    private void performCleave(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 direction = new Vec3(look.x, 0.0D, look.z).normalize();
        Vec3 start = player.position().add(0.0D, 0.5D, 0.0D);
        Vec3 end = start.add(direction.scale(5.0D));
        AABB box = new AABB(start, end).inflate(1.0D);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != player && CombatEntityUtil.areEnemies(player, entity));
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().playerAttack(player), (float) CLEAVE_DAMAGE);
            Vec3 push = direction.scale(CLEAVE_KNOCKBACK);
            target.push(push.x, 0.1D, push.z);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                player.getSoundSource(), 0.6F, 1.0F);
    }

    private void activateDomain(ServerPlayer player, ChestCavityInstance cc) {
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        OrganState state = organState(organ, STATE_ROOT);
        int tier = resolveTier(state);
        if (tier < 5) {
            return;
        }
        MultiCooldown cooldown = createCooldown(cc, organ, state);
        long now = player.level().getGameTime();
        MultiCooldown.Entry ready = cooldown.entry(KEY_DOMAIN_READY);
        if (!ready.isReady(now)) {
            return;
        }
        OptionalDouble consumed = ResourceOps.tryConsumeScaledZhenyuan(player, DOMAIN_START_COST);
        if (consumed.isEmpty()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("【清风轮】真元不足，技能中止。")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        ready.setReadyAt(now + DOMAIN_COOLDOWN_TICKS);
        OrganStateOps.setLong(state, cc, organ, KEY_DOMAIN_END, now + DOMAIN_DURATION_TICKS, value -> Math.max(0L, value), 0L);
        applyDomainAura(player);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WIND_CHARGE_BURST,
                player.getSoundSource(), 0.5F, 1.0F);
    }

    private void endDomain(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, long gameTime) {
        removeDomainEffects(player);
        OrganStateOps.setLong(state, cc, organ, KEY_DOMAIN_END, 0L, value -> Math.max(0L, value), 0L);
    }

    private ItemStack findOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (Objects.equals(id, ORGAN_ID)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
