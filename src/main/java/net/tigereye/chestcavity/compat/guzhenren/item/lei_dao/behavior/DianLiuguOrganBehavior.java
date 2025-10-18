package net.tigereye.chestcavity.compat.guzhenren.item.lei_dao.behavior;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;


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
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.IncreaseEffectContributor;
import net.tigereye.chestcavity.linkage.IncreaseEffectLedger;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;

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

    private static final String STATE_ROOT = "DianLiugu";
    private static final String CHARGE_KEY = "Charge";
    private static final int MAX_CHARGE = BehaviorConfigAccess.getInt(DianLiuguOrganBehavior.class, "MAX_CHARGE", 10);

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
        MultiCooldown cooldown = createCooldown(cc, organ);
        MultiCooldown.EntryInt chargeEntry = cooldown.entryInt(CHARGE_KEY);
        int currentCharge = Math.min(MAX_CHARGE, chargeEntry.getTicks());
        if (currentCharge >= MAX_CHARGE) {
            return;
        }

        double efficiency = 1.0 + lookupIncreaseEffect(cc);
        int gained = Math.max(1, (int) Math.floor(efficiency));
        int updatedCharge = Math.min(MAX_CHARGE, currentCharge + gained);
        chargeEntry.setTicks(updatedCharge);
    }

    private void handleNonPlayerSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (organ == null || organ.isEmpty()) {
            return;
        }
        MultiCooldown cooldown = createCooldown(cc, organ);
        MultiCooldown.EntryInt chargeEntry = cooldown.entryInt(CHARGE_KEY);
        int currentCharge = Math.min(MAX_CHARGE, chargeEntry.getTicks());
        if (currentCharge >= MAX_CHARGE) {
            return;
        }

        double efficiency = 1.0 + lookupIncreaseEffect(cc);
        int gained = Math.max(1, (int) Math.floor(efficiency));
        int updatedCharge = Math.min(MAX_CHARGE, currentCharge + gained);
        chargeEntry.setTicks(updatedCharge);
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
        MultiCooldown cooldown = createCooldown(cc, organ);
        MultiCooldown.EntryInt chargeEntry = cooldown.entryInt(CHARGE_KEY);
        int charge = Math.min(MAX_CHARGE, chargeEntry.getTicks());
        if (charge <= 0) {
            return damage;
        }

        double efficiency = 1.0 + lookupIncreaseEffect(cc);
        int updated = Math.max(0, charge - 1);
        chargeEntry.setTicks(updated);

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

        MultiCooldown cooldown = createCooldown(cc, organ);
        MultiCooldown.EntryInt chargeEntry = cooldown.entryInt(CHARGE_KEY);
        int charge = Math.min(MAX_CHARGE, chargeEntry.getTicks());
        if (charge <= 0) {
            return damage;
        }

        double efficiency = 1.0 + lookupIncreaseEffect(cc);
        int updated = Math.max(0, charge - 1);
        chargeEntry.setTicks(updated);

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
        if (cc == null) {
            return null;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        return LedgerOps.ensureChannel(context, id, NON_NEGATIVE);
    }

    private static double lookupIncreaseEffect(ChestCavityInstance cc) {
        LinkageChannel channel = ensureChannel(cc, LEI_DAO_INCREASE_EFFECT);
        return channel == null ? 0.0 : channel.get();
    }

    private static MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ) {
        MultiCooldown.Builder builder = MultiCooldown.builder(organ, STATE_ROOT)
                .withIntClamp(value -> Math.max(0, Math.min(MAX_CHARGE, value)), 0);
        if (cc != null && organ != null && !organ.isEmpty()) {
            builder.withSync(cc, organ);
        } else if (organ != null) {
            builder.withOrgan(organ);
        }
        return builder.build();
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
        int tagDuration = Math.max(duration, 40);
        ReactionTagOps.add(target, ReactionTagKeys.LIGHTNING_CHARGE, tagDuration);
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
            int tagDuration = Math.max(60, (int) Math.round(BASE_DEBUFF_SECONDS * 20.0));
            ReactionTagOps.add(victim, ReactionTagKeys.LIGHTNING_CHARGE, tagDuration);
        }
    }
}
