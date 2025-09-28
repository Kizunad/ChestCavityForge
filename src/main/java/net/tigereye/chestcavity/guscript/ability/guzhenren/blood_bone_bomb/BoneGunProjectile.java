package net.tigereye.chestcavity.guscript.ability.guzhenren.blood_bone_bomb;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.registration.CCEntities;
import net.tigereye.chestcavity.registration.CCItems;

import org.joml.Vector3f;

import java.util.Optional;

/**
 * Server-authoritative projectile that renders as the Guzhenren bone spear item.
 */
public class BoneGunProjectile extends ThrowableItemProjectile {

    private static final int LIFETIME_TICKS = 60;
    private static final float TRAIL_RADIUS = 0.25f;
    private static final DustParticleOptions TRAIL_PARTICLE =
            new DustParticleOptions(new Vector3f(0.78f, 0.04f, 0.08f), 1.2f);

    private double effectMultiplier = 1.0;
    private float impactDamage = 10.0f;

    public BoneGunProjectile(EntityType<? extends BoneGunProjectile> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public BoneGunProjectile(Level level, LivingEntity shooter, ItemStack stack) {
        super(CCEntities.BONE_GUN_PROJECTILE.get(), shooter, level);
        this.setItem(stack.copy());
        this.setNoGravity(true);
    }

    public void configurePayload(float damage, double multiplier) {
        this.impactDamage = damage;
        this.effectMultiplier = multiplier;
    }

    @Override
    protected Item getDefaultItem() {
        return CCItems.GUZHENREN_GU_QIANG;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            spawnTrailParticles();
        } else if (this.tickCount > LIFETIME_TICKS) {
            spawnFadeParticles();
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (this.level().isClientSide) {
            return;
        }

        LivingEntity victim = hit.getEntity() instanceof LivingEntity living ? living : null;
        Vec3 impact = hit.getLocation();
        playImpactEffects(impact);
        if (victim != null) {
            BloodBoneBombAbility.applyTrueDamage(this.getOwner() instanceof ServerPlayer serverPlayer ? serverPlayer : null, victim, impactDamage);
            applyStatusEffects(victim);
        }
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!this.level().isClientSide) {
            playImpactEffects(hit.getLocation());
            spawnFadeParticles();
            this.discard();
        }
    }

    private void spawnTrailParticles() {
        Vec3 position = this.position();

        int strands = 8; // 环绕条数翻倍
        double angleSpeed = 0.6; // 旋转速度翻倍
        int particlesPerStrand = 2; // 每条多喷几个点

        for (int i = 0; i < strands; i++) {
            double baseAngle = (this.tickCount * angleSpeed) + (i * (Math.PI * 2.0 / strands));

            for (int j = 0; j < particlesPerStrand; j++) {
                double offset = (j / (double) particlesPerStrand) * 0.5; // 稍微错开
                double angle = baseAngle + offset;

                double ox = Math.cos(angle) * TRAIL_RADIUS;
                double oz = Math.sin(angle) * TRAIL_RADIUS;

                this.level().addParticle(TRAIL_PARTICLE,
                        position.x + ox,
                        position.y,
                        position.z + oz,
                        0.0, 0.0, 0.0);
            }
        }
    }


    private void spawnFadeParticles() {
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        Vec3 pos = this.position();
        server.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 12, 0.2, 0.2, 0.2, 0.01);
    }

    private void playImpactEffects(Vec3 pos) {
        this.level().playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6f, 1.2f);
        this.level().playSound(null, pos.x, pos.y, pos.z, SoundEvents.BONE_BLOCK_BREAK, SoundSource.PLAYERS, 0.8f, 0.6f);
        this.level().addParticle(ParticleTypes.CRIMSON_SPORE, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0);
        if (!this.level().isClientSide && this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CRIMSON_SPORE, pos.x, pos.y, pos.z, 60, 0.4, 0.4, 0.4, 0.08);
            server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private void applyStatusEffects(LivingEntity target) {
        int bleedAmplifier = scaledAmplifier(1);
        int slowAmplifier = scaledAmplifier(2);
        int weakAmplifier = scaledAmplifier(1);

        int duration = 100;
        Optional<? extends Holder<MobEffect>> bleed = BuiltInRegistries.MOB_EFFECT.getHolder(BloodBoneBombAbility.BLEED_EFFECT_ID);
        bleed.ifPresent(effect -> target.addEffect(new MobEffectInstance(effect, duration, bleedAmplifier, false, true, true)));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, slowAmplifier, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, weakAmplifier, false, true, true));
    }

    private int scaledAmplifier(int baseLevel) {
        double scaled = Math.max(1.0, baseLevel * effectMultiplier);
        return Math.max(0, (int) Math.floor(scaled) - 1);
    }
}
