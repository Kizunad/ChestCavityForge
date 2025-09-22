package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior;

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
import net.tigereye.chestcavity.compat.guzhenren.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NBTCharge;
import net.tigereye.chestcavity.util.NetworkUtil;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * 虎骨蛊：低蓄能时提供额外回复但施加虚弱，受击时为使用者提供防御增益并反弹伤害。
 */
public enum HuGuguOrganBehavior implements OrganSlowTickListener, OrganIncomingDamageListener {
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
    private static final double BASE_ZHENYUAN_COST = 500.0;

    private static final int LOW_CHARGE_DEBUFF_DURATION = 60; // 3 seconds refresh per slow tick

    private static final int BASE_BUFF_DURATION = 20 * 60; // 1 minute in ticks

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }

        int currentUnits = getStoredUnits(organ);
        if (currentUnits >= MAX_CHARGE_UNITS) {
            return;
        }

        int addedUnits = 0;
        if (currentUnits < HALF_CHARGE_UNITS) {
            applyLowChargeDebuffs(player);
            addedUnits += BASE_RECOVERY_LOW_UNITS;

            Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
            if (handleOpt.isPresent()) {
                GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
                OptionalDouble jingliResult = handle.adjustJingli(-BASE_JINGLI_COST, true);
                if (jingliResult.isPresent()) {
                    OptionalDouble zhenyuanResult = handle.consumeScaledZhenyuan(BASE_ZHENYUAN_COST);
                    if (zhenyuanResult.isPresent()) {
                        addedUnits += BONUS_RECOVERY_LOW_UNITS;
                    } else {
                        handle.adjustJingli(BASE_JINGLI_COST, true);
                    }
                }
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

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!(victim instanceof Player player) || victim.level().isClientSide()) {
            return damage;
        }
        if (damage < MIN_DAMAGE_THRESHOLD) {
            return damage;
        }

        double increaseTotal = getIncreaseTotal(cc);
        double multiplier = 1.0 + increaseTotal;
        int duration = Math.max(20, (int)Math.round(BASE_BUFF_DURATION * multiplier));

        applyAbsorption(player, multiplier, duration);
        applyResistance(player, increaseTotal, duration);
        applySpeed(player, multiplier, duration);
        applyJump(player, multiplier, duration);

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
        playRetaliationCue(victim.level(), victim);
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
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.SKELETON_HURT, SoundSource.PLAYERS, 0.7f, 0.9f);
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.0f);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(), 12, 0.2, 0.2, 0.2, 0.05);
        }
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
}
