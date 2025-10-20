package net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.CombatEntityUtil;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.AttributeOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.engine.dot.DoTEngine;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.DoTTypes;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps;

import java.util.*;
import java.util.function.LongUnaryOperator;

/**
 * 火龙蛊（脊椎）行为实现。
 * <p>
 * 为了控制复杂度，本实现保留设计稿中的关键要素：
 * - 三个主动技：龙焰吐息、龙脊·凝空、龙降俯冲；
 * - 龙焰印记 DoT（dot/yan_dao_dragonflame）与衍生反应；
 * - 计数器（龙脊战焰）与 4→5 转解锁逻辑；
 * - 与火心蛊 / 火衣蛊 / 直撞蛊的联动触发；
 * - 被动气运回复、击退抗性与吸血窗口。
 * </p>
 */
public final class HuoLongGuOrganBehavior extends AbstractGuzhenrenOrganBehavior implements
        OrganSlowTickListener,
        OrganOnHitListener,
        OrganIncomingDamageListener,
        OrganRemovalListener {

    public static final HuoLongGuOrganBehavior INSTANCE = new HuoLongGuOrganBehavior();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huo_long_gu");
    public static final ResourceLocation BREATH_ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huo_long_gu_breath");
    public static final ResourceLocation HOVER_ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huo_long_gu_hover");
    public static final ResourceLocation DIVE_ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huo_long_gu_dive");

    private static final ResourceLocation HUOXINGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huoxingu");
    private static final ResourceLocation HUOYI_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huo_gu");
    private static final ResourceLocation ZHIZHUANG_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "zhi_zhuang_gu");

    private static final ResourceLocation KNOCKBACK_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("chestcavity", "huo_long_gu/knockback");

    private static final String STATE_ROOT = "HuoLongGu";
    private static final String KEY_BREATH_SLOT_A = "BreathReadyA";
    private static final String KEY_BREATH_SLOT_B = "BreathReadyB";
    private static final String KEY_HOVER_READY = "HoverReady";
    private static final String KEY_DIVE_READY = "DiveReady";
    private static final String KEY_ASCENT_EXPIRE_TICK = "AscentExpire";
    private static final String KEY_ASCENT_STACKED = "AscentStacks";
    private static final String KEY_ASCENT_PROJECTILE_EXPIRE = "AscentProjectileUntil";
    private static final String KEY_DIVE_EXPIRE_TICK = "DiveExpire";
    private static final String KEY_LAST_DIVE_HEALTH = "LastDiveHealth";
    private static final String KEY_INVULN_EXPIRE_TICK = "InvulnExpire";
    private static final String KEY_COUNTER = "Counter";
    private static final String KEY_LAST_COUNTER_TICK = "CounterLast";
    private static final String KEY_DECAY_GATE_TICK = "CounterDecayGate";
    private static final String KEY_COUNTER_UNLOCKED = "CounterUnlocked";
    private static final String KEY_COUNTER_DECAY_STEP = "CounterDecayStep";
    private static final String KEY_RECENT_SYNERGY_GATE = "FireCoatGate";

    private static final int BREATH_COOLDOWN_TICKS = 20 * 20;
    private static final int BREATH_MAX_CHARGES = 2;
    private static final double BREATH_ZHENYUAN_COST = 30000.0D;
    private static final double BREATH_REFUND_PER_TARGET = 3000.0D;
    private static final double BREATH_REFUND_CAP = 15000.0D;
    private static final float BREATH_DAMAGE = 200.0F;
    private static final double BREATH_RANGE = 12.0D;
    private static final double BREATH_CONE_DOT = 0.6D;
    private static final float BREATH_AOE_RADIUS = 3.0F;
    private static final double BREATH_AOE_SELF_RATIO = 0.5D;

    private static final int HOVER_DURATION_TICKS = 60;
    private static final double HOVER_ZHENYUAN_COST = 150000.0D;

    private static final int DIVE_DURATION_TICKS = 40;
    private static final double DIVE_ZHENYUAN_COST = 250000.0D;
    private static final float DIVE_HEALTH_RATIO = 0.30F;
    private static final float DIVE_BASE_DAMAGE = 400.0F;
    private static final float DIVE_RADIUS = 4.0F;
    private static final float DIVE_HORIZONTAL_SPEED = 1.4F;
    private static final float DIVE_VERTICAL_SPEED = 1.1F;

    private static final int DRAGON_FLAME_DURATION_TICKS = 6 * 20;
    private static final int DRAGON_FLAME_MAX_STACKS = 6;

    private static final int COUNTER_THRESHOLD = 200;
    private static final int COUNTER_CAP = 1000;
    private static final int COUNTER_DECAY_GRACE_TICKS = 10 * 20;
    private static final int COUNTER_DECAY_INTERVAL_TICKS = 5 * 20;

    private static final int FIRE_COAT_COOLDOWN_TICKS = 8 * 20;

    private static final double KNOCKBACK_RESIST = 0.3D;

    private static final Map<UUID, Integer> LAST_BREATH_TARGET = new HashMap<>();

    static {
        OrganActivationListeners.register(BREATH_ABILITY_ID, HuoLongGuOrganBehavior::activateBreath);
        OrganActivationListeners.register(HOVER_ABILITY_ID, HuoLongGuOrganBehavior::activateHover);
        OrganActivationListeners.register(DIVE_ABILITY_ID, HuoLongGuOrganBehavior::activateDive);
        NeoForge.EVENT_BUS.register(HuoLongGuOrganBehavior.class);
    }

    private HuoLongGuOrganBehavior() {}

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel) || level.isClientSide()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }

        OrganState state = organState(organ, STATE_ROOT);
        MultiCooldown cooldown = createCooldown(cc, organ, state);
        long now = level.getGameTime();

        OrganStateOps.Collector collector = OrganStateOps.collector(cc, organ);
        boolean dirty = false;

        dirty |= maintainBreathCharges(cooldown, now);
        dirty |= maintainAscentState(serverLevel, player, cc, organ, state, collector, now);
        dirty |= maintainDiveState(serverLevel, player, state, collector, now);
        dirty |= applyPassiveBonuses(player, state, now);
        dirty |= handleCounterDecay(state, collector, now);

        if (dirty) {
            collector.commit();
        }
    }

    @Override
    public float onHit(DamageSource source, LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!(attacker instanceof Player player) || attacker.level().isClientSide()) {
            return damage;
        }
        if (target == null || !target.isAlive() || target == attacker) {
            return damage;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return damage;
        }

        OrganState state = organState(organ, STATE_ROOT);
        long now = attacker.level().getGameTime();
        applyDragonFlame(player, cc, organ, state, target, 1, now, true);
        return damage;
    }

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!(victim instanceof Player player) || player.level().isClientSide()) {
            return damage;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return damage;
        }
        OrganState state = organState(organ, STATE_ROOT);
        long now = player.level().getGameTime();

        long invuln = state.getLong(KEY_INVULN_EXPIRE_TICK, 0L);
        if (invuln > now) {
            return 0.0F;
        }

        boolean projectile = source.is(DamageTypeTags.IS_PROJECTILE);
        long projectileWindow = state.getLong(KEY_ASCENT_PROJECTILE_EXPIRE, 0L);
        if (projectile && projectileWindow > now) {
            return damage * 0.6F;
        }
        long ascentExpire = state.getLong(KEY_ASCENT_EXPIRE_TICK, 0L);
        long diveExpire = state.getLong(KEY_DIVE_EXPIRE_TICK, 0L);
        if ((ascentExpire > now || diveExpire > now) && projectile) {
            return damage * 0.8F;
        }
        return damage;
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player)) {
            return;
        }
        AttributeInstance knockback = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        AttributeOps.removeById(knockback, KNOCKBACK_MODIFIER_ID);
    }

    private static void activateBreath(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (cc == null || cc.inventory == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        OrganState state = INSTANCE.organState(organ, STATE_ROOT);
        MultiCooldown cooldown = INSTANCE.createCooldown(cc, organ, state);
        long now = player.level().getGameTime();

        MultiCooldown.Entry chargeEntry = INSTANCE.findReadyBreathCharge(cooldown, now);
        if (chargeEntry == null) {
            return;
        }
        OptionalDouble consumed = ResourceOps.tryConsumeScaledZhenyuan(player, BREATH_ZHENYUAN_COST);
        if (consumed.isEmpty()) {
            return;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 origin = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB search = player.getBoundingBox().expandTowards(look.scale(BREATH_RANGE)).inflate(2.0D);
        List<LivingEntity> candidates = serverLevel.getEntitiesOfClass(LivingEntity.class, search,
                target -> CombatEntityUtil.areEnemies(player, target));

        double refund = 0.0D;
        int hits = 0;
        for (LivingEntity target : candidates) {
            Vec3 toTarget = target.getEyePosition().subtract(origin);
            double dist = toTarget.length();
            if (dist <= 0.0001D || dist > BREATH_RANGE) {
                continue;
            }
            Vec3 dir = toTarget.normalize();
            double dot = dir.dot(look);
            if (dot < BREATH_CONE_DOT) {
                continue;
            }
            hits++;
            target.hurt(player.damageSources().playerAttack(player), BREATH_DAMAGE);
            double aoeDamage = Math.max(0.0D, player.getMaxHealth() * 0.10D);
            applyBreathAoE(serverLevel, player, target.position(), aoeDamage);
            INSTANCE.applyDragonFlame(player, cc, organ, state, target, 2, now, false);
            refund += BREATH_REFUND_PER_TARGET;
            LAST_BREATH_TARGET.put(player.getUUID(), target.getId());
        }

        refund = Math.min(refund, BREATH_REFUND_CAP);
        if (refund > 0.0D) {
            ResourceOps.tryReplenishScaledZhenyuan(player, refund, true);
        }
        if (hits > 0) {
            INSTANCE.adjustCounter(state, cc, organ, now, hits * 2);
        }

        chargeEntry.setReadyAt(now + BREATH_COOLDOWN_TICKS);
        if (player instanceof ServerPlayer sp) {
            ActiveSkillRegistry.scheduleReadyToast(sp, BREATH_ABILITY_ID, chargeEntry.getReadyTick(), now);
        }
        INSTANCE.startAscent(serverLevel, player, cc, organ, state, now, HOVER_DURATION_TICKS / 2, true);
        spawnBreathFx(serverLevel, player, look, hits);
    }

    private static void spawnBreathFx(ServerLevel level, Player player, Vec3 look, int hits) {
        RandomSource random = player.getRandom();
        Vec3 origin = player.getEyePosition();
        double step = BREATH_RANGE / 16.0D;
        for (int i = 0; i < 16; i++) {
            Vec3 pos = origin.add(look.scale(step * i));
            double spread = 0.25D + hits * 0.05D;
            for (int j = 0; j < 4; j++) {
                double ox = (random.nextDouble() - 0.5D) * spread;
                double oy = (random.nextDouble() - 0.5D) * 0.4D;
                double oz = (random.nextDouble() - 0.5D) * spread;
                level.sendParticles(ParticleTypes.FLAME, pos.x + ox, pos.y + oy, pos.z + oz, 1, 0.0D, 0.0D, 0.0D, 0.015D);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    private static void activateHover(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (cc == null || cc.inventory == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        OrganState state = INSTANCE.organState(organ, STATE_ROOT);
        MultiCooldown cooldown = INSTANCE.createCooldown(cc, organ, state);
        long now = player.level().getGameTime();
        MultiCooldown.Entry ready = cooldown.entry(KEY_HOVER_READY);
        if (!ready.isReady(now)) {
            return;
        }
        OptionalDouble consumed = ResourceOps.tryConsumeScaledZhenyuan(player, HOVER_ZHENYUAN_COST);
        if (consumed.isEmpty()) {
            return;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        INSTANCE.startAscent(serverLevel, player, cc, organ, state, now, HOVER_DURATION_TICKS, false);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, HOVER_DURATION_TICKS, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, HOVER_DURATION_TICKS + 20, 0, false, true));
        ready.setReadyAt(now + HOVER_DURATION_TICKS + 60);
        if (player instanceof ServerPlayer sp) {
            ActiveSkillRegistry.scheduleReadyToast(sp, HOVER_ABILITY_ID, ready.getReadyTick(), now);
        }
        spawnHoverFx(serverLevel, player);
        INSTANCE.adjustCounter(state, cc, organ, now, 3);
        INSTANCE.triggerHoverEcho(serverLevel, player, cc, organ, state, now);
    }

    private static void spawnHoverFx(ServerLevel level, Player player) {
        RandomSource random = player.getRandom();
        for (int i = 0; i < 10; i++) {
            double ox = (random.nextDouble() - 0.5D) * 0.5D;
            double oz = (random.nextDouble() - 0.5D) * 0.5D;
            level.sendParticles(ParticleTypes.SMALL_FLAME, player.getX() + ox, player.getY() + 0.1D, player.getZ() + oz, 1, 0.0D, 0.01D, 0.0D, 0.0D);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.4F, 1.2F);
    }

    private static void activateDive(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (cc == null || cc.inventory == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        OrganState state = INSTANCE.organState(organ, STATE_ROOT);
        MultiCooldown cooldown = INSTANCE.createCooldown(cc, organ, state);
        long now = player.level().getGameTime();
        MultiCooldown.Entry ready = cooldown.entry(KEY_DIVE_READY);
        if (!ready.isReady(now)) {
            return;
        }
        OptionalDouble consumed = ResourceOps.tryConsumeScaledZhenyuan(player, DIVE_ZHENYUAN_COST);
        if (consumed.isEmpty()) {
            return;
        }
        float healthCost = player.getHealth() * DIVE_HEALTH_RATIO;
        if (!ResourceOps.drainHealth(player, healthCost, player.damageSources().magic())) {
            return;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 motion = new Vec3(direction.x * DIVE_HORIZONTAL_SPEED, -DIVE_VERTICAL_SPEED, direction.z * DIVE_HORIZONTAL_SPEED);
        player.setNoGravity(true);
        player.setDeltaMovement(motion);
        player.hurtMarked = true;
        player.hasImpulse = true;
        ReactionTagOps.add(player, ReactionTagKeys.DRAGON_DIVE, DIVE_DURATION_TICKS);
        OrganStateOps.setLong(state, cc, organ, KEY_DIVE_EXPIRE_TICK, now + DIVE_DURATION_TICKS, LongUnaryOperator.identity(), 0L);
        OrganStateOps.setDouble(state, cc, organ, KEY_LAST_DIVE_HEALTH, healthCost, d -> Math.max(0.0D, d), 0.0D);
        ready.setReadyAt(now + DIVE_DURATION_TICKS + 20 * 10);
        if (player instanceof ServerPlayer sp) {
            ActiveSkillRegistry.scheduleReadyToast(sp, DIVE_ABILITY_ID, ready.getReadyTick(), now);
        }
        INSTANCE.adjustCounter(state, cc, organ, now, 5);
        scheduleDiveFinish(serverLevel, player.getUUID(), now + DIVE_DURATION_TICKS);
    }

    private static void scheduleDiveFinish(ServerLevel level, UUID playerId, long finishTick) {
        TickOps.schedule(level, () -> INSTANCE.finishDive(level, playerId, finishTick), DIVE_DURATION_TICKS);
    }

    private void finishDive(ServerLevel level, UUID playerId, long finishTick) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
        if (player == null) {
            return;
        }
        player.setNoGravity(false);
        player.hurtMarked = true;
        ChestCavityInstance cc = ChestCavityEntity.of(player).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
        if (cc == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        OrganState state = organState(organ, STATE_ROOT);
        double healthCost = state.getDouble(KEY_LAST_DIVE_HEALTH, 0.0D);
        long now = level.getGameTime();
        OrganStateOps.setLong(state, cc, organ, KEY_DIVE_EXPIRE_TICK, 0L, LongUnaryOperator.identity(), 0L);
        OrganStateOps.setDouble(state, cc, organ, KEY_LAST_DIVE_HEALTH, 0.0D, d -> 0.0D, 0.0D);
        float bonusMultiplier = Math.min(0.60F, (float) Math.floor(Math.max(0.0D, healthCost) / 100.0D) * 0.15F);
        float totalDamage = DIVE_BASE_DAMAGE * (1.0F + bonusMultiplier);
        AABB area = player.getBoundingBox().inflate(DIVE_RADIUS);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area,
                target -> CombatEntityUtil.areEnemies(player, target));
        int hits = 0;
        for (LivingEntity target : victims) {
            if (!target.isAlive()) {
                continue;
            }
            target.hurt(player.damageSources().playerAttack(player), totalDamage);
            if (ReactionTagOps.has(target, ReactionTagKeys.DRAGON_FLAME_MARK)) {
                applyDragonFlame(player, cc, organ, state, target, 2, now, false);
            }
            hits++;
        }
        level.explode(player, player.getX(), player.getY(), player.getZ(), 1.6F, Level.ExplosionInteraction.MOB);
        ReactionTagOps.add(player, ReactionTagKeys.FIRE_IMMUNE, 30);
        OrganStateOps.setLong(state, cc, organ, KEY_INVULN_EXPIRE_TICK, now + 30, LongUnaryOperator.identity(), 0L);
        if (hits >= 2 && hasOrgan(cc, ZHIZHUANG_ID)) {
            rechargeBreathCharge(cc, organ, state, now, 20 * 2);
        }
        if (hits > 0) {
            adjustCounter(state, cc, organ, now, 5 + Math.min(3, hits));
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        ChestCavityInstance cc = ChestCavityEntity.of(player).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
        if (cc == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        OrganState state = INSTANCE.organState(organ, STATE_ROOT);
        INSTANCE.adjustCounter(state, cc, organ, player.level().getGameTime(), 3);
    }

    private boolean maintainBreathCharges(MultiCooldown cooldown, long now) {
        MultiCooldown.Entry slotA = cooldown.entry(KEY_BREATH_SLOT_A);
        MultiCooldown.Entry slotB = cooldown.entry(KEY_BREATH_SLOT_B);
        boolean dirty = false;
        if (slotA.getReadyTick() < 0L) {
            slotA.setReadyAt(0L);
            dirty = true;
        }
        if (slotB.getReadyTick() < 0L) {
            slotB.setReadyAt(0L);
            dirty = true;
        }
        if (slotA.getReadyTick() > 0L && slotA.getReadyTick() <= now) {
            slotA.setReadyAt(0L);
            dirty = true;
        }
        if (slotB.getReadyTick() > 0L && slotB.getReadyTick() <= now) {
            slotB.setReadyAt(0L);
            dirty = true;
        }
        return dirty;
    }

    private boolean maintainAscentState(ServerLevel level, Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, OrganStateOps.Collector collector, long now) {
        boolean dirty = false;
        long expire = state.getLong(KEY_ASCENT_EXPIRE_TICK, 0L);
        if (expire > now) {
            player.setNoGravity(true);
            player.fallDistance = 0.0F;
        } else if (expire != 0L) {
            player.setNoGravity(false);
            dirty = true;
            collector.record(state.setLong(KEY_ASCENT_EXPIRE_TICK, 0L));
            collector.record(state.setInt(KEY_ASCENT_STACKED, 0, value -> Math.max(0, value), 0));
        }
        int stacks = state.getInt(KEY_ASCENT_STACKED, 0);
        if (expire > now && stacks < 3) {
            collector.record(state.setInt(KEY_ASCENT_STACKED, stacks + 1, value -> Math.min(3, Math.max(0, value)), 0));
            adjustCounter(state, cc, organ, now, 1);
            dirty = true;
        }
        return dirty;
    }

    private boolean maintainDiveState(ServerLevel level, Player player, OrganState state, OrganStateOps.Collector collector, long now) {
        long expire = state.getLong(KEY_DIVE_EXPIRE_TICK, 0L);
        if (expire > now) {
            player.setNoGravity(true);
            player.fallDistance = 0.0F;
            return false;
        }
        if (expire != 0L) {
            player.setNoGravity(false);
            collector.record(state.setLong(KEY_DIVE_EXPIRE_TICK, 0L));
            return true;
        }
        return false;
    }

    private boolean applyPassiveBonuses(Player player, OrganState state, long now) {
        boolean dirty = false;
        AttributeInstance knockback = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            AttributeModifier modifier = new AttributeModifier(KNOCKBACK_MODIFIER_ID, KNOCKBACK_RESIST, AttributeModifier.Operation.ADD_VALUE);
            AttributeOps.replaceTransient(knockback, KNOCKBACK_MODIFIER_ID, modifier);
        }
        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isPresent()) {
            ResourceHandle handle = handleOpt.get();
            double maxQiyun = handle.read("qiyun_shangxian").orElse(0.0D);
            if (maxQiyun > 0.0D) {
                double rate = state.getBoolean(KEY_COUNTER_UNLOCKED, false) ? 0.0015D : 0.0010D;
                double delta = maxQiyun * rate;
                handle.adjustDouble("qiyun", delta, true, "qiyun_shangxian");
            }
        }
        return dirty;
    }

    private boolean handleCounterDecay(OrganState state, OrganStateOps.Collector collector, long now) {
        int counter = Math.max(0, state.getInt(KEY_COUNTER, 0));
        long decayGate = state.getLong(KEY_DECAY_GATE_TICK, 0L);
        long decayStep = state.getLong(KEY_COUNTER_DECAY_STEP, 0L);
        boolean dirty = false;
        if (counter >= COUNTER_THRESHOLD && !state.getBoolean(KEY_COUNTER_UNLOCKED, false)) {
            collector.record(state.setBoolean(KEY_COUNTER_UNLOCKED, true, false));
            dirty = true;
        }
        if (decayGate <= now && counter > 0) {
            if (decayStep <= now) {
                counter = Math.max(0, counter - 1);
                collector.record(state.setInt(KEY_COUNTER, counter, value -> Mth.clamp(value, 0, COUNTER_CAP), 0));
                collector.record(state.setLong(KEY_COUNTER_DECAY_STEP, now + COUNTER_DECAY_INTERVAL_TICKS, LongUnaryOperator.identity(), 0L));
                dirty = true;
            }
        }
        return dirty;
    }

    private MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ, OrganState state) {
        return MultiCooldown.builder(state)
                .withSync(cc, organ)
                .withLongClamp(value -> Math.max(0L, value), 0L)
                .build();
    }

    private MultiCooldown.Entry findReadyBreathCharge(MultiCooldown cooldown, long now) {
        MultiCooldown.Entry slotA = cooldown.entry(KEY_BREATH_SLOT_A);
        MultiCooldown.Entry slotB = cooldown.entry(KEY_BREATH_SLOT_B);
        boolean readyA = slotA.isReady(now);
        boolean readyB = slotB.isReady(now);
        if (readyA && readyB) {
            return slotA.getReadyTick() <= slotB.getReadyTick() ? slotA : slotB;
        }
        if (readyA) {
            return slotA;
        }
        if (readyB) {
            return slotB;
        }
        return null;
    }

    private void applyDragonFlame(Player attacker, ChestCavityInstance cc, ItemStack organ, OrganState state, LivingEntity target, int stacksToAdd, long now, boolean fromMelee) {
        if (!(target.level() instanceof ServerLevel) || cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        int currentStacks = Math.max(0, ReactionTagOps.count(target, ReactionTagKeys.DRAGON_FLAME_MARK));
        int desired = Math.min(DRAGON_FLAME_MAX_STACKS, currentStacks + Math.max(0, stacksToAdd));
        int delta = desired - currentStacks;
        ReactionTagOps.addStacked(target, ReactionTagKeys.DRAGON_FLAME_MARK, delta, DRAGON_FLAME_DURATION_TICKS);
        double attackValue = 0.0D;
        AttributeInstance attackAttr = attacker.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackValue = attackAttr.getBaseValue();
        }
        double perSecond = 6.0D + attackValue * 0.005D * desired + target.getMaxHealth() * 0.002D;
        if (ReactionTagOps.has(target, ReactionTagKeys.OIL_COATING)) {
            perSecond *= 1.35D;
        }
        DoTEngine.schedulePerSecond(attacker, target, perSecond, DRAGON_FLAME_DURATION_TICKS / 20,
                SoundEvents.BLAZE_BURN, 0.5F, 1.0F,
                DoTTypes.YAN_DAO_DRAGONFLAME,
                null,
                DoTEngine.FxAnchor.TARGET,
                Vec3.ZERO,
                1.0F);
        if (delta > 0) {
            adjustCounter(state, cc, organ, now, delta);
        }
        if (hasOrgan(cc, HUOXINGU_ID)) {
            ResourceOps.tryAdjustJingli(attacker, 2.0D, true);
            attacker.heal(0.5F);
            if (state.getBoolean(KEY_COUNTER_UNLOCKED, false)) {
                ResourceOps.tryReplenishScaledZhenyuan(attacker, 10.0D, true);
            }
        }
        if (hasOrgan(cc, HUOYI_ID) && desired >= 3) {
            long gate = state.getLong(KEY_RECENT_SYNERGY_GATE, 0L);
            if (gate <= now) {
                shortenBreathCooldown(cc, organ, state, now, 40);
                OrganStateOps.setLong(state, cc, organ, KEY_RECENT_SYNERGY_GATE, now + FIRE_COAT_COOLDOWN_TICKS, LongUnaryOperator.identity(), 0L);
            }
        }
        if (fromMelee) {
            double leech = Math.min(6.0D, desired * 1.0D);
            double healed = leech / 100.0D * target.getMaxHealth();
            attacker.heal((float) healed);
        }
    }

    private static void applyBreathAoE(ServerLevel level, Player player, Vec3 center, double amount) {
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(BREATH_AOE_RADIUS),
                entity -> entity != player);
        for (LivingEntity victim : victims) {
            float dmg = (float) amount;
            if (victim.isAlliedTo(player)) {
                dmg *= BREATH_AOE_SELF_RATIO;
            }
            victim.hurt(player.damageSources().playerAttack(player), dmg);
        }
    }

    private void startAscent(ServerLevel level, Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, long now, int duration, boolean chain) {
        player.setNoGravity(true);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        ReactionTagOps.add(player, ReactionTagKeys.DRAGON_ASCENT, duration);
        OrganStateOps.setLong(state, cc, organ, KEY_ASCENT_EXPIRE_TICK, now + duration, LongUnaryOperator.identity(), 0L);
        OrganStateOps.setLong(state, cc, organ, KEY_ASCENT_PROJECTILE_EXPIRE, now + Math.min(duration, 40), LongUnaryOperator.identity(), 0L);
        TickOps.schedule(level, () -> resetGravity(level, player.getUUID()), duration);
    }

    private static void resetGravity(ServerLevel level, UUID playerId) {
        ServerPlayer sp = level.getServer().getPlayerList().getPlayer(playerId);
        if (sp != null) {
            sp.setNoGravity(false);
            sp.hurtMarked = true;
        }
    }

    private void triggerHoverEcho(ServerLevel level, Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, long now) {
        Integer id = LAST_BREATH_TARGET.remove(player.getUUID());
        if (id == null) {
            return;
        }
        Entity entity = level.getEntity(id);
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        Vec3 origin = player.position();
        Vec3 target = living.position();
        Vec3 dir = target.subtract(origin);
        double distance = dir.length();
        if (distance <= 0.01D) {
            return;
        }
        dir = dir.normalize();
        for (int i = 0; i < 8; i++) {
            double t = i / 8.0D;
            Vec3 pos = origin.add(dir.scale(distance * t));
            level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.5D, pos.z, 1, 0.0D, 0.01D, 0.0D, 0.0D);
        }
        living.hurt(player.damageSources().playerAttack(player), 100.0F);
        applyDragonFlame(player, cc, organ, state, living, 1, now, false);
    }

    private static ItemStack findOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (ORGAN_ID.equals(id)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean hasOrgan(ChestCavityInstance cc, ResourceLocation organId) {
        if (cc == null || cc.inventory == null) {
            return false;
        }
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (organId.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private void adjustCounter(OrganState state, ChestCavityInstance cc, ItemStack organ, long now, int delta) {
        if (delta == 0 || cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        int current = Math.max(0, state.getInt(KEY_COUNTER, 0));
        int next = Mth.clamp(current + delta, 0, COUNTER_CAP);
        OrganStateOps.setInt(state, cc, organ, KEY_COUNTER, next, value -> Mth.clamp(value, 0, COUNTER_CAP), 0);
        OrganStateOps.setLong(state, cc, organ, KEY_LAST_COUNTER_TICK, now, LongUnaryOperator.identity(), 0L);
        OrganStateOps.setLong(state, cc, organ, KEY_DECAY_GATE_TICK, now + COUNTER_DECAY_GRACE_TICKS, LongUnaryOperator.identity(), 0L);
        OrganStateOps.setLong(state, cc, organ, KEY_COUNTER_DECAY_STEP, now + COUNTER_DECAY_INTERVAL_TICKS, LongUnaryOperator.identity(), 0L);
        if (next >= COUNTER_THRESHOLD) {
            OrganStateOps.setBoolean(state, cc, organ, KEY_COUNTER_UNLOCKED, true, false);
        }
    }

    private void shortenBreathCooldown(ChestCavityInstance cc, ItemStack organ, OrganState state, long now, int ticks) {
        MultiCooldown cooldown = createCooldown(cc, organ, state);
        MultiCooldown.Entry slotA = cooldown.entry(KEY_BREATH_SLOT_A);
        MultiCooldown.Entry slotB = cooldown.entry(KEY_BREATH_SLOT_B);
        if (!slotA.isReady(now)) {
            slotA.setReadyAt(Math.max(now, slotA.getReadyTick() - ticks));
        }
        if (!slotB.isReady(now)) {
            slotB.setReadyAt(Math.max(now, slotB.getReadyTick() - ticks));
        }
    }

    private void rechargeBreathCharge(ChestCavityInstance cc, ItemStack organ, OrganState state, long now, int reduction) {
        MultiCooldown cooldown = createCooldown(cc, organ, state);
        MultiCooldown.Entry slotA = cooldown.entry(KEY_BREATH_SLOT_A);
        MultiCooldown.Entry slotB = cooldown.entry(KEY_BREATH_SLOT_B);
        if (!slotA.isReady(now) && slotA.getReadyTick() - reduction <= now) {
            slotA.setReadyAt(now);
            return;
        }
        if (!slotB.isReady(now) && slotB.getReadyTick() - reduction <= now) {
            slotB.setReadyAt(now);
        }
    }
}
