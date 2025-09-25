package net.tigereye.chestcavity.compat.guzhenren.item.lei_dao.behavior;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.compat.guzhenren.linkage.IncreaseEffectLedger;
import net.tigereye.chestcavity.compat.guzhenren.linkage.IncreaseEffectContributor;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NBTCharge;
import net.tigereye.chestcavity.util.NetworkUtil;

import java.util.List;

/**
 * Behaviour implementation for 电流蛊 (DianLiugu).
 */
public enum DianLiuguOrganBehavior implements OrganSlowTickListener, OrganOnHitListener, IncreaseEffectContributor {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation LEI_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/lei_dao_increase_effect");

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final String STATE_KEY = "DianLiugu";
    private static final int MAX_CHARGE = 10;

    private static final double BASE_DAMAGE = 5.0;
    private static final double BASE_DEBUFF_SECONDS = 3.0;
    private static final double AOE_RADIUS = 3.0;
    private static final double AOE_DAMAGE_RATIO = 0.6;

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        if (cc == null) {
            return;
        }
        if (entity instanceof Player) {
            handlePlayerSlowTick(cc, organ);
            return;
        }
        handleNonPlayerSlowTick(entity, cc, organ);
    }

    private void handlePlayerSlowTick(ChestCavityInstance cc, ItemStack organ) {
        int currentCharge = Math.min(MAX_CHARGE, NBTCharge.getCharge(organ, STATE_KEY));
        if (currentCharge >= MAX_CHARGE) {
            return;
        }

        double efficiency = 1.0 + ensureChannel(cc, LEI_DAO_INCREASE_EFFECT).get();
        int gained = Math.max(1, (int) Math.floor(efficiency));
        int updatedCharge = Math.min(MAX_CHARGE, currentCharge + gained);
        if (updatedCharge != currentCharge) {
            NBTCharge.setCharge(organ, STATE_KEY, updatedCharge);
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }
    }

    private void handleNonPlayerSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (organ == null || organ.isEmpty()) {
            return;
        }
        int currentCharge = Math.min(MAX_CHARGE, NBTCharge.getCharge(organ, STATE_KEY));
        if (currentCharge >= MAX_CHARGE) {
            return;
        }

        double efficiency = 1.0 + ensureChannel(cc, LEI_DAO_INCREASE_EFFECT).get();
        int gained = Math.max(1, (int) Math.floor(efficiency));
        int updatedCharge = Math.min(MAX_CHARGE, currentCharge + gained);
        if (updatedCharge != currentCharge) {
            NBTCharge.setCharge(organ, STATE_KEY, updatedCharge);
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }
    }

    @Override
    public float onHit(
            DamageSource source,
            LivingEntity attacker,
            LivingEntity target,
            ChestCavityInstance cc,
            ItemStack organ,
            float damage
    ) {
        if (attacker == null || attacker.level().isClientSide()) {
            return damage;
        }
        if (target == null || cc == null) {
            return damage;
        }

        if (attacker instanceof Player player) {
            return handlePlayerAttack(player, source, target, cc, organ, damage);
        }
        return handleNonPlayerAttack(attacker, source, target, cc, organ, damage);
    }

    private float handlePlayerAttack(
            Player player,
            DamageSource source,
            LivingEntity target,
            ChestCavityInstance cc,
            ItemStack organ,
            float damage
    ) {
        int charge = Math.min(MAX_CHARGE, NBTCharge.getCharge(organ, STATE_KEY));
        if (charge <= 0) {
            return damage;
        }

        double efficiency = 1.0 + ensureChannel(cc, LEI_DAO_INCREASE_EFFECT).get();
        int updated = Math.max(0, charge - 1);
        if (updated != charge) {
            NBTCharge.setCharge(organ, STATE_KEY, updated);
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }

        float bonusDamage = (float)(BASE_DAMAGE * efficiency);

        applyDebuff(target, efficiency);
        playActivationEffects(player.level(), target);
        arcAdditionalTargets(player, target, source, bonusDamage * (float)AOE_DAMAGE_RATIO);

        return damage + bonusDamage;
    }

    private float handleNonPlayerAttack(
            LivingEntity attacker,
            DamageSource source,
            LivingEntity target,
            ChestCavityInstance cc,
            ItemStack organ,
            float damage
    ) {
        if (organ == null || organ.isEmpty()) {
            return damage;
        }

        int charge = Math.min(MAX_CHARGE, NBTCharge.getCharge(organ, STATE_KEY));
        if (charge <= 0) {
            return damage;
        }

        double efficiency = 1.0 + ensureChannel(cc, LEI_DAO_INCREASE_EFFECT).get();
        int updated = Math.max(0, charge - 1);
        if (updated != charge) {
            NBTCharge.setCharge(organ, STATE_KEY, updated);
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }

        float bonusDamage = (float) (BASE_DAMAGE * efficiency);
        applyDebuff(target, efficiency);
        playActivationEffects(attacker.level(), target);
        arcAdditionalTargets(attacker, target, source, bonusDamage * (float) AOE_DAMAGE_RATIO);

        return damage + bonusDamage;
    }

    /** Ensures linkage channels exist for the owning chest cavity. */
    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ensureChannel(cc, LEI_DAO_INCREASE_EFFECT);
    }

    private static LinkageChannel ensureChannel(ChestCavityInstance cc, ResourceLocation id) {
        ActiveLinkageContext context = GuzhenrenLinkageManager.getContext(cc);
        return context.getOrCreateChannel(id).addPolicy(NON_NEGATIVE);
    }

    private static void applyDebuff(LivingEntity target, double efficiency) {
        if (target == null) {
            return;
        }
        double baseTicks = BASE_DEBUFF_SECONDS * 20.0;
        double scaled = baseTicks * efficiency / (1.0 + Math.max(0.0, target.getMaxHealth()));
        int duration = Math.max(1, (int)Math.round(scaled));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 10, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, true, true));
    }

    private static void playActivationEffects(Level level, LivingEntity target) {
        if (level == null || target == null) {
            return;
        }
        double x = target.getX();
        double y = target.getY(0.5);
        double z = target.getZ();
        RandomSource random = level.getRandom();
        float pitch = 0.8f + random.nextFloat() * 0.2f;
        level.playSound(null, x, y, z, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.PLAYERS, 0.8f, pitch);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 20, 0.35, 0.4, 0.35, 0.15);
        }
    }

    @Override
    public void rebuildIncreaseEffects(
            ChestCavityInstance cc,
            ActiveLinkageContext context,
            ItemStack organ,
            IncreaseEffectLedger.Registrar registrar
    ) {
        // DianLiugu does not contribute to INCREASE effects.
    }

    private static void arcAdditionalTargets(
            LivingEntity attacker,
            LivingEntity primary,
            DamageSource source,
            float aoeDamage
    ) {
        if (attacker == null || primary == null) {
            return;
        }
        if (aoeDamage <= 0.0f) {
            return;
        }
        Level level = attacker.level();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        AABB area = primary.getBoundingBox().inflate(AOE_RADIUS);
        List<LivingEntity> victims = server.getEntitiesOfClass(LivingEntity.class, area, entity ->
                entity != null && entity.isAlive() && entity != primary && entity != attacker);
        if (victims.isEmpty()) {
            return;
        }
        DamageSource chainSource = source != null
                ? source
                : attacker.damageSources().indirectMagic(attacker, attacker);
        for (LivingEntity victim : victims) {
            victim.hurt(chainSource, aoeDamage);
            server.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    victim.getX(),
                    victim.getY(0.5),
                    victim.getZ(),
                    8,
                    0.2,
                    0.25,
                    0.2,
                    0.1
            );
        }
    }
}
