package net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.AttributeOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

import java.util.List;

/**
 * 血战蛊（血道·血脉）核心行为：战血值、血怒与血誓爆发。
 */
public final class XueZhanGuOrganBehavior extends AbstractGuzhenrenOrganBehavior implements OrganSlowTickListener, OrganOnHitListener, OrganIncomingDamageListener {

    public static final XueZhanGuOrganBehavior INSTANCE = new XueZhanGuOrganBehavior();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "xuezhangu");
    public static final ResourceLocation ABILITY_ID = ORGAN_ID;

    private static final String STATE_ROOT = "XueZhanGu";
    private static final String KEY_WAR_BLOOD = "WarBlood";
    private static final String KEY_LAST_ACTIVITY_TICK = "LastActivity";
    private static final String KEY_NEXT_HUNGER_TICK = "NextHunger";
    private static final String KEY_LAST_STEP = "LastStep";
    private static final String KEY_RAGE_ACTIVE = "BloodRageActive";
    private static final String KEY_RAGE_EXPIRE = "BloodRageExpire";
    private static final String KEY_RAGE_READY = "BloodRageReady";
    private static final String KEY_OATH_ACTIVE = "BloodOathActive";
    private static final String KEY_OATH_EXPIRE = "BloodOathExpire";
    private static final String KEY_OATH_READY = "BloodOathReady";

    private static final double MAX_WAR_BLOOD = 100.0;
    private static final double WAR_BLOOD_GAIN_DEALT_SCALAR = 0.6;
    private static final double WAR_BLOOD_GAIN_DEALT_MIN = 1.0;
    private static final double WAR_BLOOD_GAIN_TAKEN_SCALAR = 1.0;
    private static final double WAR_BLOOD_GAIN_TAKEN_RATIO_BONUS = 25.0;
    private static final double OUT_OF_COMBAT_DECAY = 0.5;
    private static final long OUT_OF_COMBAT_WINDOW_TICKS = 100L;
    private static final double MIN_WAR_BLOOD = -MAX_WAR_BLOOD;

    private static final int NEGATIVE_EVENT_INTERVAL_TICKS = 20 * 20;
    private static final float NEGATIVE_HEALTH_LOSS_MIN = 1.0f;
    private static final float NEGATIVE_HEALTH_LOSS_MAX = 5.0f;
    private static final double NEGATIVE_TELEPORT_CHANCE = 0.05;

    private static final int BLOOD_RAGE_COOLDOWN_TICKS = 30 * 20;
    private static final int BLOOD_RAGE_DURATION_TICKS = 10 * 20;
    private static final double BLOOD_RAGE_DAMAGE_BONUS = 0.30;
    private static final double BLOOD_RAGE_DEFENSE_PENALTY = -0.20;
    private static final double BLOOD_RAGE_HEALTH_THRESHOLD = 0.30;
    private static final double BLOOD_RAGE_LIFESTEAL = 0.10;
    private static final double BLOOD_RAGE_JINGLI_PER_HIT = 10.0;

    private static final int BLOOD_OATH_DURATION_TICKS = 10 * 20;
    private static final int BLOOD_OATH_COOLDOWN_TICKS = 40 * 20;
    private static final double BLOOD_OATH_DAMAGE_BONUS = 0.25;
    private static final double BLOOD_OATH_LIFESTEAL = 0.10;
    private static final double BLOOD_OATH_JINGLI_PER_HIT = 100.0;
    private static final double BLOOD_OATH_TARGET_JINGLI_DRAIN = 20.0;

    private static final double OATH_HEALTH_COST_RATIO = 0.20;
    private static final double OATH_ZHENYUAN_COST = 400.0;
    private static final double OATH_BASE_DAMAGE = 40.0;
    private static final double OATH_DAMAGE_PER_WAR_BLOOD = 0.5;
    private static final double OATH_HEAL_RATIO_PER_TARGET = 0.05;
    private static final double OATH_RADIUS = 6.0;

    private static final DustParticleOptions PASSIVE_TRAIL =
            new DustParticleOptions(new Vec3(200, 20, 30).normalize().toVector3f(), 1.2f);
    private static final DustColorTransitionOptions HEART_BEAT =
            new DustColorTransitionOptions(new Vec3(220, 30, 60).normalize().toVector3f(),
                    new Vec3(255, 80, 100).normalize().toVector3f(), 1.0f);
    private static final DustParticleOptions OATH_RING =
            new DustParticleOptions(new Vec3(240, 0, 0).normalize().toVector3f(), 1.5f);

    private static final ResourceLocation WAR_BLOOD_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "xuezhangu/warblood_attack");
    private static final ResourceLocation WAR_BLOOD_SPEED_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "xuezhangu/warblood_speed");
    private static final ResourceLocation BLOOD_RAGE_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "xuezhangu/bloodrage_attack");
    private static final ResourceLocation BLOOD_RAGE_DEFENSE_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "xuezhangu/bloodrage_defense");
    private static final ResourceLocation BLOOD_OATH_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "xuezhangu/bloodoath_attack");
    private static final ResourceLocation HEART_BEAT_SOUND = ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "custom.common.heart_beat");

    static {
        OrganActivationListeners.register(ABILITY_ID, XueZhanGuOrganBehavior::activateAbility);
    }

    private XueZhanGuOrganBehavior() {}

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || player.level().isClientSide() || cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }

        OrganState state = OrganState.of(organ, STATE_ROOT);
        long now = player.level().getGameTime();

        double warBlood = getWarBlood(state);
        long lastActivity = state.getLong(KEY_LAST_ACTIVITY_TICK, 0L);
        boolean inCombat = now - lastActivity <= OUT_OF_COMBAT_WINDOW_TICKS;

        if (!inCombat) {
            warBlood = adjustWarBlood(player, cc, organ, state, -OUT_OF_COMBAT_DECAY);
        }

        if (warBlood < 0.0 && now >= state.getLong(KEY_NEXT_HUNGER_TICK, 0L)) {
            triggerBloodHunger(player, state, now);
        }

        updateWarBloodAttributes(player, state, warBlood);

        handleBloodRage(player, cc, organ, state, now);
        handleBloodOath(player, cc, organ, state, now);

        if (player.level() instanceof ServerLevel server) {
            spawnPassiveParticles(server, player, warBlood);
        }
    }

    @Override
    public float onHit(DamageSource source, LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!(attacker instanceof Player player) || player.level().isClientSide()) {
            return damage;
        }
        if (!matchesOrgan(organ, ORGAN_ID) || cc == null || organ == null || organ.isEmpty()) {
            return damage;
        }
        if (damage <= 0.0F || target == null || !target.isAlive()) {
            return damage;
        }

        OrganState state = OrganState.of(organ, STATE_ROOT);
        long now = player.level().getGameTime();
        state.setLong(KEY_LAST_ACTIVITY_TICK, now, v -> Math.max(0L, v), 0L);

        double gain = Math.max(WAR_BLOOD_GAIN_DEALT_MIN, damage * WAR_BLOOD_GAIN_DEALT_SCALAR);
        double warBlood = adjustWarBlood(player, cc, organ, state, gain);
        updateWarBloodAttributes(player, state, warBlood);

        applyOnHitEffects(player, target, damage, state);

        if (player.level() instanceof ServerLevel server) {
            spawnImpactParticles(server, target.position());
        }

        return damage;
    }

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!(victim instanceof Player player) || player.level().isClientSide()) {
            return damage;
        }
        if (!matchesOrgan(organ, ORGAN_ID) || cc == null || organ == null || organ.isEmpty()) {
            return damage;
        }
        if (damage <= 0.0F) {
            return damage;
        }

        OrganState state = OrganState.of(organ, STATE_ROOT);
        long now = player.level().getGameTime();
        state.setLong(KEY_LAST_ACTIVITY_TICK, now, v -> Math.max(0L, v), 0L);

        double maxHealth = player.getMaxHealth();
        double ratio = maxHealth > 0.0 ? Mth.clamp(damage / maxHealth, 0.0, 1.0) : 0.0;
        double gain = damage * WAR_BLOOD_GAIN_TAKEN_SCALAR + ratio * WAR_BLOOD_GAIN_TAKEN_RATIO_BONUS;
        double warBlood = adjustWarBlood(player, cc, organ, state, gain);
        updateWarBloodAttributes(player, state, warBlood);

        return damage;
    }

    private static void handleBloodRage(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, long now) {
        boolean active = state.getBoolean(KEY_RAGE_ACTIVE, false);
        long expire = state.getLong(KEY_RAGE_EXPIRE, 0L);
        if (active && now >= expire) {
            state.setBoolean(KEY_RAGE_ACTIVE, false);
            updateBloodRageModifiers(player, false);
            INSTANCE.sendSlotUpdate(cc, organ);
            active = false;
        }

        if (!active) {
            long ready = state.getLong(KEY_RAGE_READY, 0L);
            if (now >= ready && player.getHealth() / player.getMaxHealth() <= BLOOD_RAGE_HEALTH_THRESHOLD) {
                activateBloodRage(player, cc, organ, state, now);
            }
        }
    }

    private static void handleBloodOath(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, long now) {
        boolean active = state.getBoolean(KEY_OATH_ACTIVE, false);
        long expire = state.getLong(KEY_OATH_EXPIRE, 0L);
        if (active && now >= expire) {
            state.setBoolean(KEY_OATH_ACTIVE, false);
            updateBloodOathModifiers(player, false);
            INSTANCE.sendSlotUpdate(cc, organ);
        }
    }

    private static void activateBloodRage(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, long now) {
        state.setBoolean(KEY_RAGE_ACTIVE, true);
        state.setLong(KEY_RAGE_EXPIRE, now + BLOOD_RAGE_DURATION_TICKS, v -> Math.max(0L, v), 0L);
        state.setLong(KEY_RAGE_READY, now + BLOOD_RAGE_COOLDOWN_TICKS, v -> Math.max(0L, v), 0L);
        updateBloodRageModifiers(player, true);
        INSTANCE.sendSlotUpdate(cc, organ);
    }

    private static void updateBloodRageModifiers(Player player, boolean apply) {
        AttributeInstance attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
        if (apply) {
            AttributeOps.replaceTransient(attackAttr, BLOOD_RAGE_ATTACK_ID,
                    new AttributeModifier(BLOOD_RAGE_ATTACK_ID, BLOOD_RAGE_DAMAGE_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            AttributeOps.replaceTransient(armorAttr, BLOOD_RAGE_DEFENSE_ID,
                    new AttributeModifier(BLOOD_RAGE_DEFENSE_ID, BLOOD_RAGE_DEFENSE_PENALTY, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else {
            AttributeOps.removeById(attackAttr, BLOOD_RAGE_ATTACK_ID);
            AttributeOps.removeById(armorAttr, BLOOD_RAGE_DEFENSE_ID);
        }
    }

    private static void updateBloodOathModifiers(Player player, boolean apply) {
        AttributeInstance attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (apply) {
            AttributeOps.replaceTransient(attackAttr, BLOOD_OATH_ATTACK_ID,
                    new AttributeModifier(BLOOD_OATH_ATTACK_ID, BLOOD_OATH_DAMAGE_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else {
            AttributeOps.removeById(attackAttr, BLOOD_OATH_ATTACK_ID);
        }
    }

    private static void applyOnHitEffects(Player player, LivingEntity target, float damage, OrganState state) {
        double lifesteal = 0.0;
        boolean bloodRage = state.getBoolean(KEY_RAGE_ACTIVE, false);
        boolean bloodOath = state.getBoolean(KEY_OATH_ACTIVE, false);

        if (bloodRage) {
            lifesteal += BLOOD_RAGE_LIFESTEAL;
            ResourceOps.tryAdjustJingli(player, BLOOD_RAGE_JINGLI_PER_HIT);
        }
        if (bloodOath) {
            lifesteal += BLOOD_OATH_LIFESTEAL;
            ResourceOps.tryAdjustJingli(player, BLOOD_OATH_JINGLI_PER_HIT);
            if (target instanceof Player targetPlayer) {
                ResourceOps.tryAdjustJingli(targetPlayer, -BLOOD_OATH_TARGET_JINGLI_DRAIN, true);
            } else {
                ResourceOps.tryAdjustJingli(target, -BLOOD_OATH_TARGET_JINGLI_DRAIN, true);
            }
        }

        if (lifesteal > 0.0 && damage > 0.0F) {
            player.heal(damage * (float) lifesteal);
        }
    }

    private static double adjustWarBlood(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, double delta) {
        double current = getWarBlood(state);
        double updated = Mth.clamp(current + delta, MIN_WAR_BLOOD, MAX_WAR_BLOOD);
        if (!Mth.equal((float) current, (float) updated)) {
            state.setDouble(KEY_WAR_BLOOD, updated, value -> Mth.clamp(value, MIN_WAR_BLOOD, MAX_WAR_BLOOD), 0.0D);
            INSTANCE.sendSlotUpdate(cc, organ);
        }
        return updated;
    }

    private static double getWarBlood(OrganState state) {
        return Mth.clamp(state.getDouble(KEY_WAR_BLOOD, 0.0D), MIN_WAR_BLOOD, MAX_WAR_BLOOD);
    }

    private static void updateWarBloodAttributes(Player player, OrganState state, double warBlood) {
        int step = (int) Mth.floor(warBlood / 10.0);
        int previous = state.getInt(KEY_LAST_STEP, 0);
        if (step == previous) {
            return;
        }
        state.setInt(KEY_LAST_STEP, step, v -> v, 0);

        double attackBonus = step * 0.03; // 3% per 10 战血
        double speedBonus = step * 0.02;  // 2% per 10 战血

        AttributeInstance attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance speedAttr = player.getAttribute(Attributes.ATTACK_SPEED);

        if (attackBonus != 0.0) {
            AttributeOps.replaceTransient(attackAttr, WAR_BLOOD_ATTACK_ID,
                    new AttributeModifier(WAR_BLOOD_ATTACK_ID, attackBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else {
            AttributeOps.removeById(attackAttr, WAR_BLOOD_ATTACK_ID);
        }

        if (speedBonus != 0.0) {
            AttributeOps.replaceTransient(speedAttr, WAR_BLOOD_SPEED_ID,
                    new AttributeModifier(WAR_BLOOD_SPEED_ID, speedBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else {
            AttributeOps.removeById(speedAttr, WAR_BLOOD_SPEED_ID);
        }
    }

    private static void triggerBloodHunger(Player player, OrganState state, long now) {
        RandomSource random = player.getRandom();
        float loss = Mth.randomBetween(random, NEGATIVE_HEALTH_LOSS_MIN, NEGATIVE_HEALTH_LOSS_MAX);
        player.hurt(player.damageSources().magic(), loss);
        if (random.nextDouble() < NEGATIVE_TELEPORT_CHANCE) {
            attemptRandomTeleport(player, random);
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        }
        state.setLong(KEY_NEXT_HUNGER_TICK, now + NEGATIVE_EVENT_INTERVAL_TICKS, v -> Math.max(0L, v), 0L);
    }

    private static void attemptRandomTeleport(Player player, RandomSource random) {
        double radius = 1.0 + random.nextDouble() * 4.0;
        double angle = random.nextDouble() * Math.PI * 2.0;
        double dx = Math.cos(angle) * radius;
        double dz = Math.sin(angle) * radius;
        double targetX = player.getX() + dx;
        double targetZ = player.getZ() + dz;
        double targetY = player.getY() + 0.5;
        player.randomTeleport(targetX, targetY, targetZ, true);
    }

    private static void spawnPassiveParticles(ServerLevel level, LivingEntity entity, double warBlood) {
        RandomSource random = level.random;
        double baseX = entity.getX();
        double baseY = entity.getY();
        double baseZ = entity.getZ();
        double height = entity.getBbHeight();
        double width = Math.max(0.6D, entity.getBbWidth());

        int count = Math.max(4, (int) (Math.abs(warBlood) / 10.0) + 2);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = width * 0.35D + random.nextDouble() * width * 0.25D;
            double x = baseX + Math.cos(angle) * radius;
            double z = baseZ + Math.sin(angle) * radius;
            double y = baseY + random.nextDouble() * height;
            double dy = 0.05 + random.nextDouble() * 0.05;
            level.sendParticles(PASSIVE_TRAIL, x, y, z, 0, 0.0, dy, 0.0, 0.0);
        }

        if (warBlood >= MAX_WAR_BLOOD - 0.1) {
            double heartY = baseY + height * 0.6D;
            level.sendParticles(HEART_BEAT, baseX, heartY, baseZ, 6, width * 0.1D, 0.05D, width * 0.1D, 0.01D);
        }
    }

    private static void spawnImpactParticles(ServerLevel level, Vec3 pos) {
        for (int i = 0; i < 12; i++) {
            double angle = (level.random.nextDouble() * Math.PI * 2.0D);
            double speed = 0.2D + level.random.nextDouble() * 0.2D;
            double vx = Math.cos(angle) * speed;
            double vz = Math.sin(angle) * speed;
            level.sendParticles(PASSIVE_TRAIL, pos.x, pos.y + 1.0D, pos.z, 1, vx, 0.1D, vz, 0.0D);
        }
    }

    private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (cc == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }

        OrganState state = OrganState.of(organ, STATE_ROOT);
        long now = player.level().getGameTime();
        long readyTick = state.getLong(KEY_OATH_READY, 0L);
        if (now < readyTick) {
            return;
        }

        double currentHealth = player.getHealth();
        float healthCost = (float) (currentHealth * OATH_HEALTH_COST_RATIO);
        if (healthCost >= currentHealth - 1.0f) {
            return;
        }

        if (ResourceOps.tryConsumeScaledZhenyuan(player, OATH_ZHENYUAN_COST).isEmpty()) {
            return;
        }

        player.hurt(player.damageSources().magic(), healthCost);

        state.setDouble(KEY_WAR_BLOOD, MAX_WAR_BLOOD, value -> Mth.clamp(value, MIN_WAR_BLOOD, MAX_WAR_BLOOD), MAX_WAR_BLOOD);
        state.setBoolean(KEY_OATH_ACTIVE, true);
        state.setLong(KEY_OATH_EXPIRE, now + BLOOD_OATH_DURATION_TICKS, v -> Math.max(0L, v), 0L);
        state.setLong(KEY_OATH_READY, now + BLOOD_OATH_COOLDOWN_TICKS, v -> Math.max(0L, v), 0L);
        state.setLong(KEY_LAST_ACTIVITY_TICK, now, v -> Math.max(0L, v), 0L);
        updateBloodOathModifiers(player, true);
        updateWarBloodAttributes(player, state, MAX_WAR_BLOOD);
        INSTANCE.sendSlotUpdate(cc, organ);

        if (player instanceof ServerPlayer sp) {
            ActiveSkillRegistry.scheduleReadyToast(sp, ABILITY_ID, state.getLong(KEY_OATH_READY, now), now);
        }

        if (player.level() instanceof ServerLevel server) {
            performBloodOathBurst(server, player, MAX_WAR_BLOOD);
        }
    }

    private static void performBloodOathBurst(ServerLevel level, Player player, double warBlood) {
        double damageAmount = OATH_BASE_DAMAGE + warBlood * OATH_DAMAGE_PER_WAR_BLOOD;
        AABB area = player.getBoundingBox().inflate(OATH_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive() && !entity.isAlliedTo(player));
        double healPerTarget = player.getMaxHealth() * OATH_HEAL_RATIO_PER_TARGET;
        DamageSource source = player.damageSources().playerAttack(player);

        for (LivingEntity target : targets) {
            target.hurt(source, (float) damageAmount);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            player.heal((float) healPerTarget);
            spawnImpactParticles(level, target.position());
        }

        spawnOathRing(level, player);
        BuiltInRegistries.SOUND_EVENT.getOptional(HEART_BEAT_SOUND).ifPresent(sound ->
                level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.75f, 1.0f));
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.1f, 0.6f);
    }

    private static void spawnOathRing(ServerLevel level, Player player) {
        Vec3 origin = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        for (int i = 0; i < 36; i++) {
            double angle = (Math.PI * 2.0D) * i / 36.0D;
            double radius = 0.5D + i * 0.05D;
            double x = origin.x + Math.cos(angle) * radius;
            double z = origin.z + Math.sin(angle) * radius;
            double y = origin.y + 0.1D * Math.sin(i * 0.3D);
            level.sendParticles(OATH_RING, x, y, z, 0, 0.0D, 0.02D, 0.0D, 0.0D);
        }
    }

    private static ItemStack findOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (ORGAN_ID.equals(id)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
