package net.tigereye.chestcavity.compat.guzhenren.item.lei_dao.behavior;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps;
import net.tigereye.chestcavity.engine.dot.DoTEngine;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.DoTTypes;
import net.tigereye.chestcavity.util.NetworkUtil;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * 雷盾蛊（Lei Dun Gu）行为实现：围绕护盾吸收、冲击反击与主动技雷枢展开。
 */
public enum LeiDunGuOrganBehavior implements OrganIncomingDamageListener, OrganSlowTickListener, OrganOnHitListener {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "leidungu");
    private static final ResourceLocation ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "leidungu_active");

    private static final int ABSORB_PER_HIT_MAX = 100;
    private static final int REPAIR_DELAY_TICKS = 60;
    private static final double REPAIR_COST_ZHENYUAN_BASE = 1200.0;
    private static final double REPAIR_COST_JINGLI_BASE = 20.0;
    private static final double REPAIR_DISCOUNT_4Z = 0.9;

    private static final int SHOCK_ABSORB_TRIGGER = 600;
    private static final int SHOCK_CD_TICKS = 300;
    private static final int SHOCK_RADIUS_BASE = 4;
    private static final int SHOCK_RADIUS_4Z = 5;
    private static final int SHOCK_DMG_MIN = 600;
    private static final int SHOCK_DMG_MAX = 1000;
    private static final int SHOCK_DMG_MAX_4Z = 1100;

    private static final int OVERLOAD_ABSORB_REQ = 1000;
    private static final int THUNDER_STRIKE_BASE = 1400;
    private static final float THUNDER_STRIKE_CHAIN_RATIO = 0.6f;

    private static final int COUNTER_ARC_DAMAGE = 120;
    private static final int COUNTER_ARC_COOLDOWN = 16;

    private static final int ACTIVE_CD_TICKS = 400;
    private static final double ACTIVE_BASE_ZHENYUAN_COST = 450.0;
    private static final double ACTIVE_JINGLI_COST = 12.0;
    private static final double ACTIVE_BASE_DAMAGE = 500.0;
    private static final double ACTIVE_BASE_DOT = 100.0;
    private static final int ACTIVE_BASE_DOT_SECONDS = 3;
    private static final double ACTIVE_DOT_4Z = 120.0;
    private static final double ACTIVE_DOT_5Z_STRONG = 160.0;
    private static final int ACTIVE_STRONG_SECONDS = 5;
    private static final int ACTIVE_ROOT_TICKS = 8; // 0.4s

    private static final int STABLE_WINDOW_TICKS = 30;

    private static final int TSXP_ABSORB_STEP = 200;
    private static final int TSXP_THRESHOLD_4Z = 100;
    private static final int TSXP_THRESHOLD_5Z = 350;
    private static final int TSXP_LIMIT_PER_TARGET = 2;
    private static final int TSXP_LIMIT_WINDOW_TICKS = 20;

    private static final String STATE_ROOT = "LeiDunGu";
    private static final String KEY_TIER = "Tier";
    private static final String KEY_TSXP = "Tsxp";
    private static final String KEY_TSXP_BUFFER = "TsxpBuffer";
    private static final String KEY_ABSORB_PROGRESS = "AbsorbProgress";
    private static final String KEY_OVERLOAD_PROGRESS = "OverloadProgress";
    private static final String KEY_SHIELD_ACTIVE = "ShieldActive";
    private static final String KEY_STABLE_UNTIL = "StableUntil";

    private static final String COOLDOWN_ROOT = "leidungu/state";
    private static final String KEY_SHIELD_BROKEN_UNTIL = "shield_broken_until";
    private static final String KEY_SHOCK_CD_UNTIL = "shock_cd_until";
    private static final String KEY_CHARGE_STACKS = "charge_stacks";
    private static final String KEY_COUNTER_CD_UNTIL = "counter_cd_until";
    private static final String KEY_ABILITY_CHARGES = "ability_charges";
    private static final String KEY_ABILITY_NEXT_RECHARGE = "ability_next_recharge";
    private static final String KEY_TSXP_LIMIT_PREFIX = "tsxp_limit/";
    private static final String KEY_TSXP_LIMIT_TICK_SUFFIX = "/tick";
    private static final String KEY_TSXP_LIMIT_VALUE_SUFFIX = "/value";

    private static final List<MobEffectInstance> DOT_SLOW_EFFECTS = ImmutableList.of(
            new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 6, false, true, true),
            new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20, 4, false, true, true)
    );

    static {
        OrganActivationListeners.register(ABILITY_ID, LeiDunGuOrganBehavior::activateAbility);
        NeoForge.EVENT_BUS.addListener(LeiDunGuOrganBehavior::onLivingDeath);
    }

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (victim == null || victim.level().isClientSide() || cc == null) {
            return damage;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return damage;
        }
        if (damage <= 0.0f) {
            return damage;
        }

        OrganState state = organState(organ, STATE_ROOT);
        MultiCooldown cooldown = createCooldown(cc, organ);
        boolean shieldActive = state.getBoolean(KEY_SHIELD_ACTIVE, true);
        if (!shieldActive) {
            return damage;
        }

        Level level = victim.level();
        long now = level.getGameTime();
        int tier = getTier(state);
        MultiCooldown.Entry counterEntry = cooldown.entry(KEY_COUNTER_CD_UNTIL);

        if (tier >= 5) {
            handleCounterArc(source, victim, counterEntry, now);
        }

        float incoming = damage;
        if (incoming <= ABSORB_PER_HIT_MAX) {
            absorbDamage(victim, cc, organ, state, cooldown, now, Math.round(incoming));
            return 0.0f;
        }

        long stableUntil = state.getLong(KEY_STABLE_UNTIL, 0L);
        boolean inStableWindow = tier >= 4 && stableUntil > now;
        if (inStableWindow) {
            float overflow = incoming - ABSORB_PER_HIT_MAX;
            absorbDamage(victim, cc, organ, state, cooldown, now, ABSORB_PER_HIT_MAX);
            reflectOverflow(source, victim, overflow * 0.5f);
            return Math.max(0.0f, overflow);
        }

        breakShield(victim, cc, organ, state, cooldown, now);
        return damage;
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide() || cc == null || !matchesOrgan(organ, ORGAN_ID)) {
            return;
        }
        OrganState state = organState(organ, STATE_ROOT);
        MultiCooldown cooldown = createCooldown(cc, organ);
        long now = entity.level().getGameTime();

        maintainShield(entity, cc, organ, state, cooldown, now);
        rechargeAbility(entity, cc, organ, state, cooldown, now);
        handleTsxpUpgrade(entity, cc, organ, state);
    }

    @Override
    public float onHit(DamageSource source, LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!matchesOrgan(organ, ORGAN_ID) || attacker == null || attacker.level().isClientSide()) {
            return damage;
        }
        // 雷盾蛊对主动打击无额外处理，保持原始伤害。
        return damage;
    }

    private void absorbDamage(LivingEntity owner, ChestCavityInstance cc, ItemStack organ, OrganState state, MultiCooldown cooldown, long now, int absorbed) {
        if (absorbed <= 0) {
            return;
        }
        int previous = state.getInt(KEY_ABSORB_PROGRESS, 0);
        int updated = previous + absorbed;
        OrganStateOps.setInt(state, cc, organ, KEY_ABSORB_PROGRESS, updated, value -> Math.max(0, value), 0);

        int overload = state.getInt(KEY_OVERLOAD_PROGRESS, 0) + absorbed;
        OrganStateOps.setInt(state, cc, organ, KEY_OVERLOAD_PROGRESS, overload, value -> Math.max(0, value), 0);

        accumulateTsxp(cc, organ, state, absorbed);
        tryTriggerShock(owner, cc, organ, state, cooldown, now, updated);
    }

    private void tryTriggerShock(LivingEntity owner, ChestCavityInstance cc, ItemStack organ, OrganState state, MultiCooldown cooldown, long now, int accumulated) {
        if (accumulated < SHOCK_ABSORB_TRIGGER) {
            return;
        }
        MultiCooldown.Entry shockEntry = cooldown.entry(KEY_SHOCK_CD_UNTIL);
        if (!shockEntry.isReady(now)) {
            return;
        }

        int tier = getTier(state);
        boolean overloadReady = tier >= 5 && state.getInt(KEY_OVERLOAD_PROGRESS, 0) >= OVERLOAD_ABSORB_REQ;
        int radius = tier >= 4 ? SHOCK_RADIUS_4Z : SHOCK_RADIUS_BASE;
        float baseDamage = computeShockDamage(accumulated, tier);

        int hits = overloadReady
                ? performThunderStrike(owner, tier, radius)
                : performShock(owner, tier, baseDamage, radius);
        if (hits <= 0) {
            return;
        }

        int remaining = Math.max(0, accumulated - SHOCK_ABSORB_TRIGGER);
        OrganStateOps.setInt(state, cc, organ, KEY_ABSORB_PROGRESS, remaining, value -> Math.max(0, value), 0);
        shockEntry.setReadyAt(now + SHOCK_CD_TICKS);

        MultiCooldown.EntryInt chargeEntry = cooldown.entryInt(KEY_CHARGE_STACKS)
                .withClamp(value -> Math.max(0, Math.min(3, value)));
        chargeEntry.setTicks(Math.min(3, chargeEntry.getTicks() + 1));

        addTsxp(cc, organ, state, 3);
        spawnShockEffects(owner, radius, overloadReady);

        if (overloadReady) {
            OrganStateOps.setInt(state, cc, organ, KEY_OVERLOAD_PROGRESS, 0, value -> Math.max(0, value), 0);
            broadcast(owner, "msg.leidungu.overload");
        }
    }

    private float computeShockDamage(int accumulated, int tier) {
        float base = 650.0f + (accumulated * 0.25f);
        int upper = tier >= 4 ? SHOCK_DMG_MAX_4Z : SHOCK_DMG_MAX;
        return Mth.clamp(base, SHOCK_DMG_MIN, upper);
    }

    private int performShock(LivingEntity owner, int tier, float damage, int radius) {
        if (!(owner.level() instanceof ServerLevel server)) {
            return 0;
        }
        Vec3 center = owner.position();
        AABB box = owner.getBoundingBox().inflate(radius);
        List<LivingEntity> victims = server.getEntitiesOfClass(LivingEntity.class, box,
                candidate -> candidate != null && candidate.isAlive() && candidate != owner && !candidate.isAlliedTo(owner));
        if (victims.isEmpty()) {
            return 0;
        }
        DamageSource source = owner.damageSources().indirectMagic(owner, owner);
        for (LivingEntity victim : victims) {
            victim.hurt(source, damage);
            int duration = tier >= 4 ? 100 : 60;
            ReactionTagOps.add(victim, ReactionTagKeys.LIGHTNING_CHARGE, duration);
        }
        return victims.size();
    }

    private int performThunderStrike(LivingEntity owner, int tier, int radius) {
        if (!(owner.level() instanceof ServerLevel server)) {
            return 0;
        }
        Vec3 center = owner.position();
        AABB area = owner.getBoundingBox().inflate(radius);
        List<LivingEntity> candidates = server.getEntitiesOfClass(LivingEntity.class, area,
                target -> target != null && target.isAlive() && target != owner && !target.isAlliedTo(owner));
        if (candidates.isEmpty()) {
            return 0;
        }
        candidates.sort(Comparator.comparingDouble(target -> target.distanceToSqr(center)));
        LivingEntity primary = candidates.get(0);
        DamageSource source = owner.damageSources().indirectMagic(owner, owner);
        primary.hurt(source, THUNDER_STRIKE_BASE);
        ReactionTagOps.add(primary, ReactionTagKeys.LIGHTNING_CHARGE, 120);

        int chained = 0;
        for (int i = 1; i < candidates.size() && chained < 2; i++) {
            LivingEntity victim = candidates.get(i);
            victim.hurt(source, THUNDER_STRIKE_BASE * THUNDER_STRIKE_CHAIN_RATIO);
            ReactionTagOps.add(victim, ReactionTagKeys.LIGHTNING_CHARGE, 120);
            chained++;
        }
        return 1 + chained;
    }

    private void spawnShockEffects(LivingEntity owner, int radius, boolean overload) {
        Level level = owner.level();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        RandomSource random = level.getRandom();
        Vec3 center = owner.position();
        int particles = overload ? 80 : 40;
        for (int i = 0; i < particles; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = random.nextDouble() * radius;
            double x = center.x + Math.cos(angle) * distance;
            double z = center.z + Math.sin(angle) * distance;
            double y = owner.getY(0.5);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
        }
        SoundSource channel = owner instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE;
        level.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                overload ? SoundEvents.LIGHTNING_BOLT_IMPACT : SoundEvents.ENCHANTMENT_TABLE_USE,
                channel, overload ? 1.3f : 0.9f, overload ? 0.6f : 1.2f);
    }

    private void reflectOverflow(DamageSource source, LivingEntity victim, float reflectedDamage) {
        if (reflectedDamage <= 0.0f) {
            return;
        }
        Entity attacker = source == null ? null : source.getEntity();
        if (!(attacker instanceof LivingEntity living)) {
            return;
        }
        DamageSource reflect = victim.damageSources().indirectMagic(victim, victim);
        living.hurt(reflect, reflectedDamage);
        ReactionTagOps.add(living, ReactionTagKeys.LIGHTNING_CHARGE, 40);
    }

    private void breakShield(LivingEntity owner, ChestCavityInstance cc, ItemStack organ, OrganState state, MultiCooldown cooldown, long now) {
        MultiCooldown.Entry brokenEntry = cooldown.entry(KEY_SHIELD_BROKEN_UNTIL);
        brokenEntry.setReadyAt(now + REPAIR_DELAY_TICKS);
        boolean dirty = OrganStateOps.setBoolean(state, cc, organ, KEY_SHIELD_ACTIVE, false, true).changed();
        OrganStateOps.setInt(state, cc, organ, KEY_ABSORB_PROGRESS, 0, value -> 0, 0);
        OrganStateOps.setInt(state, cc, organ, KEY_OVERLOAD_PROGRESS, 0, value -> 0, 0);
        if (dirty) {
            sendSlotUpdate(cc, organ);
        }
        Level level = owner.level();
        level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.8f, 0.8f);
    }

    private void handleCounterArc(DamageSource source, LivingEntity owner, MultiCooldown.Entry counterEntry, long now) {
        if (source == null || counterEntry == null || !counterEntry.isReady(now)) {
            return;
        }
        Entity direct = source.getDirectEntity();
        if (!(direct instanceof LivingEntity attacker)) {
            return;
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            return;
        }
        counterEntry.setReadyAt(now + COUNTER_ARC_COOLDOWN);
        DamageSource feedback = owner.damageSources().indirectMagic(owner, owner);
        attacker.hurt(feedback, COUNTER_ARC_DAMAGE);
        ReactionTagOps.add(attacker, ReactionTagKeys.LIGHTNING_CHARGE, 40);
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    private void maintainShield(LivingEntity entity, ChestCavityInstance cc, ItemStack organ, OrganState state, MultiCooldown cooldown, long now) {
        boolean shieldActive = state.getBoolean(KEY_SHIELD_ACTIVE, true);
        MultiCooldown.Entry brokenEntry = cooldown.entry(KEY_SHIELD_BROKEN_UNTIL);
        if (!shieldActive && brokenEntry.getReadyTick() <= now) {
            attemptRepair(entity, cc, organ, state, cooldown, now);
        }
    }

    private void attemptRepair(LivingEntity entity, ChestCavityInstance cc, ItemStack organ, OrganState state, MultiCooldown cooldown, long now) {
        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(entity);
        int tier = getTier(state);
        double zhenyuanBase = REPAIR_COST_ZHENYUAN_BASE * (tier >= 4 ? REPAIR_DISCOUNT_4Z : 1.0);
        double jingliCost = REPAIR_COST_JINGLI_BASE * (tier >= 4 ? REPAIR_DISCOUNT_4Z : 1.0);

        double refunded = 0.0;
        if (handleOpt.isPresent()) {
            ResourceHandle handle = handleOpt.get();
            OptionalDouble consumed = handle.consumeScaledZhenyuan(zhenyuanBase);
            if (consumed.isEmpty()) {
                return;
            }
            refunded = consumed.getAsDouble();
            OptionalDouble jingli = handle.adjustJingli(-jingliCost, true);
            if (jingli.isEmpty()) {
                handle.adjustZhenyuan(refunded, true);
                return;
            }
        }

        OrganStateOps.setBoolean(state, cc, organ, KEY_SHIELD_ACTIVE, true, true);
        cooldown.entry(KEY_SHIELD_BROKEN_UNTIL).setReadyAt(0L);
        if (tier >= 4) {
            OrganStateOps.setLong(state, cc, organ, KEY_STABLE_UNTIL, now + STABLE_WINDOW_TICKS, value -> Math.max(0L, value), 0L);
        }
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.7f, 1.2f);
        if (entity.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getY(0.7), entity.getZ(), 30, 0.3, 0.4, 0.3, 0.1);
        }
    }

    private void rechargeAbility(LivingEntity entity, ChestCavityInstance cc, ItemStack organ, OrganState state, MultiCooldown cooldown, long now) {
        MultiCooldown.EntryInt chargesEntry = cooldown.entryInt(KEY_ABILITY_CHARGES)
                .withClamp(value -> Math.max(0, Math.min(2, value)));
        MultiCooldown.Entry nextRechargeEntry = cooldown.entry(KEY_ABILITY_NEXT_RECHARGE);
        int tier = getTier(state);
        int maxCharges = tier >= 5 ? 2 : 1;
        int charges = chargesEntry.getTicks();
        if (charges > maxCharges) {
            chargesEntry.setTicks(maxCharges);
            charges = maxCharges;
        }
        if (charges < maxCharges && nextRechargeEntry.isReady(now)) {
            chargesEntry.setTicks(charges + 1);
            charges = charges + 1;
            if (charges < maxCharges) {
                nextRechargeEntry.setReadyAt(now + ACTIVE_CD_TICKS);
            }
        }
        if (charges >= maxCharges) {
            nextRechargeEntry.setReadyAt(now);
        }
    }

    private void handleTsxpUpgrade(LivingEntity entity, ChestCavityInstance cc, ItemStack organ, OrganState state) {
        int tier = getTier(state);
        if (tier >= 5) {
            return;
        }
        int tsxp = state.getInt(KEY_TSXP, 0);
        int threshold = tier == 3 ? TSXP_THRESHOLD_4Z : TSXP_THRESHOLD_5Z;
        if (tsxp < threshold) {
            return;
        }
        int newTier = Math.min(5, tier + 1);
        int remaining = tsxp - threshold;
        OrganStateOps.setInt(state, cc, organ, KEY_TIER, newTier, value -> Mth.clamp(value, 3, 5), 3);
        OrganStateOps.setInt(state, cc, organ, KEY_TSXP, remaining, value -> Math.max(0, value), 0);
        String key = newTier == 4 ? "msg.leidungu.upgrade.4z" : "msg.leidungu.upgrade.5z";
        broadcast(entity, key);
    }

    private void accumulateTsxp(ChestCavityInstance cc, ItemStack organ, OrganState state, int absorbed) {
        int buffer = state.getInt(KEY_TSXP_BUFFER, 0) + absorbed;
        int gained = 0;
        while (buffer >= TSXP_ABSORB_STEP) {
            buffer -= TSXP_ABSORB_STEP;
            gained++;
        }
        if (gained > 0) {
            addTsxp(cc, organ, state, gained);
        }
        OrganStateOps.setInt(state, cc, organ, KEY_TSXP_BUFFER, buffer, value -> Math.max(0, value), 0);
    }

    private void addTsxp(ChestCavityInstance cc, ItemStack organ, OrganState state, int amount) {
        if (amount <= 0) {
            return;
        }
        int current = state.getInt(KEY_TSXP, 0);
        int updated = current + amount;
        OrganStateOps.setInt(state, cc, organ, KEY_TSXP, updated, value -> Math.max(0, value), 0);
    }

    private static int getTier(OrganState state) {
        return Mth.clamp(state.getInt(KEY_TIER, 3), 3, 5);
    }

    private static MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ) {
        MultiCooldown.Builder builder = MultiCooldown.builder(OrganState.of(organ, COOLDOWN_ROOT))
                .withLongClamp(value -> Math.max(0L, value), 0L)
                .withIntClamp(value -> Math.max(0, value), 0);
        if (cc != null) {
            builder.withSync(cc, organ);
        } else {
            builder.withOrgan(organ);
        }
        return builder.build();
    }

    private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof Player player) || player.level().isClientSide() || cc == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        OrganState state = INSTANCE.organState(organ, STATE_ROOT);
        MultiCooldown cooldown = INSTANCE.createCooldown(cc, organ);
        MultiCooldown.EntryInt chargeEntry = cooldown.entryInt(KEY_CHARGE_STACKS)
                .withClamp(value -> Math.max(0, Math.min(3, value)));
        MultiCooldown.EntryInt abilityCharges = cooldown.entryInt(KEY_ABILITY_CHARGES)
                .withClamp(value -> Math.max(0, Math.min(2, value)));
        MultiCooldown.Entry nextRecharge = cooldown.entry(KEY_ABILITY_NEXT_RECHARGE);
        long now = player.level().getGameTime();

        int tier = getTier(state);
        int maxCharges = tier >= 5 ? 2 : 1;
        if (abilityCharges.getTicks() <= 0) {
            if (nextRecharge.getReadyTick() <= now) {
                abilityCharges.setTicks(1);
            } else {
                return;
            }
        }

        boolean strongCast = tier >= 5 && chargeEntry.getTicks() >= 3 && player.isShiftKeyDown();
        int requiredStacks = strongCast ? 3 : 1;
        if (chargeEntry.getTicks() < requiredStacks) {
            return;
        }

        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        double actualRefund = 0.0;
        if (handleOpt.isPresent()) {
            ResourceHandle handle = handleOpt.get();
            OptionalDouble consume = handle.consumeScaledZhenyuan(ACTIVE_BASE_ZHENYUAN_COST);
            if (consume.isEmpty()) {
                return;
            }
            actualRefund = consume.getAsDouble();
            OptionalDouble jingli = handle.adjustJingli(-ACTIVE_JINGLI_COST, true);
            if (jingli.isEmpty()) {
                handle.adjustZhenyuan(actualRefund, true);
                return;
            }
        }

        LivingEntity target = findAbilityTarget(player, 8.0);
        if (target == null) {
            if (handleOpt.isPresent()) {
                handleOpt.get().adjustZhenyuan(actualRefund, true);
                handleOpt.get().adjustJingli(ACTIVE_JINGLI_COST, true);
            }
            return;
        }

        applyAbilityEffects(player, target, tier, strongCast);
        chargeEntry.setTicks(chargeEntry.getTicks() - requiredStacks);
        abilityCharges.setTicks(Math.max(0, abilityCharges.getTicks() - 1));
        boolean needsRecharge = abilityCharges.getTicks() < maxCharges;
        if (needsRecharge) {
            nextRecharge.setReadyAt(now + ACTIVE_CD_TICKS);
            if (player instanceof ServerPlayer serverPlayer) {
                ActiveSkillRegistry.scheduleReadyToast(serverPlayer, ABILITY_ID, now + ACTIVE_CD_TICKS, now);
            }
        }
        INSTANCE.addTsxpForTarget(cc, organ, state, cooldown, target, 2, now, TSXP_LIMIT_PER_TARGET);
    }

    private static ItemStack findOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (INSTANCE.matchesOrgan(stack, ORGAN_ID)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static LivingEntity findAbilityTarget(Player player, double range) {
        HitResult hit = player.pick(range, 0.0f, false);
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living && living.isAlive()) {
            return living;
        }
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(range));
        AABB area = new AABB(start, end).inflate(1.5);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, area,
                candidate -> candidate != null && candidate.isAlive() && candidate != player && !candidate.isAlliedTo(player));
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(start)));
        return candidates.get(0);
    }

    private static void applyAbilityEffects(Player caster, LivingEntity target, int tier, boolean strongCast) {
        DamageSource source = caster.damageSources().indirectMagic(caster, caster);
        double initial = strongCast ? 600.0 : ACTIVE_BASE_DAMAGE;
        target.hurt(source, (float) initial);
        int durationSeconds = strongCast ? ACTIVE_STRONG_SECONDS : ACTIVE_BASE_DOT_SECONDS;
        double perSecond = strongCast ? ACTIVE_DOT_5Z_STRONG : (tier >= 4 ? ACTIVE_DOT_4Z : ACTIVE_BASE_DOT);
        DoTEngine.schedulePerSecond(caster, target, perSecond, durationSeconds, SoundEvents.BEACON_AMBIENT, 0.6f, 1.5f,
                DoTTypes.LEI_DAO_ELECTRIC_SHOCK, null, DoTEngine.FxAnchor.TARGET, Vec3.ZERO, 1.0f);
        if (caster.level() instanceof ServerLevel server) {
            for (int i = 0; i < durationSeconds; i++) {
                final int delay = i * 20;
                TickOps.schedule(server, () -> applySlowPulse(target), delay);
            }
            if (strongCast) {
                TickOps.schedule(server, () -> applyRoot(target), durationSeconds * 20);
            }
        }
        ReactionTagOps.add(target, ReactionTagKeys.LIGHTNING_CHARGE, durationSeconds * 20 + 40);
        caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.7f, strongCast ? 0.8f : 1.1f);
    }

    private static void applySlowPulse(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }
        for (MobEffectInstance effect : DOT_SLOW_EFFECTS) {
            target.addEffect(new MobEffectInstance(effect));
        }
    }

    private static void applyRoot(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ACTIVE_ROOT_TICKS, 10, false, true, true));
    }

    private static void broadcast(LivingEntity entity, String translationKey) {
        if (!(entity instanceof Player player)) {
            return;
        }
        player.displayClientMessage(Component.translatable(translationKey), true);
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim == null || victim.level().isClientSide()) {
            return;
        }
        Entity killerEntity = event.getSource().getEntity();
        if (!(killerEntity instanceof LivingEntity killer)) {
            return;
        }
        if (!ReactionTagOps.has(victim, ReactionTagKeys.LIGHTNING_CHARGE)) {
            return;
        }
        ChestCavityInstance cc = ChestCavityEntity.of(killer)
                .map(ChestCavityEntity::getChestCavityInstance)
                .orElse(null);
        if (cc == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty() || !INSTANCE.matchesOrgan(organ, ORGAN_ID)) {
            return;
        }
        OrganState state = INSTANCE.organState(organ, STATE_ROOT);
        MultiCooldown cooldown = INSTANCE.createCooldown(cc, organ);
        long now = killer.level().getGameTime();
        INSTANCE.addTsxpForTarget(cc, organ, state, cooldown, victim, 5, now, 5);
    }

    private boolean matchesOrgan(ItemStack stack, ResourceLocation organId) {
        if (stack == null || stack.isEmpty() || organId == null) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return organId.equals(id);
    }

    private OrganState organState(ItemStack stack, String rootKey) {
        return OrganState.of(stack, rootKey);
    }

    private void sendSlotUpdate(ChestCavityInstance cc, ItemStack organ) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }

    private void addTsxpForTarget(
            ChestCavityInstance cc,
            ItemStack organ,
            OrganState state,
            MultiCooldown cooldown,
            LivingEntity target,
            int amount,
            long now,
            int capOverride
    ) {
        if (target == null || amount <= 0 || cooldown == null) {
            return;
        }
        UUID uuid = target.getUUID();
        String baseKey = KEY_TSXP_LIMIT_PREFIX + uuid;
        MultiCooldown.Entry windowEntry = cooldown.entry(baseKey + KEY_TSXP_LIMIT_TICK_SUFFIX).withDefault(0L);
        int limit = Math.max(TSXP_LIMIT_PER_TARGET, capOverride);
        MultiCooldown.EntryInt valueEntry = cooldown.entryInt(baseKey + KEY_TSXP_LIMIT_VALUE_SUFFIX)
                .withClamp(current -> Math.max(0, Math.min(limit, current)));
        long windowStart = windowEntry.getReadyTick();
        if (now - windowStart >= TSXP_LIMIT_WINDOW_TICKS || now < windowStart) {
            windowEntry.setReadyAt(now);
            valueEntry.setTicks(0);
            windowStart = now;
        }
        int current = valueEntry.getTicks();
        int remaining = limit - current;
        if (remaining <= 0) {
            return;
        }
        int granted = Math.min(amount, remaining);
        if (granted <= 0) {
            return;
        }
        addTsxp(cc, organ, state, granted);
        valueEntry.setTicks(current + granted);
        windowEntry.setReadyAt(windowStart);
    }
}
