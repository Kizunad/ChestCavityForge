package net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NetworkUtil;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Predicate;

/**
 * 火龙蛊核心行为实现。功能聚焦在 DoT 管理、双充能主动技与被动增益。
 */
public enum HuoLongGuOrganBehavior implements OrganSlowTickListener, OrganOnHitListener, OrganIncomingDamageListener {
    INSTANCE;

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huo_long_gu");
    public static final ResourceLocation BREATH_ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huo_long_gu_breath");
    public static final ResourceLocation HOVER_ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huo_long_gu_hover");
    public static final ResourceLocation DIVE_ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huo_long_gu_dive");

    private static final ResourceLocation HOU_YI_ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huo_gu");
    private static final ResourceLocation HOU_XIN_ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huoxingu");
    private static final ResourceLocation HOU_ZHI_ZHUANG_ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "zhi_zhuang_gu");

    private static final String STATE_ROOT = "HuoLongGu";
    private static final String KEY_COUNTER = "Counter";
    private static final String KEY_LAST_ACTIVITY_TICK = "LastActivityTick";
    private static final String KEY_NEXT_DECAY_TICK = "NextDecayTick";
    private static final String KEY_UNLOCKED_TIER5 = "UnlockedTier5";
    private static final String KEY_BREATH_CHARGES = "BreathCharges";
    private static final String KEY_BREATH_NEXT_RECHARGE_TICK = "BreathNextRechargeTick";
    private static final String KEY_BREATH_LAST_CAST_TICK = "BreathLastCastTick";
    private static final String KEY_ASCENT_EXPIRE_TICK = "AscentExpireTick";
    private static final String KEY_ASCENT_GAINED = "AscentGain";
    private static final String KEY_DIVE_READY_TICK = "DiveReadyTick";
    private static final String KEY_DIVE_IMMUNE_TICK = "DiveImmuneTick";
    private static final String KEY_DIVE_LAST_BONUS = "DiveLastBonus";
    private static final String KEY_LIFESTEAL_GATE_TICK = "LifestealGateTick";

    private static final int BREATH_MAX_CHARGES = 2;
    private static final int BREATH_COOLDOWN_TICKS = 20 * 20;
    private static final double BREATH_COST_ZHENYUAN = 30_000.0;
    private static final double BREATH_REFUND_PER_TARGET = 3_000.0;
    private static final int BREATH_REFUND_CAP = 15_000;
    private static final float BREATH_BASE_DAMAGE = 200.0F;
    private static final double BREATH_AOE_RATIO = 0.10D;
    private static final double BREATH_AOE_RADIUS = 3.0D;
    private static final int BREATH_ASCENT_DURATION = 30;

    private static final double HOVER_COST_ZHENYUAN = 150_000.0;
    private static final int HOVER_ASCENT_DURATION = 60;

    private static final double DIVE_COST_ZHENYUAN = 250_000.0;
    private static final double DIVE_HEALTH_COST_RATIO = 0.30D;
    private static final int DIVE_DURATION_TICKS = 40;
    private static final float DIVE_BASE_DAMAGE = 400.0F;
    private static final double DIVE_RADIUS = 4.0D;
    private static final int DIVE_POST_IMMUNE_TICKS = 30;

    private static final double PASSIVE_QIYUN_REGEN_RATIO_FOURTH = 0.01D;
    private static final double PASSIVE_QIYUN_REGEN_RATIO_FIFTH = 0.015D;
    private static final double PASSIVE_LIFESTEAL_PER_STACK = 0.01D;
    private static final double PASSIVE_LIFESTEAL_CAP = 0.06D;
    private static final int PASSIVE_LIFESTEAL_GATE = 40;

    private static final ResourceLocation KNOCKBACK_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID,
            "modifiers/huo_long_gu_knockback");
    private static final AttributeModifier KNOCKBACK_MODIFIER = new AttributeModifier(KNOCKBACK_MODIFIER_ID,
            0.3D, AttributeModifier.Operation.ADD_VALUE);

    private static final Predicate<LivingEntity> VALID_TARGET = living -> living != null && living.isAlive();

    static {
        OrganActivationListeners.register(BREATH_ABILITY_ID, HuoLongGuOrganBehavior::activateBreath);
        OrganActivationListeners.register(HOVER_ABILITY_ID, HuoLongGuOrganBehavior::activateHover);
        OrganActivationListeners.register(DIVE_ABILITY_ID, HuoLongGuOrganBehavior::activateDive);
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof LivingEntity living) || cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        Level level = living.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!matchesOrgan(organ)) {
            return;
        }
        OrganState state = OrganState.of(organ, STATE_ROOT);
        long gameTime = level.getGameTime();
        boolean dirty = false;

        dirty |= tickBreathRecharge(state, gameTime);
        dirty |= tickAscent(living, state, gameTime, serverLevel);
        dirty |= tickDiveFlags(living, state, gameTime);
        dirty |= tickCounterDecay(state, gameTime);
        dirty |= tickQiyunRegen(living, state);
        dirty |= ensureKnockbackModifier(living);

        if (dirty) {
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }
    }

    @Override
    public float onHit(DamageSource source, LivingEntity attacker, LivingEntity target, ChestCavityInstance chestCavity, ItemStack organ, float damage) {
        if (attacker == null || target == null || chestCavity == null || organ == null || organ.isEmpty()) {
            return damage;
        }
        if (!matchesOrgan(organ)) {
            return damage;
        }
        OrganState state = OrganState.of(organ, STATE_ROOT);
        int stacks = DragonflameHelper.getStacks(target);
        if (stacks <= 0) {
            return damage;
        }
        long gameTime = attacker.level().getGameTime();
        long gate = state.getLong(KEY_LIFESTEAL_GATE_TICK, 0L);
        if (gameTime < gate) {
            return damage;
        }
        double ratio = Math.min(PASSIVE_LIFESTEAL_CAP, stacks * PASSIVE_LIFESTEAL_PER_STACK);
        if (ratio <= 0.0D) {
            return damage;
        }
        float heal = (float) (damage * ratio);
        if (heal > 0.0F) {
            attacker.heal(heal);
            state.setLong(KEY_LIFESTEAL_GATE_TICK, gameTime + PASSIVE_LIFESTEAL_GATE);
        }
        return damage;
    }

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (victim == null || organ == null || organ.isEmpty()) {
            return damage;
        }
        if (!matchesOrgan(organ)) {
            return damage;
        }
        OrganState state = OrganState.of(organ, STATE_ROOT);
        long now = victim.level().getGameTime();
        if (source != null && source.is(DamageTypeTags.IS_PROJECTILE)) {
            if (ReactionTagOps.has(victim, ReactionTagKeys.DRAGON_ASCENT) || ReactionTagOps.has(victim, ReactionTagKeys.DRAGON_DIVE)) {
                damage *= 0.8F;
            }
        }
        long immuneUntil = state.getLong(KEY_DIVE_IMMUNE_TICK, 0L);
        if (immuneUntil > 0L && now <= immuneUntil) {
            return 0.0F;
        }
        return damage;
    }

    private static boolean tickBreathRecharge(OrganState state, long gameTime) {
        boolean dirty = false;
        int charges = Mth.clamp(state.getInt(KEY_BREATH_CHARGES, BREATH_MAX_CHARGES), 0, BREATH_MAX_CHARGES);
        if (charges != state.getInt(KEY_BREATH_CHARGES, BREATH_MAX_CHARGES)) {
            state.setInt(KEY_BREATH_CHARGES, charges);
            dirty = true;
        }
        long nextRecharge = state.getLong(KEY_BREATH_NEXT_RECHARGE_TICK, 0L);
        if (charges < BREATH_MAX_CHARGES && nextRecharge > 0L && gameTime >= nextRecharge) {
            charges += 1;
            state.setInt(KEY_BREATH_CHARGES, charges);
            if (charges < BREATH_MAX_CHARGES) {
                state.setLong(KEY_BREATH_NEXT_RECHARGE_TICK, gameTime + BREATH_COOLDOWN_TICKS);
            } else {
                state.setLong(KEY_BREATH_NEXT_RECHARGE_TICK, 0L);
            }
            dirty = true;
        }
        return dirty;
    }

    private static boolean tickAscent(LivingEntity living, OrganState state, long gameTime, ServerLevel level) {
        boolean dirty = false;
        long expire = state.getLong(KEY_ASCENT_EXPIRE_TICK, 0L);
        if (expire > 0L && gameTime >= expire) {
            ReactionTagOps.clear(living, ReactionTagKeys.DRAGON_ASCENT);
            living.setNoGravity(false);
            state.setLong(KEY_ASCENT_EXPIRE_TICK, 0L);
            state.setInt(KEY_ASCENT_GAINED, 0);
            dirty = true;
        }
        if (ReactionTagOps.has(living, ReactionTagKeys.DRAGON_ASCENT)) {
            int gained = state.getInt(KEY_ASCENT_GAINED, 0);
            if (gained < 3) {
                state.setInt(KEY_ASCENT_GAINED, gained + 1);
                addCounter(state, 1, gameTime, true);
                dirty = true;
            }
            living.fallDistance = 0.0F;
            living.setDeltaMovement(living.getDeltaMovement().multiply(0.6, 0.0, 0.6));
        }
        return dirty;
    }

    private static boolean tickDiveFlags(LivingEntity living, OrganState state, long gameTime) {
        boolean dirty = false;
        long readyTick = state.getLong(KEY_DIVE_READY_TICK, 0L);
        if (readyTick > 0L && gameTime >= readyTick) {
            state.setLong(KEY_DIVE_READY_TICK, 0L);
            dirty = true;
        }
        long immune = state.getLong(KEY_DIVE_IMMUNE_TICK, 0L);
        if (immune > 0L && gameTime >= immune) {
            state.setLong(KEY_DIVE_IMMUNE_TICK, 0L);
            ReactionTagOps.clear(living, ReactionTagKeys.DRAGON_DIVE);
            living.setNoGravity(false);
            dirty = true;
        }
        return dirty;
    }

    private static boolean tickCounterDecay(OrganState state, long gameTime) {
        boolean dirty = false;
        int counter = Math.max(0, state.getInt(KEY_COUNTER, 0));
        state.setInt(KEY_COUNTER, counter);
        boolean unlocked = state.getBoolean(KEY_UNLOCKED_TIER5, false);
        if (!unlocked && counter >= 200) {
            state.setBoolean(KEY_UNLOCKED_TIER5, true);
            unlocked = true;
            dirty = true;
        }
        if (unlocked) {
            if (state.getLong(KEY_NEXT_DECAY_TICK, 0L) != 0L) {
                state.setLong(KEY_NEXT_DECAY_TICK, 0L);
                dirty = true;
            }
            return dirty;
        }
        long lastActivity = state.getLong(KEY_LAST_ACTIVITY_TICK, gameTime);
        long nextDecay = state.getLong(KEY_NEXT_DECAY_TICK, 0L);
        if (gameTime - lastActivity > 200L) {
            if (nextDecay <= 0L) {
                nextDecay = gameTime + 100L;
                state.setLong(KEY_NEXT_DECAY_TICK, nextDecay);
                dirty = true;
            } else if (gameTime >= nextDecay && counter > 0) {
                counter -= 1;
                state.setInt(KEY_COUNTER, counter);
                state.setLong(KEY_NEXT_DECAY_TICK, counter > 0 ? gameTime + 100L : 0L);
                dirty = true;
            }
        } else if (nextDecay != 0L) {
            state.setLong(KEY_NEXT_DECAY_TICK, 0L);
            dirty = true;
        }
        return dirty;
    }

    private static boolean tickQiyunRegen(LivingEntity living, OrganState state) {
        if (!(living instanceof Player player)) {
            return false;
        }
        boolean upgraded = state.getBoolean(KEY_UNLOCKED_TIER5, false);
        double ratio = upgraded ? PASSIVE_QIYUN_REGEN_RATIO_FIFTH : PASSIVE_QIYUN_REGEN_RATIO_FOURTH;
        if (ratio <= 0.0D) {
            return false;
        }
        return GuzhenrenResourceBridge.open(player).map(handle -> {
            double max = handle.read("qiyun_shangxian").orElse(0.0D);
            double current = handle.read("qiyun").orElse(0.0D);
            if (max <= 0.0D || current >= max) {
                return false;
            }
            double gain = Math.max(0.0D, Math.min(max - current, max * ratio));
            if (gain <= 0.0D) {
                return false;
            }
            handle.adjustDouble("qiyun", gain, true, "qiyun_shangxian");
            return true;
        }).orElse(false);
    }

    private static boolean ensureKnockbackModifier(LivingEntity living) {
        AttributeInstance attr = living.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attr == null) {
            return false;
        }
        if (!attr.hasModifier(KNOCKBACK_MODIFIER_ID)) {
            attr.addTransientModifier(KNOCKBACK_MODIFIER);
            return true;
        }
        return false;
    }

    private static boolean matchesOrgan(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return ORGAN_ID.equals(id);
    }

    private static ItemStack findOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack candidate = cc.inventory.getItem(i);
            if (matchesOrgan(candidate)) {
                return candidate;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean addCounter(OrganState state, int amount, long gameTime, boolean activity) {
        if (amount == 0) {
            return false;
        }
        int current = Math.max(0, state.getInt(KEY_COUNTER, 0));
        int next = Math.max(0, current + amount);
        state.setInt(KEY_COUNTER, next);
        if (activity) {
            state.setLong(KEY_LAST_ACTIVITY_TICK, gameTime);
        }
        return true;
    }

    private static boolean hasOrgan(ChestCavityInstance cc, ResourceLocation id) {
        if (cc == null || cc.inventory == null || id == null) {
            return false;
        }
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation rid = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id.equals(rid)) {
                return true;
            }
        }
        return false;
    }

    private static boolean consumeZhenyuan(LivingEntity entity, double amount) {
        if (amount <= 0.0D) {
            return true;
        }
        return ResourceOps.tryConsumeScaledZhenyuan(entity, amount).isPresent();
    }

    private static void refundZhenyuan(LivingEntity entity, double amount) {
        if (amount <= 0.0D) {
            return;
        }
        ResourceOps.tryReplenishScaledZhenyuan(entity, amount, true);
    }

    private static LivingEntity findBreathTarget(LivingEntity caster, double range) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(range));
        AABB box = caster.getBoundingBox().expandTowards(look.scale(range)).inflate(1.5D);
        List<LivingEntity> list = caster.level().getEntitiesOfClass(LivingEntity.class, box, VALID_TARGET);
        LivingEntity best = null;
        double bestDot = 0.0D;
        for (LivingEntity target : list) {
            if (target == caster || target.isAlliedTo(caster)) {
                continue;
            }
            Vec3 to = target.position().subtract(eye);
            double length = to.length();
            if (length <= 0.0D || length > range) {
                continue;
            }
            Vec3 norm = to.normalize();
            double dot = norm.dot(look);
            if (dot < 0.4D) {
                continue;
            }
            if (best == null || dot > bestDot) {
                best = target;
                bestDot = dot;
            }
        }
        if (best == null) {
            double bestDist = Double.MAX_VALUE;
            for (LivingEntity target : list) {
                if (target == caster || target.isAlliedTo(caster)) {
                    continue;
                }
                double dist = target.distanceToSqr(end);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = target;
                }
            }
        }
        return best;
    }

    private static boolean activateBreath(LivingEntity entity, ChestCavityInstance cc) {
        if (entity == null || cc == null) {
            return false;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return false;
        }
        Level level = entity.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        OrganState state = OrganState.of(organ, STATE_ROOT);
        long gameTime = level.getGameTime();
        int charges = state.getInt(KEY_BREATH_CHARGES, BREATH_MAX_CHARGES);
        if (charges <= 0) {
            return false;
        }
        if (!consumeZhenyuan(entity, BREATH_COST_ZHENYUAN)) {
            return false;
        }
        state.setInt(KEY_BREATH_CHARGES, charges - 1);
        if (charges - 1 < BREATH_MAX_CHARGES) {
            state.setLong(KEY_BREATH_NEXT_RECHARGE_TICK, gameTime + BREATH_COOLDOWN_TICKS);
        }
        state.setLong(KEY_BREATH_LAST_CAST_TICK, gameTime);

        LivingEntity target = findBreathTarget(entity, 12.0D);
        boolean hasHuoxin = hasOrgan(cc, HOU_XIN_ORGAN_ID);
        boolean hasHuoYi = hasOrgan(cc, HOU_YI_ORGAN_ID);
        double refund = 0.0D;
        if (target != null) {
            target.hurt(entity.damageSources().mobAttack(entity), BREATH_BASE_DAMAGE);
            DragonflameHelper.applyDragonflame(entity, target, 2);
            refund += BREATH_REFUND_PER_TARGET;
            addCounter(state, 2, gameTime, true);
            if (ReactionTagOps.has(target, ReactionTagKeys.OIL_COATING)) {
                addCounter(state, 1, gameTime, true);
            }
            applyBreathExplosion(entity, target, state, gameTime);
            if (hasHuoxin) {
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, false));
            }
        }
        if (refund > 0.0D) {
            refund = Math.min(refund, BREATH_REFUND_CAP);
            refundZhenyuan(entity, refund);
        }

        if (hasHuoYi) {
            long next = state.getLong(KEY_BREATH_NEXT_RECHARGE_TICK, 0L);
            if (next > 0L) {
                state.setLong(KEY_BREATH_NEXT_RECHARGE_TICK, Math.max(gameTime, next - 40L));
            }
        }

        grantAscent(serverLevel, entity, state, BREATH_ASCENT_DURATION);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
        return true;
    }

    private static void applyBreathExplosion(LivingEntity caster, LivingEntity primary, OrganState state, long gameTime) {
        double maxHealth = caster.getMaxHealth();
        double aoeDamage = maxHealth * BREATH_AOE_RATIO;
        if (aoeDamage <= 0.0D) {
            return;
        }
        AABB area = primary.getBoundingBox().inflate(BREATH_AOE_RADIUS);
        Level level = primary.level();
        List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, area, VALID_TARGET);
        for (LivingEntity other : list) {
            if (other == caster || other == primary) {
                continue;
            }
            float amount = (float) aoeDamage;
            if (caster.isAlliedTo(other)) {
                amount *= 0.5F;
            }
            other.hurt(caster.damageSources().mobAttack(caster), amount);
        }
    }

    private static boolean activateHover(LivingEntity entity, ChestCavityInstance cc) {
        if (entity == null || cc == null) {
            return false;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return false;
        }
        Level level = entity.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (!consumeZhenyuan(entity, HOVER_COST_ZHENYUAN)) {
            return false;
        }
        OrganState state = OrganState.of(organ, STATE_ROOT);
        long gameTime = level.getGameTime();
        grantAscent(serverLevel, entity, state, HOVER_ASCENT_DURATION);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
        return true;
    }

    private static void grantAscent(ServerLevel level, LivingEntity entity, OrganState state, int duration) {
        ReactionTagOps.add(entity, ReactionTagKeys.DRAGON_ASCENT, duration);
        state.setLong(KEY_ASCENT_EXPIRE_TICK, level.getGameTime() + duration);
        state.setInt(KEY_ASCENT_GAINED, 0);
        entity.setNoGravity(true);
        entity.fallDistance = 0.0F;
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.3, 0.0, 0.3));
        TickOps.schedule(level, () -> entity.playSound(SoundEvents.BLAZE_BURN, 0.4F, 1.2F), 1);
    }

    private static boolean activateDive(LivingEntity entity, ChestCavityInstance cc) {
        if (entity == null || cc == null) {
            return false;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return false;
        }
        Level level = entity.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        OrganState state = OrganState.of(organ, STATE_ROOT);
        long gameTime = level.getGameTime();
        long ready = state.getLong(KEY_DIVE_READY_TICK, 0L);
        if (ready > 0L && gameTime < ready) {
            return false;
        }
        if (!consumeZhenyuan(entity, DIVE_COST_ZHENYUAN)) {
            return false;
        }
        float healthCost = (float) (entity.getMaxHealth() * DIVE_HEALTH_COST_RATIO);
        ResourceOps.drainHealth(entity, healthCost, entity.damageSources().generic());
        state.setLong(KEY_DIVE_READY_TICK, gameTime + DIVE_DURATION_TICKS + 40L);
        state.setLong(KEY_DIVE_IMMUNE_TICK, gameTime + DIVE_DURATION_TICKS + DIVE_POST_IMMUNE_TICKS);
        state.setDouble(KEY_DIVE_LAST_BONUS, computeDiveBonus(healthCost));
        boolean hasZhiZhuang = hasOrgan(cc, HOU_ZHI_ZHUANG_ORGAN_ID);
        ReactionTagOps.add(entity, ReactionTagKeys.DRAGON_DIVE, DIVE_DURATION_TICKS + DIVE_POST_IMMUNE_TICKS);
        entity.setNoGravity(true);
        entity.setDeltaMovement(entity.getLookAngle().normalize().scale(1.2D).add(0.0D, -1.2D, 0.0D));
        entity.hurtMarked = true;
        entity.playSound(SoundEvents.TRIDENT_RIPTIDE_3.value(), 1.0F, 1.0F);
        TickOps.schedule(serverLevel, () -> executeDiveImpact(serverLevel, entity, state, hasZhiZhuang), DIVE_DURATION_TICKS);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
        return true;
    }

    private static double computeDiveBonus(float healthCost) {
        if (healthCost <= 0.0F) {
            return 0.0D;
        }
        double multiplier = Math.floor(healthCost / 100.0F) * 0.15D;
        return Mth.clamp(multiplier, 0.0D, 0.60D);
    }

    private static void executeDiveImpact(ServerLevel level, LivingEntity entity, OrganState state, boolean hasZhiZhuang) {
        if (entity == null || !entity.isAlive()) {
            return;
        }
        double bonus = state.getDouble(KEY_DIVE_LAST_BONUS, 0.0D);
        float damage = (float) (DIVE_BASE_DAMAGE * (1.0D + bonus));
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                entity.getBoundingBox().inflate(DIVE_RADIUS), VALID_TARGET);
        int hitCount = 0;
        for (LivingEntity target : targets) {
            if (target == entity) {
                continue;
            }
            target.hurt(entity.damageSources().mobAttack(entity), damage);
            DragonflameHelper.applyDragonflame(entity, target, 2);
            hitCount++;
        }
        addCounter(state, hitCount >= 2 ? 8 : 5, level.getGameTime(), true);
        entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, DIVE_POST_IMMUNE_TICKS, 1, false, false));
        entity.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.8F);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
        entity.setNoGravity(false);
        if (hasZhiZhuang) {
            long ready = state.getLong(KEY_DIVE_READY_TICK, 0L);
            long now = level.getGameTime();
            if (ready > now) {
                state.setLong(KEY_DIVE_READY_TICK, Math.max(now, ready - 40L));
            }
            int charges = state.getInt(KEY_BREATH_CHARGES, BREATH_MAX_CHARGES);
            if (charges < BREATH_MAX_CHARGES) {
                state.setInt(KEY_BREATH_CHARGES, charges + 1);
            }
        }
    }
}
