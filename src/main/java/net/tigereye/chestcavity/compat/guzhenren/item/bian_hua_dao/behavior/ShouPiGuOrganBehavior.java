package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.shou_pi_gu.tuning.ShouPiGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

/**
 * @deprecated Use {@link net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.shou_pi_gu.behavior.ShouPiGuPassive} instead.
 */
@Deprecated
public final class ShouPiGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganIncomingDamageListener, OrganSlowTickListener, OrganOnHitListener {

    public static final ShouPiGuOrganBehavior INSTANCE = new ShouPiGuOrganBehavior();

    public static final ResourceLocation ORGAN_ID = ShouPiGuTuning.ORGAN_ID;
    public static final ResourceLocation HUPI_GU_ID = ShouPiGuTuning.HUPI_GU_ID;
    public static final ResourceLocation TIE_GU_GU_ID = ShouPiGuTuning.TIE_GU_GU_ID;

    public static final double THICK_SKIN_REDUCTION = ShouPiGuTuning.THICK_SKIN_REDUCTION;
    public static final int THICK_SKIN_WINDOW_TICKS = ShouPiGuTuning.THICK_SKIN_WINDOW_TICKS;
    public static final int FASCIA_TRIGGER = ShouPiGuTuning.FASCIA_TRIGGER;
    public static final long FASCIA_COOLDOWN_TICKS = ShouPiGuTuning.FASCIA_COOLDOWN_TICKS;
    public static final double FASCIA_ACTIVE_REDUCTION = ShouPiGuTuning.FASCIA_ACTIVE_REDUCTION;
    public static final int STOIC_MAX_STACKS = ShouPiGuTuning.STOIC_MAX_STACKS;
    public static final long STOIC_DEFAULT_LOCK_TICKS = ShouPiGuTuning.STOIC_DEFAULT_LOCK_TICKS;
    public static final long SOFT_POOL_WINDOW_TICKS = ShouPiGuTuning.SOFT_POOL_WINDOW_TICKS;
    public static final long ROLL_DAMAGE_WINDOW_TICKS = ShouPiGuTuning.ROLL_DAMAGE_WINDOW_TICKS;
    public static final double ROLL_DAMAGE_REDUCTION = ShouPiGuTuning.ROLL_DAMAGE_REDUCTION;
    public static final double ROLL_DISTANCE = ShouPiGuTuning.ROLL_DISTANCE;
    public static final double CRASH_DISTANCE = ShouPiGuTuning.CRASH_DISTANCE;
    public static final long CRASH_IMMUNE_TICKS = ShouPiGuTuning.CRASH_IMMUNE_TICKS;
    public static final double CRASH_SPLASH_RADIUS = ShouPiGuTuning.CRASH_SPLASH_RADIUS;
    public static final double STOIC_SLOW_RADIUS = ShouPiGuTuning.STOIC_SLOW_RADIUS;
    public static final int STOIC_SLOW_TICKS = ShouPiGuTuning.STOIC_SLOW_TICKS;
    public static final int STOIC_SLOW_AMPLIFIER = ShouPiGuTuning.STOIC_SLOW_AMPLIFIER;
    public static final long SOFT_PROJECTILE_COOLDOWN_TICKS = ShouPiGuTuning.SOFT_PROJECTILE_COOLDOWN_TICKS;
    public static final double STOIC_ACTIVE_SOFT_BONUS = ShouPiGuTuning.STOIC_ACTIVE_SOFT_BONUS;
    public static final double SYNERGY_CRASH_BASE_COST = ShouPiGuTuning.SYNERGY_CRASH_BASE_COST;
    public static final long SYNERGY_CRASH_COOLDOWN_TICKS = ShouPiGuTuning.SYNERGY_CRASH_COOLDOWN_TICKS;

    public static final String KEY_STOIC_STACKS = ShouPiGuTuning.KEY_STOIC_STACKS;
    public static final String KEY_STOIC_ACCUM = ShouPiGuTuning.KEY_STOIC_ACCUM;
    public static final String KEY_STOIC_ACTIVE_UNTIL = ShouPiGuTuning.KEY_STOIC_ACTIVE_UNTIL;
    public static final String KEY_STOIC_LOCK_UNTIL = ShouPiGuTuning.KEY_STOIC_LOCK_UNTIL;
    public static final String KEY_SOFT_TEMP_BONUS = ShouPiGuTuning.KEY_SOFT_TEMP_BONUS;
    public static final String KEY_SOFT_TEMP_BONUS_EXPIRE = ShouPiGuTuning.KEY_SOFT_TEMP_BONUS_EXPIRE;
    public static final String KEY_FASCIA_COUNT = ShouPiGuTuning.KEY_FASCIA_COUNT;
    public static final String KEY_FASCIA_COOLDOWN = ShouPiGuTuning.KEY_FASCIA_COOLDOWN;
    public static final String KEY_FASCIA_ACTIVE_UNTIL = ShouPiGuTuning.KEY_FASCIA_ACTIVE_UNTIL;
    public static final String KEY_ACTIVE_DRUM_READY = ShouPiGuTuning.KEY_ACTIVE_DRUM_READY;
    public static final String KEY_ACTIVE_DRUM_EXPIRE = ShouPiGuTuning.KEY_ACTIVE_DRUM_EXPIRE;
    public static final String KEY_ROLL_READY = "RollReady";
    public static final String KEY_ROLL_EXPIRE = "RollExpire";
    public static final String KEY_CRASH_READY = "CrashReady";
    public static final String KEY_CRASH_IMMUNE = "CrashImmuneExpire";
    public static final String KEY_THICK_SKIN_READY = "ThickSkinReady";
    public static final String KEY_THICK_SKIN_EXPIRE = "ThickSkinExpire";
    public static final String KEY_SOFT_POOL_VALUE = "SoftReflectPool";
    public static final String KEY_SOFT_POOL_EXPIRE = "SoftReflectExpire";


    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    }

    @Override
    public float onIncomingDamage(
        DamageSource source,
        LivingEntity victim,
        ChestCavityInstance cc,
        ItemStack organ,
        float damage) {
        return damage;
    }

    @Override
    public float onHit(
        DamageSource source,
        LivingEntity attacker,
        LivingEntity target,
        ChestCavityInstance cc,
        ItemStack organ,
        float damage) {
        return damage;
    }
}
