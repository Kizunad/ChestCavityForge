package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.compat.guzhenren.linkage.IncreaseEffectContributor;
import net.tigereye.chestcavity.compat.guzhenren.linkage.IncreaseEffectLedger;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NBTCharge;
import net.tigereye.chestcavity.util.NetworkUtil;

import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

/**
 * 虎骨蛊：低蓄能时提供额外回复但施加虚弱，受击时为使用者提供防御增益并反弹伤害。
 */
public enum HuGuguOrganBehavior implements OrganSlowTickListener, OrganIncomingDamageListener, IncreaseEffectContributor {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation GU_DAO_INCREASE_EFFECT =
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/gu_dao_increase_effect");
    private static final ResourceLocation LI_DAO_INCREASE_EFFECT =
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/li_dao_increase_effect");
    private static final ResourceLocation BIAN_HUA_DAO_INCREASE_EFFECT =
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/bian_hua_dao_increase_effect");

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final String STATE_KEY = "HuGuguCharge";
    private static final int CHARGE_SCALE = 100; // store hundredths in NBT
    private static final int MAX_CHARGE_UNITS = 20 * CHARGE_SCALE;
    private static final int HALF_CHARGE_UNITS = MAX_CHARGE_UNITS / 2;
    private static final int BASE_RECOVERY_LOW_UNITS = (int)Math.round(0.25 * CHARGE_SCALE);
    private static final int BONUS_RECOVERY_LOW_UNITS = (int)Math.round(0.25 * CHARGE_SCALE);
    private static final int RECOVERY_HIGH_UNITS = (int)Math.round(0.10 * CHARGE_SCALE);


    private static final double MIN_DAMAGE_THRESHOLD = 10.0;

    private static final double BASE_REFLECT_RATIO = 0.5;
    private static final double BASE_MAX_REFLECT = 50.0;

    private static final double BASE_JINGLI_COST = 10.0;

    private static final double RESOURCE_EPSILON = 1.0E-4;
    private static final double BASE_ZHENYUAN_COST = 500.0;

    private static final int LOW_CHARGE_DEBUFF_DURATION = 60; // 3 seconds refresh per slow tick

    private static final int BASE_BUFF_DURATION = 20 * 60; // 1 minute in ticks

    private static final int DAMAGE_TRIGGER_COST_UNITS = CHARGE_SCALE;

    private static final double HU_GUGU_CONE_HALF_ANGLE_RADIANS = Math.toRadians(20.0);
    private static final double HU_GUGU_CONE_MIN_DISTANCE = 0.6;
    private static final double HU_GUGU_CONE_MAX_DISTANCE = 6.0;
    private static final int HU_GUGU_CONE_STEPS = 12;
    private static final DustParticleOptions HU_GUGU_CORE_DUST =
        new DustParticleOptions(new Vector3f(1.0f, 0.5f, 0.0f), 1.35f);
    private static final DustParticleOptions HU_GUGU_EDGE_DUST_YELLOW =
        new DustParticleOptions(new Vector3f(1.0f, 0.8235f, 0.4f), 1.15f);
    private static final DustParticleOptions HU_GUGU_EDGE_DUST_RED =
        new DustParticleOptions(new Vector3f(1.0f, 0.25f, 0.0f), 1.2f);
  
    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }

        int currentUnits = getStoredUnits(organ);
        if (currentUnits >= MAX_CHARGE_UNITS) {
            return;
        }

        int addedUnits = 0;
        if (currentUnits < HALF_CHARGE_UNITS) {
            if (entity instanceof Player player) {
                applyLowChargeDebuffs(player);
                addedUnits += BASE_RECOVERY_LOW_UNITS;
                if (tryConsumeLowChargeResources(player)) {
                    addedUnits += BONUS_RECOVERY_LOW_UNITS;
                }
            } else {
                // 非玩家不施加减益，也不支付资源，只进行基础恢复
                addedUnits += BASE_RECOVERY_LOW_UNITS;
            }
        } else {
            addedUnits += RECOVERY_HIGH_UNITS;
        }

        if (addedUnits <= 0) {
            return;
        }

        int updatedUnits = Math.min(MAX_CHARGE_UNITS, currentUnits + addedUnits);
        if (updatedUnits != currentUnits) {
            setStoredUnits(organ, updatedUnits);
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }
    }

    private static void applyLowChargeDebuffs(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, LOW_CHARGE_DEBUFF_DURATION, 0, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, LOW_CHARGE_DEBUFF_DURATION, 0, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, LOW_CHARGE_DEBUFF_DURATION, 0, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, LOW_CHARGE_DEBUFF_DURATION, 0, true, true));
    }

    private static boolean tryConsumeLowChargeResources(Player player) {
        var handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return false;
        }

        var handle = handleOpt.get();

        var jingliBeforeOpt = handle.getJingli();
        if (jingliBeforeOpt.isEmpty()) {
            return false;
        }

        double jingliBefore = jingliBeforeOpt.getAsDouble();
        if (!Double.isFinite(jingliBefore) || jingliBefore + RESOURCE_EPSILON < BASE_JINGLI_COST) {
            return false;
        }

        var jingliAfterOpt = handle.adjustJingli(-BASE_JINGLI_COST, true);
        if (jingliAfterOpt.isEmpty()) {
            return false;
        }

        var zhenyuanResult = handle.consumeScaledZhenyuan(BASE_ZHENYUAN_COST);
        if (zhenyuanResult.isPresent()) {
            return true;
        }

        handle.adjustJingli(BASE_JINGLI_COST, true);
        return false;
    }

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!(victim instanceof Player player) || victim.level().isClientSide()) {
            return damage;
        }
        if (damage < MIN_DAMAGE_THRESHOLD) {
            return damage;
        }


        if (!consumeChargeUnits(cc, organ, DAMAGE_TRIGGER_COST_UNITS)) {
            return damage;
        }

        double increaseTotal = getIncreaseTotal(cc);
        double multiplier = 1.0 + increaseTotal;
        int duration = Math.max(20, (int)Math.round(BASE_BUFF_DURATION * multiplier));

        applyAbsorption(player, multiplier, duration);
        applyResistance(player, increaseTotal, duration);
        applySpeed(player, multiplier, duration);
        applyJump(player, multiplier, duration);

        playHuGuguRetaliationCue(player);

        reflectDamage(source, victim, damage, multiplier);
        return damage;
    }

    public void ensureAttached(ChestCavityInstance cc) {
        ensureChannel(cc, GU_DAO_INCREASE_EFFECT);
        ensureChannel(cc, LI_DAO_INCREASE_EFFECT);
        ensureChannel(cc, BIAN_HUA_DAO_INCREASE_EFFECT);
    }

    private static void applyAbsorption(Player player, double multiplier, int duration) {
        int absorptionPoints = Math.max(4, (int)Math.round(20.0 * multiplier));
        int absorptionLevel = Math.max(1, (int)Math.round(absorptionPoints / 4.0));
        int amplifier = Math.max(0, absorptionLevel - 1);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, amplifier, true, true));
        float expected = 4.0f * (amplifier + 1);
        if (player.getAbsorptionAmount() < expected) {
            player.setAbsorptionAmount(expected);
        }
    }

    private static void applyResistance(Player player, double increaseTotal, int duration) {
        int amplifier = Math.max(0, (int)Math.round(increaseTotal));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, amplifier, true, true));
    }

    private static void applySpeed(Player player, double multiplier, int duration) {
        int level = Math.max(1, (int)Math.round(multiplier));
        int amplifier = Math.max(0, level - 1);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, amplifier, true, true));
    }

    private static void applyJump(Player player, double multiplier, int duration) {
        int level = Math.max(1, (int)Math.round(multiplier));
        int amplifier = Math.max(0, level - 1);
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, duration, amplifier, true, true));
    }

    private static void reflectDamage(DamageSource source, LivingEntity victim, float incomingDamage, double multiplier) {
        LivingEntity attacker = null;
        Entity attackerEntity = source.getEntity();
        if (attackerEntity instanceof LivingEntity living) {
            attacker = living;
        } else {
            Entity direct = source.getDirectEntity();
            if (direct instanceof LivingEntity livingDirect) {
                attacker = livingDirect;
            }
        }
        if (attacker == null) {
            return;
        }

        double reflectAmount = Math.min(BASE_MAX_REFLECT * multiplier, BASE_REFLECT_RATIO * incomingDamage * multiplier);
        if (reflectAmount <= 0) {
            return;
        }

        knockbackAttacker(victim, attacker, multiplier);
        attacker.hurt(victim.damageSources().thorns(victim), (float)reflectAmount);
    }

    private static void knockbackAttacker(LivingEntity victim, LivingEntity attacker, double multiplier) {
        Vec3 direction = attacker.position().subtract(victim.position());
        Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
        if (horizontal.lengthSqr() < 1.0E-4) {
            return;
        }
        Vec3 normalised = horizontal.normalize();
        double strength = 0.6 * multiplier;
        attacker.knockback(strength, -normalised.x, -normalised.z);
    }

    private static void playRetaliationCue(Level level, LivingEntity victim) {
        level.playSound(
            null,
            victim.getX(),
            victim.getY(),
            victim.getZ(),
            SoundEvents.POLAR_BEAR_HURT,
            SoundSource.PLAYERS,
            1.2f,
            0.9f + level.getRandom().nextFloat() * 0.2f
        );
        if (level instanceof ServerLevel serverLevel) {
            spawnHuGuguConeBurst(serverLevel, victim);
        }
    }

    private static void playHuGuguRetaliationCue(Player player) {
        playRetaliationCue(player.level(), player);
    }

    private static void spawnHuGuguConeBurst(ServerLevel serverLevel, LivingEntity victim) {
        Vec3 forward = victim.getViewVector(1.0F).normalize();
        if (forward.lengthSqr() < 1.0E-4) {
            forward = new Vec3(0.0, 0.0, 1.0);
        }

        Vec3 upReference = Math.abs(forward.y) > 0.95 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 right = forward.cross(upReference);
        if (right.lengthSqr() < 1.0E-4) {
            right = forward.cross(new Vec3(0.0, 0.0, 1.0));
        }
        right = right.normalize();
        Vec3 up = right.cross(forward).normalize();

        Vec3 mouthPosition = victim.getEyePosition().add(0.0, -0.2, 0.0);
        double distanceStep = (HU_GUGU_CONE_MAX_DISTANCE - HU_GUGU_CONE_MIN_DISTANCE) / HU_GUGU_CONE_STEPS;
        RandomSource random = serverLevel.getRandom();

        for (int step = 0; step <= HU_GUGU_CONE_STEPS; step++) {
            double distance = HU_GUGU_CONE_MIN_DISTANCE + step * distanceStep;
            double radius = Math.tan(HU_GUGU_CONE_HALF_ANGLE_RADIANS) * distance;
            int particles = Math.max(4, 6 + step * 2);

            for (int i = 0; i < particles; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double radial = radius * Math.sqrt(random.nextDouble());
                double offsetRight = Math.cos(angle) * radial;
                double offsetUp = Math.sin(angle) * radial;

                Vec3 radialOffset = right.scale(offsetRight).add(up.scale(offsetUp));
                Vec3 particlePos = mouthPosition.add(forward.scale(distance)).add(radialOffset);

                double forwardSpeed = 0.02 + random.nextDouble() * 0.04;
                Vec3 velocity = forward.scale(forwardSpeed)
                    .add(radialOffset.normalize().scale(0.01 + random.nextDouble() * 0.02));

                serverLevel.sendParticles(
                    selectHuGuguDust(random),
                    particlePos.x,
                    particlePos.y,
                    particlePos.z,
                    1,
                    velocity.x,
                    velocity.y,
                    velocity.z,
                    0.0
                );

                if (random.nextFloat() < 0.35f) {
                    serverLevel.sendParticles(
                        ParticleTypes.FLAME,
                        particlePos.x,
                        particlePos.y,
                        particlePos.z,
                        1,
                        velocity.x * 0.5,
                        velocity.y * 0.5,
                        velocity.z * 0.5,
                        0.0
                    );
                }
            }
        }
    }

    private static DustParticleOptions selectHuGuguDust(RandomSource random) {
        float pick = random.nextFloat();
        if (pick < 0.15f) {
            return HU_GUGU_EDGE_DUST_RED;
        }
        if (pick < 0.45f) {
            return HU_GUGU_EDGE_DUST_YELLOW;
        }
        return HU_GUGU_CORE_DUST;
    }

    private static double getIncreaseTotal(ChestCavityInstance cc) {
        return ensureChannel(cc, GU_DAO_INCREASE_EFFECT).get()
            + ensureChannel(cc, LI_DAO_INCREASE_EFFECT).get()
            + ensureChannel(cc, BIAN_HUA_DAO_INCREASE_EFFECT).get();
    }

    private static LinkageChannel ensureChannel(ChestCavityInstance cc, ResourceLocation id) {
        ActiveLinkageContext context = GuzhenrenLinkageManager.getContext(cc);
        return context.getOrCreateChannel(id).addPolicy(NON_NEGATIVE);
    }

    private static int getStoredUnits(ItemStack stack) {
        int stored = Math.max(0, NBTCharge.getCharge(stack, STATE_KEY));
        if (stored > MAX_CHARGE_UNITS) {
            return MAX_CHARGE_UNITS;
        }
        return stored;
    }

    private static void setStoredUnits(ItemStack stack, int units) {
        int clamped = Math.max(0, Math.min(MAX_CHARGE_UNITS, units));
        NBTCharge.setCharge(stack, STATE_KEY, clamped);
    }

    private static boolean consumeChargeUnits(ChestCavityInstance cc, ItemStack stack, int costUnits) {
        if (costUnits <= 0) {
            return true;
        }

        int current = getStoredUnits(stack);
        if (current < costUnits) {
            return false;
        }

        int updated = current - costUnits;
        if (updated == current) {
            return true;
        }

        setStoredUnits(stack, updated);
        NetworkUtil.sendOrganSlotUpdate(cc, stack);
        return true;
    }

    @Override
    public void rebuildIncreaseEffects(
            ChestCavityInstance cc,
            ActiveLinkageContext context,
            ItemStack organ,
            IncreaseEffectLedger.Registrar registrar
    ) {
        // HuGugu does not contribute to INCREASE effects.
    }

}
