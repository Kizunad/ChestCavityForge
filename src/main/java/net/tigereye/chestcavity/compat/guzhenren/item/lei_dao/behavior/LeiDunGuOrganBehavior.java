package net.tigereye.chestcavity.compat.guzhenren.item.lei_dao.behavior;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.CombatEntityUtil;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.engine.dot.DoTEngine;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.DoTTypes;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Behaviour implementation for 雷盾蛊。
 * <p>
 * 负责护盾吸收、冲击反击、主动雷枢技能、计数升级与资源消耗等核心逻辑。
 */
public final class LeiDunGuOrganBehavior extends AbstractGuzhenrenOrganBehavior implements
        OrganIncomingDamageListener, OrganSlowTickListener, OrganOnHitListener, OrganRemovalListener {

    public static final LeiDunGuOrganBehavior INSTANCE = new LeiDunGuOrganBehavior();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "leidungu");
    public static final ResourceLocation ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "leidungu_active");

    private static final String STATE_ROOT = "leidungu/state";
    private static final String KEY_SHIELD_ACTIVE = "shield_active";
    private static final String KEY_ABSORB_PROGRESS = "absorb_progress";
    private static final String KEY_ABSORB_SINCE_SHOCK = "absorbed_since_shock";
    private static final String KEY_OVERLOAD_ABSORB = "overload_absorb";
    private static final String KEY_CHARGE_STACKS = "charge_stacks";
    private static final String KEY_ACTIVE_CHARGES = "active_charges";
    private static final String KEY_SHIELD_BROKEN_UNTIL = "shield_broken_until";
    private static final String KEY_STABLE_UNTIL = "stable_window_until";
    private static final String KEY_SHOCK_CD_UNTIL = "shock_cd_until";
    private static final String KEY_ACTIVE_CD_UNTIL = "active_cd_until";
    private static final String KEY_NEXT_CHARGE_READY = "next_charge_ready";
    private static final String KEY_COUNTER_ICD_UNTIL = "counter_icd_until";

    private static final int ABSORB_PER_HIT_MAX = 100;
    private static final int REPAIR_COST_ZHENYUAN_3Z = 1200;
    private static final int REPAIR_COST_JINGLI_3Z = 20;
    private static final int REPAIR_DELAY_TICKS = 60;

    private static final int SHOCK_ABSORB_TRIGGER = 600;
    private static final int SHOCK_CD_TICKS = 300;
    private static final int SHOCK_RADIUS_BASE = 4;
    private static final int SHOCK_DMG_MIN = 600;
    private static final int SHOCK_DMG_MAX = 1000;

    private static final int ACTIVE_CD_TICKS = 400;
    private static final int ACTIVE_COST_ZHENYUAN_3Z = 450;
    private static final int ACTIVE_COST_JINGLI_3Z = 12;
    private static final int DOT_DMG_PER_SEC_3Z = 100;

    private static final float REPAIR_DISCOUNT_4Z = 0.9f;
    private static final int STABLE_WINDOW_TICKS = 30;
    private static final float REFLECT_RATIO_4Z = 0.5f;
    private static final int SHOCK_RADIUS_4Z = 5;
    private static final int SHOCK_MAX_4Z = 1100;
    private static final int DOT_DMG_PER_SEC_4Z = 120;

    private static final int OVERLOAD_ABSORB_REQ = 1000;
    private static final int THUNDER_STRIKE_BASE = 1400;
    private static final float CHAIN_RATIO = 0.6f;
    private static final int COUNTER_ARC_DMG = 120;
    private static final int COUNTER_ARC_ICD_TICKS = 16;
    private static final int ACTIVE_DOT_PER_SEC_5Z_STRONG = 160;
    private static final int ACTIVE_STRONG_DURATION_SECONDS = 5;
    private static final int ACTIVE_BASE_DURATION_SECONDS = 3;

    private static final int TIER_THREE = 3;
    private static final int TIER_FOUR = 4;
    private static final int TIER_FIVE = 5;
    private static final double T4_TSXP_THRESHOLD = 100.0;
    private static final double T5_TSXP_THRESHOLD = 350.0;

    private static final int TSXP_ABSORB_STEP = 200;

    private static final int TSXP_SHOCK_REWARD = 3;
    private static final int TSXP_ACTIVE_REWARD = 2;
    private static final int TSXP_KILL_REWARD = 5;

    private static final int MAX_CHARGE_STACKS = 3;
    private static final int MAX_ACTIVE_CHARGES = 2;

    private static final Map<UUID, Map<UUID, TsxpBucket>> TSXP_LIMITER = new HashMap<>();

    static {
        OrganActivationListeners.register(ABILITY_ID, LeiDunGuOrganBehavior::activateAbility);
    }

    private LeiDunGuOrganBehavior() {
    }

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (victim == null || victim.level().isClientSide() || cc == null || organ == null || organ.isEmpty()) {
            return damage;
        }
        if (!isPrimaryOrgan(cc, organ)) {
            return damage;
        }
        if (damage <= 0.0f) {
            return damage;
        }

        OrganState state = resolveOrganState(organ, STATE_ROOT);
        MultiCooldown cooldown = createCooldown(cc, organ);
        boolean shieldActive = state.getBoolean(KEY_SHIELD_ACTIVE, true);
        long now = victim.level().getGameTime();
        int tier = resolveTier(victim);

        if (!shieldActive) {
            return damage;
        }

        boolean inStableWindow = tier >= TIER_FOUR && now < cooldown.entry(KEY_STABLE_UNTIL).getReadyTick();
        float remainingDamage = damage;
        float absorbed = 0.0f;

        if (damage > ABSORB_PER_HIT_MAX && !inStableWindow) {
            breakShield(cc, organ, state, cooldown, now);
            resetAbsorbTrackers(cc, organ, state);
            return damage;
        }

        if (damage > ABSORB_PER_HIT_MAX && inStableWindow) {
            float overflow = damage - ABSORB_PER_HIT_MAX;
            float reflected = overflow * REFLECT_RATIO_4Z;
            LivingEntity attacker = source != null && source.getEntity() instanceof LivingEntity living ? living : null;
            if (attacker != null && CombatEntityUtil.areEnemies(attacker, victim)) {
                attacker.hurt(attacker.damageSources().magic(), reflected);
            }
            remainingDamage = overflow - reflected;
            absorbed = ABSORB_PER_HIT_MAX;
        } else {
            absorbed = Math.min(damage, ABSORB_PER_HIT_MAX);
            remainingDamage = Math.max(0.0f, damage - absorbed);
        }

        if (absorbed > 0.0f) {
            onAbsorbedDamage(victim, cc, organ, state, cooldown, absorbed, now, tier);
        }

        if (CombatEntityUtil.isMeleeHit(source)) {
            LivingEntity attacker = source != null && source.getEntity() instanceof LivingEntity living ? living : null;
            if (attacker != null && CombatEntityUtil.areEnemies(attacker, victim)) {
                handleCounterArc(victim, attacker, cooldown, now);
            }
        }

        return remainingDamage;
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide() || cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!isPrimaryOrgan(cc, organ)) {
            return;
        }

        OrganState state = resolveOrganState(organ, STATE_ROOT);
        MultiCooldown cooldown = createCooldown(cc, organ);
        long now = entity.level().getGameTime();
        maybeRepairShield(entity, cc, organ, state, cooldown, now);
        maybeRegenerateActiveCharge(entity, cc, organ, state, cooldown, now);
        maybeUpgradeTier(entity);
        pruneTsxpLimiter(entity.getUUID(), now);
    }

    @Override
    public float onHit(DamageSource source, LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (attacker == null || attacker.level().isClientSide() || target == null || cc == null || organ == null || organ.isEmpty()) {
            return damage;
        }
        if (!isPrimaryOrgan(cc, organ)) {
            return damage;
        }
        if (target.isDeadOrDying()) {
            grantTsxp(attacker, target, TSXP_KILL_REWARD, attacker.level().getGameTime());
        }
        return damage;
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        OrganStateOps.setBooleanSync(cc, organ, STATE_ROOT, KEY_SHIELD_ACTIVE, false, true);
        OrganStateOps.setIntSync(cc, organ, STATE_ROOT, KEY_CHARGE_STACKS, 0, value -> 0, 0);
        OrganStateOps.setIntSync(cc, organ, STATE_ROOT, KEY_ACTIVE_CHARGES, 0, value -> 0, 0);
    }

    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        registerRemovalHook(cc, organ, this, staleRemovalContexts);
    }

    private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        if (cc == null) {
            return;
        }
        ItemStack organ = findPrimaryOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }

        INSTANCE.activateRayAnchor(player, cc, organ);
    }

    private void activateRayAnchor(ServerPlayer player, ChestCavityInstance cc, ItemStack organ) {
        OrganState state = resolveOrganState(organ, STATE_ROOT);
        MultiCooldown cooldown = createCooldown(cc, organ);
        long now = player.level().getGameTime();
        int tier = resolveTier(player);

        if (tier < TIER_THREE) {
            tier = TIER_THREE;
        }

        int chargeStacks = Math.max(0, state.getInt(KEY_CHARGE_STACKS, 0));
        boolean strongRelease = tier >= TIER_FIVE && chargeStacks >= 3 && player.isShiftKeyDown();
        if ((!strongRelease && chargeStacks <= 0) || (strongRelease && chargeStacks < 3)) {
            return;
        }

        if (tier < TIER_FIVE) {
            long activeCd = cooldown.entry(KEY_ACTIVE_CD_UNTIL).getReadyTick();
            if (activeCd > now) {
                return;
            }
        } else {
            int storedCharges = Math.max(0, state.getInt(KEY_ACTIVE_CHARGES, 0));
            if (storedCharges <= 0) {
                return;
            }
            OrganStateOps.setInt(state, cc, organ, KEY_ACTIVE_CHARGES, storedCharges - 1, value -> Mth.clamp(value, 0, MAX_ACTIVE_CHARGES), 0);
            if (storedCharges - 1 < MAX_ACTIVE_CHARGES) {
                cooldown.entry(KEY_NEXT_CHARGE_READY).setReadyAt(now + ACTIVE_CD_TICKS);
            }
        }

        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return;
        }
        ResourceHandle handle = handleOpt.get();

        double zhenyuanCost = ACTIVE_COST_ZHENYUAN_3Z;
        double jingliCost = ACTIVE_COST_JINGLI_3Z;
        if (tier >= TIER_FOUR) {
            zhenyuanCost *= REPAIR_DISCOUNT_4Z;
            jingliCost *= REPAIR_DISCOUNT_4Z;
        }

        if (ResourceOps.tryConsumeScaledZhenyuan(handle, zhenyuanCost).isEmpty()) {
            if (tier >= TIER_FIVE) {
                OrganStateOps.setInt(state, cc, organ, KEY_ACTIVE_CHARGES, Math.min(MAX_ACTIVE_CHARGES, state.getInt(KEY_ACTIVE_CHARGES, 0) + 1), value -> Mth.clamp(value, 0, MAX_ACTIVE_CHARGES), 0);
            } else {
                cooldown.entry(KEY_ACTIVE_CD_UNTIL).setReadyAt(now);
            }
            return;
        }
        if (ResourceOps.tryAdjustJingli(handle, -jingliCost, true).isEmpty()) {
            return;
        }

        if (tier < TIER_FIVE) {
            cooldown.entry(KEY_ACTIVE_CD_UNTIL).setReadyAt(now + ACTIVE_CD_TICKS);
            ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_ID, now + ACTIVE_CD_TICKS, now);
        } else {
            int remainingCharges = Math.max(0, state.getInt(KEY_ACTIVE_CHARGES, 0));
            if (remainingCharges < MAX_ACTIVE_CHARGES) {
                long readyAt = cooldown.entry(KEY_NEXT_CHARGE_READY).getReadyTick();
                ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_ID, readyAt, now);
            }
        }

        int consumeStacks = strongRelease ? 3 : 1;
        int updatedStacks = Math.max(0, chargeStacks - consumeStacks);
        OrganStateOps.setInt(state, cc, organ, KEY_CHARGE_STACKS, updatedStacks, value -> Mth.clamp(value, 0, MAX_CHARGE_STACKS), 0);

        LivingEntity target = findAbilityTarget(player);
        if (target == null) {
            spawnActivationEffects(player, player);
            return;
        }

        float directDamage = strongRelease ? 600.0f : 500.0f;
        int dotSeconds = strongRelease ? ACTIVE_STRONG_DURATION_SECONDS : ACTIVE_BASE_DURATION_SECONDS;
        int dotPerSecond;
        if (tier >= TIER_FIVE && strongRelease) {
            dotPerSecond = ACTIVE_DOT_PER_SEC_5Z_STRONG;
        } else if (tier >= TIER_FOUR) {
            dotPerSecond = DOT_DMG_PER_SEC_4Z;
        } else {
            dotPerSecond = DOT_DMG_PER_SEC_3Z;
        }

        target.hurt(player.damageSources().magic(), directDamage);
        DoTEngine.schedulePerSecond(player, target, dotPerSecond, dotSeconds, null, 1.0f, 1.0f,
                DoTTypes.LEI_DUN_ELECTRIFY, null, DoTEngine.FxAnchor.TARGET, Vec3.ZERO, 1.0f);
        ReactionTagOps.add(target, ReactionTagKeys.LIGHTNING_CHARGE, dotSeconds * 20);
        if (strongRelease) {
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 8, 5, false, true));
        }
        spawnActivationEffects(player, target);
        grantTsxp(player, target, TSXP_ACTIVE_REWARD, now);
    }

    private LivingEntity findAbilityTarget(ServerPlayer player) {
        Level level = player.level();
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle().scale(6.0);
        AABB box = new AABB(eyePos, eyePos.add(look)).inflate(2.5);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box,
                living -> living != player && CombatEntityUtil.areEnemies(player, living));
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.stream()
                .min(Comparator.comparingDouble(living -> living.distanceToSqr(player)))
                .orElse(null);
    }

    private void spawnActivationEffects(LivingEntity caster, LivingEntity target) {
        Level level = caster.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                center.x, center.y, center.z,
                20,
                target.getBbWidth() * 0.2,
                target.getBbHeight() * 0.2,
                target.getBbWidth() * 0.2,
                0.1);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.1f);
    }

    private void onAbsorbedDamage(LivingEntity victim, ChestCavityInstance cc, ItemStack organ, OrganState state,
                                  MultiCooldown cooldown, float absorbed, long now, int tier) {
        int previous = Math.max(0, state.getInt(KEY_ABSORB_SINCE_SHOCK, 0));
        int updated = previous + Math.round(absorbed);
        OrganStateOps.setInt(state, cc, organ, KEY_ABSORB_SINCE_SHOCK, updated, value -> Math.max(0, value), 0);

        int overload = Math.max(0, state.getInt(KEY_OVERLOAD_ABSORB, 0));
        OrganStateOps.setInt(state, cc, organ, KEY_OVERLOAD_ABSORB, overload + Math.round(absorbed), value -> Math.max(0, value), 0);

        int progress = Math.max(0, state.getInt(KEY_ABSORB_PROGRESS, 0)) + Math.round(absorbed);
        int reward = progress / TSXP_ABSORB_STEP;
        int remainder = progress % TSXP_ABSORB_STEP;
        OrganStateOps.setInt(state, cc, organ, KEY_ABSORB_PROGRESS, remainder, value -> Math.max(0, value), 0);
        if (reward > 0) {
            grantTsxp(victim, null, reward, now);
        }

        maybeTriggerShock(victim, cc, organ, state, cooldown, updated, now, tier);
    }

    private void maybeTriggerShock(LivingEntity owner, ChestCavityInstance cc, ItemStack organ, OrganState state,
                                   MultiCooldown cooldown, int absorbedSinceShock, long now, int tier) {
        long readyTick = cooldown.entry(KEY_SHOCK_CD_UNTIL).getReadyTick();
        if (absorbedSinceShock < SHOCK_ABSORB_TRIGGER || readyTick > now) {
            return;
        }
        int overload = Math.max(0, state.getInt(KEY_OVERLOAD_ABSORB, 0));
        boolean thunderStrike = tier >= TIER_FIVE && overload >= OVERLOAD_ABSORB_REQ;
        if (thunderStrike) {
            triggerThunderStrike(owner, cc, organ, state, cooldown, now);
        } else {
            triggerShock(owner, cc, organ, state, tier, absorbedSinceShock, now);
        }
        OrganStateOps.setInt(state, cc, organ, KEY_ABSORB_SINCE_SHOCK, absorbedSinceShock - SHOCK_ABSORB_TRIGGER, value -> Math.max(0, value), 0);
        cooldown.entry(KEY_SHOCK_CD_UNTIL).setReadyAt(now + SHOCK_CD_TICKS);
    }

    private void triggerShock(LivingEntity owner, ChestCavityInstance cc, ItemStack organ, OrganState state,
                              int tier, int absorbedSinceShock, long now) {
        Level level = owner.level();
        int radius = tier >= TIER_FOUR ? SHOCK_RADIUS_4Z : SHOCK_RADIUS_BASE;
        int maxDamage = tier >= TIER_FOUR ? SHOCK_MAX_4Z : SHOCK_DMG_MAX;
        float damage = (float) Mth.clamp(650.0 + 0.25 * absorbedSinceShock, SHOCK_DMG_MIN, maxDamage);

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                owner.getBoundingBox().inflate(radius),
                living -> living != owner && CombatEntityUtil.areEnemies(owner, living));
        if (targets.isEmpty()) {
            return;
        }

        for (LivingEntity target : targets) {
            target.hurt(owner.damageSources().magic(), damage);
            ReactionTagOps.add(target, ReactionTagKeys.LIGHTNING_CHARGE, 60);
        }
        level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8f, 1.0f);
        spawnShockParticles(owner, radius);

        int stacks = Math.max(0, state.getInt(KEY_CHARGE_STACKS, 0));
        OrganStateOps.setInt(state, cc, organ, KEY_CHARGE_STACKS, Math.min(MAX_CHARGE_STACKS, stacks + 1), value -> Mth.clamp(value, 0, MAX_CHARGE_STACKS), 0);
        grantTsxp(owner, targets.get(0), TSXP_SHOCK_REWARD, now);
        if (tier >= TIER_FOUR) {
            ReactionTagOps.add(targets.get(0), ReactionTagKeys.LIGHTNING_CHARGE, 60);
        }
    }

    private void triggerThunderStrike(LivingEntity owner, ChestCavityInstance cc, ItemStack organ, OrganState state,
                                      MultiCooldown cooldown, long now) {
        Level level = owner.level();
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                owner.getBoundingBox().inflate(6.0),
                living -> living != owner && CombatEntityUtil.areEnemies(owner, living));
        if (targets.isEmpty()) {
            return;
        }
        targets.sort(Comparator.comparingDouble(living -> living.distanceToSqr(owner)));

        LivingEntity main = targets.get(0);
        main.hurt(owner.damageSources().magic(), THUNDER_STRIKE_BASE);
        ReactionTagOps.add(main, ReactionTagKeys.LIGHTNING_CHARGE, 80);

        for (int i = 1; i < Math.min(3, targets.size()); i++) {
            LivingEntity extra = targets.get(i);
            extra.hurt(owner.damageSources().magic(), THUNDER_STRIKE_BASE * CHAIN_RATIO);
            ReactionTagOps.add(extra, ReactionTagKeys.LIGHTNING_CHARGE, 80);
        }

        OrganStateOps.setInt(state, cc, organ, KEY_OVERLOAD_ABSORB, 0, value -> 0, 0);
        int stacks = Math.max(0, state.getInt(KEY_CHARGE_STACKS, 0));
        OrganStateOps.setInt(state, cc, organ, KEY_CHARGE_STACKS, Math.min(MAX_CHARGE_STACKS, stacks + 1), value -> Mth.clamp(value, 0, MAX_CHARGE_STACKS), 0);
        grantTsxp(owner, main, TSXP_SHOCK_REWARD, now);
        spawnShockParticles(owner, 6);
        level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 1.0f, 0.9f);
        if (owner instanceof Player player) {
            player.sendSystemMessage(Component.translatable("msg.leidungu.overload"));
        }
    }

    private void spawnShockParticles(LivingEntity owner, int radius) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                owner.getX(), owner.getY() + owner.getBbHeight() * 0.5, owner.getZ(),
                radius * 10,
                radius * 0.2,
                owner.getBbHeight() * 0.2,
                radius * 0.2,
                0.2);
    }

    private void handleCounterArc(LivingEntity victim, LivingEntity attacker, MultiCooldown cooldown, long now) {
        long ready = cooldown.entry(KEY_COUNTER_ICD_UNTIL).getReadyTick();
        if (ready > now) {
            return;
        }
        cooldown.entry(KEY_COUNTER_ICD_UNTIL).setReadyAt(now + COUNTER_ARC_ICD_TICKS);
        attacker.hurt(victim.damageSources().magic(), COUNTER_ARC_DMG);
        ReactionTagOps.add(attacker, ReactionTagKeys.LIGHTNING_CHARGE, 20);
        victim.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.7f, 1.2f);
    }

    private void maybeRepairShield(LivingEntity entity, ChestCavityInstance cc, ItemStack organ, OrganState state,
                                   MultiCooldown cooldown, long now) {
        boolean shieldActive = state.getBoolean(KEY_SHIELD_ACTIVE, true);
        if (shieldActive) {
            return;
        }
        long readyTick = cooldown.entry(KEY_SHIELD_BROKEN_UNTIL).getReadyTick();
        if (readyTick > now) {
            return;
        }
        int tier = resolveTier(entity);
        double zhenyuanCost = REPAIR_COST_ZHENYUAN_3Z;
        double jingliCost = REPAIR_COST_JINGLI_3Z;
        if (tier >= TIER_FOUR) {
            zhenyuanCost *= REPAIR_DISCOUNT_4Z;
            jingliCost *= REPAIR_DISCOUNT_4Z;
        }

        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(entity);
        if (handleOpt.isEmpty()) {
            cooldown.entry(KEY_SHIELD_BROKEN_UNTIL).setReadyAt(now + 20);
            return;
        }
        ResourceHandle handle = handleOpt.get();

        OptionalDouble jingliOpt = handle.getJingli();
        if (jingliOpt.isEmpty() || jingliOpt.getAsDouble() < jingliCost) {
            cooldown.entry(KEY_SHIELD_BROKEN_UNTIL).setReadyAt(now + 40);
            return;
        }

        OptionalDouble zhenBeforeOpt = handle.getZhenyuan();
        OptionalDouble zhenAfterOpt = ResourceOps.tryConsumeScaledZhenyuan(handle, zhenyuanCost);
        if (zhenAfterOpt.isEmpty()) {
            cooldown.entry(KEY_SHIELD_BROKEN_UNTIL).setReadyAt(now + 40);
            return;
        }

        if (ResourceOps.tryAdjustJingli(handle, -jingliCost, true).isEmpty()) {
            if (zhenBeforeOpt.isPresent()) {
                double refund = zhenBeforeOpt.getAsDouble() - zhenAfterOpt.getAsDouble();
                if (refund > 0.0) {
                    ResourceOps.tryAdjustZhenyuan(handle, refund, true);
                }
            }
            cooldown.entry(KEY_SHIELD_BROKEN_UNTIL).setReadyAt(now + 40);
            return;
        }

        OrganStateOps.setBoolean(state, cc, organ, KEY_SHIELD_ACTIVE, true, true);
        cooldown.entry(KEY_SHIELD_BROKEN_UNTIL).setReadyAt(0L);
        if (tier >= TIER_FOUR) {
            cooldown.entry(KEY_STABLE_UNTIL).setReadyAt(now + STABLE_WINDOW_TICKS);
        }
        resetAbsorbTrackers(cc, organ, state);
        Level level = entity.level();
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 1.0f);
    }

    private void resetAbsorbTrackers(ChestCavityInstance cc, ItemStack organ, OrganState state) {
        OrganStateOps.setInt(state, cc, organ, KEY_ABSORB_SINCE_SHOCK, 0, value -> 0, 0);
        OrganStateOps.setInt(state, cc, organ, KEY_ABSORB_PROGRESS, 0, value -> 0, 0);
    }

    private void maybeRegenerateActiveCharge(LivingEntity entity, ChestCavityInstance cc, ItemStack organ, OrganState state,
                                             MultiCooldown cooldown, long now) {
        int tier = resolveTier(entity);
        if (tier < TIER_FIVE) {
            return;
        }
        int charges = Math.max(0, state.getInt(KEY_ACTIVE_CHARGES, 0));
        if (charges >= MAX_ACTIVE_CHARGES) {
            return;
        }
        long readyTick = cooldown.entry(KEY_NEXT_CHARGE_READY).getReadyTick();
        if (readyTick > now) {
            return;
        }
        charges++;
        OrganStateOps.setInt(state, cc, organ, KEY_ACTIVE_CHARGES, charges, value -> Mth.clamp(value, 0, MAX_ACTIVE_CHARGES), 0);
        if (charges < MAX_ACTIVE_CHARGES) {
            cooldown.entry(KEY_NEXT_CHARGE_READY).setReadyAt(now + ACTIVE_CD_TICKS);
        } else {
            cooldown.entry(KEY_NEXT_CHARGE_READY).setReadyAt(now);
        }
    }

    private void breakShield(ChestCavityInstance cc, ItemStack organ, OrganState state, MultiCooldown cooldown, long now) {
        OrganStateOps.setBoolean(state, cc, organ, KEY_SHIELD_ACTIVE, false, true);
        cooldown.entry(KEY_SHIELD_BROKEN_UNTIL).setReadyAt(now + REPAIR_DELAY_TICKS);
        OrganStateOps.setInt(state, cc, organ, KEY_OVERLOAD_ABSORB, 0, value -> 0, 0);
        cooldown.entry(KEY_STABLE_UNTIL).setReadyAt(0L);
    }

    private void maybeUpgradeTier(LivingEntity entity) {
        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(entity);
        if (handleOpt.isEmpty()) {
            return;
        }
        ResourceHandle handle = handleOpt.get();
        int tier = getTier(handle);
        double tsxp = getTsxp(handle);
        if (tier < TIER_FOUR && tsxp >= T4_TSXP_THRESHOLD) {
            setTier(handle, TIER_FOUR);
            setTsxp(handle, 0.0);
            if (entity instanceof Player player) {
                player.sendSystemMessage(Component.translatable("msg.leidungu.upgrade.4z"));
            }
        } else if (tier < TIER_FIVE && tsxp >= T5_TSXP_THRESHOLD) {
            setTier(handle, TIER_FIVE);
            setTsxp(handle, 0.0);
            if (entity instanceof Player player) {
                player.sendSystemMessage(Component.translatable("msg.leidungu.upgrade.5z"));
            }
        }
    }

    private void grantTsxp(LivingEntity owner, LivingEntity target, int amount, long now) {
        if (owner == null || amount <= 0) {
            return;
        }
        if (target != null) {
            int allowed = consumeTsxpBudget(owner.getUUID(), target.getUUID(), amount, now);
            if (allowed <= 0) {
                return;
            }
            amount = allowed;
        }
        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(owner);
        if (handleOpt.isEmpty()) {
            return;
        }
        ResourceHandle handle = handleOpt.get();
        handle.adjustDouble("leidungu_tsxp", amount, true);
    }

    private int consumeTsxpBudget(UUID owner, UUID target, int desired, long now) {
        if (owner == null || target == null) {
            return desired;
        }
        Map<UUID, TsxpBucket> buckets = TSXP_LIMITER.computeIfAbsent(owner, uuid -> new HashMap<>());
        TsxpBucket bucket = buckets.computeIfAbsent(target, uuid -> new TsxpBucket());
        long currentSecond = now / 20L;
        if (bucket.second != currentSecond) {
            bucket.second = currentSecond;
            bucket.used = 0;
        }
        int remaining = Math.max(0, 2 - bucket.used);
        int granted = Math.min(desired, remaining);
        bucket.used += granted;
        return granted;
    }

    private void pruneTsxpLimiter(UUID owner, long now) {
        Map<UUID, TsxpBucket> buckets = TSXP_LIMITER.get(owner);
        if (buckets == null || buckets.isEmpty()) {
            return;
        }
        long threshold = now / 20L - 5;
        buckets.entrySet().removeIf(entry -> entry.getValue().second < threshold);
    }

    private int resolveTier(LivingEntity entity) {
        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(entity);
        if (handleOpt.isEmpty()) {
            return TIER_THREE;
        }
        return Math.max(TIER_THREE, getTier(handleOpt.get()));
    }

    private static MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ) {
        MultiCooldown.Builder builder = MultiCooldown.builder(OrganState.of(organ, STATE_ROOT))
                .withLongClamp(value -> Math.max(0L, value), 0L)
                .withIntClamp(value -> Math.max(0, value), 0);
        if (cc != null) {
            builder.withSync(cc, organ);
        } else {
            builder.withOrgan(organ);
        }
        return builder.build();
    }

    private static OrganState resolveOrganState(ItemStack organ, String root) {
        return OrganState.of(organ, root);
    }

    private static boolean isPrimaryOrgan(ChestCavityInstance cc, ItemStack organ) {
        if (cc == null || cc.inventory == null || organ == null || organ.isEmpty()) {
            return false;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (!matchesOrgan(stack, ORGAN_ID)) {
                continue;
            }
            return stack == organ;
        }
        return false;
    }

    private static ItemStack findPrimaryOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (matchesOrgan(stack, ORGAN_ID)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean matchesOrgan(ItemStack stack, ResourceLocation id) {
        if (stack == null || stack.isEmpty() || id == null) {
            return false;
        }
        return Objects.equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()), id);
    }

    private static double getTsxp(ResourceHandle handle) {
        OptionalDouble value = handle.read("leidungu_tsxp");
        return value.orElse(0.0);
    }

    private static void setTsxp(ResourceHandle handle, double value) {
        handle.writeDouble("leidungu_tsxp", value);
    }

    private static int getTier(ResourceHandle handle) {
        OptionalDouble value = handle.read("leidungu_tier");
        return value.isPresent() ? (int) Math.round(value.getAsDouble()) : TIER_THREE;
    }

    private static void setTier(ResourceHandle handle, int tier) {
        handle.writeDouble("leidungu_tier", tier);
    }

    private static final class TsxpBucket {
        long second;
        int used;
    }
}
